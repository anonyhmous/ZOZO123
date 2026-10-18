package com.driveflow.driver.ui.screens.help

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.BuildConfig
import com.driveflow.driver.R
import com.driveflow.driver.license.DeviceHelper
import com.driveflow.driver.ui.theme.DriveFlowTheme
import com.driveflow.driver.ui.theme.EmeraldGreen
import com.driveflow.driver.ui.theme.GraphiteSurface
import com.driveflow.driver.ui.theme.GraphiteSurfaceVariant
import com.driveflow.driver.ui.theme.ObsidianBackground
import com.driveflow.driver.ui.theme.RacingOrange
import com.driveflow.driver.ui.theme.SlateBorder
import com.driveflow.driver.ui.theme.TextBrightWhite
import com.driveflow.driver.ui.theme.TextSilverGray
import com.driveflow.driver.ui.theme.VerdictGray
import com.driveflow.driver.ui.theme.VerdictGreen
import com.driveflow.driver.ui.theme.VerdictOrange
import com.driveflow.driver.ui.theme.VerdictRed

@Composable
fun HelpScreen(
    contactName: String,
    contactPhone: String,
    governorate: String,
    onNavigateToChat: () -> Unit,
    onNavigateToCommitments: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onReplayWelcome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawDeviceId = DeviceHelper.getDeviceIdHex(context)
    val displayDeviceCode = DeviceHelper.formatDisplayCode(rawDeviceId)
    var versionTapCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("help_screen")
    ) {
        Text(
            text = stringResource(R.string.help_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextBrightWhite,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "دليل الاستخدام، معاني الألوان، والدعم الفني عبر واتساب",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldGreen
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Contact Owner Button (Prominent)
        Button(
            onClick = onNavigateToChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("contact_owner_chat_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RacingOrange, contentColor = ObsidianBackground)
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.contact_owner_btn), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 1. How It Works (The 4 Pillars)
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
                Text(
                    text = stringResource(R.string.how_it_works_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                PillarItem(stringResource(R.string.pillar1_title), stringResource(R.string.pillar1_desc))
                Spacer(modifier = Modifier.height(10.dp))
                PillarItem(stringResource(R.string.pillar2_title), stringResource(R.string.pillar2_desc))
                Spacer(modifier = Modifier.height(10.dp))
                PillarItem(stringResource(R.string.pillar3_title), stringResource(R.string.pillar3_desc))
                Spacer(modifier = Modifier.height(10.dp))
                PillarItem(stringResource(R.string.pillar4_title), stringResource(R.string.pillar4_desc))
                Spacer(modifier = Modifier.height(16.dp))

                // Replay Welcome Guide Button
                OutlinedButton(
                    onClick = onReplayWelcome,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعادة تشغيل جولة البداية", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Color Legend
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
                Text(
                    text = stringResource(R.string.legend_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LegendRow(VerdictGreen, stringResource(R.string.legend_green))
                Spacer(modifier = Modifier.height(8.dp))
                LegendRow(VerdictOrange, stringResource(R.string.legend_orange))
                Spacer(modifier = Modifier.height(8.dp))
                LegendRow(VerdictRed, stringResource(R.string.legend_red))
                Spacer(modifier = Modifier.height(8.dp))
                LegendRow(VerdictGray, stringResource(R.string.legend_gray))
                Spacer(modifier = Modifier.height(8.dp))
                LegendRow(EmeraldGreen, "أيقونة العين الخضراء: طلب ممتاز في القائمة، افتحه لمعاينة الخريطة.")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Registered Details & Device Code
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
                Text(
                    text = stringResource(R.string.your_registered_info),
                    style = MaterialTheme.typography.titleMedium,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("الاسم: ${contactName.ifBlank { "غير مسجل" }}", color = TextBrightWhite, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("الهاتف: ${contactPhone.ifBlank { "غير مسجل" }}", color = TextBrightWhite, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("المحافظة: ${governorate.ifBlank { "القاهرة" }}", color = TextBrightWhite, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.device_code_title), color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GraphiteSurfaceVariant)
                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = displayDeviceCode,
                        fontFamily = FontFamily.Monospace,
                        color = RacingOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Device Code", displayDeviceCode))
                        Toast.makeText(context, context.getString(R.string.code_copied), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = ObsidianBackground),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.copy_device_code), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Commitments Link
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToCommitments),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.commitments_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextBrightWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = TextSilverGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. How to Update Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                .testTag("how_to_update_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = RacingOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.how_to_update_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextBrightWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.how_to_update_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSilverGray,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. About & Secret 7-Tap Diagnostics Link
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_version_label, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
                color = TextSilverGray,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        versionTapCount++
                        if (versionTapCount >= 7) {
                            versionTapCount = 0
                            onNavigateToDiagnostics()
                            Toast.makeText(context, "تم فتح شاشة التشخيص الفني", Toast.LENGTH_SHORT).show()
                        } else if (versionTapCount >= 4) {
                            Toast.makeText(context, "متبقي ${7 - versionTapCount} نقرات لفتح التشخيص", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(8.dp)
                    .testTag("app_version_label")
            )
            Text(
                text = stringResource(R.string.version_tap_hint),
                color = TextSilverGray.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PillarItem(title: String, desc: String) {
    Column {
        Text(text = title, color = TextBrightWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = desc, color = TextSilverGray, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = TextBrightWhite, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun HelpScreenPreview() {
    DriveFlowTheme {
        HelpScreen(
            contactName = "أحمد علي",
            contactPhone = "01012345678",
            governorate = "القاهرة",
            onNavigateToChat = {},
            onNavigateToCommitments = {},
            onNavigateToDiagnostics = {},
            onReplayWelcome = {}
        )
    }
}
