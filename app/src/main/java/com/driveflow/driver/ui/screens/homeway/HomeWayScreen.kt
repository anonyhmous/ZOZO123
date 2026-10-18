package com.driveflow.driver.ui.screens.homeway

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.driveflow.driver.data.prefs.HomeWay
import com.driveflow.driver.ui.theme.DriveFlowTheme
import com.driveflow.driver.ui.theme.EmeraldGreen
import com.driveflow.driver.ui.theme.GraphiteSurface
import com.driveflow.driver.ui.theme.GraphiteSurfaceVariant
import com.driveflow.driver.ui.theme.ObsidianBackground
import com.driveflow.driver.ui.theme.RacingOrange
import com.driveflow.driver.ui.theme.SlateBorder
import com.driveflow.driver.ui.theme.TextBrightWhite
import com.driveflow.driver.ui.theme.TextSilverGray

@Composable
fun HomeWayScreen(
    currentHomeWay: HomeWay,
    onSaveHomeWay: (HomeWay) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(currentHomeWay.enabled) }

    // Primary Home
    var homeInput by remember {
        mutableStateOf(
            if (currentHomeWay.lat != 0.0 || currentHomeWay.lon != 0.0) "${currentHomeWay.lat}, ${currentHomeWay.lon}" else ""
        )
    }
    var homeLabel by remember { mutableStateOf(currentHomeWay.label.ifBlank { "منزلي" }) }

    // Second Place mode
    var placeInput by remember { mutableStateOf("") }
    var placeLabel by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("homeway_screen")
    ) {
        Text(
            text = stringResource(R.string.homeway_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextBrightWhite,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "تفضيل وتقريب الرحلات التي تسير في اتجاه منزلك أو وجهتك بنهاية الوردية",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldGreen
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Toggle Switch Card
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
                            .background(EmeraldGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.homeway_switch),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextBrightWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.homeway_switch_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSilverGray
                        )
                    }
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBackground,
                        checkedTrackColor = EmeraldGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode 1: Primary Home Location Card
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
                    Icon(Icons.Default.Home, contentDescription = null, tint = RacingOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.homeway_mode_home),
                        style = MaterialTheme.typography.titleMedium,
                        color = RacingOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("تسمية الوجهة:", color = TextBrightWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = homeLabel,
                    onValueChange = { homeLabel = it },
                    placeholder = { Text(stringResource(R.string.homeway_label_hint), color = TextSilverGray, fontSize = 13.sp) },
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
                Spacer(modifier = Modifier.height(10.dp))
                Text("الإحداثيات أو الرابط (lat,lon):", color = TextBrightWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = homeInput,
                    onValueChange = { homeInput = it },
                    placeholder = { Text(stringResource(R.string.homeway_paste_hint), color = TextSilverGray, fontSize = 12.sp) },
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
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            homeInput = ""
                            homeLabel = ""
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSilverGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_clear_destination), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode 2: To a specific place
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
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.homeway_mode_place),
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("اسم المكان (اختياري):", color = TextBrightWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = placeLabel,
                    onValueChange = { placeLabel = it },
                    placeholder = { Text("مثال: ورشة الصيانة", color = TextSilverGray, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextBrightWhite,
                        unfocusedTextColor = TextBrightWhite,
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = SlateBorder,
                        focusedContainerColor = GraphiteSurfaceVariant,
                        unfocusedContainerColor = GraphiteSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("الإحداثيات أو الرابط (lat,lon):", color = TextBrightWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = placeInput,
                    onValueChange = { placeInput = it },
                    placeholder = { Text(stringResource(R.string.homeway_paste_hint), color = TextSilverGray, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextBrightWhite,
                        unfocusedTextColor = TextBrightWhite,
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = SlateBorder,
                        focusedContainerColor = GraphiteSurfaceVariant,
                        unfocusedContainerColor = GraphiteSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            placeInput = ""
                            placeLabel = ""
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSilverGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_clear_destination), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                var lat = 0.0
                var lon = 0.0
                val activeInput = homeInput.ifBlank { placeInput }
                if (activeInput.contains(",")) {
                    val parts = activeInput.split(",")
                    lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 0.0
                    lon = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0
                }
                val updated = HomeWay(
                    enabled = isEnabled,
                    lat = lat,
                    lon = lon,
                    label = homeLabel.ifBlank { placeLabel.ifBlank { "المنزل" } }
                )
                onSaveHomeWay(updated)
                Toast.makeText(context, context.getString(R.string.homeway_saved_toast), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("homeway_save_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldGreen,
                contentColor = ObsidianBackground
            )
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_save_destination), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun HomeWayScreenPreview() {
    DriveFlowTheme {
        HomeWayScreen(currentHomeWay = HomeWay(enabled = true, label = "المنزل"), onSaveHomeWay = {})
    }
}
