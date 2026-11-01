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
import com.example.model.ParentalConfig
import com.example.model.SessionState
import com.example.model.TargetPlatform
import com.example.util.CryptoUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel theo dõi thời gian sử dụng màn hình, điều khiển hệ thống thông báo Local (Notification),
 * giám sát chạy nền 4 nhóm mục tiêu (YouTube & Shorts, TikTok Web, Facebook & Reels, Web Game),
 * và điều phối 4 cấp độ suy giảm (Mức 0 -> Mức 4) cho ban giám khảo KHKT.
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

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _isParentUnlocked = MutableStateFlow(false)
    val isParentUnlocked: StateFlow<Boolean> = _isParentUnlocked.asStateFlow()

    val currentDeltaA: StateFlow<Float> = hardwareManager.currentDeltaA
    val stretchReps: StateFlow<Int> = hardwareManager.repCount

    private var timerJob: Job? = null
    private var attachedActivity: Activity? = null

    init {
        updateTargetMinutesForCurrentGroup()

        // Đồng bộ số nhịp vươn vai từ cảm biến gia tốc (Δa > 6.5 m/s²)
        viewModelScope.launch {
            hardwareManager.repCount.collect { reps ->
                val current = _sessionState.value
                _sessionState.value = current.copy(stretchRepsDone = reps)
                if (reps >= current.targetStretchReps && current.isMovementStationActive) {
                    onMovementStationCompleted()
                }
            }
        }

        // Đồng bộ biến thiên gia tốc tức thời vào SessionState
        viewModelScope.launch {
            hardwareManager.currentDeltaA.collect { delta ->
                _sessionState.value = _sessionState.value.copy(currentAccelerationDelta = delta)
            }
        }
    }

    fun attachActivity(activity: Activity) {
        attachedActivity = activity
        notificationHelper?.updateBackgroundMonitorNotification(_sessionState.value)
    }

    /**
     * Nhận dữ liệu cảm biến gia tốc từ Activity khi có thay đổi gia tốc,
     * kiểm tra vượt ngưỡng 6.5 m/s² để hỗ trợ cơ chế 'Trạm vận động' cho nhóm 7-9 tuổi.
     */
    fun onAccelerometerChanged(deltaA: Float) {
        val current = _sessionState.value
        val threshold = config.value.accelerometerThreshold.takeIf { it > 0f } ?: ACCELERATION_THRESHOLD_MS2
        hardwareManager.processAccelerationDelta(
            delta = deltaA,
            threshold = threshold,
            forceCount = current.isMovementStationActive
        )
    }

    fun setAgeGroup(group: AgeGroup) {
        timerJob?.cancel()
        hardwareManager.stopLullaby()
        hardwareManager.stopListeningAccelerometer()

        val targetMin = when (group) {
            AgeGroup.TODDLER -> config.value.toddlerMinutes
            AgeGroup.PRIMARY -> config.value.primaryMinutes
            AgeGroup.MIDDLE_SCHOOL -> config.value.middleSchoolMinutes
        }

        val current = _sessionState.value
        val updated = current.copy(
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
        _sessionState.value = updated
        hardwareManager.applyWindowBrightness(attachedActivity, 100)
        notificationHelper?.updateBackgroundMonitorNotification(updated)
    }

    private fun updateTargetMinutesForCurrentGroup() {
        val currentGroup = _sessionState.value.ageGroup
        val targetMin = when (currentGroup) {
            AgeGroup.TODDLER -> config.value.toddlerMinutes
            AgeGroup.PRIMARY -> config.value.primaryMinutes
            AgeGroup.MIDDLE_SCHOOL -> config.value.middleSchoolMinutes
        }
        _sessionState.value = _sessionState.value.copy(
            targetMinutes = targetMin,
            targetStretchReps = config.value.requiredStretchReps
        )
    }

    /**
     * Bật/tắt từng nhóm trong 4 nhóm mục tiêu kiểm soát (YouTube & Shorts, TikTok Web, Facebook & Reels, Web Game)
     */
    fun toggleTargetPlatform(platform: TargetPlatform) {
        val current = _sessionState.value
        val newSet = current.enabledTargets.toMutableSet()
        if (newSet.contains(platform)) {
            if (newSet.size > 1) newSet.remove(platform)
        } else {
            newSet.add(platform)
        }
        val updated = current.copy(enabledTargets = newSet)
        _sessionState.value = updated
        notificationHelper?.updateBackgroundMonitorNotification(updated)
    }

    /**
     * Chọn nền tảng hiển thị mô phỏng trực quan và chế độ bài giảng YouTube
     */
    fun selectPreviewPlatform(platform: TargetPlatform, isYoutubeLecture: Boolean = false) {
        val updated = _sessionState.value.copy(
            selectedPreviewPlatform = platform,
            isViewingYoutubeLecture = isYoutubeLecture
        )
        _sessionState.value = updated
        notificationHelper?.updateBackgroundMonitorNotification(updated)
    }

    /**
     * Bật/tắt chế độ "Miễn giảm làm xám cho bài giảng học tập YouTube"
     */
    fun toggleExemptYoutubeEducation(exempt: Boolean) {
        val updated = _sessionState.value.copy(exemptYoutubeEducation = exempt)
        _sessionState.value = updated
        notificationHelper?.updateBackgroundMonitorNotification(updated)
    }

    /**
     * Bật/tắt chạy nền kiểm soát chặt chẽ 4 nhóm mục tiêu
     */
    fun toggleBackgroundMonitor(active: Boolean) {
        val updated = _sessionState.value.copy(isBackgroundMonitorActive = active)
        _sessionState.value = updated
        notificationHelper?.updateBackgroundMonitorNotification(updated)
    }

    /**
     * Bật/tắt "⚡ Tua nhanh thời gian (1s = 30s)" trong Chế độ trình diễn giám khảo KHKT (Demo)
     */
    fun toggleFastForward30x(enabled: Boolean) {
        val newSpeed = if (enabled) 30f else 1f
        val updated = _sessionState.value.copy(
            isFastForward30x = enabled,
            speedMultiplier = newSpeed,
            decayLevelMode = DecayLevelMode.AUTO
        )
        _sessionState.value = updated
        if (enabled && !updated.isRunning) {
            startSession()
        }
    }

    /**
     * Chọn Cấp độ suy giảm (Tự động, Mức 0, Mức 1, Mức 2, Mức 3, Mức 4) cho Ban Giám Khảo KHKT
     */
    fun selectDecayLevelMode(mode: DecayLevelMode) {
        val current = _sessionState.value
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

        val updated = current.copy(
            decayLevelMode = mode,
            elapsedSeconds = newElapsed,
            brightnessPercent = newBrightness
        )
        _sessionState.value = updated
        hardwareManager.applyWindowBrightness(attachedActivity, newBrightness)
        notificationHelper?.updateBackgroundMonitorNotification(updated)

        // Gửi thông báo tương ứng khi giám khảo bấm thử Mức 1..4
        when (mode) {
            DecayLevelMode.LEVEL_1, DecayLevelMode.LEVEL_2, DecayLevelMode.LEVEL_3 -> {
                pauseSession()
                val remMin = (updated.remainingSeconds / 60).coerceAtLeast(1)
                notificationHelper?.sendDecayStageNotification(
                    ageGroup = updated.ageGroup,
                    level = mode.levelNumber,
                    remainingMinutes = remMin
                )
            }
            DecayLevelMode.LEVEL_4 -> {
                pauseSession()
                onSessionLimitReached(updated.ageGroup)
            }
            DecayLevelMode.LEVEL_0 -> {
                pauseSession()
                hardwareManager.stopLullaby()
            }
            DecayLevelMode.AUTO -> {
                // Giữ nguyên hoặc tiếp tục chạy tự động
            }
        }
    }

    fun toggleRunning() {
        if (_sessionState.value.isRunning) {
            pauseSession()
        } else {
            startSession()
        }
    }

    fun startSession() {
        val initial = _sessionState.value
        if (initial.progress >= 1f || initial.decayLevelMode == DecayLevelMode.LEVEL_4) {
            _sessionState.value = initial.copy(
                decayLevelMode = DecayLevelMode.AUTO,
                elapsedSeconds = 0f,
                isBedtimeOverlayVisible = false,
                isMovementStationActive = false,
                isReflectionActive = false,
                reflectionCompleted = false,
                stretchRepsDone = 0,
                lastNotifiedLevel = 0
            )
            hardwareManager.resetRepCount()
        } else if (initial.decayLevelMode != DecayLevelMode.AUTO) {
            _sessionState.value = initial.copy(decayLevelMode = DecayLevelMode.AUTO)
        }

        _sessionState.value = _sessionState.value.copy(isRunning = true)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val stepSeconds = 0.2f
            while (isActive && _sessionState.value.isRunning) {
                delay((stepSeconds * 1000).toLong())
                val current = _sessionState.value
                val speed = current.speedMultiplier
                val newElapsed = current.elapsedSeconds + (stepSeconds * speed)

                val progress = (newElapsed / current.totalSeconds).coerceIn(0f, 1f)

                val newBrightness = if (current.ageGroup == AgeGroup.TODDLER) {
                    (100 - (32 * progress)).toInt().coerceIn(68, 100)
                } else {
                    100
                }

                hardwareManager.applyWindowBrightness(attachedActivity, newBrightness)

                if (newElapsed >= current.totalSeconds) {
                    onSessionLimitReached(current.ageGroup)
                    break
                } else {
                    val tempState = current.copy(
                        elapsedSeconds = newElapsed,
                        brightnessPercent = newBrightness
                    )
                    val levelNow = tempState.currentDecayLevel
                    val notifiedLevel = if (levelNow > current.lastNotifiedLevel && levelNow in 1..3) {
                        val remMin = (tempState.remainingSeconds / 60).coerceAtLeast(1)
                        notificationHelper?.sendDecayStageNotification(
                            ageGroup = current.ageGroup,
                            level = levelNow,
                            remainingMinutes = remMin
                        )
                        levelNow
                    } else {
                        current.lastNotifiedLevel
                    }

                    val finalState = tempState.copy(lastNotifiedLevel = notifiedLevel)
                    _sessionState.value = finalState
                    notificationHelper?.updateBackgroundMonitorNotification(finalState)
                }
            }
        }
    }

    fun pauseSession() {
        val updated = _sessionState.value.copy(isRunning = false)
        _sessionState.value = updated
        timerJob?.cancel()
        hardwareManager.stopLullaby()
        notificationHelper?.updateBackgroundMonitorNotification(updated)
    }

    fun resetSession() {
        pauseSession()
        hardwareManager.stopListeningAccelerometer()
        hardwareManager.stopLullaby()
        hardwareManager.resetRepCount()
        val updated = _sessionState.value.copy(
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
        _sessionState.value = updated
        hardwareManager.applyWindowBrightness(attachedActivity, 100)
        notificationHelper?.updateBackgroundMonitorNotification(updated)
    }

    fun setSpeedMultiplier(multiplier: Float) {
        _sessionState.value = _sessionState.value.copy(
            speedMultiplier = multiplier,
            isFastForward30x = (multiplier == 30f)
        )
    }

    /**
     * Kích hoạt trạng thái logic đặc thù cho từng nhóm độ tuổi (3-6, 7-9, 10-12) khi đạt ngưỡng thời gian
     * và phát thông báo Local (Notification) nhắc nhở trẻ.
     */
    private fun onSessionLimitReached(group: AgeGroup) {
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
                notificationHelper?.updateBackgroundMonitorNotification(updated)
                hardwareManager.vibratePulse(300)
                hardwareManager.startLullaby()
                repository.logSession(
                    ActivityRecord(
                        ageGroupName = "Nhóm 1 (3-6t)",
                        minutesSpent = current.targetMinutes,
                        completedSuccessfully = true,
                        notes = "Đạt giới hạn ${current.targetMinutes}p: Gửi Notification & bật 'Bạn Gấu đi ngủ'."
                    )
                )
            }
            AgeGroup.PRIMARY -> {
                hardwareManager.resetRepCount()
                val updated = current.copy(
                    isRunning = false,
                    elapsedSeconds = current.totalSeconds,
                    isMovementStationActive = true,
                    stretchRepsDone = 0,
                    lastNotifiedLevel = 4
                )
                _sessionState.value = updated
                notificationHelper?.updateBackgroundMonitorNotification(updated)
                hardwareManager.vibratePulse(250)
                hardwareManager.startListeningAccelerometer(config.value.accelerometerThreshold)
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
                hardwareManager.vibratePulse(350)
            }
        }
    }

    fun sendTestNotificationNow() {
        val current = _sessionState.value
        notificationHelper?.sendAgeLimitReachedNotification(current.ageGroup, current.targetMinutes)
        notificationHelper?.updateBackgroundMonitorNotification(current)
    }

    private fun onMovementStationCompleted() {
        hardwareManager.stopListeningAccelerometer()
        hardwareManager.playVictoryFanfare()
        _sessionState.value = _sessionState.value.copy(
            isMovementStationActive = false
        )
        repository.logSession(
            ActivityRecord(
                ageGroupName = "Nhóm 2 (7-9t)",
                minutesSpent = _sessionState.value.targetMinutes,
                completedSuccessfully = true,
                notes = "Đã hoàn thành xuất sắc 10 nhịp vươn vai qua cảm biến gia tốc (Δa > 6.5 m/s²)."
            )
        )
    }

    fun activateMovementStationDirectly() {
        setAgeGroup(AgeGroup.PRIMARY)
        onSessionLimitReached(AgeGroup.PRIMARY)
    }

    fun activateReflectionDirectly() {
        setAgeGroup(AgeGroup.MIDDLE_SCHOOL)
        onSessionLimitReached(AgeGroup.MIDDLE_SCHOOL)
    }

    fun activateBedtimeDirectly() {
        setAgeGroup(AgeGroup.TODDLER)
        onSessionLimitReached(AgeGroup.TODDLER)
    }

    fun dismissBedtimeOverlay() {
        hardwareManager.stopLullaby()
        _sessionState.value = _sessionState.value.copy(isBedtimeOverlayVisible = false)
        hardwareManager.applyWindowBrightness(attachedActivity, 100)
    }

    fun submitReflection(inputAnswer: String): Boolean {
        val target = config.value.reflectionSentence.trim().lowercase()
        val userText = inputAnswer.trim().lowercase()
        if (userText == target || userText.length >= 20) {
            _sessionState.value = _sessionState.value.copy(
                isReflectionActive = false,
                reflectionCompleted = true
            )
            hardwareManager.playChimeTone(523.25f, 250)
            repository.logSession(
                ActivityRecord(
                    ageGroupName = "Nhóm 3 (10-12t)",
                    minutesSpent = _sessionState.value.targetMinutes,
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
            _sessionState.value = _sessionState.value.copy(
                isBedtimeOverlayVisible = false,
                isMovementStationActive = false,
                isReflectionActive = false
            )
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
        val newCount = _sessionState.value.stretchRepsDone + 1
        _sessionState.value = _sessionState.value.copy(stretchRepsDone = newCount)
        hardwareManager.vibratePulse(80)
        hardwareManager.playChimeTone(440f + (newCount * 45f), 180)
        if (newCount >= _sessionState.value.targetStretchReps) {
            onMovementStationCompleted()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        hardwareManager.stopLullaby()
        hardwareManager.stopListeningAccelerometer()
        hardwareManager.applyWindowBrightness(attachedActivity, 100)
    }
}
