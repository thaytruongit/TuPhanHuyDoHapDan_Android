package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.data.ScreenDecayRepository
import com.example.hardware.HardwareManager
import com.example.hardware.NotificationHelper
import com.example.model.ActivityRecord
import com.example.model.AgeGroup
import com.example.model.DecayLevelMode
import com.example.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Engine cấp hệ thống duy trì trạng thái và bộ đếm thời gian sử dụng màn hình độc lập với vòng đời Activity.
 * Ngay cả khi người dùng tắt ứng dụng (vuốt khỏi đa nhiệm), bộ đếm và tiến trình giám sát 4 nhóm mục tiêu
 * vẫn tiếp tục hoạt động thông qua ScreenDecayForegroundService.
 */
object ScreenDecayBackgroundEngine {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var appContext: Context? = null
    private var repository: ScreenDecayRepository? = null
    private var hardwareManager: HardwareManager? = null
    private var notificationHelper: NotificationHelper? = null

    var onBrightnessChangedCallback: ((Int) -> Unit)? = null

    fun ensureInitialized(context: Context) {
        if (appContext == null) {
            val app = context.applicationContext
            appContext = app
            repository = ScreenDecayRepository(app)
            hardwareManager = HardwareManager(app)
            notificationHelper = NotificationHelper(app)

            val cfg = repository!!.config.value
            val group = _sessionState.value.ageGroup
            val targetMin = when (group) {
                AgeGroup.TODDLER -> cfg.toddlerMinutes
                AgeGroup.PRIMARY -> cfg.primaryMinutes
                AgeGroup.MIDDLE_SCHOOL -> cfg.middleSchoolMinutes
            }
            _sessionState.value = _sessionState.value.copy(
                targetMinutes = targetMin,
                targetStretchReps = cfg.requiredStretchReps
            )
        }
    }

    fun updateState(transform: (SessionState) -> SessionState) {
        val updated = transform(_sessionState.value)
        _sessionState.value = updated
        notificationHelper?.updateBackgroundMonitorNotification(updated)
    }

