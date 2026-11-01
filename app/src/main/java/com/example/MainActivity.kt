package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ScreenDecayRepository
import com.example.hardware.HardwareManager
import com.example.model.AgeGroup
import com.example.ui.ScreenDecayViewModel
import com.example.ui.components.BearSleepDialog
import com.example.ui.components.MovementStationDialog
import com.example.ui.components.ParentPinDialog
import com.example.ui.components.ReflectionFrictionDialog
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.ScienceScreen
import com.example.ui.screens.SensorLabScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.theme.MyApplicationTheme

enum class MainTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    SIMULATOR("Trải Nghiệm", Icons.Filled.HourglassBottom, Icons.Outlined.HourglassBottom, "tab_simulator"),
    SENSOR_LAB("Trạm VĐ", Icons.Filled.DirectionsRun, Icons.Outlined.DirectionsRun, "tab_sensor_lab"),
    SCIENCE("Khoa Học", Icons.Filled.Science, Icons.Outlined.Science, "tab_science"),
    PARENT("Phụ Huynh", Icons.Filled.Security, Icons.Outlined.Security, "tab_parent")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContainer(activity = this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(activity: ComponentActivity) {
    val context = LocalContext.current
    val repository = remember { ScreenDecayRepository(context) }
    val hardwareManager = remember { HardwareManager(context) }
    val viewModel = remember { ScreenDecayViewModel(repository, hardwareManager) }

    LaunchedEffect(activity) {
        viewModel.attachActivity(activity)
    }

    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val isParentUnlocked by viewModel.isParentUnlocked.collectAsStateWithLifecycle()
    val deltaA by viewModel.currentDeltaA.collectAsStateWithLifecycle()
    val stretchReps by viewModel.stretchReps.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.SIMULATOR) }
    var showParentPinModal by remember { mutableStateOf(false) }

    BackHandler(enabled = currentTab != MainTab.SIMULATOR) {
        currentTab = MainTab.SIMULATOR
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Hạ Nhiệt Màn Hình",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = sessionState.ageGroup.ageRange,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                MainTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.SIMULATOR -> {
                    SimulatorScreen(
                        sessionState = sessionState,
                        onSelectAgeGroup = { viewModel.setAgeGroup(it) },
                        onToggleRunning = { viewModel.toggleRunning() },
                        onReset = { viewModel.resetSession() },
                        onSetSpeed = { viewModel.setSpeedMultiplier(it) },
                        onPlayLullabyPreview = { viewModel.startSession() },
                        onTriggerMovementStationDirectly = {
                            viewModel.hardwareManager.startListeningAccelerometer(config.accelerometerThreshold)
                            // Switch to primary and simulate completion
                            viewModel.setAgeGroup(AgeGroup.PRIMARY)
                            viewModel.startSession()
                        },
                        onTriggerReflectionDirectly = {
                            viewModel.setAgeGroup(AgeGroup.MIDDLE_SCHOOL)
                            viewModel.startSession()
                        }
                    )
                }
                MainTab.SENSOR_LAB -> {
                    SensorLabScreen(
                        hardwareManager = viewModel.hardwareManager,
                        threshold = config.accelerometerThreshold
                    )
                }
                MainTab.SCIENCE -> {
                    ScienceScreen()
                }
                MainTab.PARENT -> {
                    ParentDashboardScreen(
                        isUnlocked = isParentUnlocked,
                        config = config,
                        logs = activityLogs,
                        onVerifyPin = { viewModel.unlockWithParentPin(it) },
                        onLock = { viewModel.lockParentDashboard() },
                        onSaveConfig = { viewModel.updateParentalConfig(it) },
                        onChangePin = { viewModel.changeParentPin(it) }
                    )
                }
            }

            // MODALS & SCIENTIFIC MECHANISM DIALOGS

            // 1. Nhóm 1: Cơ chế Bạn Gấu Đi Ngủ (Mầm non 3-6)
            if (sessionState.isBedtimeOverlayVisible) {
                BearSleepDialog(
                    onDismiss = { viewModel.dismissBedtimeOverlay() },
                    onParentOverride = { showParentPinModal = true }
                )
            }

            // 2. Nhóm 2: Cơ chế Hạ nhiệt thị giác & Trạm vận động (Tiểu học 7-9)
            if (sessionState.isMovementStationActive) {
                MovementStationDialog(
                    currentDeltaA = deltaA,
                    threshold = config.accelerometerThreshold,
                    repsDone = stretchReps,
                    targetReps = sessionState.targetStretchReps,
                    onSimulateRep = { viewModel.simulateManualRep() },
                    onParentOverride = { showParentPinModal = true }
                )
            }

            // 3. Nhóm 3: Cơ chế Ma sát nhận thức & Phản tư chống dán (Lớp 5-7 10-12)
            if (sessionState.isReflectionActive) {
                ReflectionFrictionDialog(
                    targetSentence = config.reflectionSentence,
                    onSubmitReflection = { viewModel.submitReflection(it) },
                    onParentOverride = { showParentPinModal = true }
                )
            }

            // Parental PIN Override Dialog
            if (showParentPinModal) {
                ParentPinDialog(
                    onDismiss = { showParentPinModal = false },
                    onVerifyPin = { viewModel.unlockWithParentPin(it) },
                    onSuccess = { showParentPinModal = false }
                )
            }
        }
    }
}
