package com.driveflow.driver.ui.screens.diagnostics

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.data.prefs.OcrCalibration
import com.driveflow.driver.ui.theme.DriveFlowTheme
import com.driveflow.driver.ui.theme.EmeraldGreen
import com.driveflow.driver.ui.theme.GraphiteSurface
import com.driveflow.driver.ui.theme.ObsidianBackground
import com.driveflow.driver.ui.theme.RacingOrange
import com.driveflow.driver.ui.theme.SlateBorder
import com.driveflow.driver.ui.theme.TextBrightWhite
import com.driveflow.driver.ui.theme.TextSilverGray
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(
    diagnosticCounters: Map<String, Int>,
    ocrCalibrations: Map<RideApp, OcrCalibration>,
    onUpdateCalibration: (RideApp, OcrCalibration) -> Unit,
    onNavigateToCalibration: () -> Unit = {},
    onReportCounters: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    scope.launch {
                        if (content.contains("regions") || content.contains("offer_card")) {
                            try {
                                val cal = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<com.driveflow.driver.ocr.OcrDefaultConfigFile>(content)
                                val app = when (cal.app.lowercase()) {
                                    "uber" -> RideApp.UBER
                                    "didi" -> RideApp.DIDI
                                    else -> RideApp.INDRIVE
                                }
                                AppGraph.ocrCalibrationManager.saveCalibration(app, cal.regions)
                                Toast.makeText(context, "تم استيراد إعدادات OCR بنجاح", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل استيراد ملف OCR", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            AppGraph.settingsStore.setPolicyJson(content)
                            Toast.makeText(context, "تم استيراد القواعد بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ في قراءة الملف", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("diagnostics_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BugReport, contentDescription = null, tint = RacingOrange, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.diagnostics_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextBrightWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "أدوات متقدمة لمعايرة القراءة ومراقبة أداء الخدمة",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Diagnostics Counters Card
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
                    Text(
                        text = stringResource(R.string.diag_counters_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = RacingOrange,
                        fontWeight = FontWeight.Bold
                    )
                    // Report Button opening Chat with counters
                    Button(
                        onClick = {
                            val ocrFails = diagnosticCounters.filterKeys { it.startsWith("ocr_fail") }
                            val msg = StringBuilder("تقرير تشخيص (Diagnostics):\n")
                            if (ocrFails.isEmpty()) {
                                msg.append("لا توجد مشاكل OCR مسجلة.")
                            } else {
                                ocrFails.forEach { (k, v) -> msg.append("$k: $v\n") }
                            }
                            onReportCounters(msg.toString())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RacingOrange, contentColor = ObsidianBackground),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("report_counters_button")
                    ) {
                        Text("إرسال للدعم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                val captures = diagnosticCounters["captures"] ?: 0
                val ocrFails = diagnosticCounters["ocr_fails"] ?: 0
                val totalReads = diagnosticCounters["reads"] ?: 0

                Text(stringResource(R.string.diag_counter_captures, captures), color = TextBrightWhite, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(stringResource(R.string.diag_counter_ocr_fails, ocrFails), color = TextBrightWhite, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(stringResource(R.string.diag_counter_reads, totalReads), color = TextBrightWhite, fontSize = 14.sp)

                // Detailed OCR Failure Codes
                val failureCounters = diagnosticCounters.filterKeys { it.startsWith("ocr_fail:") }
                if (failureCounters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("تفاصيل أخطاء القراءة:", color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    failureCounters.forEach { (key, count) ->
                        Text("• $key: $count", color = TextBrightWhite, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open Calibration Screen Button
        Button(
            onClick = onNavigateToCalibration,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = ObsidianBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_calibration_screen_button")
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("فتح شاشة المعايرة البصرية (Calibration Screen)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Export / Import Settings JSON
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
                    text = "النسخ الاحتياطي ونقل الإعدادات",
                    style = MaterialTheme.typography.titleMedium,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Export Button
                    Button(
                        onClick = {
                            scope.launch {
                                val json = AppGraph.settingsStore.getPolicy()
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, json.toString())
                                    type = "application/json"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة إعدادات DriveFlow")
                                context.startActivity(shareIntent)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RacingOrange, contentColor = ObsidianBackground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.diag_export_settings), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Import Button
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.diag_import_settings), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Per-App OCR Crop Rectangles Sliders
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
                    text = stringResource(R.string.diag_ocr_rects_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                RideApp.entries.forEach { app ->
                    val cal = ocrCalibrations[app] ?: OcrCalibration()
                    var offsetX by remember(cal) { mutableFloatStateOf(cal.offsetX.toFloat()) }
                    var offsetY by remember(cal) { mutableFloatStateOf(cal.offsetY.toFloat()) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "${app.name} (${app.code})",
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.slider_crop_x, offsetX.toInt()), color = TextBrightWhite, fontSize = 12.sp)
                        Slider(
                            value = offsetX,
                            onValueChange = {
                                offsetX = it
                                onUpdateCalibration(app, cal.copy(offsetX = it.toInt()))
                            },
                            valueRange = -200f..200f,
                            colors = SliderDefaults.colors(thumbColor = RacingOrange, activeTrackColor = RacingOrange)
                        )
                        Text(stringResource(R.string.slider_crop_y, offsetY.toInt()), color = TextBrightWhite, fontSize = 12.sp)
                        Slider(
                            value = offsetY,
                            onValueChange = {
                                offsetY = it
                                onUpdateCalibration(app, cal.copy(offsetY = it.toInt()))
                            },
                            valueRange = -200f..200f,
                            colors = SliderDefaults.colors(thumbColor = EmeraldGreen, activeTrackColor = EmeraldGreen)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DiagnosticsScreenPreview() {
    DriveFlowTheme {
        DiagnosticsScreen(
            diagnosticCounters = mapOf("captures" to 14, "ocr_fails" to 1, "reads" to 38),
            ocrCalibrations = emptyMap(),
            onUpdateCalibration = { _, _ -> }
        )
    }
}
