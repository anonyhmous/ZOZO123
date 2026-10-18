package com.driveflow.driver.ui.screens.consent

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.R
import com.driveflow.driver.ui.theme.DriveFlowTheme
import com.driveflow.driver.ui.theme.EmeraldGreen
import com.driveflow.driver.ui.theme.GraphiteSurface
import com.driveflow.driver.ui.theme.ObsidianBackground
import com.driveflow.driver.ui.theme.RacingOrange
import com.driveflow.driver.ui.theme.SlateBorder
import com.driveflow.driver.ui.theme.TextBrightWhite
import com.driveflow.driver.ui.theme.TextSilverGray

@Composable
fun ConsentScreen(
    onAgreeAndContinue: () -> Unit,
    onViewCommitments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("consent_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.consent_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextBrightWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.consent_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = EmeraldGreen
        )
        Spacer(modifier = Modifier.height(24.dp))

        ConsentPointCard(
            icon = Icons.Default.Visibility,
            title = stringResource(R.string.consent_point1_title),
            desc = stringResource(R.string.consent_point1_desc)
        )
        Spacer(modifier = Modifier.height(14.dp))
        ConsentPointCard(
            icon = Icons.Default.Block,
            title = stringResource(R.string.consent_point2_title),
            desc = stringResource(R.string.consent_point2_desc)
        )
        Spacer(modifier = Modifier.height(14.dp))
        ConsentPointCard(
            icon = Icons.Default.PhoneAndroid,
            title = stringResource(R.string.consent_point3_title),
            desc = stringResource(R.string.consent_point3_desc)
        )
        Spacer(modifier = Modifier.height(14.dp))
        ConsentPointCard(
            icon = Icons.Default.CloudOff,
            title = stringResource(R.string.consent_point4_title),
            desc = stringResource(R.string.consent_point4_desc)
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onAgreeAndContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("consent_agree_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RacingOrange,
                contentColor = ObsidianBackground
            )
        ) {
            Text(
                text = stringResource(R.string.btn_agree_continue),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onViewCommitments,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("consent_view_commitments_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Text(
                text = stringResource(R.string.btn_view_commitments),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ConsentPointCard(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(RacingOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RacingOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextBrightWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSilverGray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConsentScreenPreview() {
    DriveFlowTheme {
        ConsentScreen(onAgreeAndContinue = {}, onViewCommitments = {})
    }
}
