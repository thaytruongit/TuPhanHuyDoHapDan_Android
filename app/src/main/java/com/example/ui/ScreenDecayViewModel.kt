package com.example.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ScreenDecayRepository
import com.example.hardware.HardwareManager
import com.example.model.ActivityRecord
import com.example.model.AgeGroup
import com.example.model.ParentalConfig
import com.example.model.SessionState
import com.example.util.CryptoUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel theo dõi thời gian sử dụng màn hình của người dùng và kích hoạt
 * các trạng thái logic tự phân hủy độ hấp dẫn cho từng nhóm độ tuổi:
 * - Nhóm 1 (3-6 tuổi): Trình duyệt buồn ngủ – Bạn Gấu đi ngủ (Sepia, sáng 68%, âm lượng 12%, dừng ở phút 15)
 * - Nhóm 2 (7-9 tuổi): Hạ nhiệt thị giác & Trạm vận động (Làm mờ gợi ý, đếm 10 nhịp vươn vai khi Δa > 6.5 m/s² ở phút 25)
 * - Nhóm 3 (10-12 tuổi): Ma sát nhận thức & Phản tư (Thang độ xám 100%, trễ cuộn 900ms, SHA-256 & gõ phản tư chống dán ở phút 30)
 */
class ScreenDecayViewModel(
    private val repository: ScreenDecayRepository,
    val hardwareManager: HardwareManager
) : ViewModel() {

    companion object {
        const val ACCELERATION_THRESHOLD_MS2 = 6.5f

        fun provideFactory(
            repository: ScreenDecayRepository,
            hardwareManager: HardwareManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScreenDecayViewModel(repository, hardwareManager) as T
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

        _sessionState.value = SessionState(
            ageGroup = group,
            isRunning = false,
            elapsedSeconds = 0f,
            targetMinutes = targetMin,
            speedMultiplier = _sessionState.value.speedMultiplier,
            brightnessPercent = 100,
            targetStretchReps = config.value.requiredStretchReps
        )
        hardwareManager.applyWindowBrightness(attachedActivity, 100)
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

    fun toggleRunning() {
        if (_sessionState.value.isRunning) {
            pauseSession()
        } else {
            startSession()
        }
    }

    fun startSession() {
        if (_sessionState.value.progress >= 1f) {
            _sessionState.value = _sessionState.value.copy(
                elapsedSeconds = 0f,
                isBedtimeOverlayVisible = false,
                isMovementStationActive = false,
                isReflectionActive = false,
                reflectionCompleted = false,
                stretchRepsDone = 0
            )
            hardwareManager.resetRepCount()
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

                // Nhóm 1 (3-6 tuổi): Độ sáng giảm dần từ 100% về 68% ở phút 15
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
                    _sessionState.value = current.copy(
                        elapsedSeconds = newElapsed,
                        brightnessPercent = newBrightness
                    )
                }
            }
        }
    }

    fun pauseSession() {
        _sessionState.value = _sessionState.value.copy(isRunning = false)
        timerJob?.cancel()
        hardwareManager.stopLullaby()
    }

    fun resetSession() {
        pauseSession()
        hardwareManager.stopListeningAccelerometer()
        hardwareManager.stopLullaby()
        hardwareManager.resetRepCount()
        _sessionState.value = _sessionState.value.copy(
            elapsedSeconds = 0f,
            isBedtimeOverlayVisible = false,
            isMovementStationActive = false,
            isReflectionActive = false,
            reflectionCompleted = false,
            stretchRepsDone = 0,
            brightnessPercent = 100
        )
        hardwareManager.applyWindowBrightness(attachedActivity, 100)
    }

    fun setSpeedMultiplier(multiplier: Float) {
        _sessionState.value = _sessionState.value.copy(speedMultiplier = multiplier)
    }

    /**
     * Kích hoạt trạng thái logic đặc thù cho từng nhóm độ tuổi (3-6, 7-9, 10-12) khi đạt ngưỡng thời gian
     */
    private fun onSessionLimitReached(group: AgeGroup) {
        val current = _sessionState.value
        when (group) {
            AgeGroup.TODDLER -> {
                // Nhóm 1 (3-6 tuổi): Cơ chế Bạn Gấu đi ngủ, Sepia, sáng 68%, âm lượng 12%, dừng ở phút 15
                _sessionState.value = current.copy(
                    isRunning = false,
                    elapsedSeconds = current.totalSeconds,
                    isBedtimeOverlayVisible = true,
                    brightnessPercent = 68
                )
                hardwareManager.vibratePulse(300)
                hardwareManager.startLullaby()
                repository.logSession(
                    ActivityRecord(
                        ageGroupName = "Nhóm 1 (3-6t)",
                        minutesSpent = current.targetMinutes,
                        completedSuccessfully = true,
                        notes = "Cơ chế 'Bạn Gấu đi ngủ' tự động dừng sau ${current.targetMinutes} phút."
                    )
                )
            }
            AgeGroup.PRIMARY -> {
                // Nhóm 2 (7-9 tuổi): Cơ chế Hạ nhiệt thị giác & Trạm vận động (Δa > 6.5 m/s² ở phút 25)
                hardwareManager.resetRepCount()
                _sessionState.value = current.copy(
                    isRunning = false,
                    elapsedSeconds = current.totalSeconds,
                    isMovementStationActive = true,
                    stretchRepsDone = 0
                )
                hardwareManager.vibratePulse(250)
                hardwareManager.startListeningAccelerometer(config.value.accelerometerThreshold)
            }
            AgeGroup.MIDDLE_SCHOOL -> {
                // Nhóm 3 (10-12 tuổi): Cơ chế Ma sát nhận thức & Phản tư (Thang độ xám 100%, trễ cuộn 900ms, phản tư ở phút 30)
                _sessionState.value = current.copy(
                    isRunning = false,
                    elapsedSeconds = current.totalSeconds,
                    isReflectionActive = true,
                    reflectionCompleted = false
                )
                hardwareManager.vibratePulse(350)
            }
        }
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
                    notes = "Hoàn thành phản tư ma sát nhận thức: \"$inputAnswer\""
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
