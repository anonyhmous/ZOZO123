package com.driveflow.driver.engine

import com.driveflow.driver.core.model.DistanceUnit
import com.driveflow.driver.core.model.LongTripMode
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.core.model.StopsMode
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import kotlin.math.ceil

object RulesEngine {
    /**
     * Evaluates a ride request against the driver's policy.
     * Returns null if the app is disabled in the policy.
     */
    fun evaluate(request: ParsedRequest, policy: Policy): Verdict? {
        // Rule 1: App enabled check
        if (policy.appEnabled[request.app] == false) {
            return null
        }

        // Common Early Checks (1 - 15)
        val earlyVerdict = evaluateEarlyChecks(request, policy)
        if (earlyVerdict != null) {
            return earlyVerdict
        }

        // Helper calculations
        val effectivePickupKm = if (policy.freePickupKm != null && request.pickupKm != null && request.pickupKm < policy.freePickupKm) {
            0.0
        } else {
            request.pickupKm ?: 0.0
        }

        val calculatedTotalKm: Double? = request.totalKm ?: if (request.pickupKm != null && request.tripKm != null) {
            effectivePickupKm + request.tripKm
        } else {
            null
        }

        val commissionFactor = if (policy.commissionEnabled[request.app] == true) {
            1.0 - ((policy.commissionPct[request.app] ?: 0.0) / 100.0)
        } else {
            1.0
        }

        val carCostPerKm = policy.carCostPerKm
        val requiredFare = policy.minPricePerKm * (calculatedTotalKm ?: 0.0) + (policy.stopFee ?: 0.0) * request.extraStops

        val grossPerKm = if (request.fare != null && calculatedTotalKm != null && calculatedTotalKm > 0.0) {
            request.fare / calculatedTotalKm
        } else {
            null
        }

        val netFare = if (request.fare != null) request.fare * commissionFactor else null
        val netProfit = if (netFare != null && calculatedTotalKm != null) {
            netFare - (carCostPerKm * calculatedTotalKm)
        } else {
            null
        }

        val netPerKm = if (netProfit != null && calculatedTotalKm != null && calculatedTotalKm > 0.0) {
            netProfit / calculatedTotalKm
        } else {
            null
        }

        val valueToCompare = if (policy.basisNet) netPerKm else grossPerKm

        // LIST Evaluation (isDetailOpen == false)
        if (!request.isDetailOpen) {
            // Check 16: Preferred area match
            val isPreferredDest = ArabicTextNormalizer.matchesAnyArea(request.destinationAddress, policy.preferredAreas) != null
            if (isPreferredDest) {
                return Verdict(
                    tone = Tone.GRAY,
                    reasonCode = "preferred_area_match",
                    titleAr = "منطقة مفضلة لديك",
                    titleEn = "Preferred Area",
                    detailAr = "المشوار متجه لمنطقة مفضلة؛ افتح الطلب لمعاينة المسار",
                    detailEn = "Destination is in your preferred area; open request for full details",
                    flags = buildFlags(request, calculatedTotalKm, policy.distanceUnit, preferred = true),
                    totalKm = calculatedTotalKm,
                    grossPerKm = grossPerKm,
                    netPerKm = netPerKm,
                    netProfit = netProfit,
                    suggestedPrice = null
                )
            }

            // Check 17: Otherwise GREEN with list_ok
            return Verdict(
                tone = Tone.GREEN,
                reasonCode = "list_ok",
                titleAr = "طلب واعد ومناسب",
                titleEn = "Promising Request",
                detailAr = "الطلب واعد ومبدئياً مناسب؛ افتح لمعاينة المسار الكامل",
                detailEn = "Request looks promising; open to inspect full route details",
                flags = buildFlags(request, calculatedTotalKm, policy.distanceUnit),
                totalKm = calculatedTotalKm,
                grossPerKm = grossPerKm,
                netPerKm = netPerKm,
                netProfit = netProfit,
                suggestedPrice = null
            )
        }

        // DETAIL Evaluation (isDetailOpen == true)
        // D8: Check if total distance is missing
        if (calculatedTotalKm == null) {
            val flags = buildFlags(request, null, policy.distanceUnit).toMutableList()
            if (request.app == RideApp.INDRIVE && request.tripKm == null) {
                flags.add("trip_unknown")
                // Apply distance floor / cap using pickup distance if pickup alone violates
                if (request.pickupKm != null) {
                    if (policy.maxPickupKm != null && request.pickupKm > policy.maxPickupKm) {
                        return Verdict(
                            tone = Tone.RED,
                            reasonCode = "pickup_over_policy",
                            titleAr = "مسافة الالتقاط بعيدة",
                            titleEn = "Pickup Too Far",
                            detailAr = "مسافة الوصول للزبون تتجاوز حدك الأقصى",
                            detailEn = "Pickup distance exceeds your policy maximum",
                            flags = flags,
                            totalKm = null,
                            grossPerKm = null,
                            netPerKm = null,
                            netProfit = null,
                            suggestedPrice = null
                        )
                    }
                }
            }
            return Verdict(
                tone = Tone.GRAY,
                reasonCode = "detail_incomplete",
                titleAr = "تفاصيل غير مكتملة",
                titleEn = "Incomplete Details",
                detailAr = "انتظر لحظة ليتم قراءة مسافة المشوار والأرقام من الخريطة",
                detailEn = "Keep the request on screen while we read map distance and figures",
                flags = flags,
                totalKm = null,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // D1: Total distance floor
        if (policy.minTotalKm != null && calculatedTotalKm < policy.minTotalKm) {
            return Verdict(
                tone = Tone.RED,
                reasonCode = "skip_total_distance_floor",
                titleAr = "المشوار قصير جداً",
                titleEn = "Trip Too Short",
                detailAr = "المسافة الكلية (${formatDistance(calculatedTotalKm, policy.distanceUnit)}) أقل من الحد الأدنى المقبول",
                detailEn = "Total distance (${formatDistance(calculatedTotalKm, policy.distanceUnit)}) is below your minimum threshold",
                flags = buildFlags(request, calculatedTotalKm, policy.distanceUnit),
                totalKm = calculatedTotalKm,
                grossPerKm = grossPerKm,
                netPerKm = netPerKm,
                netProfit = netProfit,
                suggestedPrice = null
            )
        }

        // D2: Total distance cap
        if (policy.maxTotalKm != null && calculatedTotalKm > policy.maxTotalKm) {
            return Verdict(
                tone = Tone.RED,
                reasonCode = "skip_total_distance_cap",
                titleAr = "المشوار طويل جداً",
                titleEn = "Trip Too Long",
                detailAr = "المسافة الكلية (${formatDistance(calculatedTotalKm, policy.distanceUnit)}) تتجاوز الحد الأقصى لمسافاتك",
                detailEn = "Total distance (${formatDistance(calculatedTotalKm, policy.distanceUnit)}) exceeds your distance cap",
                flags = buildFlags(request, calculatedTotalKm, policy.distanceUnit),
                totalKm = calculatedTotalKm,
                grossPerKm = grossPerKm,
                netPerKm = netPerKm,
                netProfit = netProfit,
                suggestedPrice = null
            )
        }

        // D3: Rider trip distance cap
        if (policy.maxTripKm != null && request.tripKm != null && request.tripKm > policy.maxTripKm) {
            return Verdict(
                tone = Tone.RED,
                reasonCode = "skip_trip_distance_cap",
                titleAr = "مسافة الرحلة طويلة",
                titleEn = "Trip Distance Capped",
                detailAr = "مسافة توصيل الراكب (${formatDistance(request.tripKm, policy.distanceUnit)}) تتجاوز حدك الأقصى",
                detailEn = "Rider trip distance (${formatDistance(request.tripKm, policy.distanceUnit)}) exceeds your trip cap",
                flags = buildFlags(request, calculatedTotalKm, policy.distanceUnit),
                totalKm = calculatedTotalKm,
                grossPerKm = grossPerKm,
                netPerKm = netPerKm,
                netProfit = netProfit,
                suggestedPrice = null
            )
        }

        // D4: Long trip mode check
        if (policy.longTripKm != null && calculatedTotalKm > policy.longTripKm) {
            if (policy.longTripMode == LongTripMode.AVOID) {
                return Verdict(
                    tone = Tone.RED,
                    reasonCode = "long_trip",
                    titleAr = "رحلة سفر مستبعدة",
                    titleEn = "Long Trip Avoided",
                    detailAr = "المسافة (${formatDistance(calculatedTotalKm, policy.distanceUnit)}) سفر وأنت تستبعد الرحلات الطويلة",
                    detailEn = "Trip (${formatDistance(calculatedTotalKm, policy.distanceUnit)}) exceeds long trip limit which you avoid",
                    flags = buildFlags(request, calculatedTotalKm, policy.distanceUnit),
                    totalKm = calculatedTotalKm,
                    grossPerKm = grossPerKm,
                    netPerKm = netPerKm,
                    netProfit = netProfit,
                    suggestedPrice = null
                )
            } else if (policy.longTripMode == LongTripMode.WARN) {
                return Verdict(
                    tone = Tone.ORANGE,
                    reasonCode = "long_trip",
                    titleAr = "انتبه: مشوار سفر طويل",
                    titleEn = "Long Trip Warning",
                    detailAr = "مشوار سفر (${formatDistance(calculatedTotalKm, policy.distanceUnit)})، تأكد من ملائمة طريق العودة",
                    detailEn = "Trip is a long distance (${formatDistance(calculatedTotalKm, policy.distanceUnit)}); check return feasibility",
                    flags = buildFlags(request, calculatedTotalKm, policy.distanceUnit),
                    totalKm = calculatedTotalKm,
                    grossPerKm = grossPerKm,
                    netPerKm = netPerKm,
                    netProfit = netProfit,
                    suggestedPrice = null
                )
            }
        }

        // D5: Price check (including extra stops fee if configured)
        val isPriceFine = if (calculatedTotalKm > 0.0) {
            if (policy.basisNet) {
                val requiredNetProfit = (policy.minPricePerKm * calculatedTotalKm) + ((policy.stopFee ?: 0.0) * request.extraStops)
                netProfit != null && netProfit >= requiredNetProfit
            } else {
                request.fare != null && request.fare >= requiredFare
            }
        } else {
            valueToCompare != null && valueToCompare >= policy.minPricePerKm
        }

        if (isPriceFine) {
            // D6: Price is fine! Check road conditions
            val flags = buildFlags(request, calculatedTotalKm, policy.distanceUnit).toMutableList()
            if (policy.stopsMode == StopsMode.PREFER && request.extraStops > 0) {
                flags.add("extra_stops_preferred")
            }
            if (request.roadState == RoadState.HARD || request.roadState == RoadState.SLOW) {
                return Verdict(
                    tone = Tone.ORANGE,
                    reasonCode = "review_slow_route",
                    titleAr = "السعر جيد لكن الطريق زحام",
                    titleEn = "Slow Route Warning",
                    detailAr = "السعر يطابق شروطك (${formatRate(valueToCompare)}/كم) لكن الطريق به تكدس وبطء شديد",
                    detailEn = "Rate meets your policy (${formatRate(valueToCompare)}/km) but traffic along route is congested",
                    flags = flags,
                    totalKm = calculatedTotalKm,
                    grossPerKm = grossPerKm,
                    netPerKm = netPerKm,
                    netProfit = netProfit,
                    suggestedPrice = null
                )
            }

            return Verdict(
                tone = Tone.GREEN,
                reasonCode = "accept_if_road_ok",
                titleAr = "مشوار ممتاز ومربح",
                titleEn = "Great Profitable Ride",
                detailAr = "السعر يطابق قواعدك بمعدل ${formatRate(valueToCompare)}/كم وصافي ربح ${formatProfit(netProfit)}",
                detailEn = "Trip fully satisfies your policy with ${formatRate(valueToCompare)}/km and net profit ${formatProfit(netProfit)}",
                flags = flags,
                totalKm = calculatedTotalKm,
                grossPerKm = grossPerKm,
                netPerKm = netPerKm,
                netProfit = netProfit,
                suggestedPrice = null
            )
        } else {
            // D7: Price is below policy
            val flags = buildFlags(request, calculatedTotalKm, policy.distanceUnit)
            if (request.app == RideApp.INDRIVE) {
                val step = policy.negotiationStep.coerceAtLeast(1)
                val suggestedPrice = ceil(requiredFare / step) * step
                if (policy.maxOffer != null && suggestedPrice > policy.maxOffer) {
                    return Verdict(
                        tone = Tone.RED,
                        reasonCode = "skip_app_counter_cap",
                        titleAr = "تجاوز حد المزايدة",
                        titleEn = "Over Counter Cap",
                        detailAr = "السعر العادل (${formatAmount(suggestedPrice)}) يتجاوز أقصى عرض حددته (${formatAmount(policy.maxOffer)})",
                        detailEn = "Fair price (${formatAmount(suggestedPrice)}) exceeds your maximum offer cap (${formatAmount(policy.maxOffer)})",
                        flags = flags,
                        totalKm = calculatedTotalKm,
                        grossPerKm = grossPerKm,
                        netPerKm = netPerKm,
                        netProfit = netProfit,
                        suggestedPrice = suggestedPrice
                    )
                } else {
                    return Verdict(
                        tone = Tone.ORANGE,
                        reasonCode = "counter_or_skip",
                        titleAr = "فاوض أو تخطى",
                        titleEn = "Counter or Skip",
                        detailAr = "السعر منخفض؛ ارفع السعر إلى ${formatAmount(suggestedPrice)} أو تخطى الطلب",
                        detailEn = "Current rate is low; counter with ${formatAmount(suggestedPrice)} or skip",
                        flags = flags,
                        totalKm = calculatedTotalKm,
                        grossPerKm = grossPerKm,
                        netPerKm = netPerKm,
                        netProfit = netProfit,
                        suggestedPrice = suggestedPrice
                    )
                }
            } else {
                // Fixed fare (UBER or DIDI)
                val shortfall = (requiredFare - (request.fare ?: 0.0)).coerceAtLeast(0.0)
                return Verdict(
                    tone = Tone.RED,
                    reasonCode = "skip_price_below_rate",
                    titleAr = "السعر أقل من المطلوب",
                    titleEn = "Below Required Rate",
                    detailAr = "سعر المشوار (${formatRate(valueToCompare)}/كم) يقل عن سعرك المطلوب بفارق ${formatAmount(shortfall)} جنيه",
                    detailEn = "Trip rate (${formatRate(valueToCompare)}/km) is below your minimum by ${formatAmount(shortfall)}",
                    flags = flags,
                    totalKm = calculatedTotalKm,
                    grossPerKm = grossPerKm,
                    netPerKm = netPerKm,
                    netProfit = netProfit,
                    suggestedPrice = null
                )
            }
        }
    }

    /**
     * Evaluates initial checks (1 - 15) shared by both LIST and DETAIL modes.
     * If any check matches, it returns a Verdict immediately.
     */
    private fun evaluateEarlyChecks(request: ParsedRequest, policy: Policy): Verdict? {
        // 2: policy_missing
        if (policy.minPricePerKm <= 0.0) {
            return Verdict(
                tone = Tone.GRAY,
                reasonCode = "policy_missing",
                titleAr = "لم يتم تحديد السعر",
                titleEn = "Policy Missing",
                detailAr = "يرجى تحديد الحد الأدنى لسعر الكيلومتر في شاشة قواعدي",
                detailEn = "Please configure your minimum price per km in My Rules",
                flags = emptyList(),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 3: currency_mismatch
        if (request.currency != null && !request.currency.equals(policy.currency, ignoreCase = true)) {
            return Verdict(
                tone = Tone.GRAY,
                reasonCode = "currency_mismatch",
                titleAr = "العملة غير متطابقة",
                titleEn = "Currency Mismatch",
                detailAr = "عملة المشوار (${request.currency}) تختلف عن عملتك (${policy.currency})",
                detailEn = "Request currency (${request.currency}) differs from your policy currency (${policy.currency})",
                flags = emptyList(),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 4: list_fields_missing
        if (request.fare == null || request.pickupKm == null) {
            return Verdict(
                tone = Tone.GRAY,
                reasonCode = "list_fields_missing",
                titleAr = "بيانات المشوار غير مكتملة",
                titleEn = "Missing Basic Fields",
                detailAr = "تعذر قراءة السعر أو مسافة الالتقاط للزبون",
                detailEn = "Could not read fare or pickup distance from screen",
                flags = emptyList(),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 5: avoided_area
        val avoidedMatch = ArabicTextNormalizer.matchesAnyArea(request.pickupAddress, policy.avoidedAreas)
            ?: ArabicTextNormalizer.matchesAnyArea(request.destinationAddress, policy.avoidedAreas)
            ?: ArabicTextNormalizer.matchesAnyArea(request.riderNote, policy.avoidedAreas)

        if (avoidedMatch != null) {
            return Verdict(
                tone = Tone.RED,
                reasonCode = "avoided_area",
                titleAr = "منطقة مستبعدة لديك",
                titleEn = "Avoided Area",
                detailAr = "المشوار يمر بمنطقة مستبعدة بشروطك: $avoidedMatch",
                detailEn = "Trip passes through an avoided area: $avoidedMatch",
                flags = listOf("avoided_area:$avoidedMatch"),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 6: payment_refused
        if (policy.allowedPayments.isNotEmpty() && request.paymentMethod != null && !policy.allowedPayments.contains(request.paymentMethod)) {
            return Verdict(
                tone = Tone.RED,
                reasonCode = "payment_refused",
                titleAr = "طريقة دفع غير مقبولة",
                titleEn = "Payment Not Allowed",
                detailAr = "طريقة الدفع (${request.paymentMethod}) مستبعدة في شروطك",
                detailEn = "Payment method (${request.paymentMethod}) is not permitted by your policy",
                flags = listOf("payment:${request.paymentMethod}"),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 7: note_word
        if (!request.riderNote.isNullOrBlank() && policy.bannedNoteWords.isNotEmpty()) {
            val matchedBannedWord = ArabicTextNormalizer.matchesAnyArea(request.riderNote, policy.bannedNoteWords)
            if (matchedBannedWord != null) {
                return Verdict(
                    tone = Tone.RED,
                    reasonCode = "note_word",
                    titleAr = "ملاحظة محظورة من الراكب",
                    titleEn = "Banned Note Word",
                    detailAr = "الملاحظة تحتوي على كلمة مستبعدة لديك: $matchedBannedWord",
                    detailEn = "Rider note contains a banned word: $matchedBannedWord",
                    flags = listOf("banned_note:$matchedBannedWord"),
                    totalKm = request.totalKm,
                    grossPerKm = null,
                    netPerKm = null,
                    netProfit = null,
                    suggestedPrice = null
                )
            }
        }

        // 8: extra_stops with AVOID
        if (request.extraStops > 0 && policy.stopsMode == StopsMode.AVOID) {
            return Verdict(
                tone = Tone.RED,
                reasonCode = "extra_stops",
                titleAr = "وقفات إضافية مستبعدة",
                titleEn = "Extra Stops Avoided",
                detailAr = "الطلب يحتوي وقفات إضافية وأنت تستبعدها بشروطك",
                detailEn = "Trip contains extra stops which are disabled in your policy",
                flags = listOf("extra_stops:${request.extraStops}"),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 9: pickup_over_policy
        val isPickupKmOver = policy.maxPickupKm != null && request.pickupKm != null && request.pickupKm > policy.maxPickupKm
        val isPickupMinOver = policy.maxPickupMinutes != null && request.pickupMinutes != null && request.pickupMinutes > policy.maxPickupMinutes
        if (isPickupKmOver || isPickupMinOver) {
            val pickupDetailAr = if (isPickupKmOver) {
                "مسافة الالتقاط (${formatDistance(request.pickupKm, policy.distanceUnit)}) تتجاوز حدك الأقصى (${formatDistance(policy.maxPickupKm, policy.distanceUnit)})"
            } else {
                "وقت الالتقاط (${request.pickupMinutes} د) يتجاوز حدك الأقصى (${policy.maxPickupMinutes} د)"
            }
            return Verdict(
                tone = Tone.RED,
                reasonCode = "pickup_over_policy",
                titleAr = "الزبون بعيد جداً",
                titleEn = "Pickup Too Far",
                detailAr = pickupDetailAr,
                detailEn = "Pickup distance or time exceeds your policy limits",
                flags = listOf("pickup_too_far"),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 10: rider_trips_below_policy
        if (policy.minRiderTrips != null && policy.minRiderTrips > 0 && request.riderTrips != null && request.riderTrips < policy.minRiderTrips) {
            return Verdict(
                tone = Tone.ORANGE,
                reasonCode = "rider_trips_below_policy",
                titleAr = "رحلات الراكب قليلة",
                titleEn = "Low Rider Trips",
                detailAr = "عدد رحلات الراكب (${request.riderTrips}) أقل من شرطك الأدنى (${policy.minRiderTrips})",
                detailEn = "Rider completed trips (${request.riderTrips}) is below your minimum threshold (${policy.minRiderTrips})",
                flags = listOf("trips:${request.riderTrips}"),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 11: rider_rating_below_policy
        if (policy.minRiderRating != null && request.riderRating != null && request.riderRating < policy.minRiderRating) {
            return Verdict(
                tone = Tone.ORANGE,
                reasonCode = "rider_rating_below_policy",
                titleAr = "تقييم الراكب منخفض",
                titleEn = "Low Rider Rating",
                detailAr = "تقييم الراكب (${request.riderRating}) أقل من شرطك الأدنى (${policy.minRiderRating})",
                detailEn = "Rider rating (${request.riderRating}) is below your required rating (${policy.minRiderRating})",
                flags = listOf("rating:${request.riderRating}"),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 12: rider_fields_unreadable
        val isRatingMissing = policy.minRiderRating != null && request.riderRating == null
        val isTripsMissing = policy.minRiderTrips != null && policy.minRiderTrips > 0 && request.riderTrips == null
        val isAgeMissing = policy.maxRequestAgeMinutes != null && request.requestAgeSeconds == null
        if (isRatingMissing || isTripsMissing || isAgeMissing) {
            return Verdict(
                tone = Tone.ORANGE,
                reasonCode = "rider_fields_unreadable",
                titleAr = "بيانات الراكب غير واضحة",
                titleEn = "Rider Info Unreadable",
                detailAr = "تعذر قراءة تقييم الراكب أو عدد رحلاته أو عمر الطلب بدقة",
                detailEn = "Required rider rating, trips count, or request age could not be read",
                flags = listOf("fields_unreadable"),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 13: offer_older_than_policy
        if (policy.maxRequestAgeMinutes != null && request.requestAgeSeconds != null && (request.requestAgeSeconds / 60) > policy.maxRequestAgeMinutes) {
            val ageMin = request.requestAgeSeconds / 60
            return Verdict(
                tone = Tone.ORANGE,
                reasonCode = "offer_older_than_policy",
                titleAr = "الطلب قديم في القائمة",
                titleEn = "Stale Request",
                detailAr = "الطلب معروض منذ $ageMin دقيقة، ويتجاوز حدك الأقصى (${policy.maxRequestAgeMinutes} د)",
                detailEn = "Request was posted $ageMin min ago, exceeding your limit (${policy.maxRequestAgeMinutes} min)",
                flags = listOf("age_min:$ageMin"),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 14: extra_stops with WARN
        if (request.extraStops > 0 && policy.stopsMode == StopsMode.WARN) {
            return Verdict(
                tone = Tone.ORANGE,
                reasonCode = "extra_stops",
                titleAr = "تنبيه: وقفات إضافية",
                titleEn = "Extra Stops Warning",
                detailAr = "الطلب يحتوي على ${request.extraStops} وقفة إضافية؛ راجع السعر جيداً",
                detailEn = "Trip contains ${request.extraStops} extra stop(s); review price accordingly",
                flags = listOf("extra_stops:${request.extraStops}"),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        // 15: uber_saver
        if (request.app == RideApp.UBER && policy.warnOnUberSaver && request.productName?.contains("saver", ignoreCase = true) == true) {
            return Verdict(
                tone = Tone.ORANGE,
                reasonCode = "uber_saver",
                titleAr = "تنبيه: عرض أوبر سيفر المخفض",
                titleEn = "Uber Saver Warning",
                detailAr = "هذا الطلب فئة أوبر سيفر بسعر مخفض، تأكد من ملائمة العائد لجهدك",
                detailEn = "Uber Saver discounted trip detected; review rate carefully",
                flags = listOf("uber_saver"),
                totalKm = request.totalKm,
                grossPerKm = null,
                netPerKm = null,
                netProfit = null,
                suggestedPrice = null
            )
        }

        return null
    }

    private fun buildFlags(
        request: ParsedRequest,
        totalKm: Double?,
        unit: DistanceUnit,
        preferred: Boolean = false
    ): List<String> {
        val flags = mutableListOf<String>()
        if (request.pickupKm != null) {
            flags.add("pickup:${formatDistance(request.pickupKm, unit)}")
        }
        if (totalKm != null) {
            flags.add("total:${formatDistance(totalKm, unit)}")
        }
        if (request.riderRating != null) {
            flags.add("rating:★${request.riderRating}")
        }
        if (request.riderTrips != null) {
            flags.add("trips:${request.riderTrips}")
        }
        if (request.roadState == RoadState.SLOW) {
            flags.add("slow_road")
        } else if (request.roadState == RoadState.HARD) {
            flags.add("hard_road")
        }
        if (preferred) {
            flags.add("preferred_area")
        }
        return flags
    }

    private fun formatDistance(km: Double?, unit: DistanceUnit): String {
        if (km == null) return ""
        val miles = km / 1.609344
        return when (unit) {
            DistanceUnit.KM -> "%.1f كم".format(km)
            DistanceUnit.MILES -> "%.1f mi".format(miles)
            DistanceUnit.BOTH -> "%.1f كم (%.1f mi)".format(km, miles)
        }
    }

    private fun formatRate(rate: Double?): String {
        if (rate == null) return "0"
        return "%.1f".format(rate)
    }

    private fun formatProfit(profit: Double?): String {
        if (profit == null) return "0 ج.م"
        return "%.0f ج.م".format(profit)
    }

    private fun formatAmount(amount: Double): String {
        return "%.0f".format(amount)
    }
}
