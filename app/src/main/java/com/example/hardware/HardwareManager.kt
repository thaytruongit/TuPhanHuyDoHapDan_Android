package com.example.hardware

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class HardwareManager(private val context: Context) : SensorEventListener {

    companion object {
        const val DEFAULT_ACCELERATION_THRESHOLD = 6.5f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _currentDeltaA = MutableStateFlow(0f)
    val currentDeltaA: StateFlow<Float> = _currentDeltaA.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private var targetThreshold: Float = DEFAULT_ACCELERATION_THRESHOLD
    private var isListening: Boolean = false
    private var isCountingActive: Boolean = false
    private var isAboveThreshold: Boolean = false
    private var lastRepTimestamp: Long = 0L

    private val scope = CoroutineScope(Dispatchers.Default)
    private var lullabyJob: Job? = null

    fun startListeningAccelerometer(
        threshold: Float = DEFAULT_ACCELERATION_THRESHOLD,
        onRepDetected: ((Int) -> Unit)? = null
    ) {
        targetThreshold = threshold
        _repCount.value = 0
        isAboveThreshold = false
        lastRepTimestamp = 0L
        isCountingActive = true
        if (!isListening && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        }
    }

    fun stopListeningAccelerometer() {
        isCountingActive = false
        if (isListening) {
            sensorManager?.unregisterListener(this)
            isListening = false
        }
    }

    fun resetRepCount() {
        _repCount.value = 0
        isAboveThreshold = false
    }

    /**
     * Xử lý dữ liệu biến thiên gia tốc Δa (m/s²) truyền từ dịch vụ cảm biến gia tốc trong Activity
     * hoặc từ SensorEventListener nội bộ khi vượt ngưỡng 6.5 m/s².
     */
    fun processAccelerationDelta(
        delta: Float,
        threshold: Float = targetThreshold,
        forceCount: Boolean = false
    ): Boolean {
        _currentDeltaA.value = delta
        if (!isCountingActive && !forceCount) return false

        val now = System.currentTimeMillis()
        // Đếm 1 nhịp vươn vai khi Δa > 6.5 m/s² kèm chống nhiễu (debounce 650ms)
        if (delta > threshold && !isAboveThreshold && (now - lastRepTimestamp > 650)) {
            isAboveThreshold = true
            lastRepTimestamp = now
            val newCount = _repCount.value + 1
            _repCount.value = newCount
            vibratePulse(80)
            playChimeTone(440f + (newCount * 45f), 180)
            return true
        } else if (delta < (threshold * 0.45f)) {
            isAboveThreshold = false
        }
        return false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = abs(magnitude - SensorManager.GRAVITY_EARTH)
            processAccelerationDelta(delta, targetThreshold)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun vibratePulse(durationMs: Long = 100) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun playChimeTone(frequencyHz: Float, durationMs: Int) {
        scope.launch {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / frequencyHz)
                    val envelope = (1f - (i.toFloat() / numSamples)).coerceIn(0f, 1f)
                    samples[i] = (sin(angle) * Short.MAX_VALUE * 0.4f * envelope).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                delay(durationMs.toLong() + 50)
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }

    fun startLullaby() {
        stopLullaby()
        lullabyJob = scope.launch {
            val notes = listOf(261.63f, 293.66f, 329.63f, 392.00f, 440.00f, 392.00f, 329.63f)
            var index = 0
            while (true) {
                val freq = notes[index % notes.size]
                playChimeTone(freq, 600)
                delay(800)
                index++
            }
        }
    }

    fun stopLullaby() {
        lullabyJob?.cancel()
        lullabyJob = null
    }

    fun playVictoryFanfare() {
        scope.launch {
            val victoryNotes = listOf(392f, 523.25f, 659.25f, 783.99f)
            for (freq in victoryNotes) {
                playChimeTone(freq, 220)
                delay(180)
            }
        }
    }

    fun applyWindowBrightness(activity: Activity?, brightnessPercent: Int) {
        if (activity == null) return
        activity.runOnUiThread {
            try {
                val lp = activity.window.attributes
                val fraction = (brightnessPercent / 100f).coerceIn(0.15f, 1.0f)
                lp.screenBrightness = fraction
                activity.window.attributes = lp
            } catch (_: Exception) {}
        }
    }
}
