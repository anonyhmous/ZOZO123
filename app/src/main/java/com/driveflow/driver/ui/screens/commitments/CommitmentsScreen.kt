package com.driveflow.driver.ui.screens.commitments

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun CommitmentsScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("commitments_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.commitments_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextBrightWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "مبادئ أمان وخصوصية صارمة تحكم عمل التطبيق",
                    style = MaterialTheme.typography.bodySmall,
                    color = RacingOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        CommitmentCard(
            icon = Icons.Default.Block,
            title = stringResource(R.string.commit1_title),
            desc = stringResource(R.string.commit1_desc)
        )
        Spacer(modifier = Modifier.height(14.dp))
        CommitmentCard(
            icon = Icons.Default.Storage,
            title = stringResource(R.string.commit2_title),
            desc = stringResource(R.string.commit2_desc)
        )
        Spacer(modifier = Modifier.height(14.dp))
        CommitmentCard(
            icon = Icons.Default.CloudOff,
            title = stringResource(R.string.commit3_title),
            desc = stringResource(R.string.commit3_desc)
        )
        Spacer(modifier = Modifier.height(14.dp))
        CommitmentCard(
            icon = Icons.Default.Check,
            title = stringResource(R.string.commit4_title),
            desc = stringResource(R.string.commit4_desc)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CommitmentCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                .padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RacingOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RacingOrange,
                    modifier = Modifier.size(22.dp)
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
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CommitmentsScreenPreview() {
    DriveFlowTheme {
        CommitmentsScreen()
    }
}
