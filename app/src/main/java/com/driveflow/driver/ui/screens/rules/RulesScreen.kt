package com.driveflow.driver.ui.screens.rules

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.core.model.DistanceUnit
import com.driveflow.driver.core.model.LongTripMode
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.StopsMode
import com.driveflow.driver.core.model.VehicleType
import com.driveflow.driver.core.util.NumberUtils
import com.driveflow.driver.ui.theme.DriveFlowTheme
import com.driveflow.driver.ui.theme.EmeraldGreen
import com.driveflow.driver.ui.theme.GraphiteSurface
import com.driveflow.driver.ui.theme.GraphiteSurfaceVariant
import com.driveflow.driver.ui.theme.ObsidianBackground
import com.driveflow.driver.ui.theme.RacingOrange
import com.driveflow.driver.ui.theme.SlateBorder
import com.driveflow.driver.ui.theme.TextBrightWhite
import com.driveflow.driver.ui.theme.TextSilverGray
import com.driveflow.driver.ui.theme.VerdictRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesScreen(
    currentPolicy: Policy,
    onSavePolicy: (Policy) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Vehicle & Motorcycle engine selection
    var vehicleType by remember { mutableStateOf(currentPolicy.vehicleType) }

    // Form states
    var minPricePerKmText by remember { mutableStateOf(if (currentPolicy.minPricePerKm > 0.0) currentPolicy.minPricePerKm.toString() else "") }
    var currency by remember { mutableStateOf(currentPolicy.currency) }
    var distanceUnit by remember { mutableStateOf(currentPolicy.distanceUnit) }
    var governorate by remember { mutableStateOf(currentPolicy.governorate ?: "القاهرة") }
    var minTotalKmText by remember { mutableStateOf(currentPolicy.minTotalKm?.toString() ?: "") }
    var maxTotalKmText by remember { mutableStateOf(currentPolicy.maxTotalKm?.toString() ?: "") }
    var maxTripKmText by remember { mutableStateOf(currentPolicy.maxTripKm?.toString() ?: "") }
    var maxPickupKmText by remember { mutableStateOf(currentPolicy.maxPickupKm?.toString() ?: "") }
    var maxPickupMinutesText by remember { mutableStateOf(currentPolicy.maxPickupMinutes?.toString() ?: "") }
    var freePickupKmText by remember { mutableStateOf(currentPolicy.freePickupKm?.toString() ?: "") }
    var minRiderRatingText by remember { mutableStateOf(currentPolicy.minRiderRating?.toString() ?: "") }
    var minRiderTripsText by remember { mutableStateOf(currentPolicy.minRiderTrips?.toString() ?: "") }
    var maxRequestAgeMinutesText by remember { mutableStateOf(currentPolicy.maxRequestAgeMinutes?.toString() ?: "") }
    var cashOnly by remember { mutableStateOf(currentPolicy.allowedPayments.contains("CASH_ONLY")) }
    var bannedWordInput by remember { mutableStateOf("") }
    var bannedWordsList by remember { mutableStateOf(currentPolicy.bannedNoteWords) }
    var stopsMode by remember { mutableStateOf(currentPolicy.stopsMode) }
    var stopFeeText by remember { mutableStateOf(currentPolicy.stopFee?.toString() ?: "") }
    var longTripMode by remember { mutableStateOf(currentPolicy.longTripMode) }
    var longTripKmText by remember { mutableStateOf(currentPolicy.longTripKm?.toString() ?: "") }
    var warnOnUberSaver by remember { mutableStateOf(currentPolicy.warnOnUberSaver) }

    var fuelCostText by remember {
        mutableStateOf(currentPolicy.fuelCostPerKm?.toString() ?: currentPolicy.vehicleType.defaultFuelCostPerKm.toString())
    }
    var upkeepCostText by remember {
        mutableStateOf(currentPolicy.upkeepCostPerKm?.toString() ?: currentPolicy.vehicleType.defaultUpkeepCostPerKm.toString())
    }

    var basisNet by remember { mutableStateOf(currentPolicy.basisNet) }

    val commissionPct = remember {
        mutableStateMapOf<RideApp, String>().apply {
            RideApp.entries.forEach { app ->
                put(app, (currentPolicy.commissionPct[app] ?: if (app == RideApp.INDRIVE) 10.0 else 22.0).toString())
            }
        }
    }
    val commissionEnabled = remember {
        mutableStateMapOf<RideApp, Boolean>().apply {
            RideApp.entries.forEach { app ->
                put(app, currentPolicy.commissionEnabled[app] ?: true)
            }
        }
    }
    val appEnabled = remember {
        mutableStateMapOf<RideApp, Boolean>().apply {
            RideApp.entries.forEach { app ->
                put(app, currentPolicy.appEnabled[app] ?: true)
            }
        }
    }

    var negotiationStepText by remember { mutableStateOf(currentPolicy.negotiationStep.toString()) }
    var maxOfferText by remember { mutableStateOf(currentPolicy.maxOffer?.toString() ?: "") }

    // Section expanded states
    var expVehicle by remember { mutableStateOf(true) }
    var expCountry by remember { mutableStateOf(true) }
    var expDistance by remember { mutableStateOf(false) }
    var expRider by remember { mutableStateOf(false) }
    var expStops by remember { mutableStateOf(false) }
    var expCost by remember { mutableStateOf(false) }
    var expNegotiation by remember { mutableStateOf(false) }
    var expApps by remember { mutableStateOf(false) }

    // Errors
    var minPriceError by remember { mutableStateOf<String?>(null) }
    var distanceError by remember { mutableStateOf<String?>(null) }
    var ratingError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("rules_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.rules_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextBrightWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تخصيص كامل لمعادلات الربحية واستهلاك الوقود للمركبات والدراجات",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🚀 SECTION 1: VEHICLE & MOTORCYCLE ENGINE SELECTION (150cc, 200cc, 250cc)
        CollapsibleCard(
            title = stringResource(R.string.section_vehicle_engine),
            summary = vehicleType.displayNameAr,
            isExpanded = expVehicle,
            onToggle = { expVehicle = !expVehicle }
        ) {
            Text(
                text = stringResource(R.string.vehicle_type_label),
                color = RacingOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleType.entries.forEach { type ->
                    val isSelected = vehicleType == type
                    val isMotorcycle = type != VehicleType.CAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) RacingOrange.copy(alpha = 0.15f) else GraphiteSurfaceVariant)
                            .border(
                                1.5.dp,
                                if (isSelected) RacingOrange else SlateBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                vehicleType = type
                                fuelCostText = type.defaultFuelCostPerKm.toString()
                                upkeepCostText = type.defaultUpkeepCostPerKm.toString()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.vehicle_preset_applied, type.displayNameAr),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isMotorcycle) Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = if (isSelected) RacingOrange else TextSilverGray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = type.displayNameAr,
                                    color = if (isSelected) RacingOrange else TextBrightWhite,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "وقود: ${type.defaultFuelCostPerKm} ج/كم • صيانة: ${type.defaultUpkeepCostPerKm} ج/كم",
                                    color = TextSilverGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RacingOrange)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("نشط", color = ObsidianBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section (a): Country and price (REQUIRED: minPricePerKm)
        val minPriceVal = NumberUtils.parseDoubleOrNull(minPricePerKmText)
        val countrySummary = if (minPriceVal != null && minPriceVal > 0) {
            "$minPriceVal $currency/كم ($governorate)"
        } else {
            "حدد السعر المطلوب"
        }

        CollapsibleCard(
            title = stringResource(R.string.section_country_price),
            summary = countrySummary,
            isExpanded = expCountry,
            onToggle = { expCountry = !expCountry }
        ) {
            OutlinedInputField(
                label = stringResource(R.string.field_min_price_per_km),
                value = minPricePerKmText,
                onValueChange = {
                    minPricePerKmText = it
                    minPriceError = null
                },
                placeholder = "مثال: 8.5 أو 10",
                error = minPriceError,
                keyboardType = KeyboardType.Decimal
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_currency),
                        value = currency,
                        onValueChange = { currency = it },
                        placeholder = "EGP"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_governorate),
                        value = governorate,
                        onValueChange = { governorate = it },
                        placeholder = "القاهرة"
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("وحدة المسافات:", color = RacingOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                DistanceUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = distanceUnit == unit,
                        onClick = { distanceUnit = unit },
                        label = { Text(unit.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RacingOrange,
                            selectedLabelColor = ObsidianBackground
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section (b): Distance
        val distSummary = "أقصى كلية: ${maxTotalKmText.ifBlank { "غير محدد" }} | التقاط: ${maxPickupKmText.ifBlank { "غير محدد" }}"
        CollapsibleCard(
            title = stringResource(R.string.section_distance),
            summary = distSummary,
            isExpanded = expDistance,
            onToggle = { expDistance = !expDistance }
        ) {
            if (distanceError != null) {
                Text(distanceError!!, color = VerdictRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_min_total_km),
                        value = minTotalKmText,
                        onValueChange = { minTotalKmText = it; distanceError = null },
                        placeholder = "مثال: 3"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_max_total_km),
                        value = maxTotalKmText,
                        onValueChange = { maxTotalKmText = it; distanceError = null },
                        placeholder = "مثال: 45"
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_max_trip_km),
                        value = maxTripKmText,
                        onValueChange = { maxTripKmText = it },
                        placeholder = "مثال: 35"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_max_pickup_km),
                        value = maxPickupKmText,
                        onValueChange = { maxPickupKmText = it },
                        placeholder = "مثال: 5"
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_max_pickup_minutes),
                        value = maxPickupMinutesText,
                        onValueChange = { maxPickupMinutesText = it },
                        placeholder = "مثال: 10"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_free_pickup_km),
                        value = freePickupKmText,
                        onValueChange = { freePickupKmText = it },
                        placeholder = "مثال: 1.5"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section (c): Rider and payment
        val riderSummary = "التقييم: ${minRiderRatingText.ifBlank { "غير مقيد" }} | ${if (cashOnly) "كاش فقط" else "كل طرق الدفع"}"
        CollapsibleCard(
            title = stringResource(R.string.section_rider_payment),
            summary = riderSummary,
            isExpanded = expRider,
            onToggle = { expRider = !expRider }
        ) {
            if (ratingError != null) {
                Text(ratingError!!, color = VerdictRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_min_rider_rating),
                        value = minRiderRatingText,
                        onValueChange = { minRiderRatingText = it; ratingError = null },
                        placeholder = "مثال: 4.6"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_min_rider_trips),
                        value = minRiderTripsText,
                        onValueChange = { minRiderTripsText = it },
                        placeholder = "مثال: 10"
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedInputField(
                label = stringResource(R.string.field_max_request_age),
                value = maxRequestAgeMinutesText,
                onValueChange = { maxRequestAgeMinutesText = it },
                placeholder = "مثال: 5 دقائق"
            )
            Spacer(modifier = Modifier.height(14.dp))
            // Payment method toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (cashOnly) stringResource(R.string.payment_cash_only) else stringResource(R.string.payment_all),
                    color = TextBrightWhite,
                    fontSize = 14.sp
                )
                Switch(
                    checked = cashOnly,
                    onCheckedChange = { cashOnly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBackground,
                        checkedTrackColor = RacingOrange
                    )
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            // Banned Note Words
            Text(stringResource(R.string.field_banned_words), color = RacingOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = bannedWordInput,
                    onValueChange = { bannedWordInput = it },
                    placeholder = { Text("أضف كلمة +", color = TextSilverGray, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextBrightWhite,
                        unfocusedTextColor = TextBrightWhite,
                        focusedBorderColor = RacingOrange,
                        unfocusedBorderColor = SlateBorder,
                        focusedContainerColor = GraphiteSurfaceVariant,
                        unfocusedContainerColor = GraphiteSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val word = bannedWordInput.trim()
                        if (word.isNotEmpty() && !bannedWordsList.contains(word)) {
                            bannedWordsList = bannedWordsList + word
                            bannedWordInput = ""
                        }
                    },
                    modifier = Modifier
                        .background(RacingOrange, RoundedCornerShape(8.dp))
                        .size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = ObsidianBackground)
                }
            }
            if (bannedWordsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    bannedWordsList.forEach { word ->
                        Box(
                            modifier = Modifier
                                .background(GraphiteSurfaceVariant, RoundedCornerShape(16.dp))
                                .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(word, color = TextBrightWhite, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = VerdictRed,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            bannedWordsList = bannedWordsList - word
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section (d): Stops and long trips
        val stopsSummary = "الوقفات: ${stopsMode.name} | السفر: ${longTripMode.name}"
        CollapsibleCard(
            title = stringResource(R.string.section_stops_long),
            summary = stopsSummary,
            isExpanded = expStops,
            onToggle = { expStops = !expStops }
        ) {
            Text("الوقفات الإضافية:", color = RacingOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                StopsMode.entries.forEach { mode ->
                    FilterChip(
                        selected = stopsMode == mode,
                        onClick = { stopsMode = mode },
                        label = { Text(mode.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RacingOrange,
                            selectedLabelColor = ObsidianBackground
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedInputField(
                label = stringResource(R.string.field_stop_fee),
                value = stopFeeText,
                onValueChange = { stopFeeText = it },
                placeholder = "مثال: 20 ج.م"
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("السفر الطويل:", color = RacingOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                LongTripMode.entries.forEach { mode ->
                    FilterChip(
                        selected = longTripMode == mode,
                        onClick = { longTripMode = mode },
                        label = { Text(mode.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = ObsidianBackground
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedInputField(
                label = stringResource(R.string.field_long_trip_km),
                value = longTripKmText,
                onValueChange = { longTripKmText = it },
                placeholder = "مثال: 35 كم"
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.field_warn_uber_saver), color = TextBrightWhite, fontSize = 13.sp)
                Switch(
                    checked = warnOnUberSaver,
                    onCheckedChange = { warnOnUberSaver = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBackground, checkedTrackColor = RacingOrange)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section (e): Vehicle expenses & commission
        val fuelVal = NumberUtils.parseDoubleOrNull(fuelCostText) ?: vehicleType.defaultFuelCostPerKm
        val upkeepVal = NumberUtils.parseDoubleOrNull(upkeepCostText) ?: vehicleType.defaultUpkeepCostPerKm
        val totalCostPerKm = fuelVal + upkeepVal
        val costSummary = "المصاريف: $totalCostPerKm ج/كم | ${if (basisNet) "صافي" else "إجمالي"}"

        CollapsibleCard(
            title = stringResource(R.string.section_car_cost),
            summary = costSummary,
            isExpanded = expCost,
            onToggle = { expCost = !expCost }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_fuel_cost),
                        value = fuelCostText,
                        onValueChange = { fuelCostText = it },
                        placeholder = "${vehicleType.defaultFuelCostPerKm}"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_upkeep_cost),
                        value = upkeepCostText,
                        onValueChange = { upkeepCostText = it },
                        placeholder = "${vehicleType.defaultUpkeepCostPerKm}"
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.field_basis_net), color = TextBrightWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("خصم العمولة والوقود قبل الحكم على الطلب", color = TextSilverGray, fontSize = 11.sp)
                }
                Switch(
                    checked = basisNet,
                    onCheckedChange = { basisNet = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBackground, checkedTrackColor = EmeraldGreen)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("نسب عمولة الشركات:", color = RacingOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            RideApp.entries.forEach { app ->
                val appTitle = when (app) {
                    RideApp.INDRIVE -> "inDrive"
                    RideApp.UBER -> "Uber"
                    RideApp.DIDI -> "DiDi"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(appTitle, color = TextBrightWhite, modifier = Modifier.width(70.dp), fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = commissionPct[app] ?: "",
                        onValueChange = { commissionPct[app] = it },
                        modifier = Modifier.width(90.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextBrightWhite,
                            unfocusedTextColor = TextBrightWhite,
                            focusedBorderColor = RacingOrange,
                            unfocusedBorderColor = SlateBorder,
                            focusedContainerColor = GraphiteSurfaceVariant,
                            unfocusedContainerColor = GraphiteSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text("%", color = TextSilverGray)
                    Switch(
                        checked = commissionEnabled[app] ?: true,
                        onCheckedChange = { commissionEnabled[app] = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBackground, checkedTrackColor = RacingOrange)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section (f): Negotiation
        val negSummary = "الخطوة: ${negotiationStepText.ifBlank { "5" }} ج | السقف: ${maxOfferText.ifBlank { "غير محدد" }}"
        CollapsibleCard(
            title = stringResource(R.string.section_negotiation),
            summary = negSummary,
            isExpanded = expNegotiation,
            onToggle = { expNegotiation = !expNegotiation }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_negotiation_step),
                        value = negotiationStepText,
                        onValueChange = { negotiationStepText = it },
                        placeholder = "5"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedInputField(
                        label = stringResource(R.string.field_max_offer),
                        value = maxOfferText,
                        onValueChange = { maxOfferText = it },
                        placeholder = "مثال: 400"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section (g): App Enable switches
        CollapsibleCard(
            title = stringResource(R.string.section_apps_enabled),
            summary = "inDrive (${if (appEnabled[RideApp.INDRIVE] == true) "مفعل" else "معطل"}), Uber, DiDi",
            isExpanded = expApps,
            onToggle = { expApps = !expApps }
        ) {
            RideApp.entries.forEach { app ->
                val name = when (app) {
                    RideApp.INDRIVE -> "inDrive"
                    RideApp.UBER -> "Uber Driver"
                    RideApp.DIDI -> "DiDi Driver"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, color = TextBrightWhite, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = appEnabled[app] ?: true,
                        onCheckedChange = { appEnabled[app] = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBackground, checkedTrackColor = EmeraldGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Save Button
        Button(
            onClick = {
                val parsedMinPrice = NumberUtils.parseDoubleOrNull(minPricePerKmText)
                if (parsedMinPrice == null || parsedMinPrice <= 0.0) {
                    minPriceError = "الحد الأدنى لسعر الكيلومتر مطلوب وأكبر من الصفر"
                    expCountry = true
                    return@Button
                }
                val minTot = NumberUtils.parseDoubleOrNull(minTotalKmText)
                val maxTot = NumberUtils.parseDoubleOrNull(maxTotalKmText)
                if (minTot != null && maxTot != null && minTot > maxTot) {
                    distanceError = "الحد الأدنى للمسافة لا يمكن أن يتجاوز الحد الأقصى"
                    expDistance = true
                    return@Button
                }
                val riderRating = NumberUtils.parseDoubleOrNull(minRiderRatingText)
                if (riderRating != null && (riderRating < 0.0 || riderRating > 5.0)) {
                    ratingError = "التقييم يجب أن يكون بين 0 و 5"
                    expRider = true
                    return@Button
                }

                val commPctMap = commissionPct.mapNotNull { (app, text) ->
                    val num = NumberUtils.parseDoubleOrNull(text)
                    if (num != null) app to num else null
                }.toMap()

                val newPolicy = Policy(
                    vehicleType = vehicleType,
                    minPricePerKm = parsedMinPrice,
                    minTotalKm = minTot,
                    maxTotalKm = maxTot,
                    maxTripKm = NumberUtils.parseDoubleOrNull(maxTripKmText),
                    maxPickupKm = NumberUtils.parseDoubleOrNull(maxPickupKmText),
                    maxPickupMinutes = NumberUtils.parseIntOrNull(maxPickupMinutesText),
                    minRiderRating = riderRating,
                    minRiderTrips = NumberUtils.parseIntOrNull(minRiderTripsText),
                    maxRequestAgeMinutes = NumberUtils.parseIntOrNull(maxRequestAgeMinutesText),
                    negotiationStep = NumberUtils.parseIntOrNull(negotiationStepText) ?: 5,
                    maxOffer = NumberUtils.parseDoubleOrNull(maxOfferText),
                    currency = currency.ifBlank { "EGP" },
                    governorate = governorate,
                    avoidedAreas = currentPolicy.avoidedAreas,
                    preferredAreas = currentPolicy.preferredAreas,
                    allowedPayments = if (cashOnly) setOf("CASH_ONLY") else emptySet(),
                    bannedNoteWords = bannedWordsList,
                    stopsMode = stopsMode,
                    stopFee = NumberUtils.parseDoubleOrNull(stopFeeText),
                    longTripMode = longTripMode,
                    longTripKm = NumberUtils.parseDoubleOrNull(longTripKmText),
                    warnOnUberSaver = warnOnUberSaver,
                    freePickupKm = NumberUtils.parseDoubleOrNull(freePickupKmText),
                    fuelCostPerKm = fuelVal,
                    upkeepCostPerKm = upkeepVal,
                    commissionPct = commPctMap,
                    commissionEnabled = commissionEnabled.toMap(),
                    appEnabled = appEnabled.toMap(),
                    distanceUnit = distanceUnit,
                    basisNet = basisNet
                )
                onSavePolicy(newPolicy)
                Toast.makeText(context, context.getString(R.string.rules_saved_toast), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("rules_save_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RacingOrange, contentColor = ObsidianBackground)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_save_rules), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun CollapsibleCard(
    title: String,
    summary: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextBrightWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldGreen,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = RacingOrange
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun OutlinedInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = RacingOrange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextSilverGray, fontSize = 13.sp) },
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextBrightWhite,
                unfocusedTextColor = TextBrightWhite,
                focusedBorderColor = RacingOrange,
                unfocusedBorderColor = SlateBorder,
                focusedContainerColor = GraphiteSurfaceVariant,
                unfocusedContainerColor = GraphiteSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        )
        if (error != null) {
            Text(error, color = VerdictRed, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, start = 2.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RulesScreenPreview() {
    DriveFlowTheme {
        RulesScreen(currentPolicy = Policy(), onSavePolicy = {})
    }
}
