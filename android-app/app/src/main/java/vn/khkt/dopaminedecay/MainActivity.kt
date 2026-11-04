package vn.khkt.dopaminedecay

import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var statusBanner: TextView
    private lateinit var btnMainToggle: Button
    private lateinit var btnPermOverlay: Button
    private lateinit var btnPermUsage: Button
    private lateinit var btnPermAcc: Button
    private lateinit var btnPermBattery: Button
    private lateinit var seekTestTime: SeekBar
    private lateinit var tvTestTimeStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("dopamine_decay_prefs", Context.MODE_PRIVATE)

        // Mặc định BẬT chạy nền tự động và chế độ kiểm thử 120s ở Mức 0
        if (!prefs.contains("service_running")) {
            prefs.edit()
                .putBoolean("service_running", true)
                .putBoolean("test_120s_mode", true)
                .putInt("demo_override", -1)
                .apply()
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F8FAFC")) // Giao diện trắng sáng chuẩn yêu cầu
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 64)
        }
        scroll.addView(root)

        // 1. HEADER
        val title = TextView(this).apply {
            text = "🛡️ TỰ PHÂN HUỶ ĐỘ HẤP DẪN MÀN HÌNH
Hệ thống Kiểm Soát Nghiêm Ngặt (Android V8)"
            textSize = 17.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F172A"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        root.addView(title)

        // 2. BANNER TRẠNG THÁI
        statusBanner = TextView(this).apply {
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(32, 28, 32, 28)
            background = GradientDrawable().apply {
                cornerRadius = 28f
                setColor(Color.parseColor("#059669"))
            }
        }
        root.addView(statusBanner)

        // 3. NÚT BẬT / TẮT CHƯƠNG TRÌNH (Tắt cần mật khẩu Phụ huynh 666666) + NÚT THU NHỎ CHẠY NỀN NGAY LẬP TỨC
        btnMainToggle = Button(this).apply {
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 10)
            }
            layoutParams = lp
            setOnClickListener { onMainToggleClicked() }
        }
        root.addView(btnMainToggle)

        val btnMinimizeNow = Button(this).apply {
            text = "⚡ THU NHỎ ỨNG DỤNG & CHẠY NỀN NGAY LẬP TỨC"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.parseColor("#0284C7"))
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 20)
            }
            layoutParams = lp
            setOnClickListener {
                startMonitorServiceIfPermitted()
                moveTaskToBack(true)
                Toast.makeText(
                    this@MainActivity,
                    "🟢 Đã thu nhỏ chạy nền! Hãy mở YouTube, TikTok, Facebook hoặc Roblox.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        root.addView(btnMinimizeNow)

        // 4. MỞ TRỰC TIẾP 4 ỨNG DỤNG CHỈ ĐỊNH ĐỂ KIỂM THỬ NGAY
        val quickLaunchTitle = TextView(this).apply {
            text = "🚀 MỞ NHANH ỨNG DỤNG CHỈ ĐỊNH ĐỂ KIỂM THỬ:"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1E293B"))
            setPadding(0, 8, 0, 10)
        }
        root.addView(quickLaunchTitle)

        val launchRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val launchRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        fun createLaunchBtn(label: String, bgHex: String, pkgList: List<String>, fallbackUrl: String): Button {
            return Button(this).apply {
                text = label
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundColor(Color.parseColor(bgHex))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(6, 6, 6, 6)
                }
                setOnClickListener {
                    startMonitorServiceIfPermitted()
                    launchTargetAppOrWeb(pkgList, fallbackUrl)
                }
            }
        }

        launchRow1.addView(createLaunchBtn("🔴 Mở YouTube", "#DC2626", listOf("com.google.android.youtube", "com.google.android.apps.youtube.kids"), "https://m.youtube.com"))
        launchRow1.addView(createLaunchBtn("⚫ Mở TikTok", "#0F172A", listOf("com.ss.android.ugc.trill", "com.zhiliaoapp.musically", "com.zhiliaoapp.musically.go"), "https://www.tiktok.com"))
        launchRow2.addView(createLaunchBtn("🔵 Mở Facebook", "#2563EB", listOf("com.facebook.katana", "com.facebook.lite"), "https://m.facebook.com"))
        launchRow2.addView(createLaunchBtn("🟣 Mở Roblox / Game", "#7C3AED", listOf("com.roblox.client", "com.roblox.client.vnggames"), "https://www.roblox.com"))

        root.addView(launchRow1)
        root.addView(launchRow2)

        // 5. THANH TRƯỢT KIỂM THỬ THỜI GIAN 120 GIÂY (MỨC 0 -> MỨC 4) & ĐẶT LẠI CẦN MẬT KHẨU
        val testBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(Color.WHITE)
                setStroke(2, Color.parseColor("#E2E8F0"))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 16)
            }
            layoutParams = lp
        }

        val cbTest120s = CheckBox(this).apply {
            text = "⚡ Chế độ kiểm thử 120 giây (Mức 0 → Mức 4 trình diễn Ban Giám Khảo)"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F172A"))
            isChecked = prefs.getBoolean("test_120s_mode", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("test_120s_mode", checked).apply()
            }
        }
        testBox.addView(cbTest120s)

        tvTestTimeStatus = TextView(this).apply {
            text = "⏱️ Thời gian hiện tại: 0s / 120s (Mặc định Mức 0)"
            textSize = 13.5f
            setTextColor(Color.parseColor("#0284C7"))
            setPadding(0, 10, 0, 8)
        }
        testBox.addView(tvTestTimeStatus)

        seekTestTime = SeekBar(this).apply {
            max = 120
            progress = prefs.getFloat("saved_active_seconds", 0f).toInt().coerceIn(0, 120)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val isLocked = prefs.getBoolean("hard_locked_level4", false)
                        if (isLocked) {
                            Toast.makeText(
                                this@MainActivity,
                                "🔒 Đã khóa cứng ở Mức 4! Cần mật khẩu Phụ huynh (666666) để đặt lại.",
                                Toast.LENGTH_SHORT
                            ).show()
                            sb?.progress = 120
                            promptPasswordToResetTimer()
                            return
                        }
                        prefs.edit().putFloat("saved_active_seconds", value.toFloat()).apply()
                        val lvl = when {
                            value >= 120 -> 4
                            value >= 90 -> 3
                            value >= 60 -> 2
                            value >= 30 -> 1
                            else -> 0
                        }
                        if (lvl == 4) {
                            prefs.edit().putBoolean("hard_locked_level4", true).apply()
                        }
                        tvTestTimeStatus.text = "⏱️ Thời gian kiểm thử: ${value}s / 120s (Mức $lvl)"
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        testBox.addView(seekTestTime)

        val btnResetTimer = Button(this).apply {
            text = "🔑 Đặt lại thời gian về Mức 0 (Yêu cầu mật khẩu Phụ huynh)"
            textSize = 13f
            setBackgroundColor(Color.parseColor("#F59E0B"))
            setTextColor(Color.WHITE)
            setOnClickListener { promptPasswordToResetTimer() }
        }
        testBox.addView(btnResetTimer)
        root.addView(testBox)

        // 6. CÁC BƯỚC CẤP QUYỀN HỆ THỐNG ĐỂ KIỂM SOÁT 100% KHÔNG BỊ TẮT NGẦM
        val permHeader = TextView(this).apply {
            text = "📋 CẤP QUYỀN HỆ THỐNG ĐỂ KIỂM SOÁT NGHIÊM NGẶT:"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1E293B"))
            setPadding(0, 12, 0, 8)
        }
        root.addView(permHeader)

        btnPermOverlay = Button(this).apply {
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            }
        }
        root.addView(btnPermOverlay)

        btnPermUsage = Button(this).apply {
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
        root.addView(btnPermUsage)

        btnPermAcc = Button(this).apply {
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(
                    this@MainActivity,
                    "Bật 'Tự Phân Huỷ Độ Hấp Dẫn - Kiểm soát chặt chẽ' trong mục Dịch vụ đã cài đặt",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        root.addView(btnPermAcc)

        btnPermBattery = Button(this).apply {
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(i)
                    } catch (_: Exception) {}
                }
            }
        }
        root.addView(btnPermBattery)

        // 7. TUỲ CHỌN ĐỘ TUỔI & 4 ỨNG DỤNG CHỈ ĐỊNH (Không cần mật khẩu khi tùy chọn)
        val optHeader = TextView(this).apply {
            text = "
🎯 TUỲ CHỌN NHÓM TUỔI & 4 ỨNG DỤNG CHỈ ĐỊNH:"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1E293B"))
        }
        root.addView(optHeader)

        val ageGroup = RadioGroup(this).apply {
            val ages = listOf(
                "3-6" to "👶 3 – 6 tuổi (Mầm non: Bạn Gấu đi ngủ, Sepia ấm)",
                "7-9" to "🧒 7 – 9 tuổi (Tiểu học: Vươn vai 10 nhịp cảm biến gia tốc)",
                "10-12" to "🧑 10 – 12 tuổi (Lớp 5–7: Thang xám 100% & Khóa cứng Mức 4)"
            )
            val savedAge = prefs.getString("age_group", "10-12")
            ages.forEach { (k, label) ->
                val rb = RadioButton(this@MainActivity).apply {
                    text = label
                    textSize = 13.5f
                    setTextColor(Color.parseColor("#334155"))
                    isChecked = (k == savedAge)
                    setOnClickListener {
                        prefs.edit().putString("age_group", k).apply()
                    }
                }
                addView(rb)
            }
        }
        root.addView(ageGroup)

        val platforms = listOf(
            "youtube" to "🔴 Kiểm soát chặt chẽ YouTube (App YouTube, YouTube Kids & Web)",
            "tiktok" to "⚫ Kiểm soát chặt chẽ TikTok (App TikTok, TikTok Lite & Web)",
            "facebook" to "🔵 Kiểm soát chặt chẽ Facebook (App Facebook, Reels & Web)",
            "webgame" to "🟣 Kiểm soát chặt chẽ Roblox & Web Game (App Roblox, Poki, Y8...)"
        )
        platforms.forEach { (key, label) ->
            val cb = CheckBox(this).apply {
                text = label
                textSize = 13.5f
                setTextColor(Color.parseColor("#334155"))
                isChecked = prefs.getBoolean("plat_$key", true)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean("plat_$key", checked).apply()
                }
            }
            root.addView(cb)
        }

        setContentView(scroll)
        startMonitorServiceIfPermitted()
    }

    override fun onResume() {
        super.onResume()
        refreshStatusUI()
    }

    private fun refreshStatusUI() {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        val hasUsage = hasUsageStatsPermission()
        val hasAcc = DopamineAccessibilityService.instance != null || isAccessibilityEnabled()
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val hasBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(packageName) == true
        } else true

        val isRunning = prefs.getBoolean("service_running", true)
        val isHardLocked = prefs.getBoolean("hard_locked_level4", false)
        val curSec = prefs.getFloat("saved_active_seconds", 0f).toInt()

        seekTestTime.progress = curSec.coerceIn(0, 120)
        tvTestTimeStatus.text = if (isHardLocked) {
            "🔒 ĐÃ KHÓA CỨNG Ở MỨC 4 (120s) - Phải nhập mật khẩu 666666 để mở lại!"
        } else {
            "⏱️ Thời gian hiện tại: ${curSec}s / 120s"
        }

        if (isRunning && hasOverlay && (hasUsage || hasAcc)) {
            statusBanner.text = "🟢 HỆ THỐNG ĐANG CHẠY NỀN KIỂM SOÁT CHẶT CHẼ
(YouTube • TikTok • Facebook • Roblox / Web Game)"
            (statusBanner.background as? GradientDrawable)?.setColor(Color.parseColor("#059669"))
            btnMainToggle.text = "🔒 TẮT CHƯƠNG TRÌNH (YÊU CẦU MẬT KHẨU PHỤ HUYNH)"
            btnMainToggle.setBackgroundColor(Color.parseColor("#DC2626"))
        } else {
            statusBanner.text = "⚠️ CẦN CẤP ĐỦ QUYỀN BÊN DƯỚI ĐỂ KIỂM SOÁT CÁC ỨNG DỤNG"
            (statusBanner.background as? GradientDrawable)?.setColor(Color.parseColor("#D97706"))
            btnMainToggle.text = "▶️ BẬT CHẠY NỀN KIỂM SOÁT NGAY"
            btnMainToggle.setBackgroundColor(Color.parseColor("#059669"))
        }

        btnPermOverlay.text = if (hasOverlay) "✅ 1. Quyền Lớp phủ màn hình (ĐÃ CẤP)" else "⚠️ 1. Cấp quyền Lớp phủ màn hình (Bắt buộc - Bấm để bật)"
        btnPermUsage.text = if (hasUsage) "✅ 2. Quyền Giám sát mở ứng dụng (ĐÃ CẤP)" else "⚠️ 2. Cấp quyền Truy cập dữ liệu sử dụng (Bắt buộc - Bấm để bật)"
        btnPermAcc.text = if (hasAcc) "✅ 3. Dịch vụ Trợ năng Kiểm soát chặt chẽ (ĐÃ BẬT)" else "⚠️ 3. Bật Dịch vụ Trợ năng (Bắt Web Game & Chặn vượt rào)"
        btnPermBattery.text = if (hasBattery) "✅ 4. Chạy ngầm không giới hạn Pin (ĐÃ CẤP)" else "⚙️ 4. Cho phép chạy ngầm không giới hạn Pin (Bấm để bật)"
    }

    private fun onMainToggleClicked() {
        val isRunning = prefs.getBoolean("service_running", true)
        if (!isRunning) {
            prefs.edit().putBoolean("service_running", true).apply()
            startMonitorServiceIfPermitted()
            refreshStatusUI()
        } else {
            // Tắt chương trình BẮT BUỘC phải có mật khẩu Phụ huynh 666666
            val input = EditText(this).apply {
                hint = "Nhập mật khẩu Phụ huynh (666666)"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }
            AlertDialog.Builder(this)
                .setTitle("🔒 Xác thực Phụ huynh để Tắt")
                .setMessage("Nhập mật khẩu Phụ huynh (Mặc định: 666666) để tạm dừng hệ thống kiểm soát:")
                .setView(input)
                .setPositiveButton("Xác nhận") { _, _ ->
                    val pin = input.text.toString().trim()
                    val savedPin = prefs.getString("parent_pin", TargetAppRegistry.DEFAULT_PIN) ?: TargetAppRegistry.DEFAULT_PIN
                    if (TargetAppRegistry.verifyPin(pin, savedPin)) {
                        prefs.edit().putBoolean("service_running", false).apply()
                        stopService(Intent(this, DopamineMonitorService::class.java).apply {
                            action = DopamineMonitorService.ACTION_STOP
                        })
                        refreshStatusUI()
                        Toast.makeText(this, "Đã tắt dịch vụ kiểm soát.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "❌ Sai mật khẩu Phụ huynh!", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun promptPasswordToResetTimer() {
        val input = EditText(this).apply {
            hint = "Nhập mật khẩu Phụ huynh (666666)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("🔑 Đặt lại thời gian & Mở khóa Mức 4")
            .setMessage("Việc đặt lại thời gian hoặc mở khóa Mức 4 bắt buộc phải có mật khẩu Phụ huynh (Mặc định: 666666):")
            .setView(input)
            .setPositiveButton("Mở khóa & Đặt lại") { _, _ ->
                val pin = input.text.toString().trim()
                val savedPin = prefs.getString("parent_pin", TargetAppRegistry.DEFAULT_PIN) ?: TargetAppRegistry.DEFAULT_PIN
                if (TargetAppRegistry.verifyPin(pin, savedPin)) {
                    prefs.edit()
                        .putBoolean("hard_locked_level4", false)
                        .putFloat("saved_active_seconds", 0f)
                        .putInt("demo_override", -1)
                        .apply()
                    startMonitorServiceIfPermitted()
                    refreshStatusUI()
                    Toast.makeText(this, "✅ Đã mở khóa Mức 4 và đặt lại thời gian về 0s (Mức 0)!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ Sai mật khẩu Phụ huynh!", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun startMonitorServiceIfPermitted() {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        if (!hasOverlay) return
        prefs.edit().putBoolean("service_running", true).apply()
        val intent = Intent(this, DopamineMonitorService::class.java).apply {
            action = DopamineMonitorService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: Exception) {}
    }

    private fun launchTargetAppOrWeb(pkgCandidates: List<String>, fallbackUrl: String) {
        for (pkg in pkgCandidates) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Báo trước cho MonitorService biết gói mục tiêu vừa được mở
                DopamineMonitorService.onAccessibilityPackageDetected(pkg, "", false)
                startActivity(launchIntent)
                return
            }
        }
        // Nếu máy chưa cài App gốc, mở trong Kén Trình Duyệt Kiểm Soát Nội Bộ
        val cocoonIntent = Intent(this, SafeCocoonActivity::class.java).apply {
            putExtra("target_url", fallbackUrl)
        }
        DopamineMonitorService.onAccessibilityPackageDetected("com.android.chrome", fallbackUrl, false)
        startActivity(cocoonIntent)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(packageName)
    }
}
