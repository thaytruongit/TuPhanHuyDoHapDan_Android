package vn.khkt.dopaminedecay

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import kotlin.math.abs
import kotlin.math.sqrt

class DopamineMonitorService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SYNC_CONFIG = "ACTION_SYNC_CONFIG"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "dopamine_decay_strict_channel"

        @Volatile
        var currentActivePkg: String = ""
        @Volatile
        var currentBrowserContext: String = ""
        @Volatile
        var isEduLectureActive: Boolean = false
        @Volatile
        var scrollEventCounter: Int = 0
        @Volatile
        var lastAccessibilityTimestamp: Long = 0L

        fun onAccessibilityPackageDetected(pkgName: String, browserContext: String, eduDetected: Boolean) {
            if (TargetAppRegistry.IGNORED_SYSTEM_PACKAGES.contains(pkgName)) return
            currentActivePkg = pkgName
            if (browserContext.isNotEmpty()) {
                currentBrowserContext = browserContext
            } else if (!TargetAppRegistry.BROWSER_PACKAGES.contains(pkgName)) {
                currentBrowserContext = ""
            }
            isEduLectureActive = eduDetected
            lastAccessibilityTimestamp = System.currentTimeMillis()
        }

        fun notifyUserScrolled() {
            scrollEventCounter++
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var audioManager: AudioManager
    private lateinit var sensorManager: SensorManager
    private lateinit var prefs: SharedPreferences
    private var usageStatsManager: UsageStatsManager? = null

    // 3 Lớp cửa sổ Overlay chuyên biệt:
    // 1. Lớp màng lọc suy giảm độ hấp dẫn xuyên cảm ứng (filterOverlayView)
    // 2. Nút tròn nhỏ nổi ở góc màn hình (floatingOrbView) giống bản Web/iOS
    // 3. Màn chắn Khóa cứng Mức 4 yêu cầu mật khẩu Phụ huynh 666666 (level4LockView)
    private var filterOverlayView: View? = null
    private var floatingOrbView: TextView? = null
    private var floatingOrbParams: WindowManager.LayoutParams? = null
    private var quickTestDialogView: View? = null
    private var level4LockView: View? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoringActive = false
    private var lastKnownForegroundPkg: String = ""
    private var activeCategory: String = ""

    private var activeSeconds: Float = 0f
    private var swipeCount: Int = 0
    private var currentLevel: Int = 0
    private var lastAppliedLevel: Int = -1
    private var isHardLockedAtLevel4: Boolean = false
    private var stretchReps: Int = 0
    private var lastAcceleration: Float = 9.8f
    private var tickQuarterCounter: Int = 0

    // Quét 250ms/lần (4 lần/giây) để chặn tức khắc khi trẻ mở YouTube, TikTok, Facebook, Roblox
    private val fastLoopRunnable = object : Runnable {
        override fun run() {
            if (isMonitoringActive) {
                tickQuarterCounter = (tickQuarterCounter + 1) % 4
                val isFullSecond = (tickQuarterCounter == 0)
                checkAndEnforceStrictControl(isFullSecond)
                handler.postDelayed(this, 250)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        prefs = getSharedPreferences("dopamine_decay_prefs", Context.MODE_PRIVATE)

        // Khôi phục trạng thái khóa cứng Mức 4 và thời gian từ bộ nhớ cục bộ
        isHardLockedAtLevel4 = prefs.getBoolean("hard_locked_level4", false)
        activeSeconds = prefs.getFloat("saved_active_seconds", 0f)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopMonitoring()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        isHardLockedAtLevel4 = prefs.getBoolean("hard_locked_level4", false)
        activeSeconds = prefs.getFloat("saved_active_seconds", activeSeconds)

        startForeground(
            NOTIFICATION_ID,
            buildNotification("Đang chạy nền kiểm soát chặt chẽ (YouTube, TikTok, FB, Roblox)")
        )
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        if (isMonitoringActive) return
        isMonitoringActive = true
        ensureFilterOverlayCreated()
        ensureFloatingOrbCreated()
        handler.post(fastLoopRunnable)
    }

    private fun stopMonitoring() {
        isMonitoringActive = false
        handler.removeCallbacks(fastLoopRunnable)
        setHardwareGrayscale(false)
        removeQuickTestDialog()
        removeLevel4LockOverlay()
        removeFloatingOrb()
        removeFilterOverlay()
    }

    /**
     * THUẬT TOÁN PHÁT HIỆN GÓI FOREGROUND BẤT TỬ (KHÔNG BAO GIỜ MẤT DẤU SAU 3 GIÂY)
     * Kết hợp:
     * 1. Sự kiện thời gian thực từ DopamineAccessibilityService
     * 2. Truy vấn UsageEvents trong cửa sổ 60 giây gần nhất + giữ trạng thái (State-Holding)
     * 3. Dự phòng UsageStats gần nhất nếu người dùng giữ nguyên 1 màn hình video dài > 60 giây
     */
    private fun resolveForegroundPackage(): String {
        val now = System.currentTimeMillis()

        // 1. Nếu Accessibility vừa báo cáo trong vòng 2.5 giây qua, ưu tiên tuyệt đối
        if (currentActivePkg.isNotEmpty() && (now - lastAccessibilityTimestamp) <= 2500L) {
            if (!TargetAppRegistry.IGNORED_SYSTEM_PACKAGES.contains(currentActivePkg)) {
                lastKnownForegroundPkg = currentActivePkg
                return lastKnownForegroundPkg
            }
        }

        // 2. Quét UsageEvents trong 60 giây gần nhất để bắt sự kiện ACTIVITY_RESUMED mới nhất
        val usm = usageStatsManager
        if (usm != null) {
            try {
                val events = usm.queryEvents(now - 60_000L, now)
                val event = UsageEvents.Event()
                var latestResumedPkg = ""
                var latestTime = 0L

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val pkg = event.packageName ?: continue
                    if (TargetAppRegistry.IGNORED_SYSTEM_PACKAGES.contains(pkg)) continue

                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                        event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        if (event.timeStamp >= latestTime) {
                            latestTime = event.timeStamp
                            latestResumedPkg = pkg
                        }
                    }
                }

                if (latestResumedPkg.isNotEmpty()) {
                    lastKnownForegroundPkg = latestResumedPkg
                    currentActivePkg = latestResumedPkg
                    return lastKnownForegroundPkg
                }

                // 3. Nếu người dùng đang xem 1 video dài hơn 60 giây mà không chuyển Activity,
                // giữ nguyên lastKnownForegroundPkg hoặc kiểm tra UsageStats trong 10 phút qua
                if (lastKnownForegroundPkg.isNotEmpty()) {
                    return lastKnownForegroundPkg
                }

                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 600_000L, now)
                val mostRecent = stats
                    ?.filter { !TargetAppRegistry.IGNORED_SYSTEM_PACKAGES.contains(it.packageName) }
                    ?.maxByOrNull { it.lastTimeUsed }
                if (mostRecent != null && mostRecent.lastTimeUsed > 0) {
                    lastKnownForegroundPkg = mostRecent.packageName
                    return lastKnownForegroundPkg
                }
            } catch (_: Exception) {}
        }

        return lastKnownForegroundPkg
    }

    /**
     * VÒNG LẶP KIỂM SOÁT NGHIÊM NGẶT (250ms/lần)
     */
    private fun checkAndEnforceStrictControl(isFullSecond: Boolean) {
        val fgPkg = resolveForegroundPackage()
        val matchedCategory = TargetAppRegistry.classifyPackage(fgPkg, currentBrowserContext)

        if (matchedCategory != null) {
            val isCategoryEnabled = prefs.getBoolean("plat_$matchedCategory", true)
            if (isCategoryEnabled) {
                activeCategory = matchedCategory

                // Nếu đã chạm Mức 4 (Khóa cứng): Chặn ngay lập tức trong 250ms, tắt nhạc nền và hiện màn khóa!
                if (isHardLockedAtLevel4) {
                    currentLevel = 4
                    applyVisualFilter(4, prefs.getString("age_group", "10-12") ?: "10-12")
                    pauseAndMuteMediaStrictly()
                    showLevel4HardLockOverlay()
                    updateFloatingOrbUI(4)
                    return
                }

                // Mỗi 1 giây thực tế: Tăng bộ đếm thời gian (trừ khi đã khóa ở Mức 4)
                if (isFullSecond) {
                    val speedMultiplier = prefs.getInt("speed_multiplier", 1).coerceIn(1, 30)
                    val isTest120s = prefs.getBoolean("test_120s_mode", true)

                    // Cộng phạt lượt cuộn vô thức
                    if (scrollEventCounter > 0) {
                        swipeCount += scrollEventCounter
                        scrollEventCounter = 0
                    }

                    activeSeconds += (1f * speedMultiplier)
                    if (isTest120s && activeSeconds >= 120f) {
                        activeSeconds = 120f
                    }
                    prefs.edit().putFloat("saved_active_seconds", activeSeconds).apply()
                }

                evaluateAndEnforceLevel()
                return
            }
        }

        // Khi người dùng ở ngoài màn hình chính hoặc ứng dụng không thuộc diện kiểm soát:
        // Tạm ẩn lớp phủ lọc màu & màn khóa để phụ huynh/học sinh dùng máy bình thường,
        // nhưng GIỮ NGUYÊN bộ đếm thời gian & trạng thái khóa Mức 4!
        if (activeCategory.isNotEmpty()) {
            activeCategory = ""
            lastAppliedLevel = -1
            setHardwareGrayscale(false)
            filterOverlayView?.setBackgroundColor(Color.TRANSPARENT)
            floatingOrbView?.visibility = View.GONE
            removeQuickTestDialog()
            removeLevel4LockOverlay()
            updateNotification("Đang chạy nền bảo vệ (YouTube, TikTok, FB, Roblox)")
        }
    }

    /**
     * ĐÁNH GIÁ CẤP ĐỘ SUY GIẢM (MỨC 0 -> MỨC 4)
     * Hỗ trợ cả Chế độ Kiểm thử 120 giây (0s-30s-60s-90s-120s) và Chế độ Thực tế theo 3 Nhóm tuổi
     */
    private fun evaluateAndEnforceLevel() {
        val ageGroup = prefs.getString("age_group", "10-12") ?: "10-12"
        val isTest120s = prefs.getBoolean("test_120s_mode", true)
        val demoOverride = prefs.getInt("demo_override", -1)

        // Ngưỡng thời gian (giây):
        // - Chế độ Kiểm thử 120s: 30s (M1) | 60s (M2) | 90s (M3) | 120s (M4 Khóa cứng)
        // - Chế độ Thực tế theo tuổi: 3-6t (6p,10p,13p,15p) | 7-9t (10p,15p,20p,25p) | 10-12t (15p,20p,25p,30p)
        val thresholds = if (isTest120s) {
            intArrayOf(30, 60, 90, 120)
        } else {
            when (ageGroup) {
                "3-6" -> intArrayOf(6 * 60, 10 * 60, 13 * 60, 15 * 60)
                "7-9" -> intArrayOf(10 * 60, 15 * 60, 20 * 60, 25 * 60)
                else -> intArrayOf(15 * 60, 20 * 60, 25 * 60, 30 * 60)
            }
        }

        // Công thức T_eff = T_active + min(300, k * N_scroll)
        val scrollPenalty = if (isTest120s) (swipeCount * 1) else (swipeCount * 2).coerceAtMost(300)
        val effectiveSec = activeSeconds + scrollPenalty

        // Kiểm tra Lá chắn Học tập Thông minh (EDU_KEYWORDS) khi ở chế độ Tự động
        if (isEduLectureActive && demoOverride == -1 && !isTest120s) {
            currentLevel = 0
        } else if (demoOverride in 0..4) {
            currentLevel = demoOverride
            if (currentLevel == 4) {
                lockAtLevel4Strictly()
            }
        } else {
            currentLevel = when {
                effectiveSec >= thresholds[3] -> 4
                effectiveSec >= thresholds[2] -> 3
                effectiveSec >= thresholds[1] -> 2
                effectiveSec >= thresholds[0] -> 1
                else -> 0
            }
            if (currentLevel == 4) {
                lockAtLevel4Strictly()
            }
        }

        // Chỉ cập nhật màu lớp phủ khi thay đổi cấp độ (Chống nhấp nháy màn hình 100%)
        if (lastAppliedLevel != currentLevel) {
            lastAppliedLevel = currentLevel
            applyVisualFilter(currentLevel, ageGroup)
        }
        applyAudioDecay(currentLevel, ageGroup)
        updateFloatingOrbUI(currentLevel)

        val catName = TargetAppRegistry.getPlatformDisplayName(activeCategory)
        updateNotification("Đang kiểm soát: $catName | Mức $currentLevel (${activeSeconds.toInt()}s)")
    }

    private fun lockAtLevel4Strictly() {
        isHardLockedAtLevel4 = true
        prefs.edit().putBoolean("hard_locked_level4", true).apply()
        pauseAndMuteMediaStrictly()
        showLevel4HardLockOverlay()
    }

    /**
     * TẮT TIẾNG & DỪNG PHÁT VIDEO NGAY LẬP TỨC KHI CHẠM MỨC 4
     */
    private fun pauseAndMuteMediaStrictly() {
        try {
            // Phát sự kiện dừng Media tới YouTube / TikTok / Facebook
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)

            // Hạ âm lượng giải trí về mức tối thiểu
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } catch (_: Exception) {}
    }

    /**
     * 1. LỚP MÀNG LỌC MÀU XUYÊN CẢM ỨNG (ỔN ĐỊNH, KHÔNG NHẤP NHÁY)
     */
    private fun ensureFilterOverlayCreated() {
        if (!Settings.canDrawOverlays(this)) return
        if (filterOverlayView != null) return

        try {
            filterOverlayView = View(this).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            windowManager.addView(filterOverlayView, params)
        } catch (_: Exception) {}
    }

    private fun removeFilterOverlay() {
        filterOverlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            filterOverlayView = null
        }
    }

    private fun applyVisualFilter(level: Int, ageGroup: String) {
        ensureFilterOverlayCreated()
        val view = filterOverlayView ?: return

        // Kích hoạt Thang độ xám phần cứng (Daltonizer) ở Mức 3 & Mức 4 nếu máy đã cấp quyền Secure Settings
        setHardwareGrayscale(level >= 3 && ageGroup != "3-6")

        val overlayColor = if (ageGroup == "3-6") {
            when (level) {
                0 -> Color.TRANSPARENT
                1 -> Color.argb(55, 245, 158, 11)   // Mức 1: Giảm rực màu 35% (Vàng ấm dịu mắt)
                2 -> Color.argb(110, 180, 83, 9)    // Mức 2: Sepia 65% làm dịu kích thích
                3 -> Color.argb(165, 92, 45, 12)    // Mức 3: Sepia đậm 90% chuẩn bị đi ngủ
                else -> Color.argb(225, 15, 23, 42) // Mức 4: Khóa cứng màn hình
            }
        } else {
            when (level) {
                0 -> Color.TRANSPARENT
                1 -> Color.argb(65, 100, 116, 139)  // Mức 1: Khử bão hòa màu 35% (Sương xám nhẹ)
                2 -> Color.argb(128, 71, 85, 105)   // Mức 2: Khử bão hòa màu 70% (Xám trung tính)
                3 -> Color.argb(188, 30, 41, 59)    // Mức 3: Thang xám đậm 100% (Mất hoàn toàn độ rực rỡ)
                else -> Color.argb(235, 15, 23, 42) // Mức 4: Khóa cứng màn hình
            }
        }
        view.setBackgroundColor(overlayColor)
    }

    private fun setHardwareGrayscale(enable: Boolean) {
        try {
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer_enabled", if (enable) 1 else 0)
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer", if (enable) 0 else -1)
        } catch (_: Exception) {
            // Nếu chưa cấp WRITE_SECURE_SETTINGS qua ADB thì lớp phủ filterOverlayView vẫn hoạt động 100%
        }
    }

    private fun applyAudioDecay(level: Int, ageGroup: String) {
        if (level < 2) return
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val ratio = when (level) {
                2 -> 0.60f
                3 -> 0.30f
                else -> 0.0f
            }
            val targetVol = (maxVol * ratio).toInt()
            if (curVol > targetVol) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            }
        } catch (_: Exception) {}
    }

    /**
     * 2. NÚT TRÒN NỔI GÓC MÀN HÌNH (FLOATING ORB) GIỐNG BẢN WEB / iOS
     * Giúp Giám khảo & Phụ huynh thấy trực tiếp Mức 0 -> Mức 4 ngay bên trong App YouTube, TikTok, Facebook, Roblox
     * Bấm vào nút tròn để mở Thanh trượt kiểm thử nhanh 120s hoặc Khóa/Mở khóa bằng mật khẩu 666666!
     */
    private fun ensureFloatingOrbCreated() {
        if (!Settings.canDrawOverlays(this)) return
        if (floatingOrbView != null) return

        try {
            val orb = TextView(this).apply {
                text = "M0\n0s"
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                visibility = View.GONE
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#10B981"))
                    setStroke(4, Color.WHITE)
                }
            }

            val sizePx = (58 * resources.displayMetrics.density).toInt()
            val params = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                x = (16 * resources.displayMetrics.density).toInt()
                y = (90 * resources.displayMetrics.density).toInt()
            }

            // Hỗ trợ vừa kéo thả di chuyển vị trí, vừa chạm để mở bảng điều khiển nhanh
            var initialX = 0
            var initialY = 0
            var touchX = 0f
            var touchY = 0f
            var isClick = false

            orb.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        isClick = true
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (touchX - event.rawX).toInt()
                        val dy = (touchY - event.rawY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isClick = false
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try { windowManager.updateViewLayout(orb, params) } catch (_: Exception) {}
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            showQuickTestControlDialog()
                        }
                        true
                    }
                    else -> false
                }
            }

            floatingOrbView = orb
            floatingOrbParams = params
            windowManager.addView(orb, params)
        } catch (_: Exception) {}
    }

    private fun updateFloatingOrbUI(level: Int) {
        ensureFloatingOrbCreated()
        val orb = floatingOrbView ?: return
        orb.visibility = View.VISIBLE
        orb.text = "M$level\n${activeSeconds.toInt()}s"

        val bgColor = when (level) {
            0 -> "#10B981" // Xanh lá
            1 -> "#F59E0B" // Vàng cam
            2 -> "#EA580C" // Cam đậm
            3 -> "#475569" // Xám chì
            else -> "#DC2626" // Đỏ khóa Mức 4
        }
        (orb.background as? GradientDrawable)?.setColor(Color.parseColor(bgColor))
    }

    private fun removeFloatingOrb() {
        floatingOrbView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            floatingOrbView = null
        }
    }

    /**
     * HỘP THOẠI ĐIỀU KHIỂN & THANH TRƯỢT KIỂM THỬ NHANH NGAY TRÊN YOUTUBE / TIKTOK / FACEBOOK / ROBLOX
     */
    private fun showQuickTestControlDialog() {
        if (!Settings.canDrawOverlays(this)) return
        if (quickTestDialogView != null) {
            removeQuickTestDialog()
            return
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 40, 44, 40)
            background = GradientDrawable().apply {
                cornerRadius = 36f
                setColor(Color.WHITE)
                setStroke(3, Color.parseColor("#CBD5E1"))
            }
        }

        val catName = TargetAppRegistry.getPlatformDisplayName(activeCategory)
        val header = TextView(this).apply {
            text = "🛡️ Kiểm soát: $catName (Mức $currentLevel)"
            textSize = 16.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F172A"))
        }
        card.addView(header)

        val subInfo = TextView(this).apply {
            text = "Kéo thanh trượt thời gian (0s → 120s) để kiểm thử tức thì từ Mức 0 đến Mức 4 ngay trên ứng dụng:"
            textSize = 13f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, 8, 0, 16)
        }
        card.addView(subInfo)

        val seekLabel = TextView(this).apply {
            text = "⏱️ Thời gian: ${activeSeconds.toInt()}s / 120s (Mức $currentLevel)"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0284C7"))
        }
        card.addView(seekLabel)

        val seekBar = SeekBar(this).apply {
            max = 120
            progress = activeSeconds.toInt().coerceIn(0, 120)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        if (isHardLockedAtLevel4) {
                            Toast.makeText(
                                this@DopamineMonitorService,
                                "🔒 Đã khóa cứng ở Mức 4! Phải nhập mật khẩu Phụ huynh (666666) để đặt lại!",
                                Toast.LENGTH_SHORT
                            ).show()
                            sb?.progress = 120
                            return
                        }
                        activeSeconds = value.toFloat()
                        prefs.edit().putFloat("saved_active_seconds", activeSeconds).apply()
                        prefs.edit().putInt("demo_override", -1).apply()
                        evaluateAndEnforceLevel()
                        seekLabel.text = "⏱️ Thời gian: ${activeSeconds.toInt()}s / 120s (Mức $currentLevel)"
                        header.text = "🛡️ Kiểm soát: $catName (Mức $currentLevel)"
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        card.addView(seekBar)

        // Hàng nút chọn nhanh M0, M1, M2, M3, M4
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
        }
        val levelSeconds = intArrayOf(0, 35, 65, 95, 120)
        for (lvl in 0..4) {
            val b = Button(this).apply {
                text = "M$lvl"
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(4, 0, 4, 0)
                }
                setOnClickListener {
                    if (isHardLockedAtLevel4 && lvl < 4) {
                        Toast.makeText(
                            this@DopamineMonitorService,
                            "🔒 Đã khóa ở Mức 4! Vui lòng nhập mã PIN 666666 để mở lại.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    activeSeconds = levelSeconds[lvl].toFloat()
                    seekBar.progress = levelSeconds[lvl]
                    prefs.edit().putFloat("saved_active_seconds", activeSeconds).apply()
                    evaluateAndEnforceLevel()
                    if (lvl == 4) {
                        removeQuickTestDialog()
                    }
                }
            }
            btnRow.addView(b)
        }
        card.addView(btnRow)

        val closeBtn = Button(this).apply {
            text = "Đóng bảng điều khiển nhanh"
            setBackgroundColor(Color.parseColor("#0F172A"))
            setTextColor(Color.WHITE)
            setOnClickListener { removeQuickTestDialog() }
        }
        card.addView(closeBtn)

        try {
            val params = WindowManager.LayoutParams(
                (340 * resources.displayMetrics.density).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            quickTestDialogView = card
            windowManager.addView(card, params)
        } catch (_: Exception) {}
    }

    private fun removeQuickTestDialog() {
        quickTestDialogView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            quickTestDialogView = null
        }
    }

    /**
     * 3. MÀN CHẮN KHÓA CỨNG MỨC 4 (STRICT HARD-LOCK OVERLAY)
     * - Không cho phép tự động chạy lại từ đầu.
     * - Không thể vuốt Back để bỏ qua.
     * - Bắt buộc nhập đúng Mật khẩu Phụ huynh (666666) mới được mở khóa hoặc đặt lại thời gian!
     */
    private fun showLevel4HardLockOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        if (level4LockView != null) return
        removeQuickTestDialog()

        val ageGroup = prefs.getString("age_group", "10-12") ?: "10-12"
        val catName = TargetAppRegistry.getPlatformDisplayName(activeCategory)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(52, 64, 52, 64)
            setBackgroundColor(Color.argb(248, 15, 23, 42))
        }

        val badgeView = TextView(this).apply {
            text = "🔒 KHÓA CỨNG MỨC 4 • ĐÃ HẾT THỜI GIAN CHO PHÉP ($catName)"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FCA5A5"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        root.addView(badgeView)

        val titleView = TextView(this).apply {
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            text = when (ageGroup) {
                "3-6" -> "🌙 🐻 Bạn Điện Thoại Đã Đi Ngủ Rồi!"
                "7-9" -> "🔋 🏃‍♂️ Hết Năng Lượng Màn Hình (Mức 4)!"
                else -> "🛑 Đã Đạt Giới Hạn Mức 4 – Khóa Bảo Vệ!"
            }
        }
        root.addView(titleView)

        val descView = TextView(this).apply {
            textSize = 14.5f
            setTextColor(Color.parseColor("#CBD5E1"))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
            text = "Ứng dụng $catName đã bị khóa hoàn toàn để bảo vệ mắt và não bộ. Trẻ không thể tự tải lại hay đặt lại từ đầu.\n\nPhụ huynh vui lòng nhập mật khẩu (666666) nếu muốn mở khóa và đặt lại thời gian:"
        }
        root.addView(descView)

        // Nếu nhóm 7-9 tuổi, hiển thị thêm bộ đếm cảm biến gia tốc 10 nhịp vươn vai minh chứng KHKT
        if (ageGroup == "7-9") {
            stretchReps = 0
            val sensorLabel = TextView(this).apply {
                text = "🤸 Cảm biến gia tốc đếm nhịp vươn vai: 0 / 10 nhịp"
                textSize = 14f
                setTextColor(Color.parseColor("#38BDF8"))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 20)
                tag = "sensor_label_tag"
            }
            root.addView(sensorLabel)
            val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        }

        val pinInput = EditText(this).apply {
            hint = "Nhập mật khẩu Phụ huynh (Mặc định: 666666)"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            background = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(Color.parseColor("#1E293B"))
                setStroke(3, Color.parseColor("#475569"))
            }
            setPadding(32, 28, 32, 28)
        }
        root.addView(pinInput)

        val unlockBtn = Button(this).apply {
            text = "🔓 MỞ KHÓA & ĐẶT LẠI VỀ MỨC 0 (CẦN MẬT KHẨU)"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.parseColor("#10B981"))
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 24, 0, 16)
            }
            layoutParams = lp
            setOnClickListener {
                val entered = pinInput.text.toString().trim()
                val savedPin = prefs.getString("parent_pin", TargetAppRegistry.DEFAULT_PIN) ?: TargetAppRegistry.DEFAULT_PIN
                if (TargetAppRegistry.verifyPin(entered, savedPin)) {
                    unlockAndResetByParent()
                    Toast.makeText(
                        this@DopamineMonitorService,
                        "✅ Đã xác thực mật khẩu Phụ huynh! Đã mở khóa và đặt lại về Mức 0.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    pinInput.setText("")
                    Toast.makeText(
                        this@DopamineMonitorService,
                        "❌ Sai mật khẩu Phụ huynh! Không thể mở khóa.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        root.addView(unlockBtn)

        val exitHomeBtn = Button(this).apply {
            text = "🏠 Thoát ra Màn hình chính (Nghỉ mắt)"
            textSize = 14f
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
            setOnClickListener {
                // Chỉ thoát ra màn hình chính, KHÔNG reset thời gian (nếu trẻ mở lại YouTube/TikTok vẫn bị khóa Mức 4!)
                goHomeStrictly()
            }
        }
        root.addView(exitHomeBtn)

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            level4LockView = root
            windowManager.addView(root, params)
        } catch (_: Exception) {}
    }

    private fun unlockAndResetByParent() {
        isHardLockedAtLevel4 = false
        activeSeconds = 0f
        swipeCount = 0
        currentLevel = 0
        lastAppliedLevel = -1
        prefs.edit()
            .putBoolean("hard_locked_level4", false)
            .putFloat("saved_active_seconds", 0f)
            .putInt("demo_override", -1)
            .apply()
        setHardwareGrayscale(false)
        removeLevel4LockOverlay()
        applyVisualFilter(0, prefs.getString("age_group", "10-12") ?: "10-12")
        updateFloatingOrbUI(0)
    }

    private fun removeLevel4LockOverlay() {
        sensorManager.unregisterListener(this)
        level4LockView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            level4LockView = null
        }
    }

    private fun goHomeStrictly() {
        DopamineAccessibilityService.instance?.enforceGlobalHomeAction()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try { startActivity(homeIntent) } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val accel = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = accel - lastAcceleration
        lastAcceleration = accel

        if (delta > 6.5f) {
            stretchReps = (stretchReps + 1).coerceAtMost(10)
            val label = level4LockView?.findViewWithTag<TextView>("sensor_label_tag")
            label?.text = "🤸 Cảm biến gia tốc đếm nhịp vươn vai: $stretchReps / 10 nhịp"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Tự Phân Huỷ Độ Hấp Dẫn - Kiểm soát chạy nền",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Dịch vụ chạy nền kiểm soát chặt chẽ YouTube, TikTok, Facebook, Roblox"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(chan)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("🛡️ Tự Phân Huỷ Độ Hấp Dẫn (Đang Bảo Vệ)")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * TỰ ĐỘNG HỒI SINH DỊCH VỤ TRONG 500MS NẾU BỊ VUỐT TẮT KHỎI ĐA NHIỆM
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (prefs.getBoolean("service_running", true)) {
            val restartIntent = Intent(applicationContext, DopamineMonitorService::class.java).apply {
                action = ACTION_START
                setPackage(packageName)
            }
            val pi = PendingIntent.getService(
                applicationContext,
                1002,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            val am = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500L, pi)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        if (prefs.getBoolean("service_running", true)) {
            sendBroadcast(Intent("vn.khkt.dopaminedecay.ACTION_REVIVE_SERVICE").setPackage(packageName))
        }
    }
}
