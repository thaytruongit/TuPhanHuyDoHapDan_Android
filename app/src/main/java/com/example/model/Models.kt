package com.example.model

/**
 * Phân khúc 3 nhóm độ tuổi theo đề tài nghiên cứu (3–12 tuổi)
 */
enum class AgeGroup(
    val title: String,
    val subtitle: String,
    val ageRange: String,
    val defaultMinutes: Int,
    val mechanismName: String,
    val mechanismDescription: String
) {
    TODDLER(
        title = "Nhóm 1: Mầm non",
        subtitle = "Trình duyệt buồn ngủ – Bạn Gấu đi ngủ",
        ageRange = "3 – 6 tuổi",
        defaultMinutes = 15,
        mechanismName = "Trình duyệt buồn ngủ",
        mechanismDescription = "Tông vàng ấm Sepia, giảm độ sáng 68%, âm lượng 12%, tự động dừng ở phút 15 bằng hoạt cảnh Bạn Gấu đi ngủ ru êm."
    ),
    PRIMARY(
        title = "Nhóm 2: Tiểu học",
        subtitle = "Hạ nhiệt thị giác & Trạm vận động",
        ageRange = "7 – 9 tuổi",
        defaultMinutes = 25,
        mechanismName = "Hạ nhiệt thị giác & Vận động",
        mechanismDescription = "Làm mờ dần các gợi ý hấp dẫn (Visual Cool-down), kích hoạt trạm vận động ở phút 25 yêu cầu 10 nhịp vươn vai qua cảm biến gia tốc (Δa > 6.5 m/s²)."
    ),
    MIDDLE_SCHOOL(
        title = "Nhóm 3: Lớp 5 – Lớp 7",
        subtitle = "Ma sát nhận thức & Phản tư",
        ageRange = "10 – 12 tuổi",
        defaultMinutes = 30,
        mechanismName = "Ma sát nhận thức & Phản tư",
        mechanismDescription = "Thang độ xám 100% (Holte & Ferraro 2020, PMC10159421), trễ cuộn 900 ms, khóa mật khẩu SHA-256 và gõ câu phản tư chống dán ở phút 30."
    )
}

data class SessionState(
    val ageGroup: AgeGroup = AgeGroup.TODDLER,
    val isRunning: Boolean = false,
    val elapsedSeconds: Float = 0f,
    val targetMinutes: Int = 15,
    val speedMultiplier: Float = 1f, // 1x, 15x, 60x for demo/testing
    val brightnessPercent: Int = 100, // 100% down to 68% for Toddler
    val isBedtimeOverlayVisible: Boolean = false,
    val isMovementStationActive: Boolean = false,
    val stretchRepsDone: Int = 0,
    val targetStretchReps: Int = 10,
    val isReflectionActive: Boolean = false,
    val reflectionCompleted: Boolean = false,
    val currentAccelerationDelta: Float = 0f
) {
    val totalSeconds: Float get() = targetMinutes * 60f
    val progress: Float get() = (elapsedSeconds / totalSeconds).coerceIn(0f, 1f)
    val remainingSeconds: Int get() = ((totalSeconds - elapsedSeconds).coerceAtLeast(0f)).toInt()
}

data class ActivityRecord(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val ageGroupName: String,
    val minutesSpent: Int,
    val completedSuccessfully: Boolean,
    val notes: String
)

data class ParentalConfig(
    val pinSha256: String = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4", // SHA-256 of "1234"
    val toddlerMinutes: Int = 15,
    val primaryMinutes: Int = 25,
    val middleSchoolMinutes: Int = 30,
    val accelerometerThreshold: Float = 6.5f,
    val requiredStretchReps: Int = 10,
    val reflectionSentence: String = "Thời gian của mình là quý giá, mình chọn dừng lại để nghỉ ngơi và đọc sách."
)
