package com.example.data

import android.content.Context
import com.example.model.ActivityRecord
import com.example.model.ParentalConfig
import com.example.util.CryptoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ScreenDecayRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("screen_decay_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<ParentalConfig> = _config.asStateFlow()

    private val _activityLogs = MutableStateFlow(loadLogs())
    val activityLogs: StateFlow<List<ActivityRecord>> = _activityLogs.asStateFlow()

    private fun loadConfig(): ParentalConfig {
        val defaultHash = CryptoUtils.sha256("1234")
        val pinSha256 = prefs.getString("pin_sha256", defaultHash) ?: defaultHash
        val toddlerMin = prefs.getInt("toddler_min", 15)
        val primaryMin = prefs.getInt("primary_min", 25)
        val middleMin = prefs.getInt("middle_min", 30)
        val threshold = prefs.getFloat("accel_threshold", 6.5f)
        val reps = prefs.getInt("stretch_reps", 10)
        val reflection = prefs.getString(
            "reflection_sentence",
            "Thời gian của mình là quý giá, mình chọn dừng lại để nghỉ ngơi và đọc sách."
        ) ?: "Thời gian của mình là quý giá, mình chọn dừng lại để nghỉ ngơi và đọc sách."

        return ParentalConfig(
            pinSha256 = pinSha256,
            toddlerMinutes = toddlerMin,
            primaryMinutes = primaryMin,
            middleSchoolMinutes = middleMin,
            accelerometerThreshold = threshold,
            requiredStretchReps = reps,
            reflectionSentence = reflection
        )
    }

    fun saveConfig(newConfig: ParentalConfig) {
        prefs.edit()
            .putString("pin_sha256", newConfig.pinSha256)
            .putInt("toddler_min", newConfig.toddlerMinutes)
            .putInt("primary_min", newConfig.primaryMinutes)
            .putInt("middle_min", newConfig.middleSchoolMinutes)
            .putFloat("accel_threshold", newConfig.accelerometerThreshold)
            .putInt("stretch_reps", newConfig.requiredStretchReps)
            .putString("reflection_sentence", newConfig.reflectionSentence)
            .apply()
        _config.value = newConfig
    }

    fun updatePin(newPlainPin: String) {
        val newHash = CryptoUtils.sha256(newPlainPin.trim())
        val updated = _config.value.copy(pinSha256 = newHash)
        saveConfig(updated)
    }

    private fun loadLogs(): List<ActivityRecord> {
        val jsonStr = prefs.getString("activity_logs_json", null) ?: return defaultLogs()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ActivityRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ActivityRecord(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        ageGroupName = obj.optString("ageGroupName", "Nhóm"),
                        minutesSpent = obj.optInt("minutesSpent", 0),
                        completedSuccessfully = obj.optBoolean("completedSuccessfully", true),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            defaultLogs()
        }
    }

    private fun defaultLogs(): List<ActivityRecord> {
        val now = System.currentTimeMillis()
        return listOf(
            ActivityRecord(
                id = 1,
                timestamp = now - 3600_000 * 4,
                ageGroupName = "Nhóm 1 (3-6t)",
                minutesSpent = 15,
                completedSuccessfully = true,
                notes = "Bạn Gấu đi ngủ thành công, bé chuyển sang nghe truyện audio."
            ),
            ActivityRecord(
                id = 2,
                timestamp = now - 3600_000 * 24,
                ageGroupName = "Nhóm 2 (7-9t)",
                minutesSpent = 25,
                completedSuccessfully = true,
                notes = "Hoàn thành 10 nhịp vươn vai ở Trạm vận động (Δa > 6.5 m/s²)."
            ),
            ActivityRecord(
                id = 3,
                timestamp = now - 3600_000 * 48,
                ageGroupName = "Nhóm 3 (10-12t)",
                minutesSpent = 30,
                completedSuccessfully = true,
                notes = "Chuyển Grayscale 100%, gõ phản tư chống dán và rời máy tự giác."
            )
        )
    }

    fun logSession(record: ActivityRecord) {
        val current = _activityLogs.value.toMutableList()
        current.add(0, record)
        val trimmed = current.take(50) // keep latest 50
        _activityLogs.value = trimmed

        try {
            val jsonArray = JSONArray()
            for (item in trimmed) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("ageGroupName", item.ageGroupName)
                    put("minutesSpent", item.minutesSpent)
                    put("completedSuccessfully", item.completedSuccessfully)
                    put("notes", item.notes)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("activity_logs_json", jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }
}
