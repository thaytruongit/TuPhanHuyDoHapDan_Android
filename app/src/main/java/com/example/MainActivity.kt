package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ScreenDecayRepository
import com.example.hardware.HardwareManager
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
import kotlin.math.abs
import kotlin.math.sqrt

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

/**
 * MainActivity triển khai SensorEventListener để lắng nghe cảm biến gia tốc (Accelerometer)
 * phát hiện các thay đổi gia tốc vượt ngưỡng Δa > 6.5 m/s² nhằm hỗ trợ cơ chế 'Trạm vận động'
 * cho nhóm 7-9 tuổi, đồng thời kết nối với ScreenDecayViewModel để điều phối 3 nhóm tuổi (3-6, 7-9, 10-12).
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    companion object {
        const val ACCELERATION_THRESHOLD = 6.5f
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null

    private val repository by lazy { ScreenDecayRepository(applicationContext) }
    private val hardwareManager by lazy { HardwareManager(applicationContext) }

    private val viewModel: ScreenDecayViewModel by viewModels {
        ScreenDecayViewModel.provideFactory(repository, hardwareManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Khởi tạo dịch vụ cảm biến gia tốc (Sensor.TYPE_ACCELEROMETER)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        viewModel.attachActivity(this)

        setContent {
            MyApplicationTheme {
                MainAppContainer(
                    activity = this,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Đăng ký lắng nghe cảm biến gia tốc khi Activity hoạt động
        accelerometerSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Hủy đăng ký cảm biến khi Activity tạm dừng để tiết kiệm pin
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Tính độ lớn gia tốc tổng hợp và độ lệch so với gia tốc trọng trường chuẩn (9.81 m/s²)
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val deltaA = abs(magnitude - SensorManager.GRAVITY_EARTH)

            // Chuyển biến thiên gia tốc sang ViewModel; khi Δa > 6.5 m/s² sẽ kích hoạt đếm nhịp ở Trạm vận động (nhóm 7-9 tuổi)
            viewModel.onAccelerometerChanged(deltaA)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Không yêu cầu xử lý thay đổi độ chính xác
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    activity: ComponentActivity,
    viewModel: ScreenDecayViewModel
) {
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
                        onPlayLullabyPreview = { viewModel.activateBedtimeDirectly() },
                        onTriggerMovementStationDirectly = { viewModel.activateMovementStationDirectly() },
                        onTriggerReflectionDirectly = { viewModel.activateReflectionDirectly() }
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

            // 1. Nhóm 1 (3-6 tuổi): Cơ chế Bạn Gấu Đi Ngủ
            if (sessionState.isBedtimeOverlayVisible) {
                BearSleepDialog(
                    onDismiss = { viewModel.dismissBedtimeOverlay() },
                    onParentOverride = { showParentPinModal = true }
                )
            }

            // 2. Nhóm 2 (7-9 tuổi): Cơ chế Hạ nhiệt thị giác & Trạm vận động (Δa > 6.5 m/s²)
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

            // 3. Nhóm 3 (10-12 tuổi): Cơ chế Ma sát nhận thức & Phản tư chống dán
            if (sessionState.isReflectionActive) {
                ReflectionFrictionDialog(
                    targetSentence = config.reflectionSentence,
                    onSubmitReflection = { viewModel.submitReflection(it) },
                    onParentOverride = { showParentPinModal = true }
                )
            }

            // Xác thực Mã PIN Phụ Huynh (SHA-256)
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
