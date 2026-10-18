package com.driveflow.driver.ui.screens.subscribe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.di.LicenseVerificationResult
import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.license.DefaultLicenseRepository
import com.driveflow.driver.license.DeviceHelper
import com.driveflow.driver.license.LicenseRepository
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
import kotlinx.coroutines.launch

@Composable
fun SubscribeScreen(
    licenseStatus: LicenseStatus,
    remainingDays: Int,
    onNavigateToChat: (requestType: String) -> Unit,
    licenseRepository: LicenseRepository = DefaultLicenseRepository(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rawDeviceId = DeviceHelper.getDeviceIdHex(context)
    val displayDeviceCode = DeviceHelper.formatDisplayCode(rawDeviceId)

    var tokenInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isActivating by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("subscribe_screen")
    ) {
        Text(
            text = stringResource(R.string.subscribe_screen_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextBrightWhite,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "تفعيل رخصة القيادة الذكية بدون خوادم عبر إنستاباي وواتساب",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldGreen
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Status & Price Header Card with [PRO] Badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "حالة الاشتراك:",
                            color = TextSilverGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val (statusBadgeText, badgeColor) = when (licenseStatus) {
                            LicenseStatus.ACTIVE -> stringResource(R.string.status_active) to EmeraldGreen
                            LicenseStatus.TRIAL -> stringResource(R.string.status_trial) to VerdictOrange
                            LicenseStatus.FREE -> stringResource(R.string.status_free) to TextSilverGray
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(badgeColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusBadgeText,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "سعر الاشتراك:",
                            color = TextSilverGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AppGraph.priceText,
                            color = RacingOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                if (licenseStatus != LicenseStatus.FREE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.days_remaining, remainingDays),
                        color = EmeraldGreen,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Device Code Box with Copy
                Text(stringResource(R.string.device_code_title), color = TextBrightWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GraphiteSurfaceVariant)
                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayDeviceCode,
                            fontFamily = FontFamily.Monospace,
                            color = RacingOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Device Code", displayDeviceCode))
                                Toast.makeText(context, context.getString(R.string.code_copied), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Step 1: Pay by InstaPay
        NumberedStepCard(
            stepNumber = "1",
            title = stringResource(R.string.subscribe_step1_title),
            desc = stringResource(R.string.subscribe_step1_desc, AppGraph.priceText),
            icon = Icons.Default.Payment
        ) {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("InstaPay", AppGraph.ownerInstapay))
                    Toast.makeText(context, context.getString(R.string.instapay_copied), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RacingOrange),
                border = androidx.compose.foundation.BorderStroke(1.dp, RacingOrange.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.copy_instapay_num, AppGraph.ownerInstapay),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Step 2: Send Device Code on WhatsApp
        NumberedStepCard(
            stepNumber = "2",
            title = stringResource(R.string.subscribe_step2_title),
            desc = stringResource(R.string.subscribe_step2_desc),
            icon = Icons.AutoMirrored.Filled.Chat
        ) {
            Button(
                onClick = {
                    val reqType = if (licenseStatus == LicenseStatus.ACTIVE) "تجديد اشتراك" else "تفعيل رخصة جديدة"
                    onNavigateToChat(reqType)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RacingOrange, contentColor = ObsidianBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_open_whatsapp_subscribe), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Step 3: Paste & Activate Key
        NumberedStepCard(
            stepNumber = "3",
            title = stringResource(R.string.subscribe_step3_title),
            desc = stringResource(R.string.subscribe_step3_desc),
            icon = Icons.Default.Key
        ) {
            OutlinedTextField(
                value = tokenInput,
                onValueChange = {
                    tokenInput = it
                    errorMessage = null
                    successMessage = null
                },
                placeholder = { Text(stringResource(R.string.token_input_label), color = TextSilverGray, fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("license_key_input"),
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
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = VerdictRed,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
            if (successMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = successMessage!!,
                    color = VerdictGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val token = tokenInput.trim()
                    if (token.isEmpty()) {
                        errorMessage = "يرجى لصق المفتاح أولاً"
                        return@Button
                    }
                    isActivating = true
                    scope.launch {
                        val result = licenseRepository.activate(token, context)
                        isActivating = false
                        when (result) {
                            is LicenseVerificationResult.Valid -> {
                                errorMessage = null
                                successMessage = context.getString(R.string.activation_success, result.expiresDay)
                            }
                            is LicenseVerificationResult.Error -> {
                                successMessage = null
                                errorMessage = when (result.code) {
                                    LicenseVerificationResult.ErrorCode.BAD_FORMAT -> context.getString(R.string.err_bad_format)
                                    LicenseVerificationResult.ErrorCode.BAD_SIGNATURE -> context.getString(R.string.err_bad_signature)
                                    LicenseVerificationResult.ErrorCode.WRONG_DEVICE -> context.getString(R.string.err_wrong_device)
                                    LicenseVerificationResult.ErrorCode.EXPIRED -> context.getString(R.string.err_expired)
                                    LicenseVerificationResult.ErrorCode.NOT_YET_VALID -> context.getString(R.string.err_not_yet_valid)
                                    LicenseVerificationResult.ErrorCode.CLOCK_TAMPERED -> context.getString(R.string.err_clock_tampered)
                                }
                            }
                        }
                    }
                },
                enabled = !isActivating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("activate_license_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = ObsidianBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.activate_button), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun NumberedStepCard(
    stepNumber: String,
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(RacingOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = ObsidianBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextBrightWhite,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TextSilverGray,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SubscribeScreenPreview() {
    DriveFlowTheme {
        SubscribeScreen(
            licenseStatus = LicenseStatus.FREE,
            remainingDays = 0,
            onNavigateToChat = {}
        )
    }
}
