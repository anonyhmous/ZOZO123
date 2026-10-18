package com.driveflow.driver.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.driveflow.driver.R
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.data.prefs.AlertMode
import com.driveflow.driver.data.prefs.BarPosition
import com.driveflow.driver.data.prefs.HomeWay
import com.driveflow.driver.data.prefs.MarkerSize
import com.driveflow.driver.ui.screens.alerts.AlertsScreen
import com.driveflow.driver.ui.screens.areas.AreasScreen
import com.driveflow.driver.ui.screens.chat.ChatScreen
import com.driveflow.driver.ui.screens.commitments.CommitmentsScreen
import com.driveflow.driver.ui.screens.consent.ConsentScreen
import com.driveflow.driver.ui.screens.contact.ContactScreen
import com.driveflow.driver.ui.screens.diagnostics.DiagnosticsScreen
import com.driveflow.driver.ui.screens.help.HelpScreen
import com.driveflow.driver.ui.screens.home.HomeScreen
import com.driveflow.driver.ui.screens.homeway.HomeWayScreen
import com.driveflow.driver.ui.screens.rules.RulesScreen
import com.driveflow.driver.ui.screens.settings.SettingsScreen
import com.driveflow.driver.ui.screens.subscribe.SubscribeScreen
import com.driveflow.driver.ui.screens.welcome.WelcomeScreen
import com.driveflow.driver.ui.theme.GraphiteSurface
import com.driveflow.driver.ui.theme.ObsidianBackground
import com.driveflow.driver.ui.theme.RacingOrange
import com.driveflow.driver.ui.theme.TextSilverGray
import kotlinx.coroutines.launch

object Destinations {
    const val WELCOME = "welcome"
    const val CONSENT = "consent"
    const val CONTACT = "contact"
    const val HOME = "home"
    const val RULES = "rules"
    const val AREAS = "areas"
    const val ALERTS = "alerts"
    const val HOMEWAY = "homeway"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val SUBSCRIBE = "subscribe"
    const val CHAT = "chat"
    const val COMMITMENTS = "commitments"
    const val DIAGNOSTICS = "diagnostics"
    const val CALIBRATION = "calibration"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val settingsStore = AppGraph.settingsStore
    val scope = rememberCoroutineScope()

    val welcomeSeen by settingsStore.welcomeSeenFlow.collectAsState(initial = false)
    val consentVersion by settingsStore.consentVersionFlow.collectAsState(initial = 0)
    val contactName by settingsStore.contactNameFlow.collectAsState(initial = "")
    val contactPhone by settingsStore.contactPhoneFlow.collectAsState(initial = "")
    val policy by settingsStore.policyFlow.collectAsState(initial = Policy())
    val alertEnabled by settingsStore.alertEnabledFlow.collectAsState(initial = true)
    val alertMode by settingsStore.alertModeFlow.collectAsState(initial = AlertMode.TONE_VIBRATE)
    val homeWay by settingsStore.homeWayFlow.collectAsState(initial = HomeWay())
    val markerSize by settingsStore.markerSizeFlow.collectAsState(initial = MarkerSize.NORMAL)
    val barOpacity by settingsStore.barOpacityPercentFlow.collectAsState(initial = 85)
    val barPosition by settingsStore.barPositionFlow.collectAsState(initial = BarPosition(isAuto = true))
    val autoOffHours by settingsStore.autoOffHoursFlow.collectAsState(initial = 3)
    val currentLanguage by settingsStore.languageFlow.collectAsState(initial = "ar")
    val licenseToken by settingsStore.licenseTokenFlow.collectAsState(initial = null)
    val chatHistory by settingsStore.chatHistoryFlow.collectAsState(initial = emptyList())
    val diagnosticsCounters by settingsStore.diagnosticsCountersFlow.collectAsState(initial = emptyMap())
    val ocrCalibrations by settingsStore.ocrCalibrationFlow.collectAsState(initial = emptyMap())

    var readerEnabled by remember { mutableStateOf(false) }

