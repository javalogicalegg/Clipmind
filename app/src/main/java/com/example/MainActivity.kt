package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.OfflineWelcomeBackDialog
import com.example.ui.components.TopTelemetryBar
import com.example.ui.screens.ChronicleScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FactoryScreen
import com.example.ui.screens.ResearchScreen
import com.example.ui.screens.SandboxAndThemeScreen
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ClipMindTheme
import com.example.util.HapticProfile
import com.example.viewmodel.SimulationViewModel

enum class ClipDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    FACTORY("factory", "Factory", AppIcons.PrecisionManufacturing, AppIcons.PrecisionManufacturing),
    DASHBOARD("dashboard", "Telemetry", AppIcons.Analytics, AppIcons.Analytics),
    RESEARCH("research", "Research", AppIcons.Science, AppIcons.Science),
    CHRONICLE("chronicle", "Chronicle", AppIcons.Public, AppIcons.Public),
    SANDBOX_THEMES("sandbox_themes", "Lab", AppIcons.Tune, AppIcons.Tune)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SimulationViewModel = viewModel(
                factory = SimulationViewModel.provideFactory(applicationContext)
            )
            val themePreset by viewModel.themePreset.collectAsStateWithLifecycle()

            LaunchedEffect(themePreset.isDark) {
                runCatching {
                    val barStyle = if (themePreset.isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(
                        statusBarStyle = barStyle,
                        navigationBarStyle = barStyle
                    )
                }
            }

            ClipMindTheme(preset = themePreset) {
                ClipMindApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ClipMindApp(viewModel: SimulationViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val buyMultiplier by viewModel.buyMultiplier.collectAsStateWithLifecycle()
    val themePreset by viewModel.themePreset.collectAsStateWithLifecycle()
    val hapticProfile by viewModel.hapticProfile.collectAsStateWithLifecycle()
    val notation by viewModel.numberNotation.collectAsStateWithLifecycle()
    val offlineReport by viewModel.offlineReport.collectAsStateWithLifecycle()
    val telemetryHistory by viewModel.telemetryHistory.collectAsStateWithLifecycle()
    val chronicleEntries by viewModel.chronicleEntries.collectAsStateWithLifecycle()
    val sandboxSnapshots by viewModel.sandboxSnapshots.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(ClipDestination.FACTORY) }

    // Return to Factory tab when pressing system Back from secondary tabs
    BackHandler(enabled = currentTab != ClipDestination.FACTORY) {
        viewModel.hapticManager.performDetentTick(hapticProfile)
        currentTab = ClipDestination.FACTORY
    }

    // Observe Lifecycle for Foreground 10 Hz Loop, Offline Catch-Up & Auto-Save
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed()
                Lifecycle.Event.ON_STOP -> viewModel.onAppPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    offlineReport?.let { report ->
        OfflineWelcomeBackDialog(
            report = report,
            notation = notation,
            onDismiss = viewModel::dismissOfflineReport
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        ClipDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    viewModel.hapticManager.performDetentTick(hapticProfile)
                                    currentTab = dest
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        ClipDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    viewModel.hapticManager.performDetentTick(hapticProfile)
                                    currentTab = dest
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                modifier = Modifier.testTag("nav_rail_${dest.route}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    TopTelemetryBar(
                        state = state,
                        notation = notation,
                        hapticProfile = hapticProfile,
                        onToggleMode = { nextMode ->
                            viewModel.switchSimulationMode(nextMode)
                        },
                        onCycleHaptic = {
                            val nextProfile = when (hapticProfile) {
                                HapticProfile.FLAGSHIP_CRISP -> HapticProfile.SUBTLE_TICKS
                                HapticProfile.SUBTLE_TICKS -> HapticProfile.OFF
                                HapticProfile.OFF -> HapticProfile.FLAGSHIP_CRISP
                            }
                            viewModel.selectHapticProfile(nextProfile)
                        },
                        onOpenThemesTab = {
                            viewModel.hapticManager.performDetentTick(hapticProfile)
                            currentTab = ClipDestination.SANDBOX_THEMES
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val contentModifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 840.dp)
                            .fillMaxWidth()

                        when (currentTab) {
                            ClipDestination.FACTORY -> FactoryScreen(
                                state = state,
                                buyMultiplier = buyMultiplier,
                                notation = notation,
                                onMakeClip = viewModel::makePaperclipManual,
                                onAdjustPrice = viewModel::adjustClipPrice,
                                onToggleAutoPricer = viewModel::toggleAutoPricer,
                                onUpgradeMarketing = viewModel::upgradeMarketing,
                                onBuyWire = viewModel::buyWireSpool,
                                onToggleAutoWire = viewModel::toggleAutoWire,
                                onSelectBuyMultiplier = viewModel::setBuyMultiplier,
                                onBuyGenerator = viewModel::buyGenerator,
                                modifier = contentModifier
                            )

                            ClipDestination.DASHBOARD -> DashboardScreen(
                                state = state,
                                telemetryHistory = telemetryHistory,
                                notation = notation,
                                modifier = contentModifier
                            )

                            ClipDestination.RESEARCH -> ResearchScreen(
                                state = state,
                                notation = notation,
                                onAllocateTrust = viewModel::allocateTrust,
                                onQuantumComputeBurst = viewModel::quantumComputeBurst,
                                onToggleAutoResearch = viewModel::toggleAutoResearch,
                                onSelectAutoResearchRule = viewModel::setAutoResearchRule,
                                onPurchaseResearch = viewModel::purchaseResearch,
                                modifier = contentModifier
                            )

                            ClipDestination.CHRONICLE -> ChronicleScreen(
                                state = state,
                                chronicleEntries = chronicleEntries,
                                notation = notation,
                                modifier = contentModifier
                            )

                            ClipDestination.SANDBOX_THEMES -> SandboxAndThemeScreen(
                                state = state,
                                currentTheme = themePreset,
                                currentHapticProfile = hapticProfile,
                                currentNotation = notation,
                                snapshots = sandboxSnapshots,
                                onSwitchMode = viewModel::switchSimulationMode,
                                onSetTickSpeed = viewModel::setSandboxTickSpeed,
                                onTogglePause = viewModel::toggleSandboxPause,
                                onStepSingleTick = viewModel::stepSandboxSingleTick,
                                onToggleUnlimitedResources = viewModel::toggleSandboxUnlimitedResources,
                                onGrantResourceBurst = viewModel::grantSandboxResourceBurst,
                                onJumpToTier = viewModel::jumpSandboxToTier,
                                onToggleResearchInSandbox = viewModel::toggleResearchUpgradeInSandbox,
                                onCaptureSnapshot = viewModel::captureSandboxSnapshot,
                                onSelectTheme = viewModel::selectThemePreset,
                                onSelectHapticProfile = viewModel::selectHapticProfile,
                                onSelectNotation = viewModel::selectNumberNotation,
                                onResetSlot = viewModel::resetSimulationSlot,
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
