package com.driveflow.driver.ui.screens.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.core.model.EgyptData
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

@Composable
fun ContactScreen(
    initialName: String = "",
    initialPhone: String = "",
    initialGov: String = "",
    onSaveAndContinue: (name: String, phone: String, gov: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var selectedGov by remember { mutableStateOf(initialGov.ifBlank { EgyptData.governorates.first().nameAr }) }
    var govMenuExpanded by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("contact_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.contact_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextBrightWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.contact_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSilverGray,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Name Input
                Text(
                    text = stringResource(R.string.contact_name_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    placeholder = { Text(stringResource(R.string.contact_name_hint), color = TextSilverGray) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = RacingOrange)
                    },
                    isError = nameError != null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextBrightWhite,
                        unfocusedTextColor = TextBrightWhite,
                        focusedBorderColor = RacingOrange,
                        unfocusedBorderColor = SlateBorder,
                        focusedContainerColor = GraphiteSurfaceVariant,
                        unfocusedContainerColor = GraphiteSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        color = VerdictRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // WhatsApp Number Input with Country Code
                Text(
                    text = stringResource(R.string.contact_phone_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(GraphiteSurfaceVariant, RoundedCornerShape(10.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "+20",
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = null
                        },
                        placeholder = { Text(stringResource(R.string.contact_phone_hint), color = TextSilverGray) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldGreen)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_phone_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextBrightWhite,
                            unfocusedTextColor = TextBrightWhite,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = SlateBorder,
                            focusedContainerColor = GraphiteSurfaceVariant,
                            unfocusedContainerColor = GraphiteSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                if (phoneError != null) {
                    Text(
                        text = phoneError!!,
                        color = VerdictRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Governorate Spinner
                Text(
                    text = stringResource(R.string.contact_gov_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = RacingOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedGov,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = RacingOrange)
                        },
                        trailingIcon = {
                            IconButton(onClick = { govMenuExpanded = !govMenuExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = TextBrightWhite)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_gov_spinner"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextBrightWhite,
                            unfocusedTextColor = TextBrightWhite,
                            focusedBorderColor = RacingOrange,
                            unfocusedBorderColor = SlateBorder,
                            focusedContainerColor = GraphiteSurfaceVariant,
                            unfocusedContainerColor = GraphiteSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    DropdownMenu(
                        expanded = govMenuExpanded,
                        onDismissRequest = { govMenuExpanded = false },
                        modifier = Modifier
                            .background(GraphiteSurface)
                            .fillMaxWidth(0.85f)
                    ) {
                        EgyptData.governorates.forEach { gov ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${gov.nameAr} (${gov.nameEn})",
                                        color = if (selectedGov == gov.nameAr) RacingOrange else TextBrightWhite
                                    )
                                },
                                onClick = {
                                    selectedGov = gov.nameAr
                                    govMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val trimmedName = name.trim()
                val normalizedPhone = NumberUtils.normalizeArabicDigits(phone)
                var valid = true

                if (trimmedName.length < 2 || !trimmedName.all { it.isLetter() || it.isWhitespace() }) {
                    nameError = "يرجى كتابة اسم صحيح بالحروف فقط"
                    valid = false
                }
                val digitsOnly = normalizedPhone.filter { it.isDigit() }
                if (digitsOnly.length !in 10..11) {
                    phoneError = "يرجى إدخال رقم صحيح (10 أو 11 رقم)"
                    valid = false
                }
                if (valid) {
                    onSaveAndContinue(trimmedName, digitsOnly, selectedGov)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("contact_save_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RacingOrange,
                contentColor = ObsidianBackground
            )
        ) {
            Text(
                text = stringResource(R.string.btn_save_continue),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun ContactScreenPreview() {
    DriveFlowTheme {
        ContactScreen(onSaveAndContinue = { _, _, _ -> })
    }
}
