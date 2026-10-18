package com.driveflow.driver.engine

import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe reactive store for verified ride verdicts.
 * Features:
 * - 400 ms stability debounce when tone changes to prevent marker flicker.
 * - Direct pass-through if tone is identical to prevent lag.
 * - Tracks 'blinks' (tone flips) for diagnostic reporting.
 * - Local counters hooks for verdict reasons and dismissals.
 */
class DecisionStore(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val debounceDelayMs: Long = 400L,
    private val diagnosticCounterIncrementer: (suspend (String) -> Unit)? = null
) {
    private val _decisions = MutableStateFlow<Map<String, Verdict>>(emptyMap())
    val decisions: StateFlow<Map<String, Verdict>> = _decisions.asStateFlow()

    private val pendingJobs = mutableMapOf<String, Job>()
    private val pendingVerdicts = mutableMapOf<String, Verdict>()
    private val lastToneMap = mutableMapOf<String, Tone>()
    private val mutex = Mutex()

    var blinkCount = 0
        private set

    suspend fun postDecision(key: String, verdict: Verdict) {
        mutex.withLock {
            pendingVerdicts[key] = verdict
            pendingJobs[key]?.cancel()

            val current = _decisions.value[key]
            // If card already exists and tone is unchanged, update directly without debounce
            if (current != null && current.tone == verdict.tone) {
                commitVerdict(key, verdict)
                return
            }

            // If debounceDelayMs <= 0 (e.g. in immediate tests), commit immediately
            if (debounceDelayMs <= 0L) {
                commitVerdict(key, verdict)
                return
            }

            // Debounce tone changes
            pendingJobs[key] = scope.launch {
                delay(debounceDelayMs)
                mutex.withLock {
                    val pending = pendingVerdicts.remove(key)
                    if (pending != null) {
                        commitVerdict(key, pending)
                    }
                }
            }
        }
    }

    private suspend fun commitVerdict(key: String, verdict: Verdict) {
        val previousTone = lastToneMap[key]
        if (previousTone != null && previousTone != verdict.tone) {
            blinkCount++
            diagnosticCounterIncrementer?.invoke("blinks")
        }
        lastToneMap[key] = verdict.tone
        val updated = _decisions.value.toMutableMap()
        updated[key] = verdict
        _decisions.value = updated
        diagnosticCounterIncrementer?.invoke("verdict:${verdict.reasonCode}")
    }

    suspend fun remove(key: String) {
        mutex.withLock {
            pendingJobs.remove(key)?.cancel()
            pendingVerdicts.remove(key)
            lastToneMap.remove(key)
            if (_decisions.value.containsKey(key)) {
                val updated = _decisions.value.toMutableMap()
                updated.remove(key)
                _decisions.value = updated
                diagnosticCounterIncrementer?.invoke("left_after")
            }
        }
    }

    suspend fun retainAll(keys: Set<String>) {
        mutex.withLock {
            val toRemove = _decisions.value.keys - keys
            for (k in toRemove) {
                pendingJobs.remove(k)?.cancel()
                pendingVerdicts.remove(k)
                lastToneMap.remove(k)
                diagnosticCounterIncrementer?.invoke("left_after")
            }
            _decisions.value = _decisions.value.filterKeys { keys.contains(it) }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            pendingJobs.values.forEach { it.cancel() }
            pendingJobs.clear()
            pendingVerdicts.clear()
            lastToneMap.clear()
            _decisions.value = emptyMap()
        }
    }
}
