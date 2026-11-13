package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.configureAppCheck
import com.example.ui.screens.CoreScreen
import com.example.ui.screens.ProtocolsScreen
import com.example.ui.screens.SystemsScreen
import com.example.ui.theme.HudDeepNavy
import com.example.ui.theme.HudObsidian
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.NovaAriseTheme
import com.example.ui.viewmodel.NovaViewModel

const val ROUTE_CORE = "core"
const val ROUTE_SYSTEMS = "systems"
const val ROUTE_PROTOCOLS = "protocols"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure App Check with launch intent before any UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        setContent {
            val viewModel: NovaViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            NovaAriseTheme(ariseOverdrive = uiState.preferences.ariseOverdrive) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NovaAriseApp(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
            }
        }
    }
}

@Composable
private fun NovaAriseApp(
    viewModel: NovaViewModel,
    uiState: com.example.ui.viewmodel.NovaUiState
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.startVoiceListening()
        }
    }

    val handleOrbOrMicTap: () -> Unit = {
        when {
            uiState.isListening -> viewModel.stopVoiceListening()
            uiState.isSpeaking -> viewModel.toggleSpeechPlayback()
            hasMicPermission -> viewModel.startVoiceListening()
            else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val destinations = remember {
        listOf(
            NavDestinationItem(
                route = ROUTE_CORE,
                labelRes = R.string.nav_core,
                selectedIcon = Icons.Filled.Bolt,
                unselectedIcon = Icons.Outlined.Bolt,
                testTag = "nav_core"
            ),
            NavDestinationItem(
                route = ROUTE_SYSTEMS,
                labelRes = R.string.nav_systems,
                selectedIcon = Icons.Filled.Memory,
                unselectedIcon = Icons.Outlined.Memory,
                testTag = "nav_systems"
            ),
            NavDestinationItem(
                route = ROUTE_PROTOCOLS,
                labelRes = R.string.nav_protocols,
                selectedIcon = Icons.Filled.Tune,
                unselectedIcon = Icons.Outlined.Tune,
                testTag = "nav_protocols"
            )
        )
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = HudObsidian,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = HudDeepNavy,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        destinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val label = stringResource(item.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = HudSurfaceElevated,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                if (useNavigationRail) {
                    NavigationRail(containerColor = HudDeepNavy) {
                        destinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val label = stringResource(item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = HudSurfaceElevated,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_CORE
                    ) {
                        composable(ROUTE_CORE) {
                            CoreScreen(
                                uiState = uiState,
                                hasMicPermission = hasMicPermission,
                                onRequestMicPermission = {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                onOrbTap = handleOrbOrMicTap,
                                onSendCommand = { viewModel.sendCommand(it) },
                                onToggleAutoSpeak = { viewModel.setAutoSpeak(it) },
                                onToggleSpeakPlayback = { viewModel.toggleSpeechPlayback() },
                                onRetryLastCommand = { viewModel.retryLastCommand() },
                                onResetSession = { viewModel.resetSession() },
                                onClearVoiceError = { viewModel.clearVoiceError() }
                            )
                        }

                        composable(ROUTE_SYSTEMS) {
                            SystemsScreen(
                                uiState = uiState,
                                onRefreshTelemetry = { viewModel.refreshTelemetry() },
                                onToggleFlashlight = { viewModel.toggleFlashlight() },
                                onToggleAriseOverdrive = { viewModel.setAriseOverdrive(it) },
                                onToggleAutoSpeak = { viewModel.setAutoSpeak(it) },
                                onRunAiAudit = { viewModel.runAiSystemAudit() },
                                onDismissBanner = { viewModel.dismissHardwareBanner() }
                            )
                        }

                        composable(ROUTE_PROTOCOLS) {
                            ProtocolsScreen(
                                uiState = uiState,
                                onSelectPersona = { viewModel.setPersonaMode(it) },
                                onToggleAutoSpeak = { viewModel.setAutoSpeak(it) },
                                onSpeechRateChange = { viewModel.setSpeechRate(it) },
                                onSpeechPitchChange = { viewModel.setSpeechPitch(it) },
                                onTestVoice = { viewModel.testVoiceGreeting() },
                                onExecuteProtocol = { protocol ->
                                    viewModel.executeProtocol(protocol)
                                    navigateToTab(ROUTE_CORE)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