    fun startTimer() {
        val current = _sessionState.value
        if (current.progress >= 1f || current.decayLevelMode == DecayLevelMode.LEVEL_4) {
            _sessionState.value = current.copy(
                decayLevelMode = DecayLevelMode.AUTO,
                elapsedSeconds = 0f,
                isBedtimeOverlayVisible = false,
                isMovementStationActive = false,
                isReflectionActive = false,
                reflectionCompleted = false,
                stretchRepsDone = 0,
                lastNotifiedLevel = 0
            )
            hardwareManager?.resetRepCount()
        } else if (current.decayLevelMode != DecayLevelMode.AUTO) {
            _sessionState.value = current.copy(decayLevelMode = DecayLevelMode.AUTO)
        }

        _sessionState.value = _sessionState.value.copy(isRunning = true)
        appContext?.let { ScreenDecayForegroundService.startOrUpdateService(it) }

        timerJob?.cancel()
        timerJob = engineScope.launch {
            val stepSeconds = 0.2f
            while (isActive && _sessionState.value.isRunning) {
                delay((stepSeconds * 1000).toLong())
                val state = _sessionState.value
                val speed = state.speedMultiplier
                val newElapsed = state.elapsedSeconds + (stepSeconds * speed)
                val progress = (newElapsed / state.totalSeconds).coerceIn(0f, 1f)

                val newBrightness = if (state.ageGroup == AgeGroup.TODDLER) {
                    (100 - (32 * progress)).toInt().coerceIn(68, 100)
                } else {
                    100
                }
                onBrightnessChangedCallback?.invoke(newBrightness)

                if (newElapsed >= state.totalSeconds) {
                    triggerAgeLimitReached(state.ageGroup)
                    break
                } else {
                    val tempState = state.copy(
                        elapsedSeconds = newElapsed,
                        brightnessPercent = newBrightness
                    )
                    val levelNow = tempState.currentDecayLevel
                    val notifiedLevel = if (levelNow > state.lastNotifiedLevel && levelNow in 1..3) {
                        val remMin = (tempState.remainingSeconds / 60).coerceAtLeast(1)
                        notificationHelper?.sendDecayStageNotification(
                            ageGroup = state.ageGroup,
                            level = levelNow,
                            remainingMinutes = remMin
                        )
                        levelNow
                    } else {
                        state.lastNotifiedLevel
                    }

                    val finalState = tempState.copy(lastNotifiedLevel = notifiedLevel)
                    _sessionState.value = finalState
                    notificationHelper?.updateBackgroundMonitorNotification(finalState)
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        val updated = _sessionState.value.copy(isRunning = false)
        _sessionState.value = updated
        hardwareManager?.stopLullaby()
        notificationHelper?.updateBackgroundMonitorNotification(updated)
    }

    fun toggleTimer() {
        if (_sessionState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun cycleDemoLevelFromNotification() {
        val current = _sessionState.value
        val nextMode = when (current.currentDecayLevel) {
            0 -> DecayLevelMode.LEVEL_1
            1 -> DecayLevelMode.LEVEL_2
            2 -> DecayLevelMode.LEVEL_3
            3 -> DecayLevelMode.LEVEL_4
            else -> DecayLevelMode.LEVEL_0
        }
        val ratio = nextMode.forcedProgress ?: 0f
        val newElapsed = current.totalSeconds * ratio
        val updated = current.copy(
            isRunning = false,
            decayLevelMode = nextMode,
            elapsedSeconds = newElapsed
        )
        _sessionState.value = updated
        notificationHelper?.updateBackgroundMonitorNotification(updated)

        if (nextMode == DecayLevelMode.LEVEL_4) {
            triggerAgeLimitReached(updated.ageGroup)
        } else if (nextMode.levelNumber in 1..3) {
            val remMin = (updated.remainingSeconds / 60).coerceAtLeast(1)
            notificationHelper?.sendDecayStageNotification(
                ageGroup = updated.ageGroup,
                level = nextMode.levelNumber,
                remainingMinutes = remMin
            )
        }
    }

    fun triggerAgeLimitReached(group: AgeGroup) {
        timerJob?.cancel()
        val current = _sessionState.value
        notificationHelper?.sendAgeLimitReachedNotification(group, current.targetMinutes)

        when (group) {
            AgeGroup.TODDLER -> {
                val updated = current.copy(
                    isRunning = false,
                    elapsedSeconds = current.totalSeconds,
                    isBedtimeOverlayVisible = true,
                    brightnessPercent = 68,
                    lastNotifiedLevel = 4
                )
                _sessionState.value = updated
                onBrightnessChangedCallback?.invoke(68)
                notificationHelper?.updateBackgroundMonitorNotification(updated)
                hardwareManager?.vibratePulse(300)
                hardwareManager?.startLullaby()
                repository?.logSession(
                    ActivityRecord(
                        ageGroupName = "Nhóm 1 (3-6t)",
                        minutesSpent = current.targetMinutes,
                        completedSuccessfully = true,
                        notes = "Đạt giới hạn ${current.targetMinutes}p (Chạy nền): Gửi Notification & bật 'Bạn Gấu đi ngủ'."
                    )
                )
            }
            AgeGroup.PRIMARY -> {
                hardwareManager?.resetRepCount()
                val updated = current.copy(
                    isRunning = false,
                    elapsedSeconds = current.totalSeconds,
                    isMovementStationActive = true,
                    stretchRepsDone = 0,
                    lastNotifiedLevel = 4
                )
                _sessionState.value = updated
                notificationHelper?.updateBackgroundMonitorNotification(updated)
                hardwareManager?.vibratePulse(250)
                val threshold = repository?.config?.value?.accelerometerThreshold ?: 6.5f
                hardwareManager?.startListeningAccelerometer(threshold)
            }
            AgeGroup.MIDDLE_SCHOOL -> {
                val updated = current.copy(
                    isRunning = false,
                    elapsedSeconds = current.totalSeconds,
                    isReflectionActive = true,
                    reflectionCompleted = false,
                    lastNotifiedLevel = 4
                )
                _sessionState.value = updated
                notificationHelper?.updateBackgroundMonitorNotification(updated)
                hardwareManager?.vibratePulse(350)
            }
        }
    }
}

/**
 * Dịch vụ chạy nền trên hệ thống (Foreground Service với START_STICKY & stopWithTask="false").
 * Duy trì kiểm soát chặt chẽ 4 nhóm mục tiêu và đếm thời gian ngay cả khi người dùng tắt ứng dụng.
 */
class ScreenDecayForegroundService : Service() {

    companion object {
        const val ACTION_START_OR_UPDATE = "com.example.action.START_OR_UPDATE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_TOGGLE_RUN = "com.example.action.TOGGLE_RUN"
        const val ACTION_CYCLE_DEMO_LEVEL = "com.example.action.CYCLE_DEMO_LEVEL"

        fun startOrUpdateService(context: Context) {
            try {
                val intent = Intent(context, ScreenDecayForegroundService::class.java).apply {
                    action = ACTION_START_OR_UPDATE
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, ScreenDecayForegroundService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                }
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }

    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        ScreenDecayBackgroundEngine.ensureInitialized(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        promoteToForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ScreenDecayBackgroundEngine.ensureInitialized(applicationContext)

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_RUN -> {
                ScreenDecayBackgroundEngine.toggleTimer()
            }
            ACTION_CYCLE_DEMO_LEVEL -> {
                ScreenDecayBackgroundEngine.cycleDemoLevelFromNotification()
            }
        }

        promoteToForeground()
        // START_STICKY đảm bảo hệ điều hành Android tự động khởi chạy lại dịch vụ nếu bị thu hồi bộ nhớ
        return START_STICKY
    }

    private fun promoteToForeground() {
        try {
            val state = ScreenDecayBackgroundEngine.sessionState.value
            val notification = notificationHelper.buildForegroundMonitorNotification(state)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID_BG_MONITOR,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID_BG_MONITOR, notification)
            }
        } catch (_: Exception) {}
    }

    /**
     * Khi người dùng vuốt tắt ứng dụng khỏi màn hình đa nhiệm (Recents),
     * hàm onTaskRemoved giữ dịch vụ tiếp tục chạy và đặt lịch tự khôi phục tức thì.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val state = ScreenDecayBackgroundEngine.sessionState.value
        if (state.isBackgroundMonitorActive) {
            try {
                val restartIntent = Intent(applicationContext, ScreenDecayForegroundService::class.java).apply {
                    action = ACTION_START_OR_UPDATE
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_ONE_SHOT
                }
                val pendingIntent = PendingIntent.getService(
                    applicationContext,
                    99,
                    restartIntent,
                    flags
                )
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 500L,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
