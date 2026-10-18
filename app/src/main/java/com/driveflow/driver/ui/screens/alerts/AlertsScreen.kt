package com.driveflow.driver.ui.screens.alerts

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.data.prefs.AlertMode
import com.driveflow.driver.ui.theme.DriveFlowTheme
import com.driveflow.driver.ui.theme.EmeraldGreen
import com.driveflow.driver.ui.theme.GraphiteSurface
import com.driveflow.driver.ui.theme.ObsidianBackground
import com.driveflow.driver.ui.theme.RacingOrange
import com.driveflow.driver.ui.theme.SlateBorder
import com.driveflow.driver.ui.theme.TextBrightWhite
import com.driveflow.driver.ui.theme.TextSilverGray
import com.driveflow.driver.ui.theme.VerdictOrange

@Composable
fun AlertsScreen(
    alertEnabled: Boolean,
    alertMode: AlertMode,
    onToggleAlertEnabled: (Boolean) -> Unit,
    onSelectAlertMode: (AlertMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val isVolumeMuted = audioManager?.let {
        it.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0 &&
        it.getStreamVolume(AudioManager.STREAM_RING) == 0
    } ?: false

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("alerts_screen")
    ) {
        Text(
            text = stringResource(R.string.alerts_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextBrightWhite,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "تخصيص نغمة واهتزاز التنبيهات الفورية للطلبات الممتازة",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldGreen
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Muted Warning
        if (isVolumeMuted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VerdictOrange)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolumeOff, contentDescription = null, tint = VerdictOrange)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.alert_volume_muted_warn),
                        color = TextBrightWhite,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Main Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(RacingOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = RacingOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.alert_enable_switch),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextBrightWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (alertEnabled) "التنبيه التلقائي مفعل" else "التنبيهات متوقفة",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (alertEnabled) EmeraldGreen else TextSilverGray
                        )
                    }
                }
                Switch(
                    checked = alertEnabled,
                    onCheckedChange = onToggleAlertEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBackground,
                        checkedTrackColor = RacingOrange
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Alert Mode Options (Sound only vs Sound + Vibration)
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
                    text = stringResource(R.string.alert_mode_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Option 1: Sound and Vibration
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectAlertMode(AlertMode.TONE_VIBRATE) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = alertMode == AlertMode.TONE_VIBRATE,
                        onClick = { onSelectAlertMode(AlertMode.TONE_VIBRATE) },
                        colors = RadioButtonDefaults.colors(selectedColor = RacingOrange)
                    )
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.alert_mode_sound_vibrate),
                        color = TextBrightWhite,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Option 2: Sound only
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectAlertMode(AlertMode.TONE) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = alertMode == AlertMode.TONE,
                        onClick = { onSelectAlertMode(AlertMode.TONE) },
                        colors = RadioButtonDefaults.colors(selectedColor = RacingOrange)
                    )
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = RacingOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.alert_mode_sound),
                        color = TextBrightWhite,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "When it rings" Explanation Card
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.alert_explanation_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.alert_explanation_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSilverGray,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Button to Hear Alert
        Button(
            onClick = {
                try {
                    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(context, notification)
                    r.play()
                    if (alertMode == AlertMode.TONE_VIBRATE) {
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator?.vibrate(300)
                        }
                    }
                    Toast.makeText(context, context.getString(R.string.test_alert_toast), Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("test_alert_sound_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RacingOrange,
                contentColor = ObsidianBackground
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_test_alert),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun AlertsScreenPreview() {
    DriveFlowTheme {
        AlertsScreen(
            alertEnabled = true,
            alertMode = AlertMode.TONE_VIBRATE,
            onToggleAlertEnabled = {},
            onSelectAlertMode = {}
        )
    }
}
