package com.driveflow.driver.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.data.prefs.BarPosition
import com.driveflow.driver.data.prefs.MarkerSize
import com.driveflow.driver.ui.theme.DriveFlowTheme
import com.driveflow.driver.ui.theme.EmeraldGreen
import com.driveflow.driver.ui.theme.GraphiteSurface
import com.driveflow.driver.ui.theme.GraphiteSurfaceVariant
import com.driveflow.driver.ui.theme.ObsidianBackground
import com.driveflow.driver.ui.theme.RacingOrange
import com.driveflow.driver.ui.theme.SlateBorder
import com.driveflow.driver.ui.theme.TextBrightWhite
import com.driveflow.driver.ui.theme.TextSilverGray
import com.driveflow.driver.ui.theme.VerdictGreen
import com.driveflow.driver.ui.theme.VerdictOrange
import com.driveflow.driver.ui.theme.VerdictRed

@Composable
fun SettingsScreen(
    currentMarkerSize: MarkerSize,
    onSelectMarkerSize: (MarkerSize) -> Unit,
    barOpacityPercent: Int,
    onChangeBarOpacity: (Int) -> Unit,
    barPosition: BarPosition,
    onResetBarPosition: () -> Unit,
    autoOffHours: Int,
    onSelectAutoOffHours: (Int) -> Unit,
    currentLanguage: String,
    onToggleLanguage: () -> Unit,
    onNavigateToSubscribe: () -> Unit,
    policy: Policy = Policy(),
    onNavigateToRules: () -> Unit = {},
    onNavigateToAreas: () -> Unit = {},
    onNavigateToHomeWay: () -> Unit = {},
    onToggleAppEnabled: (RideApp, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        Text(
            text = stringResource(R.string.settings_title_header),
            style = MaterialTheme.typography.headlineMedium,
            color = TextBrightWhite,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "إدارة مظهر المؤشرات العائمة وحسابات الوقود والقواعد والنظام",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldGreen
        )
        Spacer(modifier = Modifier.height(20.dp))

        // CATEGORY 1: Rules & Vehicle Category
        SettingsCategoryHeader("قواعد التسعير وفئة المركبة")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsClickableRow(
                    icon = Icons.Default.TwoWheeler,
                    iconTint = RacingOrange,
                    title = "فئة المركبة ومعدل الاستهلاك",
                    subtitle = "${policy.vehicleType.displayNameAr} (تكلفة الوقود والصيانة: ${policy.carCostPerKm} ج/كم)",
                    onClick = onNavigateToRules
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))
                SettingsClickableRow(
                    icon = Icons.Default.Rule,
                    iconTint = RacingOrange,
                    title = "سعر الكيلو والحدود",
                    subtitle = if (policy.minPricePerKm > 0.0) "${policy.minPricePerKm} ${policy.currency}/كم" else "لم يتم الضبط",
                    onClick = onNavigateToRules
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // CATEGORY 2: Zones & Heading Home
        SettingsCategoryHeader("المناطق وطريق العودة")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsClickableRow(
                    icon = Icons.Default.LocationOn,
                    iconTint = EmeraldGreen,
                    title = "المناطق المفضلة والمستبعدة",
                    subtitle = "المستبعدة (${policy.avoidedAreas.size}) • المفضلة (${policy.preferredAreas.size})",
                    onClick = onNavigateToAreas
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))
                SettingsClickableRow(
                    icon = Icons.Default.Home,
                    iconTint = EmeraldGreen,
                    title = "وضع العودة للمنزل (السكة)",
                    subtitle = "تحديد موقع المنزل لتقريب المشاوير",
                    onClick = onNavigateToHomeWay
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // CATEGORY 3: Floating Overlay Customization
        SettingsCategoryHeader("تخصيص المؤشرات والشريط العائم")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Marker Size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Straighten, contentDescription = null, tint = RacingOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.marker_size_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextBrightWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarkerSize.entries.forEach { size ->
                        val label = when (size) {
                            MarkerSize.SMALL -> stringResource(R.string.marker_size_small)
                            MarkerSize.NORMAL -> stringResource(R.string.marker_size_normal)
                            MarkerSize.LARGE -> stringResource(R.string.marker_size_large)
                        }
                        FilterChip(
                            selected = currentMarkerSize == size,
                            onClick = { onSelectMarkerSize(size) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RacingOrange,
                                selectedLabelColor = ObsidianBackground
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Live Marker Preview
                Text(
                    text = stringResource(R.string.marker_preview_title),
                    color = RacingOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val badgePadding = when (currentMarkerSize) {
                    MarkerSize.SMALL -> 4.dp
                    MarkerSize.NORMAL -> 8.dp
                    MarkerSize.LARGE -> 12.dp
                }
                val textSize = when (currentMarkerSize) {
                    MarkerSize.SMALL -> 11.sp
                    MarkerSize.NORMAL -> 13.sp
                    MarkerSize.LARGE -> 15.sp
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SampleRequestCard("inDrive • 120 ج.م (9 كم)", "13.3 ج/كم", VerdictGreen, badgePadding, textSize)
                    SampleRequestCard("Uber • 65 ج.م (7 كم)", "9.2 ج/كم", VerdictOrange, badgePadding, textSize)
                    SampleRequestCard("DiDi • 40 ج.م (8 كم)", "5.0 ج/كم", VerdictRed, badgePadding, textSize)
                }

                Spacer(modifier = Modifier.height(18.dp))
                // Bar Opacity Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Opacity, contentDescription = null, tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.bar_opacity_label, barOpacityPercent),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextBrightWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = barOpacityPercent.toFloat(),
                    onValueChange = { onChangeBarOpacity(it.toInt()) },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldGreen,
                        activeTrackColor = EmeraldGreen,
                        inactiveTrackColor = SlateBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                // Live Sample Bar Preview with Dual Action Buttons
                Text(
                    text = stringResource(R.string.bar_preview_title),
                    color = EmeraldGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val sampleAlpha = barOpacityPercent / 100f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GraphiteSurfaceVariant.copy(alpha = sampleAlpha))
                        .border(1.5.dp, RacingOrange.copy(alpha = sampleAlpha), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(VerdictGreen))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("135 ج.م (11 كم)", color = TextBrightWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text("صافي: +92 ج.م", color = RacingOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Preview Dual Buttons
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RacingOrange)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔄 كاملة", color = ObsidianBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EmeraldGreen)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡ أساسية", color = ObsidianBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Bar Position Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.bar_pos_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextBrightWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (barPosition.isAuto) stringResource(R.string.bar_pos_auto) else stringResource(R.string.bar_pos_custom),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSilverGray
                        )
                    }
                    IconButton(
                        onClick = onResetBarPosition,
                        modifier = Modifier.testTag("reset_bar_position_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.btn_reset_pos),
                            tint = RacingOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Auto-off timer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = RacingOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auto_off_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextBrightWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 3, 6, 0).forEach { hours ->
                        val label = when (hours) {
                            1 -> stringResource(R.string.auto_off_hours_1)
                            3 -> stringResource(R.string.auto_off_hours_3)
                            6 -> stringResource(R.string.auto_off_hours_6)
                            else -> stringResource(R.string.auto_off_never)
                        }
                        FilterChip(
                            selected = autoOffHours == hours,
                            onClick = { onSelectAutoOffHours(hours) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RacingOrange,
                                selectedLabelColor = ObsidianBackground
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextSilverGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.auto_off_bank_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSilverGray,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // CATEGORY 4: App Reader Toggles & System
        SettingsCategoryHeader("تطبيقات القراءة والنظام")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Supported Apps Toggles
                RideApp.entries.forEach { app ->
                    val appName = when (app) {
                        RideApp.INDRIVE -> "inDrive"
                        RideApp.UBER -> "Uber Driver"
                        RideApp.DIDI -> "DiDi Driver"
                    }
                    val isEnabled = policy.appEnabled[app] ?: true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = appName, color = TextBrightWhite, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { onToggleAppEnabled(app, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBackground,
                                checkedTrackColor = EmeraldGreen
                            )
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))
                }

                // Language Switch Row
                SettingsClickableRow(
                    icon = Icons.Default.Language,
                    iconTint = EmeraldGreen,
                    title = stringResource(R.string.language_row_title),
                    subtitle = if (currentLanguage == "ar") stringResource(R.string.lang_ar) else stringResource(R.string.lang_en),
                    onClick = onToggleLanguage
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))

                // Subscription Row
                SettingsClickableRow(
                    icon = Icons.Default.Shield,
                    iconTint = RacingOrange,
                    title = stringResource(R.string.subscription_row_title),
                    subtitle = "ترقية الحساب وإدارة مفتاح الرخصة [PRO]",
                    onClick = onNavigateToSubscribe
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        color = RacingOrange,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}

@Composable
fun SettingsClickableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextBrightWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSilverGray
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextSilverGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SampleRequestCard(
    title: String,
    badgeText: String,
    badgeColor: Color,
    padding: androidx.compose.ui.unit.Dp,
    textSize: androidx.compose.ui.unit.TextUnit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GraphiteSurfaceVariant)
            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = padding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = TextBrightWhite, fontSize = textSize, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(text = badgeText, color = badgeColor, fontSize = textSize, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    DriveFlowTheme {
        SettingsScreen(
            currentMarkerSize = MarkerSize.NORMAL,
            onSelectMarkerSize = {},
            barOpacityPercent = 85,
            onChangeBarOpacity = {},
            barPosition = BarPosition(isAuto = true),
            onResetBarPosition = {},
            autoOffHours = 3,
            onSelectAutoOffHours = {},
            currentLanguage = "ar",
            onToggleLanguage = {},
            onNavigateToSubscribe = {}
        )
    }
}
