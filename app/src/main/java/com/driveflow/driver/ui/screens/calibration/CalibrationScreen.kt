package com.driveflow.driver.ui.screens.calibration

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.data.prefs.FractionRect
import com.driveflow.driver.ocr.OcrCalibrationManager
import com.driveflow.driver.ui.theme.EmeraldGreen
import com.driveflow.driver.ui.theme.GraphiteSurface
import com.driveflow.driver.ui.theme.ObsidianBackground
import com.driveflow.driver.ui.theme.RacingOrange
import com.driveflow.driver.ui.theme.SlateBorder
import com.driveflow.driver.ui.theme.TextBrightWhite
import com.driveflow.driver.ui.theme.TextSilverGray
import kotlinx.coroutines.launch

@Composable
fun CalibrationScreen(
    calibrationManager: OcrCalibrationManager,
    onTakeLiveCapture: suspend () -> Bitmap? = { null },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedApp by remember { mutableStateOf(RideApp.UBER) }
    var selectedRegionKey by remember { mutableStateOf("offer_card") }
    var liveCaptureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rawOcrText by remember { mutableStateOf("") }
    var parsedSummary by remember { mutableStateOf("") }

    val currentRegions = remember { mutableStateMapOf<String, FractionRect>() }

    // Load initial calibration regions for selectedApp
    LaunchedEffect(selectedApp) {
        val cal = calibrationManager.getCalibration(selectedApp)
        currentRegions.clear()
        if (cal.regions.isNotEmpty()) {
            currentRegions.putAll(cal.regions)
        } else {
            // Default regions
            if (selectedApp == RideApp.UBER) {
                currentRegions["offer_card"] = FractionRect(0.04f, 0.42f, 0.96f, 0.94f)
                currentRegions["fare"] = FractionRect(0.05f, 0.44f, 0.55f, 0.56f)
                currentRegions["distances"] = FractionRect(0.05f, 0.63f, 0.95f, 0.77f)
                currentRegions["rating"] = FractionRect(0.65f, 0.44f, 0.95f, 0.56f)
            } else {
                currentRegions["offer_card"] = FractionRect(0.04f, 0.38f, 0.96f, 0.92f)
                currentRegions["fare"] = FractionRect(0.05f, 0.40f, 0.60f, 0.52f)
                currentRegions["distances"] = FractionRect(0.05f, 0.60f, 0.95f, 0.74f)
                currentRegions["rate_per_km"] = FractionRect(0.05f, 0.52f, 0.60f, 0.60f)
            }
        }
        if (!currentRegions.containsKey(selectedRegionKey)) {
            selectedRegionKey = currentRegions.keys.firstOrNull() ?: "offer_card"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("calibration_screen")
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = RacingOrange, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "معايرة القراءة البصرية (OCR)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextBrightWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ضبط أبعاد مستطيلات الاقتصاص لشاشات Uber و DiDi",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Selector (Uber / DiDi)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GraphiteSurface, RoundedCornerShape(12.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(RideApp.UBER, RideApp.DIDI).forEach { app ->
                val isSelected = selectedApp == app
                Button(
                    onClick = { selectedApp = app },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) RacingOrange else Color.Transparent,
                        contentColor = if (isSelected) ObsidianBackground else TextBrightWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("tab_${app.code}")
                ) {
                    Text(
                        text = if (app == RideApp.UBER) "Uber Driver" else "DiDi Driver",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Region Tabs
        val regionKeys = currentRegions.keys.toList()
        if (regionKeys.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = regionKeys.indexOf(selectedRegionKey).coerceAtLeast(0),
                containerColor = GraphiteSurface,
                contentColor = EmeraldGreen,
                indicator = { tabPositions ->
                    val idx = regionKeys.indexOf(selectedRegionKey).coerceAtLeast(0)
                    if (idx < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                            color = EmeraldGreen
                        )
                    }
                }
            ) {
                regionKeys.forEach { key ->
                    val isSel = selectedRegionKey == key
                    Tab(
                        selected = isSel,
                        onClick = { selectedRegionKey = key },
                        text = {
                            Text(
                                text = formatRegionLabel(key),
                                color = if (isSel) RacingOrange else TextSilverGray,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Capture / Preview Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val bmp = liveCaptureBitmap
                if (bmp != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f)
                            .background(Color.Black, RoundedCornerShape(8.dp))
                    ) {
                        // Draw screenshot and interactive crop box
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(selectedRegionKey) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val cur = currentRegions[selectedRegionKey] ?: return@detectDragGestures
                                        val deltaX = dragAmount.x / size.width
                                        val deltaY = dragAmount.y / size.height
                                        val newLeft = (cur.left + deltaX).coerceIn(0f, cur.right - 0.05f)
                                        val newRight = (cur.right + deltaX).coerceIn(newLeft + 0.05f, 1f)
                                        val newTop = (cur.top + deltaY).coerceIn(0f, cur.bottom - 0.05f)
                                        val newBottom = (cur.bottom + deltaY).coerceIn(newTop + 0.05f, 1f)
                                        currentRegions[selectedRegionKey] = FractionRect(newLeft, newTop, newRight, newBottom)
                                    }
                                }
                        ) {
                            drawImage(bmp.asImageBitmap(), dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
                            // Draw region rectangles
                            currentRegions.forEach { (key, rect) ->
                                val isCurrent = key == selectedRegionKey
                                val color = if (isCurrent) EmeraldGreen else RacingOrange.copy(alpha = 0.5f)
                                val left = rect.left * size.width
                                val top = rect.top * size.height
                                val w = (rect.right - rect.left) * size.width
                                val h = (rect.bottom - rect.top) * size.height
                                drawRect(
                                    color = color,
                                    topLeft = Offset(left, top),
                                    size = Size(w, h),
                                    style = Stroke(width = if (isCurrent) 4f else 2f)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(ObsidianBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Crop, contentDescription = null, tint = TextSilverGray, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد لقطة شاشة حالية", color = TextSilverGray, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action: Take Live Capture
                Button(
                    onClick = {
                        scope.launch {
                            val captured = onTakeLiveCapture()
                            if (captured != null) {
                                liveCaptureBitmap = captured
                                Toast.makeText(context, "تم التقاط الشاشة بنجاح", Toast.LENGTH_SHORT).show()
                            } else {
                                // Create synthetic test image for calibration preview in emulator
                                val dummy = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(dummy)
                                canvas.drawColor(android.graphics.Color.parseColor("#181B20"))
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 60f
                                }
                                canvas.drawText("UberX • 145.00 EGP", 100f, 1100f, paint)
                                canvas.drawText("6 min (2.4 km) away", 100f, 1300f, paint)
                                canvas.drawText("18 min (11.2 km) trip", 100f, 1500f, paint)
                                canvas.drawText("★ 4.92 • Nasr City", 100f, 1700f, paint)
                                liveCaptureBitmap = dummy
                                rawOcrText = "UberX • 145.00 EGP\n6 min (2.4 km) away\n18 min (11.2 km) trip\n★ 4.92"
                                parsedSummary = "السعر: 145 | الالتقاط: 2.4 | الرحلة: 11.2 | التقييم: 4.92"
                                Toast.makeText(context, "تم تحميل نموذج تجريبي للمعايرة", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = ObsidianBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("take_capture_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("التقاط إطار تجريبي للشاشة", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Raw OCR Output & Parsed Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Text(
                    text = "نتيجة الفحص الفعلي (OCR Preview)",
                    style = MaterialTheme.typography.titleMedium,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("النص الخام المقروء:", color = TextSilverGray, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianBackground, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = rawOcrText.ifBlank { "لا توجد بيانات خام بعد" },
                        color = TextBrightWhite,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("الأرقام المستخرجة:", color = TextSilverGray, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianBackground, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = parsedSummary.ifBlank { "لا توجد أرقام مستخرجة بعد" },
                        color = EmeraldGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Save Button
        Button(
            onClick = {
                scope.launch {
                    calibrationManager.saveCalibration(selectedApp, currentRegions.toMap())
                    Toast.makeText(context, "تم حفظ حدود المعايرة بنجاح", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = RacingOrange, contentColor = ObsidianBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_calibration_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ هذه المناطق (Save these regions)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private fun formatRegionLabel(key: String): String {
    return when (key) {
        "offer_card" -> "كارت العرض"
        "fare" -> "السعر"
        "product" -> "نوع الفئة"
        "distances" -> "المسافات"
        "rating" -> "التقييم"
        "addresses" -> "العناوين"
        "rate_per_km" -> "سعر الكيلو"
        "detail_route" -> "مسار المشوار"
        else -> key
    }
}
