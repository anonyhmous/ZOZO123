package com.driveflow.driver.ui.screens.welcome

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.launch

data class WelcomePage(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val iconTint: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        WelcomePage(R.string.welcome_page1_title, R.string.welcome_page1_desc, Icons.Default.Speed, RacingOrange),
        WelcomePage(R.string.welcome_page2_title, R.string.welcome_page2_desc, Icons.Default.CheckCircle, EmeraldGreen),
        WelcomePage(R.string.welcome_page3_title, R.string.welcome_page3_desc, Icons.Default.Calculate, RacingOrange),
        WelcomePage(R.string.welcome_page4_title, R.string.welcome_page4_desc, Icons.Default.NotificationsActive, EmeraldGreen),
        WelcomePage(R.string.welcome_page5_title, R.string.welcome_page5_desc, Icons.Default.Navigation, RacingOrange),
        WelcomePage(R.string.welcome_page6_title, R.string.welcome_page6_desc, Icons.Default.CreditCard, EmeraldGreen)
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(24.dp)
            .testTag("welcome_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(
                    onClick = onFinished,
                    modifier = Modifier.testTag("welcome_skip_button")
                ) {
                    Text(
                        text = stringResource(R.string.btn_skip),
                        color = TextSilverGray,
                        fontSize = 15.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val page = pages[pageIndex]
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(page.iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = page.iconTint,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = stringResource(page.titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextBrightWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(page.descRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSilverGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Indicator Dots
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) RacingOrange else TextSilverGray.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons: Next or Get Started
        if (pagerState.currentPage < pages.size - 1) {
            Button(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("welcome_next_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RacingOrange,
                    contentColor = ObsidianBackground
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_next),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            Button(
                onClick = onFinished,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("welcome_get_started_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen,
                    contentColor = ObsidianBackground
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_get_started),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    DriveFlowTheme {
        WelcomeScreen(onFinished = {})
    }
}
