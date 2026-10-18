package com.driveflow.driver.reader.indrive

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.AppReader
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.ReadSource
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.core.model.TripDistanceOcr
import com.driveflow.driver.core.util.TextParsers
import com.driveflow.driver.reader.model.ReaderConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class InDriveReader(
    private val context: Context,
    private val tripDistanceOcr: TripDistanceOcr? = null
) : AppReader {
    override val app: RideApp = RideApp.INDRIVE
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var config: ReaderConfig = loadDefaultConfig()
        private set

    init {
        // Observe overrides from SettingsStore
        coroutineScope.launch {
            AppGraph.settingsStore.readerConfigOverrideFlow.collect { overrides ->
                val overrideJson = overrides[RideApp.INDRIVE]
                if (!overrideJson.isNullOrBlank()) {
                    try {
                        config = json.decodeFromString(overrideJson)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun loadDefaultConfig(): ReaderConfig {
        return try {
            val content = context.assets.open("reader_default_indrive.json").bufferedReader().use { it.readText() }
            json.decodeFromString<ReaderConfig>(content)
        } catch (_: Exception) {
            ReaderConfig(
                appCode = "indrive",
                packageName = RideApp.INDRIVE.pkg,
                idPrefix = "sinet.startup.inDriver:id/"
            )
        }
    }

    override fun isRideAppWindow(root: AccessibilityNodeInfo): Boolean {
        val pkg = root.packageName?.toString() ?: ""
        return pkg == RideApp.INDRIVE.pkg
    }

    override fun readList(root: AccessibilityNodeInfo): List<ParsedRequest> {
        val requests = mutableListOf<ParsedRequest>()
        // 1. Find the RecyclerView container
        val recyclerNode = findNodeById(root, "list_recycler")
        val containerNode = recyclerNode ?: root

        // 2. Find cards
        val cardId = config.getFullId("list_card")
        val cards = if (cardId.isNotBlank()) {
            findNodesById(containerNode, "list_card")
        } else {
            emptyList()
        }

        val cardNodes = if (cards.isNotEmpty()) {
            cards
        } else {
            // Fallback: iterate direct children of recycler
            val children = mutableListOf<AccessibilityNodeInfo>()
            for (i in 0 until containerNode.childCount) {
                containerNode.getChild(i)?.let { children.add(it) }
            }
            children
        }

        for (card in cardNodes) {
            val parsed = parseListCard(card)
            if (parsed != null) {
                requests.add(parsed)
            }
        }
        return requests
    }

    private fun parseListCard(card: AccessibilityNodeInfo): ParsedRequest? {
        val cardBounds = Rect()
        card.getBoundsInScreen(cardBounds)

        // Read fare
        val fareText = getNodeTextWithFallback(card, "list_fare")
        val (fare, currency) = TextParsers.parseFare(fareText)
        if (fare == null) {
            reportCaptureFail("list_fare")
        }

        // Pickup distance away from driver
        val pickupDistText = getNodeTextWithFallback(card, "list_pickup_distance")
        val pickupResult = TextParsers.parseDistance(pickupDistText)
        val pickupKm = pickupResult?.km
        if (pickupKm == null && pickupDistText.isNotBlank()) {
            reportCaptureFail("list_pickup_distance")
        }

        // Rider trips
        val tripsText = getNodeTextWithFallback(card, "list_rider_trips")
        val riderTrips = TextParsers.parseTrips(tripsText)

        // Rider rating
        val ratingText = getNodeTextWithFallback(card, "list_rider_rating")
        val riderRating = TextParsers.parseRating(ratingText)

        // Request age
        val ageText = getNodeTextWithFallback(card, "list_request_age")
        val requestAgeSeconds = TextParsers.parseAgeSeconds(ageText)

        // Addresses
        val fromAddress = getNodeTextWithFallback(card, "list_from_address").ifBlank { null }
        val toAddress = getNodeTextWithFallback(card, "list_to_address").ifBlank { null }

        // Must have at least fare or addresses to be a valid offer card
        if (fare == null && fromAddress == null && toAddress == null) {
            return null
        }

        val key = computeCardHash(fare, pickupKm, fromAddress, toAddress, cardBounds)
        return ParsedRequest(
            app = RideApp.INDRIVE,
            key = key,
            source = ReadSource.ACCESSIBILITY,
            fare = fare,
            currency = currency ?: "EGP",
            pickupKm = pickupKm,
            tripKm = null, // In list mode, trip distance is not on map yet
            totalKm = pickupKm,
            pickupMinutes = null,
            riderRating = riderRating,
            riderTrips = riderTrips,
            requestAgeSeconds = requestAgeSeconds,
            pickupAddress = fromAddress,
            destinationAddress = toAddress,
            extraStops = 0,
            paymentMethod = null,
            riderNote = null,
            offerCount = null,
            roadState = RoadState.UNKNOWN,
            productName = "inDrive",
            bounds = cardBounds,
            isDetailOpen = false,
            confidence = if (fare != null && pickupKm != null) 1.0f else 0.7f
        )
    }

    override fun readDetail(root: AccessibilityNodeInfo): ParsedRequest? {
        val bottomSheet = findNodeById(root, "detail_bottom_sheet")
            ?: findNodeById(root, "detail_container")
            ?: return null

        val detailBounds = Rect()
        bottomSheet.getBoundsInScreen(detailBounds)

        // Fare
        val fareText = getNodeTextWithFallback(bottomSheet, "detail_fare")
        val (fare, currency) = TextParsers.parseFare(fareText)

        // Pickup distance
        val pickupDistText = getNodeTextWithFallback(bottomSheet, "detail_pickup_distance")
        val pickupResult = TextParsers.parseDistance(pickupDistText)
        val pickupKm = pickupResult?.km

        // Rider trips & rating
        val tripsText = getNodeTextWithFallback(bottomSheet, "detail_rider_trips")
        val riderTrips = TextParsers.parseTrips(tripsText)
        val ratingText = getNodeTextWithFallback(bottomSheet, "detail_rider_rating")
        val riderRating = TextParsers.parseRating(ratingText)

        // Age
        val ageText = getNodeTextWithFallback(bottomSheet, "detail_request_age")
        val requestAgeSeconds = TextParsers.parseAgeSeconds(ageText)

        // Addresses
        val fromAddress = getNodeTextWithFallback(bottomSheet, "detail_from_address").ifBlank { null }
        val toAddress = getNodeTextWithFallback(bottomSheet, "detail_to_address").ifBlank { null }

        // Extra stops
        val extraStopsNode = findNodeById(bottomSheet, "detail_extra_stops")
        val extraStops = extraStopsNode?.childCount ?: 0

        // Rider note
        val riderNote = getNodeTextWithFallback(bottomSheet, "detail_rider_note").ifBlank { null }

        // Offer price button text (read-only, never clicked!)
        val offerBtnText = getNodeTextWithFallback(bottomSheet, "detail_offer_price_button")
        val (offeredPrice, _) = TextParsers.parseFare(offerBtnText)

        val effectiveFare = fare ?: offeredPrice
        val totalKm = pickupKm
        val key = computeCardHash(effectiveFare, pickupKm, fromAddress, toAddress, detailBounds)

        return ParsedRequest(
            app = RideApp.INDRIVE,
            key = key,
            source = ReadSource.ACCESSIBILITY,
            fare = effectiveFare,
            currency = currency ?: "EGP",
            pickupKm = pickupKm,
            tripKm = null,
            totalKm = totalKm,
            pickupMinutes = null,
            riderRating = riderRating,
            riderTrips = riderTrips,
            requestAgeSeconds = requestAgeSeconds,
            pickupAddress = fromAddress,
            destinationAddress = toAddress,
            extraStops = extraStops,
            paymentMethod = null,
            riderNote = riderNote,
            offerCount = null,
            roadState = RoadState.UNKNOWN,
            productName = "inDrive Detail",
            bounds = detailBounds,
            isDetailOpen = true,
            confidence = 0.5f
        )
    }

    override fun mapBounds(root: AccessibilityNodeInfo): Rect? {
        val mapNode = findNodeById(root, "detail_map")
            ?: findNodeById(root, "detail_map_interceptor")
        val bottomSheetNode = findNodeById(root, "detail_bottom_sheet")

        if (mapNode != null) {
            val mapRect = Rect()
            mapNode.getBoundsInScreen(mapRect)
            if (bottomSheetNode != null) {
                val sheetRect = Rect()
                bottomSheetNode.getBoundsInScreen(sheetRect)
                // Intersect map with the area above the bottom sheet top
                if (mapRect.bottom > sheetRect.top && sheetRect.top > mapRect.top) {
                    mapRect.bottom = sheetRect.top
                }
            }
            return mapRect
        }
        return null
    }

    fun isCloseDetailNode(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        return text.contains("إغلاق") || text.contains("رجوع") || text.equals("Close", ignoreCase = true)
    }

    private fun findNodeById(root: AccessibilityNodeInfo, key: String): AccessibilityNodeInfo? {
        val fullId = config.getFullId(key)
        if (fullId.isNotBlank()) {
            val nodes = root.findAccessibilityNodeInfosByViewId(fullId)
            if (!nodes.isNullOrEmpty()) return nodes[0]
        }
        val altIds = config.getAlternativeFullIds(key)
        for (altId in altIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(altId)
            if (!nodes.isNullOrEmpty()) return nodes[0]
        }
        return null
    }

    private fun findNodesById(root: AccessibilityNodeInfo, key: String): List<AccessibilityNodeInfo> {
        val fullId = config.getFullId(key)
        if (fullId.isNotBlank()) {
            val nodes = root.findAccessibilityNodeInfosByViewId(fullId)
            if (!nodes.isNullOrEmpty()) return nodes
        }
        val altIds = config.getAlternativeFullIds(key)
        for (altId in altIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(altId)
            if (!nodes.isNullOrEmpty()) return nodes
        }
        return emptyList()
    }

    private fun getNodeTextWithFallback(parent: AccessibilityNodeInfo, key: String): String {
        val node = findNodeById(parent, key)
        val text = node?.text?.toString()?.trim()
        if (!text.isNullOrBlank()) return text

        val patternStr = config.fallbacks[key]?.textPattern
        if (!patternStr.isNullOrBlank()) {
            val regex = Regex(patternStr)
            val matched = scanSubtreeForPattern(parent, regex)
            if (matched != null) return matched
        }
        return ""
    }

    private fun scanSubtreeForPattern(root: AccessibilityNodeInfo, regex: Regex): String? {
        val nodeText = root.text?.toString()?.trim()
        if (nodeText != null && regex.containsMatchIn(nodeText)) {
            return nodeText
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = scanSubtreeForPattern(child, regex)
            if (found != null) return found
        }
        return null
    }

    private fun reportCaptureFail(fieldKey: String) {
        coroutineScope.launch {
            AppGraph.settingsStore.incrementDiagnosticCounter("capture_fail:$fieldKey")
        }
    }

    private fun computeCardHash(
        fare: Double?,
        pickupKm: Double?,
        fromAddress: String?,
        toAddress: String?,
        bounds: Rect
    ): String {
        val raw = "$fare|$pickupKm|$fromAddress|$toAddress|${bounds.top}"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
