package com.driveflow.driver.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.data.prefs.AlertMode
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

@Composable
fun HomeScreen(
    policy: Policy,
    alertEnabled: Boolean,
    alertMode: AlertMode,
    headingHomeEnabled: Boolean,
    licenseStatus: LicenseStatus,
    remainingDays: Int,
    freeListRemaining: Int,
    freeDetailsRemaining: Int,
    readerEnabled: Boolean,
    onToggleReader: (Boolean) -> Unit,
    onToggleAlert: (Boolean) -> Unit,
    onToggleHeadingHome: (Boolean) -> Unit,
    onToggleAppEnabled: (RideApp, Boolean) -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToHeadingHome: () -> Unit,
    onNavigateToSubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showRestrictedDialog by remember { mutableStateOf(false) }
    var showBatteryGuideDialog by remember { mutableStateOf(false) }

    val isAccessibilityGranted = readerEnabled
    val isOverlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
    val isRulesConfigured = policy.minPricePerKm > 0.0
    val isSetupComplete = isAccessibilityGranted && isOverlayGranted && isRulesConfigured

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("home_screen")
    ) {
        // App Header with [PRO] badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_driveflow_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(46.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextBrightWhite,
                            fontWeight = FontWeight.Bold
                        )
                        if (licenseStatus == LicenseStatus.ACTIVE) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RacingOrange.copy(alpha = 0.2f))
                                    .border(1.dp, RacingOrange, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "[PRO]",
                                    color = RacingOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hero Card: DriveFlow is ON / OFF
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (readerEnabled) GraphiteSurface else GraphiteSurfaceVariant
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (readerEnabled) EmeraldGreen.copy(alpha = 0.7f) else SlateBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (readerEnabled) EmeraldGreen.copy(alpha = 0.2f) else SlateBorder.copy(alpha = 0.4f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (readerEnabled) EmeraldGreen else TextSilverGray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (readerEnabled) stringResource(R.string.home_hero_on) else stringResource(R.string.home_hero_off),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextBrightWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (readerEnabled) stringResource(R.string.home_hero_sub_on) else stringResource(R.string.home_hero_sub_off),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSilverGray
                        )
                    }
                }

                Switch(
                    checked = readerEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isAccessibilityGranted) {
                            showRestrictedDialog = true
                        } else {
                            onToggleReader(isChecked)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBackground,
                        checkedTrackColor = EmeraldGreen,
                        uncheckedThumbColor = TextSilverGray,
                        uncheckedTrackColor = GraphiteSurfaceVariant
                    ),
                    modifier = Modifier.testTag("reader_main_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3-Step Setup Checklist (if not yet complete)
        if (!isSetupComplete) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RacingOrange.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = RacingOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.setup_checklist_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = RacingOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    // Step 1: Accessibility
                    SetupStepRow(
                        title = stringResource(R.string.setup_step1),
                        isDone = isAccessibilityGranted,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                showRestrictedDialog = true
                            } else {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Step 2: Overlay
                    SetupStepRow(
                        title = stringResource(R.string.setup_step2),
                        isDone = isOverlayGranted,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Step 3: Rules
                    SetupStepRow(
                        title = stringResource(R.string.setup_step3),
                        isDone = isRulesConfigured,
                        onClick = onNavigateToRules
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Control Rows
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row: My Rules & Vehicle
                val vehicleName = policy.vehicleType.displayNameAr
                val rulesSummary = if (policy.minPricePerKm > 0.0) {
                    "$vehicleName • ${policy.minPricePerKm} ${policy.currency}/كم"
                } else {
                    "$vehicleName • حدد سعر الكيلو المطلوب"
                }
                HomeFeatureRow(
                    icon = Icons.Default.Rule,
                    title = stringResource(R.string.row_my_rules),
                    subtitle = rulesSummary,
                    onClick = onNavigateToRules,
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextSilverGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SlateBorder)
                )
                // Row: Alert
                val alertSub = if (alertEnabled) {
                    if (alertMode == AlertMode.TONE_VIBRATE) "صوت واهتزاز للطلبات الذهبية" else "صوت فقط للطلبات الذهبية"
                } else {
                    "التنبيهات معطلة"
                }
                HomeFeatureRow(
                    icon = Icons.Default.NotificationsActive,
                    title = stringResource(R.string.row_alerts),
                    subtitle = alertSub,
                    onClick = onNavigateToAlerts,
                    trailingContent = {
                        Switch(
                            checked = alertEnabled,
                            onCheckedChange = onToggleAlert,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBackground,
                                checkedTrackColor = RacingOrange
                            )
                        )
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SlateBorder)
                )
                // Row: Heading Home
                HomeFeatureRow(
                    icon = Icons.Default.Home,
                    title = stringResource(R.string.row_heading_home),
                    subtitle = if (headingHomeEnabled) "مفعل (تفضيل مشاوير السكة)" else "متوقف",
                    onClick = onNavigateToHeadingHome,
                    trailingContent = {
                        Switch(
                            checked = headingHomeEnabled,
                            onCheckedChange = onToggleHeadingHome,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBackground,
                                checkedTrackColor = EmeraldGreen
                            )
                        )
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SlateBorder)
                )
                // Row: Plan & License with PRO badge
                val statusColor = when (licenseStatus) {
                    LicenseStatus.ACTIVE -> EmeraldGreen
                    LicenseStatus.TRIAL -> VerdictOrange
                    LicenseStatus.FREE -> TextSilverGray
                }
                val statusText = when (licenseStatus) {
                    LicenseStatus.ACTIVE -> "[PRO] نشط (متبقي $remainingDays يوم)"
                    LicenseStatus.TRIAL -> "تجريبي (متبقي $remainingDays يوم)"
                    LicenseStatus.FREE -> "مجاني (متبقي: $freeListRemaining طلب / $freeDetailsRemaining تفاصيل)"
                }
                HomeFeatureRow(
                    icon = Icons.Default.Shield,
                    title = stringResource(R.string.row_plan),
                    subtitle = statusText,
                    onClick = onNavigateToSubscribe,
                    trailingContent = {
                        Button(
                            onClick = onNavigateToSubscribe,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = statusColor.copy(alpha = 0.2f),
                                contentColor = statusColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (licenseStatus == LicenseStatus.ACTIVE) "[PRO]" else stringResource(R.string.btn_details),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Supported Apps Row
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
                Text(
                    text = stringResource(R.string.supported_apps_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                RideApp.entries.forEach { app ->
                    val isAppEnabled = policy.appEnabled[app] ?: true
                    val appName = when (app) {
                        RideApp.INDRIVE -> stringResource(R.string.app_indrive)
                        RideApp.UBER -> stringResource(R.string.app_uber)
                        RideApp.DIDI -> stringResource(R.string.app_didi)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = appName, color = TextBrightWhite, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isAppEnabled,
                            onCheckedChange = { onToggleAppEnabled(app, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBackground,
                                checkedTrackColor = EmeraldGreen
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keep Reader Running (Battery optimization guide)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBatteryGuideDialog = true },
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.BatterySaver,
                    contentDescription = null,
                    tint = RacingOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.keep_reader_running_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextBrightWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.keep_reader_running_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSilverGray
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = TextSilverGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Android 13+ Restricted Settings Dialog
    if (showRestrictedDialog) {
        AlertDialog(
            onDismissRequest = { showRestrictedDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.restricted_settings_title),
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.restricted_settings_msg),
                    color = TextBrightWhite,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedDialog = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RacingOrange, contentColor = ObsidianBackground)
                ) {
                    Text(stringResource(R.string.btn_understood), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedDialog = false }) {
                    Text(stringResource(R.string.btn_skip), color = TextSilverGray)
                }
            },
            containerColor = GraphiteSurface
        )
    }

    // Battery Optimization Guide Dialog
    if (showBatteryGuideDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryGuideDialog = false },
            title = {
                Text(
                    text = "دليل استقرار واستمرار عمل القارئ",
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "لضمان عدم إيقاف النظام للتطبيق أثناء العمل:\n\n" +
                                "١. هواتف شاومي (Xiaomi / Redmi / Poco):\n" +
                                "فعّل التشغيل التلقائي (Autostart) واضبط موفر البطارية على بدون قيود.\n\n" +
                                "٢. هواتف أوبو وريلمي (Oppo / Realme):\n" +
                                "فعّل السماح بنشاط الخلفية وألغِ التجميد التلقائي.\n\n" +
                                "٣. هواتف سامسونج (Samsung):\n" +
                                "أضف DriveFlow لقائمة التطبيقات التي لا تنام أبداً.",
                        color = TextBrightWhite,
                        lineHeight = 22.sp,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryGuideDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = ObsidianBackground)
                ) {
                    Text("فتح الإعدادات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryGuideDialog = false }) {
                    Text("إغلاق", color = TextSilverGray)
                }
            },
            containerColor = GraphiteSurface
        )
    }
}

@Composable
fun SetupStepRow(
    title: String,
    isDone: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) EmeraldGreen else VerdictOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = if (isDone) TextSilverGray else TextBrightWhite,
                fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextSilverGray,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun HomeFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RacingOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
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
        trailingContent()
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    DriveFlowTheme {
        HomeScreen(
            policy = Policy(minPricePerKm = 8.5),
            alertEnabled = true,
            alertMode = AlertMode.TONE_VIBRATE,
            headingHomeEnabled = false,
            licenseStatus = LicenseStatus.ACTIVE,
            remainingDays = 30,
            freeListRemaining = 18,
            freeDetailsRemaining = 4,
            readerEnabled = true,
            onToggleReader = {},
            onToggleAlert = {},
            onToggleHeadingHome = {},
            onToggleAppEnabled = { _, _ -> },
            onNavigateToRules = {},
            onNavigateToAlerts = {},
            onNavigateToHeadingHome = {},
            onNavigateToSubscribe = {}
        )
    }
}
