package vn.khkt.dopaminedecay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * DỊCH VỤ TRỢ NĂNG KIỂM SOÁT NGHIÊM NGẶT (DopamineAccessibilityService.kt)
 * 1. Nhận diện tức thì (độ trễ < 20ms) khi mở App YouTube, TikTok, Facebook, Roblox.
 * 2. Quét thanh URL & tiêu đề trên Chrome / Samsung Internet / Cốc Cốc để bắt chính xác Web Game (Roblox.com, Poki, Y8...)
 *    và bài giảng học tập (EDU_KEYWORDS).
 * 3. Cộng dồn thao tác cuộn/vuốt (typeViewScrolled) vào công thức T_eff.
 * 4. Đảm bảo dịch vụ DopamineMonitorService luôn sống 100%.
 */
class DopamineAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: DopamineAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        ensureMonitorServiceRunning()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // Bỏ qua các gói giao diện hệ thống, bàn phím hoặc chính lớp phủ của ứng dụng
        if (TargetAppRegistry.IGNORED_SYSTEM_PACKAGES.contains(pkg)) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                var windowTextContext = ""
                var isEduLecture = false

                // Nếu là trình duyệt hoặc YouTube, trích xuất nhanh URL / Tiêu đề để nhận diện Web Game & Bài giảng
                if (TargetAppRegistry.BROWSER_PACKAGES.contains(pkg) ||
                    pkg.contains("browser") ||
                    pkg.contains("chrome") ||
                    pkg.contains("youtube")) {
                    windowTextContext = extractVisibleUrlAndTitle()
                    if (pkg.contains("youtube") || windowTextContext.contains("youtube")) {
                        isEduLecture = TargetAppRegistry.isEducationalContent(windowTextContext)
                    }
                }

                // Báo cáo ngay lập tức về DopamineMonitorService
                DopamineMonitorService.onAccessibilityPackageDetected(
                    pkgName = pkg,
                    browserContext = windowTextContext,
                    eduDetected = isEduLecture
                )

                // Nếu phát hiện mở ứng dụng mục tiêu, đảm bảo ForegroundService đang chạy
                val matchedCategory = TargetAppRegistry.classifyPackage(pkg, windowTextContext)
                if (matchedCategory != null) {
                    ensureMonitorServiceRunning()
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                DopamineMonitorService.notifyUserScrolled()
            }
        }
    }

    /**
     * Đọc nhanh thanh URL trình duyệt và tiêu đề trang (giới hạn chiều sâu cây DOM để đạt độ trễ < 18ms)
     */
    private fun extractVisibleUrlAndTitle(): String {
        val root = try { rootInActiveWindow } catch (_: Exception) { null } ?: return ""
        val sb = StringBuilder()
        try {
            // Tìm trực tiếp các ID thanh địa chỉ phổ biến của Chrome, Samsung Browser, Firefox, Edge, Cốc Cốc
            val urlBarIds = listOf(
                "com.android.chrome:id/url_bar",
                "com.sec.android.app.sbrowser:id/location_bar_edit_text",
                "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
                "com.microsoft.emmx:id/url_bar",
                "com.coccoc.trinhduyet:id/url_bar",
                "com.opera.browser:id/url_field",
                "com.brave.browser:id/url_bar"
            )
            for (id in urlBarIds) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    for (n in nodes) {
                        n.text?.let { sb.append(it).append(" ") }
                    }
                }
            }
            // Quét nhẹ 25 nút đầu tiên để lấy tiêu đề bài giảng hoặc tên Web Game (Roblox, Poki, Y8)
            traverseNodesLimited(root, sb, 0, 25)
        } catch (_: Exception) {
        } finally {
            try { root.recycle() } catch (_: Exception) {}
        }
        return sb.toString()
    }

    private fun traverseNodesLimited(node: AccessibilityNodeInfo?, sb: StringBuilder, depth: Int, maxDepth: Int) {
        if (node == null || depth > maxDepth || sb.length > 600) return
        node.text?.let {
            if (it.length in 3..120) {
                sb.append(it).append(" ")
            }
        }
        val count = node.childCount.coerceAtMost(8)
        for (i in 0 until count) {
            val child = try { node.getChild(i) } catch (_: Exception) { null }
            if (child != null) {
                traverseNodesLimited(child, sb, depth + 1, maxDepth)
                try { child.recycle() } catch (_: Exception) {}
            }
        }
    }

    private fun ensureMonitorServiceRunning() {
        val prefs = getSharedPreferences("dopamine_decay_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("service_running", true)) return
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

    /**
     * Cưỡng chế đưa về màn hình chính nếu trẻ cố tình vượt mặt màn khóa Mức 4
     */
    fun enforceGlobalHomeAction() {
        try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onInterrupt() {}
}
