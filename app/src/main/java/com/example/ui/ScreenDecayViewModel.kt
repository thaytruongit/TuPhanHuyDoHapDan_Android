package com.example.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ScreenDecayRepository
import com.example.hardware.HardwareManager
import com.example.hardware.NotificationHelper
import com.example.model.ActivityRecord
import com.example.model.AgeGroup
import com.example.model.DecayLevelMode
import com.example.model.MonitoredGameTitle
import com.example.model.ParentalConfig
import com.example.model.SessionState
import com.example.model.TargetPlatform
import com.example.service.ScreenDecayBackgroundEngine
import com.example.service.ScreenDecayForegroundService
import com.example.util.CryptoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel kết nối trực tiếp với ScreenDecayBackgroundEngine và ScreenDecayForegroundService
 * giúp bộ đếm thời gian và việc giám sát 4 nhóm mục tiêu chạy nền xuyên suốt trên hệ thống
 * ngay cả khi người dùng tắt hoặc vuốt đóng ứng dụng.
 */
class ScreenDecayViewModel(
    private val repository: ScreenDecayRepository,
    val hardwareManager: HardwareManager,
    private val notificationHelper: NotificationHelper? = null
) : ViewModel() {

    companion object {
        const val ACCELERATION_THRESHOLD_MS2 = 6.5f

        fun provideFactory(
            repository: ScreenDecayRepository,
            hardwareManager: HardwareManager,
            notificationHelper: NotificationHelper? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScreenDecayViewModel(repository, hardwareManager, notificationHelper) as T
            }
        }
    }

    val config: StateFlow<ParentalConfig> = repository.config
    val activityLogs: StateFlow<List<ActivityRecord>> = repository.activityLogs

    // Liên kết trực tiếp với StateFlow của Engine chạy nền cấp hệ thống
    val sessionState: StateFlow<SessionState> = ScreenDecayBackgroundEngine.sessionState

    private val _isParentUnlocked = MutableStateFlow(false)
    val isParentUnlocked: StateFlow<Boolean> = _isParentUnlocked.asStateFlow()

    val currentDeltaA: StateFlow<Float> = hardwareManager.currentDeltaA
    val stretchReps: StateFlow<Int> = hardwareManager.repCount

    private var attachedActivity: Activity? = null

    init {
        updateTargetMinutesForCurrentGroup()

        // Đồng bộ độ sáng cửa sổ khi Activity đang mở
        ScreenDecayBackgroundEngine.onBrightnessChangedCallback = { brightness ->
            hardwareManager.applyWindowBrightness(attachedActivity, brightness)
        }

        // Đồng bộ số nhịp vươn vai từ cảm biến gia tốc (Δa > 6.5 m/s²)
        viewModelScope.launch {
            hardwareManager.repCount.collect { reps ->
                val current = sessionState.value
                ScreenDecayBackgroundEngine.updateState { it.copy(stretchRepsDone = reps) }
                if (reps >= current.targetStretchReps && current.isMovementStationActive) {
                    onMovementStationCompleted()
                }
            }
        }

        // Đồng bộ biến thiên gia tốc tức thời vào SessionState
        viewModelScope.launch {
            hardwareManager.currentDeltaA.collect { delta ->
                ScreenDecayBackgroundEngine.updateState { it.copy(currentAccelerationDelta = delta) }
            }
        }
    }

    fun attachActivity(activity: Activity) {
        attachedActivity = activity
        ScreenDecayBackgroundEngine.ensureInitialized(activity.applicationContext)
        if (sessionState.value.isBackgroundMonitorActive) {
            ScreenDecayForegroundService.startOrUpdateService(activity.applicationContext)
        }
        notificationHelper?.updateBackgroundMonitorNotification(sessionState.value)
    }

    /**
     * Nhận dữ liệu cảm biến gia tốc từ Activity khi có thay đổi gia tốc,
     * kiểm tra vượt ngưỡng 6.5 m/s² để hỗ trợ cơ chế 'Trạm vận động' cho nhóm 7-9 tuổi.
     */
    fun onAccelerometerChanged(deltaA: Float) {
        val current = sessionState.value
        val threshold = config.value.accelerometerThreshold.takeIf { it > 0f } ?: ACCELERATION_THRESHOLD_MS2
        hardwareManager.processAccelerationDelta(
            delta = deltaA,
            threshold = threshold,
            forceCount = current.isMovementStationActive
        )
    }

    fun setAgeGroup(group: AgeGroup) {
        ScreenDecayBackgroundEngine.pauseTimer()
        hardwareManager.stopLullaby()
        hardwareManager.stopListeningAccelerometer()

        val targetMin = when (group) {
            AgeGroup.TODDLER -> config.value.toddlerMinutes
            AgeGroup.PRIMARY -> config.value.primaryMinutes
            AgeGroup.MIDDLE_SCHOOL -> config.value.middleSchoolMinutes
        }

        ScreenDecayBackgroundEngine.updateState { current ->
            current.copy(
                ageGroup = group,
                isRunning = false,
                elapsedSeconds = 0f,
                targetMinutes = targetMin,
                brightnessPercent = 100,
                isBedtimeOverlayVisible = false,
                isMovementStationActive = false,
                isReflectionActive = false,
                reflectionCompleted = false,
                stretchRepsDone = 0,
                lastNotifiedLevel = 0
            )
        }
        hardwareManager.applyWindowBrightness(attachedActivity, 100)
    }

    private fun updateTargetMinutesForCurrentGroup() {
        val currentGroup = sessionState.value.ageGroup
        val targetMin = when (currentGroup) {
            AgeGroup.TODDLER -> config.value.toddlerMinutes
            AgeGroup.PRIMARY -> config.value.primaryMinutes
            AgeGroup.MIDDLE_SCHOOL -> config.value.middleSchoolMinutes
        }
        ScreenDecayBackgroundEngine.updateState {
            it.copy(
                targetMinutes = targetMin,
                targetStretchReps = config.value.requiredStretchReps
            )
        }
    }

    fun toggleTargetPlatform(platform: TargetPlatform) {
        ScreenDecayBackgroundEngine.updateState { current ->
            val newSet = current.enabledTargets.toMutableSet()
            val newGames = current.enabledGames.toMutableSet()
            if (newSet.contains(platform)) {
                if (newSet.size > 1) {
                    newSet.remove(platform)
                    if (platform == TargetPlatform.WEB_GAME) {
                        newGames.clear()
                    }
                }
            } else {
                newSet.add(platform)
                if (platform == TargetPlatform.WEB_GAME && newGames.isEmpty()) {
                    newGames.addAll(MonitoredGameTitle.values())
                }
            }
            current.copy(
                enabledTargets = newSet,
                enabledGames = newGames
            )
        }
    }

    fun toggleMonitoredGame(game: MonitoredGameTitle) {
        ScreenDecayBackgroundEngine.updateState { current ->
            val newGames = current.enabledGames.toMutableSet()
            val newTargets = current.enabledTargets.toMutableSet()
            if (newGames.contains(game)) {
                newGames.remove(game)
                if (newGames.isEmpty() && newTargets.size > 1) {
                    newTargets.remove(TargetPlatform.WEB_GAME)
                }
            } else {
                newGames.add(game)
                newTargets.add(TargetPlatform.WEB_GAME)
            }
            current.copy(
                enabledTargets = newTargets,
                enabledGames = newGames
            )
        }
    }

    fun selectPreviewPlatform(platform: TargetPlatform, isYoutubeLecture: Boolean = false) {
        ScreenDecayBackgroundEngine.updateState {
            it.copy(
                selectedPreviewPlatform = platform,
                isViewingYoutubeLecture = isYoutubeLecture
            )
        }
    }

    fun toggleExemptYoutubeEducation(exempt: Boolean) {
        ScreenDecayBackgroundEngine.updateState { it.copy(exemptYoutubeEducation = exempt) }
    }

    /**
     * Bật/tắt dịch vụ Foreground Service chạy nền trên hệ thống ngay cả khi tắt ứng dụng
     */
    fun toggleBackgroundMonitor(active: Boolean) {
        ScreenDecayBackgroundEngine.updateState { it.copy(isBackgroundMonitorActive = active) }
        val ctx = attachedActivity?.applicationContext ?: return
        if (active) {
            ScreenDecayForegroundService.startOrUpdateService(ctx)
        } else {
            ScreenDecayForegroundService.stopService(ctx)
            notificationHelper?.cancelBackgroundMonitorNotification()
        }
    }

    /**
     * Tích bật để khởi chạy toàn bộ chương trình kiểm soát & chạy nền hệ thống
     */
    fun startProgramAndBackgroundMonitor() {
        ScreenDecayBackgroundEngine.updateState { it.copy(isBackgroundMonitorActive = true) }
        attachedActivity?.applicationContext?.let { ctx ->
            ScreenDecayForegroundService.startOrUpdateService(ctx)
        }
        ScreenDecayBackgroundEngine.startTimer()
    }

    /**
     * Huỷ/dừng chạy chương trình sau khi đã xác thực đúng mật khẩu Phụ huynh
     */
    fun stopProgramAfterParentVerified() {
        ScreenDecayBackgroundEngine.pauseTimer()
        ScreenDecayBackgroundEngine.updateState {
            it.copy(
                isRunning = false,
                isBackgroundMonitorActive = false
            )
        }
        attachedActivity?.applicationContext?.let { ctx ->
            ScreenDecayForegroundService.stopService(ctx)
        }
        notificationHelper?.cancelBackgroundMonitorNotification()
    }

    fun toggleFastForward30x(enabled: Boolean) {
        val newSpeed = if (enabled) 30f else 1f
        ScreenDecayBackgroundEngine.updateState {
            it.copy(
                isFastForward30x = enabled,
                speedMultiplier = newSpeed,
                decayLevelMode = DecayLevelMode.AUTO
            )
        }
        if (enabled && !sessionState.value.isRunning) {
            startSession()
        }
    }

    fun selectDecayLevelMode(mode: DecayLevelMode) {
        val current = sessionState.value
        val forcedRatio = mode.forcedProgress
        val newElapsed = if (forcedRatio != null) {
            current.totalSeconds * forcedRatio
        } else {
            current.elapsedSeconds
        }
        val effectiveProgress = forcedRatio ?: current.progress
        val newBrightness = if (current.ageGroup == AgeGroup.TODDLER) {
            (100 - (32 * effectiveProgress)).toInt().coerceIn(68, 100)
        } else {
            100
        }

        ScreenDecayBackgroundEngine.updateState {
            it.copy(
                decayLevelMode = mode,
                elapsedSeconds = newElapsed,
                brightnessPercent = newBrightness
            )
        }
        hardwareManager.applyWindowBrightness(attachedActivity, newBrightness)

        when (mode) {
            DecayLevelMode.LEVEL_1, DecayLevelMode.LEVEL_2, DecayLevelMode.LEVEL_3 -> {
                pauseSession()
                val remMin = (sessionState.value.remainingSeconds / 60).coerceAtLeast(1)
                notificationHelper?.sendDecayStageNotification(
                    ageGroup = sessionState.value.ageGroup,
                    level = mode.levelNumber,
                    remainingMinutes = remMin
                )
            }
            DecayLevelMode.LEVEL_4 -> {
                pauseSession()
                ScreenDecayBackgroundEngine.triggerAgeLimitReached(sessionState.value.ageGroup)
            }
            DecayLevelMode.LEVEL_0 -> {
                pauseSession()
                hardwareManager.stopLullaby()
            }
            DecayLevelMode.AUTO -> {}
        }
    }

    fun toggleRunning() {
        ScreenDecayBackgroundEngine.toggleTimer()
    }

    fun startSession() {
        ScreenDecayBackgroundEngine.startTimer()
    }

    fun pauseSession() {
        ScreenDecayBackgroundEngine.pauseTimer()
    }

    fun resetSession() {
        ScreenDecayBackgroundEngine.pauseTimer()
        hardwareManager.stopListeningAccelerometer()
        hardwareManager.stopLullaby()
        hardwareManager.resetRepCount()
        ScreenDecayBackgroundEngine.updateState {
            it.copy(
                decayLevelMode = DecayLevelMode.AUTO,
                elapsedSeconds = 0f,
                isBedtimeOverlayVisible = false,
                isMovementStationActive = false,
                isReflectionActive = false,
                reflectionCompleted = false,
                stretchRepsDone = 0,
                brightnessPercent = 100,
                lastNotifiedLevel = 0
            )
        }
        hardwareManager.applyWindowBrightness(attachedActivity, 100)
    }

    fun setSpeedMultiplier(multiplier: Float) {
        ScreenDecayBackgroundEngine.updateState {
            it.copy(
                speedMultiplier = multiplier,
                isFastForward30x = (multiplier == 30f)
            )
        }
    }

    fun sendTestNotificationNow() {
        val current = sessionState.value
        notificationHelper?.sendAgeLimitReachedNotification(current.ageGroup, current.targetMinutes)
        notificationHelper?.updateBackgroundMonitorNotification(current)
    }

    private fun onMovementStationCompleted() {
        hardwareManager.stopListeningAccelerometer()
        hardwareManager.playVictoryFanfare()
        ScreenDecayBackgroundEngine.updateState {
            it.copy(isMovementStationActive = false)
        }
        repository.logSession(
            ActivityRecord(
                ageGroupName = "Nhóm 2 (7-9t)",
                minutesSpent = sessionState.value.targetMinutes,
                completedSuccessfully = true,
                notes = "Đã hoàn thành xuất sắc 10 nhịp vươn vai qua cảm biến gia tốc (Δa > 6.5 m/s²)."
            )
        )
    }

    fun activateMovementStationDirectly() {
        setAgeGroup(AgeGroup.PRIMARY)
        ScreenDecayBackgroundEngine.triggerAgeLimitReached(AgeGroup.PRIMARY)
        hardwareManager.resetRepCount()
        hardwareManager.startListeningAccelerometer(config.value.accelerometerThreshold)
    }

    fun activateReflectionDirectly() {
        setAgeGroup(AgeGroup.MIDDLE_SCHOOL)
        ScreenDecayBackgroundEngine.triggerAgeLimitReached(AgeGroup.MIDDLE_SCHOOL)
    }

    fun openLearningGoalSetupDialog() {
        ScreenDecayBackgroundEngine.updateState {
            it.copy(isLearningGoalSetupDialogVisible = true)
        }
    }

    fun closeLearningGoalSetupDialog() {
        ScreenDecayBackgroundEngine.updateState {
            it.copy(isLearningGoalSetupDialogVisible = false)
        }
    }

    fun saveLearningGoalAndOptionallyTest(updatedConfig: ParentalConfig, launchNow: Boolean) {
        repository.saveConfig(updatedConfig)
        updateTargetMinutesForCurrentGroup()
        ScreenDecayBackgroundEngine.updateState {
            it.copy(isLearningGoalSetupDialogVisible = false)
        }
        if (launchNow) {
            activateReflectionDirectly()
        }
    }

    fun activateBedtimeDirectly() {
        setAgeGroup(AgeGroup.TODDLER)
        ScreenDecayBackgroundEngine.triggerAgeLimitReached(AgeGroup.TODDLER)
    }

    fun dismissBedtimeOverlay() {
        hardwareManager.stopLullaby()
        ScreenDecayBackgroundEngine.updateState { it.copy(isBedtimeOverlayVisible = false) }
        hardwareManager.applyWindowBrightness(attachedActivity, 100)
    }

    fun submitReflection(inputAnswer: String): Boolean {
        val target = config.value.reflectionSentence.trim().lowercase()
        val userText = inputAnswer.trim().lowercase()
        if (userText == target || userText.length >= 20) {
            ScreenDecayBackgroundEngine.updateState {
                it.copy(
                    isReflectionActive = false,
                    reflectionCompleted = true
                )
            }
            hardwareManager.playChimeTone(523.25f, 250)
            repository.logSession(
                ActivityRecord(
                    ageGroupName = "Nhóm 3 (10-12t)",
                    minutesSpent = sessionState.value.targetMinutes,
                    completedSuccessfully = true,
                    notes = "Hoàn thành gõ mục tiêu học tập (Ma sát nhận thức): \"$inputAnswer\""
                )
            )
            return true
        }
        return false
    }

    fun unlockWithParentPin(inputPin: String): Boolean {
        val isValid = CryptoUtils.verifyPin(inputPin, config.value.pinSha256)
        if (isValid) {
            _isParentUnlocked.value = true
            ScreenDecayBackgroundEngine.updateState {
                it.copy(
                    isBedtimeOverlayVisible = false,
                    isMovementStationActive = false,
                    isReflectionActive = false
                )
            }
            hardwareManager.stopLullaby()
            hardwareManager.stopListeningAccelerometer()
            hardwareManager.applyWindowBrightness(attachedActivity, 100)
        }
        return isValid
    }

    fun lockParentDashboard() {
        _isParentUnlocked.value = false
    }

    fun updateParentalConfig(newConfig: ParentalConfig) {
        repository.saveConfig(newConfig)
        updateTargetMinutesForCurrentGroup()
    }

    fun changeParentPin(newPin: String) {
        repository.updatePin(newPin)
    }

    fun simulateManualRep() {
        val newCount = sessionState.value.stretchRepsDone + 1
        ScreenDecayBackgroundEngine.updateState { it.copy(stretchRepsDone = newCount) }
        hardwareManager.vibratePulse(80)
        hardwareManager.playChimeTone(440f + (newCount * 45f), 180)
        if (newCount >= sessionState.value.targetStretchReps) {
            onMovementStationCompleted()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Không hủy timerJob của ScreenDecayBackgroundEngine khi Activity bị hủy
        // để ứng dụng tiếp tục chạy nền trên hệ thống ngay cả khi tắt ứng dụng.
        ScreenDecayBackgroundEngine.onBrightnessChangedCallback = null
    }
}
