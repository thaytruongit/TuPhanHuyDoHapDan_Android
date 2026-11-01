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
        mechanismDescription = "Ẩn số Like/View/Thông báo đỏ, giảm độ bão hòa màu về trắng đen 100%, làm trễ cuộn vô tận (900 ms) và yêu cầu gõ mục tiêu học tập chống dán ở phút 30."
    )
}

/**
 * 4 Nhóm mục tiêu kiểm soát chạy nền
 */
enum class TargetPlatform(
    val title: String,
    val emoji: String,
    val packageHint: String
) {
    YOUTUBE_SHORTS("YouTube & Shorts", "▶️", "com.google.android.youtube"),
    TIKTOK_WEB("TikTok Web", "🎵", "com.zhiliaoapp.musically"),
    FACEBOOK_REELS("Facebook & Reels", "📘", "com.facebook.katana"),
    WEB_GAME("Web Game", "🎮", "web.game.portal")
}

/**
 * Chế độ trình diễn giám khảo KHKT (Demo): Tự động hoặc chọn ngay Mức 0 -> Mức 4
 */
enum class DecayLevelMode(
    val label: String,
    val levelNumber: Int,
    val forcedProgress: Float?,
    val description: String
) {
    AUTO("Tự động", -1, null, "Tự động tăng cấp độ suy giảm theo thời gian thực tế"),
    LEVEL_0("Mức 0", 0, 0.0f, "Mức 0 (0%): Bình thường – Hiển thị đầy đủ màu sắc, số Like/View và thông báo đỏ"),
    LEVEL_1("Mức 1", 1, 0.28f, "Mức 1 (28%): Khởi đầu hạ nhiệt – Ẩn chấm thông báo đỏ gây xao nhãng, giảm nhẹ độ rực màu"),
    LEVEL_2("Mức 2", 2, 0.55f, "Mức 2 (55%): Trung bình – Ẩn số Like/View, làm mờ gợi ý 8dp, bão hòa màu giảm 55%, trễ cuộn 450ms"),
    LEVEL_3("Mức 3", 3, 0.82f, "Mức 3 (82%): Ma sát cao – Ẩn hoàn toàn Like/View/Thông báo đỏ, trắng đen 85%, trễ cuộn vô tận 900ms"),
    LEVEL_4("Mức 4", 4, 1.0f, "Mức 4 (100%): Đạt giới hạn – Trắng đen 100%, phát Notification & kích hoạt khóa bắt buộc theo độ tuổi")
}

data class SessionState(
    val ageGroup: AgeGroup = AgeGroup.MIDDLE_SCHOOL,
    val isRunning: Boolean = false,
    val elapsedSeconds: Float = 0f,
    val targetMinutes: Int = 30,
    val speedMultiplier: Float = 1f,
    val isFastForward30x: Boolean = false, // Tua nhanh thời gian (1s = 30s)
    val decayLevelMode: DecayLevelMode = DecayLevelMode.AUTO,
    val enabledTargets: Set<TargetPlatform> = TargetPlatform.values().toSet(),
    val selectedPreviewPlatform: TargetPlatform = TargetPlatform.YOUTUBE_SHORTS,
    val exemptYoutubeEducation: Boolean = true, // Miễn giảm làm xám cho bài giảng học tập YouTube
    val isViewingYoutubeLecture: Boolean = false, // Đang xem bài giảng học tập trên YouTube
    val isBackgroundMonitorActive: Boolean = true, // Chạy nền kiểm soát chặt chẽ 4 nhóm mục tiêu
    val brightnessPercent: Int = 100, // 100% down to 68% for Toddler
    val isBedtimeOverlayVisible: Boolean = false,
    val isMovementStationActive: Boolean = false,
    val stretchRepsDone: Int = 0,
    val targetStretchReps: Int = 10,
    val isReflectionActive: Boolean = false,
    val reflectionCompleted: Boolean = false,
    val currentAccelerationDelta: Float = 0f,
    val lastNotifiedLevel: Int = 0
) {
    val totalSeconds: Float get() = targetMinutes * 60f

    val progress: Float
        get() = decayLevelMode.forcedProgress ?: (elapsedSeconds / totalSeconds).coerceIn(0f, 1f)

    val currentDecayLevel: Int
        get() = when {
            decayLevelMode != DecayLevelMode.AUTO -> decayLevelMode.levelNumber
            progress >= 1.0f -> 4
            progress >= 0.75f -> 3
            progress >= 0.50f -> 2
            progress >= 0.25f -> 1
            else -> 0
        }

    val remainingSeconds: Int
        get() = ((totalSeconds * (1f - progress)).coerceAtLeast(0f)).toInt()

    // Ma sát nhận thức: Ẩn thông báo đỏ từ Mức 1+, ẩn số Like/View từ Mức 2+
    val hideRedNotifications: Boolean get() = currentDecayLevel >= 1
    val hideLikeAndViewCounts: Boolean get() = currentDecayLevel >= 2
    val infiniteScrollDelayMs: Long
        get() = when (currentDecayLevel) {
            0, 1 -> 0L
            2 -> 450L
            else -> 900L
        }

    // Kiểm tra miễn giảm làm xám khi đang xem bài giảng học tập YouTube
    val isGrayscaleExemptedNow: Boolean
        get() = exemptYoutubeEducation &&
                selectedPreviewPlatform == TargetPlatform.YOUTUBE_SHORTS &&
                isViewingYoutubeLecture
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
    val reflectionSentence: String = "Em hoàn thành bài tập Toán và đọc 10 trang sách khoa học trước khi giải trí."
)
