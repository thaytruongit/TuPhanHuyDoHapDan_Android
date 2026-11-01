package com.example.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScreenDecayViewModel(
    private val repository: ScreenDecayRepository,
    val hardwareManager: HardwareManager
) : ViewModel() {

    val config: StateFlow<ParentalConfig> = repository.config
    val activityLogs: StateFlow<List<ActivityRecord>> = repository.activityLogs

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _isParentUnlocked = MutableStateFlow(false)
    val isParentUnlocked: StateFlow<Boolean> = _isParentUnlocked.asStateFlow()

    val currentDeltaA = hardwareManager.currentDeltaA
    val stretchReps = hardwareManager.repCount

    private var timerJob: Job? = null
    private var attachedActivity: Activity? = null

    init {
        // Apply initial target minutes from config
        updateTargetMinutesForCurrentGroup()

        // Sync stretch reps from hardware manager
        viewModelScope.launch {
            hardwareManager.repCount.collect { reps ->
                val current = _sessionState.value
                _sessionState.value = current.copy(stretchRepsDone = reps)
                if (reps >= current.targetStretchReps && current.isMovementStationActive) {
                    onMovementStationCompleted()
                }
            }
        }
    }

    fun attachActivity(activity: Activity) {
        attachedActivity = activity
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
            // Already finished, restart
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

                // Compute real-time decay parameters
                val progress = (newElapsed / current.totalSeconds).coerceIn(0f, 1f)

                // For Toddler: brightness drops gradually from 100% to 68% at minute 15
                val newBrightness = if (current.ageGroup == AgeGroup.TODDLER) {
                    (100 - (32 * progress)).toInt().coerceIn(68, 100)
                } else {
                    100
                }

                hardwareManager.applyWindowBrightness(attachedActivity, newBrightness)

                if (newElapsed >= current.totalSeconds) {
                    // Reached the target minute! Trigger the age-specific mechanism
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

    private fun onSessionLimitReached(group: AgeGroup) {
        val current = _sessionState.value
        when (group) {
            AgeGroup.TODDLER -> {
                // Nhóm 1: Cơ chế Bạn Gấu đi ngủ, Sepia, sáng 68%, âm lượng 12%, dừng ở phút 15
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
                // Nhóm 2: Cơ chế Hạ nhiệt thị giác & Trạm vận động (Δa > 6.5 m/s² ở phút 25)
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
                // Nhóm 3: Cơ chế Ma sát nhận thức & Phản tư (Thang độ xám 100%, trễ cuộn 900ms, phản tư ở phút 30)
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
            // Also dismiss any blocking dialog if parent overrides
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

    // Manual increment for testing or when accelerometer sensor is simulated on non-physical device
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