    val startDestination = remember(welcomeSeen, consentVersion, contactName) {
        when {
            !welcomeSeen -> Destinations.WELCOME
            consentVersion < 1 -> Destinations.CONSENT
            contactName.isBlank() -> Destinations.CONTACT
            else -> Destinations.HOME
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val bottomBarRoutes = listOf(Destinations.HOME, Destinations.SETTINGS, Destinations.HELP)
    val shouldShowBottomBar = currentRoute in bottomBarRoutes

    val isProActive = settingsStore.isProActiveSync() || !licenseToken.isNullOrBlank()
    val licenseStatus = if (isProActive) LicenseStatus.ACTIVE else LicenseStatus.FREE

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = GraphiteSurface,
                    contentColor = RacingOrange,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    // Tab 1: Home
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.tab_home)) },
                        label = { Text(stringResource(R.string.tab_home)) },
                        selected = currentRoute == Destinations.HOME,
                        onClick = {
                            navController.navigate(Destinations.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianBackground,
                            selectedTextColor = RacingOrange,
                            indicatorColor = RacingOrange,
                            unselectedIconColor = TextSilverGray,
                            unselectedTextColor = TextSilverGray
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    // Tab 2: Settings
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.tab_settings)) },
                        label = { Text(stringResource(R.string.tab_settings)) },
                        selected = currentRoute == Destinations.SETTINGS,
                        onClick = {
                            navController.navigate(Destinations.SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianBackground,
                            selectedTextColor = RacingOrange,
                            indicatorColor = RacingOrange,
                            unselectedIconColor = TextSilverGray,
                            unselectedTextColor = TextSilverGray
                        ),
                        modifier = Modifier.testTag("nav_tab_settings")
                    )

                    // Tab 3: Help
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.HelpOutline, contentDescription = stringResource(R.string.tab_help)) },
                        label = { Text(stringResource(R.string.tab_help)) },
                        selected = currentRoute == Destinations.HELP,
                        onClick = {
                            navController.navigate(Destinations.HELP) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianBackground,
                            selectedTextColor = RacingOrange,
                            indicatorColor = RacingOrange,
                            unselectedIconColor = TextSilverGray,
                            unselectedTextColor = TextSilverGray
                        ),
                        modifier = Modifier.testTag("nav_tab_help")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Welcome Screen
            composable(Destinations.WELCOME) {
                WelcomeScreen(
                    onFinished = {
                        scope.launch {
                            settingsStore.setWelcomeSeen(true)
                            navController.navigate(Destinations.CONSENT) {
                                popUpTo(Destinations.WELCOME) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // 2. Consent Screen
            composable(Destinations.CONSENT) {
                ConsentScreen(
                    onAgreeAndContinue = {
                        scope.launch {
                            settingsStore.setConsentVersion(1)
                            navController.navigate(Destinations.CONTACT) {
                                popUpTo(Destinations.CONSENT) { inclusive = true }
                            }
                        }
                    },
                    onViewCommitments = {
                        navController.navigate(Destinations.COMMITMENTS)
                    }
                )
            }

            // 3. Contact Screen
            composable(Destinations.CONTACT) {
                ContactScreen(
                    initialName = contactName,
                    initialPhone = contactPhone,
                    initialGov = policy.governorate ?: "",
                    onSaveAndContinue = { name, phone, gov ->
                        scope.launch {
                            settingsStore.setContactName(name)
                            settingsStore.setContactPhone(phone)
                            settingsStore.setPolicy(policy.copy(governorate = gov))
                            settingsStore.setSetupDone(true)
                            navController.navigate(Destinations.HOME) {
                                popUpTo(Destinations.CONTACT) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // 4. Home Screen
            composable(Destinations.HOME) {
                HomeScreen(
                    policy = policy,
                    alertEnabled = alertEnabled,
                    alertMode = alertMode,
                    headingHomeEnabled = homeWay.enabled,
                    licenseStatus = licenseStatus,
                    remainingDays = 30,
                    freeListRemaining = AppGraph.freeDailyList,
                    freeDetailsRemaining = AppGraph.freeDailyDetails,
                    readerEnabled = readerEnabled,
                    onToggleReader = { readerEnabled = it },
                    onToggleAlert = { scope.launch { settingsStore.setAlertEnabled(it) } },
                    onToggleHeadingHome = { scope.launch { settingsStore.setHomeWay(homeWay.copy(enabled = it)) } },
                    onToggleAppEnabled = { app, isEnabled ->
                        val updated = policy.appEnabled.toMutableMap().apply { put(app, isEnabled) }
                        scope.launch { settingsStore.setPolicy(policy.copy(appEnabled = updated)) }
                    },
                    onNavigateToRules = { navController.navigate(Destinations.RULES) },
                    onNavigateToAlerts = { navController.navigate(Destinations.ALERTS) },
                    onNavigateToHeadingHome = { navController.navigate(Destinations.HOMEWAY) },
                    onNavigateToSubscribe = { navController.navigate(Destinations.SUBSCRIBE) }
                )
            }

            // 5. Rules Screen
            composable(Destinations.RULES) {
                RulesScreen(
                    currentPolicy = policy,
                    onSavePolicy = { newPolicy ->
                        scope.launch { settingsStore.setPolicy(newPolicy) }
                    }
                )
            }

            // 6. Areas Screen
            composable(Destinations.AREAS) {
                AreasScreen(
                    currentPolicy = policy,
                    onSaveAreas = { avoided, preferred ->
                        scope.launch {
                            settingsStore.setPolicy(
                                policy.copy(avoidedAreas = avoided, preferredAreas = preferred)
                            )
                        }
                    }
                )
            }

            // 7. Alerts Screen
            composable(Destinations.ALERTS) {
                AlertsScreen(
                    alertEnabled = alertEnabled,
                    alertMode = alertMode,
                    onToggleAlertEnabled = { scope.launch { settingsStore.setAlertEnabled(it) } },
                    onSelectAlertMode = { scope.launch { settingsStore.setAlertMode(it) } }
                )
            }

            // 8. HomeWay Screen
            composable(Destinations.HOMEWAY) {
                HomeWayScreen(
                    currentHomeWay = homeWay,
                    onSaveHomeWay = { scope.launch { settingsStore.setHomeWay(it) } }
                )
            }

            // 9. Settings Screen
            composable(Destinations.SETTINGS) {
                SettingsScreen(
                    currentMarkerSize = markerSize,
                    onSelectMarkerSize = { scope.launch { settingsStore.setMarkerSize(it) } },
                    barOpacityPercent = barOpacity,
                    onChangeBarOpacity = { scope.launch { settingsStore.setBarOpacityPercent(it) } },
                    barPosition = barPosition,
                    onResetBarPosition = { scope.launch { settingsStore.setBarPosition(BarPosition(isAuto = true)) } },
                    autoOffHours = autoOffHours,
                    onSelectAutoOffHours = { scope.launch { settingsStore.setAutoOffHours(it) } },
                    currentLanguage = currentLanguage,
                    onToggleLanguage = {
                        val newLang = if (currentLanguage == "ar") "en" else "ar"
                        scope.launch { settingsStore.setLanguage(newLang) }
                    },
                    onNavigateToSubscribe = { navController.navigate(Destinations.SUBSCRIBE) },
                    policy = policy,
                    onNavigateToRules = { navController.navigate(Destinations.RULES) },
                    onNavigateToAreas = { navController.navigate(Destinations.AREAS) },
                    onNavigateToHomeWay = { navController.navigate(Destinations.HOMEWAY) },
                    onToggleAppEnabled = { app, isEnabled ->
                        val updated = policy.appEnabled.toMutableMap().apply { put(app, isEnabled) }
                        scope.launch { settingsStore.setPolicy(policy.copy(appEnabled = updated)) }
                    }
                )
            }

            // 10. Help Screen
            composable(Destinations.HELP) {
                HelpScreen(
                    contactName = contactName,
                    contactPhone = contactPhone,
                    governorate = policy.governorate ?: "",
                    onNavigateToChat = { navController.navigate(Destinations.CHAT) },
                    onNavigateToCommitments = { navController.navigate(Destinations.COMMITMENTS) },
                    onNavigateToDiagnostics = { navController.navigate(Destinations.DIAGNOSTICS) },
                    onReplayWelcome = { navController.navigate(Destinations.WELCOME) }
                )
            }

            // 11. Subscribe Screen
            composable(Destinations.SUBSCRIBE) {
                SubscribeScreen(
                    licenseStatus = licenseStatus,
                    remainingDays = 30,
                    onNavigateToChat = { reqType ->
                        navController.navigate("${Destinations.CHAT}?reqType=$reqType")
                    }
                )
            }

            // 12. Chat Screen
            composable("${Destinations.CHAT}?reqType={reqType}") { backStackEntry ->
                val prefilledReqType = backStackEntry.arguments?.getString("reqType") ?: ""
                ChatScreen(
                    contactName = contactName,
                    contactPhone = contactPhone,
                    licenseStatus = licenseStatus,
                    chatHistory = chatHistory,
                    initialRequestType = prefilledReqType,
                    onAddChatMessage = { message ->
                        scope.launch { settingsStore.addChatMessage(message) }
                    }
                )
            }
            composable(Destinations.CHAT) {
                ChatScreen(
                    contactName = contactName,
                    contactPhone = contactPhone,
                    licenseStatus = licenseStatus,
                    chatHistory = chatHistory,
                    initialRequestType = "",
                    onAddChatMessage = { message ->
                        scope.launch { settingsStore.addChatMessage(message) }
                    }
                )
            }

            // 13. Commitments Screen
            composable(Destinations.COMMITMENTS) {
                CommitmentsScreen()
            }

            // 14. Diagnostics Screen
            composable(Destinations.DIAGNOSTICS) {
                DiagnosticsScreen(
                    diagnosticCounters = diagnosticsCounters,
                    ocrCalibrations = ocrCalibrations,
                    onUpdateCalibration = { app, cal ->
                        scope.launch { settingsStore.setOcrCalibration(app, cal) }
                    },
                    onNavigateToCalibration = {
                        navController.navigate(Destinations.CALIBRATION)
                    },
                    onReportCounters = { message ->
                        scope.launch {
                            settingsStore.addChatMessage(message)
                        }
                        navController.navigate(Destinations.CHAT)
                    }
                )
            }

            // 15. Calibration Screen
            composable(Destinations.CALIBRATION) {
                com.driveflow.driver.ui.screens.calibration.CalibrationScreen(
                    calibrationManager = AppGraph.ocrCalibrationManager,
                    onTakeLiveCapture = { null }
                )
            }
        }
    }
}
