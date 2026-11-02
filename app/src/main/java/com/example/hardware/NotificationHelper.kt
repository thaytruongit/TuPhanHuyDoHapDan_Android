package com.example.hardware

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AgeGroup
import com.example.model.SessionState
import com.example.service.ScreenDecayForegroundService

/**
 * Hệ thống thông báo Local (Notification) & Foreground Notification phục vụ chạy nền trên hệ thống
 * ngay cả khi tắt ứng dụng.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_LIMIT_ALERTS = "screen_decay_limit_alerts"
        const val CHANNEL_BG_MONITOR = "screen_decay_bg_monitor"
        const val NOTIFICATION_ID_LIMIT_ALERT = 1001
        const val NOTIFICATION_ID_STAGE_ALERT = 1002
        const val NOTIFICATION_ID_BG_MONITOR = 2001
    }

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val alertChannel = NotificationChannel(
                CHANNEL_LIMIT_ALERTS,
                "Cảnh báo giới hạn thời gian màn hình",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nhắc nhở trẻ khi thời gian sử dụng màn hình đạt giới hạn theo độ tuổi"
                enableVibration(true)
            }

            val monitorChannel = NotificationChannel(
                CHANNEL_BG_MONITOR,
                "Dịch vụ chạy nền Tự Phân Hủy Độ Hấp Dẫn",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kiểm soát chạy nền hệ thống ngay cả khi tắt ứng dụng"
                setShowBadge(false)
            }

            manager.createNotificationChannel(alertChannel)
            manager.createNotificationChannel(monitorChannel)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun createServiceActionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ScreenDecayForegroundService::class.java).apply {
            this.action = action
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getService(context, requestCode, intent, flags)
    }

    /**
     * Gửi thông báo Local (High Priority) khi thời gian sử dụng màn hình đạt giới hạn theo độ tuổi (Mức 4)
     */
    fun sendAgeLimitReachedNotification(ageGroup: AgeGroup, targetMinutes: Int) {
        if (!hasNotificationPermission()) return

        val (title, body) = when (ageGroup) {
            AgeGroup.TODDLER -> Pair(
                "🐻 Đã hết $targetMinutes phút! Bạn Gấu buồn ngủ rồi!",
                "Nhóm 3–6 tuổi: Màn hình chuyển tông vàng ấm Sepia (sáng 68%, âm lượng 12%). Bé hãy cho mắt nghỉ ngơi cùng Bạn Gấu nhé!"
            )
            AgeGroup.PRIMARY -> Pair(
                "🏃 Đã đạt giới hạn $targetMinutes phút! Đến Trạm Vận Động!",
                "Nhóm 7–9 tuổi: Hãy đứng dậy thực hiện 10 nhịp vươn vai (Δa > 6.5 m/s²) để thư giãn mắt và cơ xương!"
            )
            AgeGroup.MIDDLE_SCHOOL -> Pair(
                "🧠 Đã đạt giới hạn $targetMinutes phút! Kích hoạt Ma sát nhận thức!",
                "Nhóm 10–12 tuổi: Đã ẩn Like/View/Thông báo đỏ, chuyển trắng đen 100%, trễ cuộn 900ms. Hãy nhập mục tiêu học tập!"
            )
        }

        try {
            val contentIntent = createContentIntent()
            val notification = NotificationCompat.Builder(context, CHANNEL_LIMIT_ALERTS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setFullScreenIntent(contentIntent, true)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_LIMIT_ALERT, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * Gửi thông báo nhắc nhở khi chuyển cấp độ suy giảm (Mức 1, Mức 2, Mức 3)
     */
    fun sendDecayStageNotification(ageGroup: AgeGroup, level: Int, remainingMinutes: Int) {
        if (!hasNotificationPermission() || level !in 1..3) return

        val title = "⚠️ Tự Phân Hủy Độ Hấp Dẫn – Cấp độ Mức $level (${ageGroup.ageRange})"
        val body = when (level) {
            1 -> "Còn khoảng $remainingMinutes phút. Đã ẩn chấm thông báo đỏ gây xao nhãng và bắt đầu hạ nhiệt màu sắc."
            2 -> "Còn khoảng $remainingMinutes phút. Đã ẩn số Like/View, làm mờ gợi ý 8dp và giảm 55% độ bão hòa màu."
            3 -> "Sắp hết giờ (còn $remainingMinutes phút)! Trắng đen 85%, ẩn toàn bộ Like/View và áp dụng trễ cuộn vô tận 900ms."
            else -> ""
        }

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_LIMIT_ALERTS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(createContentIntent())
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_STAGE_ALERT, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * Tạo Notification dùng cho ForegroundService chạy ngầm trên hệ thống ngay cả khi tắt ứng dụng
     */
    fun buildForegroundMonitorNotification(sessionState: SessionState): Notification {
        val activeTargets = sessionState.enabledTargets.joinToString(", ") { platform ->
            if (platform == com.example.model.TargetPlatform.WEB_GAME && sessionState.enabledGames.isNotEmpty()) {
                "Game (${sessionState.enabledGames.joinToString("/") { it.title }})"
            } else {
                platform.title
            }
        }
        val remMin = sessionState.remainingSeconds / 60
        val remSec = sessionState.remainingSeconds % 60
        val statusLabel = if (sessionState.isRunning) "Đang đếm giờ" else "Đang giám sát"
        val eduStatus = if (sessionState.exemptYoutubeEducation) "Miễn xám YT: BẬT" else "Miễn xám YT: TẮT"

        val title = "🛡️ Chạy nền hệ thống ($statusLabel • Mức ${sessionState.currentDecayLevel}/4)"
        val body = "${sessionState.ageGroup.ageRange} | Còn: %02d:%02d | Mục tiêu: $activeTargets | $eduStatus".format(remMin, remSec)

        val toggleActionTitle = if (sessionState.isRunning) "Tạm dừng" else "Chạy bộ đếm"
        val toggleIntent = createServiceActionIntent(ScreenDecayForegroundService.ACTION_TOGGLE_RUN, 10)
        val nextLevelIntent = createServiceActionIntent(ScreenDecayForegroundService.ACTION_CYCLE_DEMO_LEVEL, 11)

        return NotificationCompat.Builder(context, CHANNEL_BG_MONITOR)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(createContentIntent())
            .addAction(0, toggleActionTitle, toggleIntent)
            .addAction(0, "Đổi Mức (Demo)", nextLevelIntent)
            .build()
    }

    /**
     * Cập nhật thông báo chạy nền kiểm soát chặt chẽ 4 nhóm mục tiêu
     */
    fun updateBackgroundMonitorNotification(sessionState: SessionState) {
        if (!hasNotificationPermission()) return
        if (!sessionState.isBackgroundMonitorActive) {
            cancelBackgroundMonitorNotification()
            return
        }

        try {
            val notification = buildForegroundMonitorNotification(sessionState)
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BG_MONITOR, notification)
        } catch (_: SecurityException) {}
    }

    fun cancelBackgroundMonitorNotification() {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_BG_MONITOR)
        } catch (_: Exception) {}
    }
}
