package com.driveflow.driver.engine

import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class OpportunityEvent(
    val request: ParsedRequest,
    val verdict: Verdict,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Detects golden / profitable trip opportunities and triggers alerts.
 * Enforces a 60-second cooldown per trip key to prevent annoying repeat chimes.
 */
class OpportunityDetector(
    private val alertCooldownMs: Long = 60_000L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val _opportunityEvents = MutableSharedFlow<OpportunityEvent>(extraBufferCapacity = 32)
    val opportunityEvents: SharedFlow<OpportunityEvent> = _opportunityEvents.asSharedFlow()

    private val lastAlertTimeMap = mutableMapOf<String, Long>()

    suspend fun inspect(request: ParsedRequest, verdict: Verdict): Boolean {
        if (verdict.tone != Tone.GREEN) return false

        // Eligible if accept_if_road_ok, or list_ok for non-detail requests
        val isEligibleReason = verdict.reasonCode == "accept_if_road_ok" ||
                (verdict.reasonCode == "list_ok" && !request.isDetailOpen)
        if (!isEligibleReason) return false

        val now = timeProvider()
        val lastAlertTime = lastAlertTimeMap[request.key]
        if (lastAlertTime != null && (now - lastAlertTime) < alertCooldownMs) {
            return false // Still in cooldown
        }

        lastAlertTimeMap[request.key] = now
        val event = OpportunityEvent(request, verdict, now)
        _opportunityEvents.emit(event)
        return true
    }

    fun clear() {
        lastAlertTimeMap.clear()
    }
}
