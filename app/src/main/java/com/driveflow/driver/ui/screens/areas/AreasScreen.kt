package com.driveflow.driver.ui.screens.areas

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.driveflow.driver.core.model.EgyptData
import com.driveflow.driver.core.model.Policy
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
fun AreasScreen(
    currentPolicy: Policy,
    onSaveAreas: (avoided: List<String>, preferred: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var avoidedList by remember { mutableStateOf(currentPolicy.avoidedAreas) }
    var preferredList by remember { mutableStateOf(currentPolicy.preferredAreas) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Avoided, 1 = Preferred
    var newAreaInput by remember { mutableStateOf("") }

    var areaToRemove by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var areaToMove by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val govName = currentPolicy.governorate
    val suggestions = EgyptData.getDistrictsForGovernorate(govName).ifEmpty {
        listOf("التجمع الخامس", "المعادي", "الزمالك", "المهندسين", "مدينة نصر", "مصر الجديدة", "6 أكتوبر", "الهرم", "فيصل")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("areas_screen")
    ) {
        Text(
            text = stringResource(R.string.areas_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextBrightWhite,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "تحديد المناطق المفضلة للتركيز عليها والمناطق المستبعدة للتحذير منها",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldGreen
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Warning if governorate is unset
        if (govName.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RacingOrange.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RacingOrange)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.warn_gov_unset),
                        color = TextBrightWhite,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tabs: Avoided vs Preferred
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = GraphiteSurface,
            contentColor = RacingOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = if (selectedTab == 0) VerdictRed else EmeraldGreen
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = VerdictRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "المستبعدة (${avoidedList.size})",
                            color = if (selectedTab == 0) VerdictRed else TextSilverGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "المفضلة (${preferredList.size})",
                            color = if (selectedTab == 1) EmeraldGreen else TextSilverGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Input Field to Add Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newAreaInput,
                onValueChange = { newAreaInput = it },
                placeholder = { Text(stringResource(R.string.add_area_hint), color = TextSilverGray, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextBrightWhite,
                    unfocusedTextColor = TextBrightWhite,
                    focusedBorderColor = if (selectedTab == 0) VerdictRed else EmeraldGreen,
                    unfocusedBorderColor = SlateBorder,
                    focusedContainerColor = GraphiteSurfaceVariant,
                    unfocusedContainerColor = GraphiteSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val area = newAreaInput.trim()
                    if (area.isNotEmpty()) {
                        if (selectedTab == 0) {
                            if (!avoidedList.contains(area)) {
                                avoidedList = avoidedList + area
                                onSaveAreas(avoidedList, preferredList)
                            }
                        } else {
                            if (!preferredList.contains(area)) {
                                preferredList = preferredList + area
                                onSaveAreas(avoidedList, preferredList)
                            }
                        }
                        newAreaInput = ""
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 0) VerdictRed else EmeraldGreen,
                    contentColor = ObsidianBackground
                ),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.btn_add), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Suggestions Chips from Egyptian districts
        Text(
            text = stringResource(R.string.suggested_districts_title),
            color = RacingOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val currentList = if (selectedTab == 0) avoidedList else preferredList
            suggestions.filter { !currentList.contains(it) }.take(10).forEach { district ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(GraphiteSurface)
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            if (selectedTab == 0) {
                                avoidedList = avoidedList + district
                            } else {
                                preferredList = preferredList + district
                            }
                            onSaveAreas(avoidedList, preferredList)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("+ $district", color = TextBrightWhite, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active List Items
        val activeList = if (selectedTab == 0) avoidedList else preferredList
        val isAvoidedTab = selectedTab == 0
        if (activeList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAvoidedTab) stringResource(R.string.empty_avoided_areas) else stringResource(R.string.empty_preferred_areas),
                    color = TextSilverGray,
                    fontSize = 14.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activeList.forEach { areaName ->
                    AreaItemRow(
                        name = areaName,
                        isAvoided = isAvoidedTab,
                        onDeleteClick = {
                            areaToRemove = areaName to isAvoidedTab
                        },
                        onMoveClick = {
                            areaToMove = areaName to isAvoidedTab
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    // Confirm Delete Dialog
    if (areaToRemove != null) {
        val (areaName, isAvoided) = areaToRemove!!
        AlertDialog(
            onDismissRequest = { areaToRemove = null },
            title = { Text(stringResource(R.string.confirm_remove_area_title), color = RacingOrange) },
            text = { Text(stringResource(R.string.confirm_remove_area_msg, areaName), color = TextBrightWhite) },
            confirmButton = {
                Button(
                    onClick = {
                        if (isAvoided) {
                            avoidedList = avoidedList - areaName
                        } else {
                            preferredList = preferredList - areaName
                        }
                        onSaveAreas(avoidedList, preferredList)
                        areaToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdictRed)
                ) {
                    Text("نعم، حذف", color = TextBrightWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { areaToRemove = null }) {
                    Text("إلغاء", color = TextSilverGray)
                }
            },
            containerColor = GraphiteSurface
        )
    }

    // Confirm Move Dialog
    if (areaToMove != null) {
        val (areaName, fromAvoided) = areaToMove!!
        AlertDialog(
            onDismissRequest = { areaToMove = null },
            title = { Text(stringResource(R.string.confirm_move_area_title), color = RacingOrange) },
            text = { Text(stringResource(R.string.confirm_move_area_msg, areaName), color = TextBrightWhite) },
            confirmButton = {
                Button(
                    onClick = {
                        if (fromAvoided) {
                            avoidedList = avoidedList - areaName
                            if (!preferredList.contains(areaName)) preferredList = preferredList + areaName
                        } else {
                            preferredList = preferredList - areaName
                            if (!avoidedList.contains(areaName)) avoidedList = avoidedList + areaName
                        }
                        onSaveAreas(avoidedList, preferredList)
                        areaToMove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = ObsidianBackground)
                ) {
                    Text("نعم، نقل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { areaToMove = null }) {
                    Text("إلغاء", color = TextSilverGray)
                }
            },
            containerColor = GraphiteSurface
        )
    }
}

@Composable
fun AreaItemRow(
    name: String,
    isAvoided: Boolean,
    onDeleteClick: () -> Unit,
    onMoveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isAvoided) VerdictRed else EmeraldGreen)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextBrightWhite,
                    fontWeight = FontWeight.Medium
                )
            }
            Row {
                IconButton(onClick = onMoveClick) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Move",
                        tint = RacingOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextSilverGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AreasScreenPreview() {
    DriveFlowTheme {
        AreasScreen(
            currentPolicy = Policy(avoidedAreas = listOf("حلوان", "المرج"), preferredAreas = listOf("المعادي", "التجمع")),
            onSaveAreas = { _, _ -> }
        )
    }
}
