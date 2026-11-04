package vn.khkt.dopaminedecay

import java.security.MessageDigest

/**
 * DANH MỤC ỨNG DỤNG MỤC TIÊU (TargetAppRegistry.kt)
 * Nơi tập trung toàn bộ danh sách package, quy tắc phân loại và kiểm tra mật khẩu Phụ huynh,
 * dùng chung cho DopamineMonitorService, DopamineAccessibilityService và MainActivity.
 *
 * Các nhóm (category) trả về khớp với khoá cài đặt "plat_<category>" trong MainActivity:
 *   "youtube" | "tiktok" | "facebook" | "webgame"
 */
object TargetAppRegistry {

    /** Mật khẩu Phụ huynh mặc định (dùng khi chưa đặt "parent_pin"). */
    const val DEFAULT_PIN = "666666"

    // ─── PACKAGE ỨNG DỤNG GỐC ────────────────────────────────────────────────

    val YOUTUBE_PACKAGES = setOf(
        "com.google.android.youtube",
        "com.google.android.apps.youtube.kids",
        "com.google.android.apps.youtube.music",
        "app.revanced.android.youtube",
        "com.vanced.android.youtube"
    )

    val TIKTOK_PACKAGES = setOf(
        "com.zhiliaoapp.musically",
        "com.zhiliaoapp.musically.go",
        "com.ss.android.ugc.trill",
        "com.ss.android.ugc.aweme",
        "com.ss.android.ugc.tiktok.lite"
    )

    val FACEBOOK_PACKAGES = setOf(
        "com.facebook.katana",
        "com.facebook.lite",
        "com.facebook.orca",
        "com.instagram.android"
    )

    val WEBGAME_PACKAGES = setOf(
        "com.roblox.client",
        "com.mojang.minecraftpe",
        "com.supercell.clashofclans",
        "com.dts.freefireth",
        "com.garena.game.kgvn",
        "com.vng.pubgmobile",
        "com.tencent.ig"
    )

    /** Trình duyệt: cần đọc URL/tiêu đề để biết trẻ đang xem trang nào. */
    val BROWSER_PACKAGES = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.coccoc.trinhduyet",
        "com.UCMobile.intl",
        "com.mi.globalbrowser",
        "com.heytap.browser",
        "com.vivo.browser",
        "com.android.browser"
    )

    /** Gói hệ thống / bàn phím / launcher: bỏ qua, không được tính là "ứng dụng đang mở". */
    val IGNORED_SYSTEM_PACKAGES = setOf(
        "vn.khkt.dopaminedecay",
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.oppo.launcher",
        "com.coloros.launcher",
        "com.realme.launcher",
        "com.vivo.launcher",
        "com.huawei.android.launcher",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.sec.android.inputmethod",
        "com.touchtype.swiftkey",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )

    // ─── TỪ KHOÁ NHẬN DIỆN TRÊN WEB ──────────────────────────────────────────

    private val YOUTUBE_WEB = listOf("youtube.com", "youtu.be", "m.youtube", "youtube")
    private val TIKTOK_WEB = listOf("tiktok.com", "tiktok")
    private val FACEBOOK_WEB = listOf("facebook.com", "fb.com", "fb.watch", "m.facebook", "instagram.com")
    private val WEBGAME_WEB = listOf(
        "roblox.com", "poki.com", "y8.com", "miniclip.com", "crazygames.com",
        "friv.com", "game24h", "gamevui", "chơi game", "choi game", "play game"
    )

    /** Từ khoá nội dung học tập: video bài giảng sẽ không bị tính giờ phân huỷ. */
    private val EDUCATION_KEYWORDS = listOf(
        "bài giảng", "bai giang", "bài học", "bai hoc", "học toán", "hoc toan",
        "tiếng anh", "tieng anh", "toán lớp", "toan lop", "lớp 1", "lớp 2", "lớp 3",
        "lớp 4", "lớp 5", "lớp 6", "lớp 7", "tin học", "tin hoc", "khoa học", "khoa hoc",
        "giải bài", "giai bai", "ôn tập", "on tap", "luyện thi", "luyen thi",
        "học tập", "hoc tap", "giáo dục", "giao duc", "sgk", "kết nối tri thức",
        "chân trời sáng tạo", "cánh diều", "vtv7", "olm", "hocmai", "vietjack",
        "lesson", "lecture", "tutorial", "khan academy", "education", "learn english",
        "math for kids", "phonics", "science for kids"
    )

    // ─── PHÂN LOẠI ───────────────────────────────────────────────────────────

    /**
     * Xác định nhóm của ứng dụng đang mở.
     * @param pkg package đang ở tiền cảnh
     * @param browserContext URL/tiêu đề đọc được từ cửa sổ (chỉ có khi là trình duyệt)
     * @return "youtube" | "tiktok" | "facebook" | "webgame", hoặc null nếu không phải mục tiêu
     */
    fun classifyPackage(pkg: String, browserContext: String = ""): String? {
        if (pkg.isEmpty() || pkg in IGNORED_SYSTEM_PACKAGES) return null

        when (pkg) {
            in YOUTUBE_PACKAGES -> return "youtube"
            in TIKTOK_PACKAGES -> return "tiktok"
            in FACEBOOK_PACKAGES -> return "facebook"
            in WEBGAME_PACKAGES -> return "webgame"
        }

        if (pkg in BROWSER_PACKAGES && browserContext.isNotBlank()) {
            val ctx = browserContext.lowercase()
            return when {
                YOUTUBE_WEB.any { ctx.contains(it) } -> "youtube"
                TIKTOK_WEB.any { ctx.contains(it) } -> "tiktok"
                FACEBOOK_WEB.any { ctx.contains(it) } -> "facebook"
                WEBGAME_WEB.any { ctx.contains(it) } -> "webgame"
                else -> null
            }
        }
        return null
    }

    /** Nhận diện video/trang có nội dung học tập dựa trên tiêu đề hoặc URL. */
    fun isEducationalContent(context: String): Boolean {
        if (context.isBlank()) return false
        val ctx = context.lowercase()
        return EDUCATION_KEYWORDS.any { ctx.contains(it) }
    }

    /** Tên hiển thị thân thiện của từng nhóm. */
    fun getPlatformDisplayName(category: String): String = when (category) {
        "youtube" -> "YouTube"
        "tiktok" -> "TikTok"
        "facebook" -> "Facebook"
        "webgame" -> "Roblox / Web Game"
        "" -> "Giải trí"
        else -> category.replaceFirstChar { it.uppercase() }
    }

    // ─── MẬT KHẨU PHỤ HUYNH ──────────────────────────────────────────────────

    /**
     * So khớp mật khẩu nhập vào với mật khẩu đã lưu.
     * Chấp nhận mật khẩu lưu dạng thường hoặc dạng băm SHA-256 (64 ký tự hex).
     * So sánh theo thời gian hằng để tránh dò mật khẩu theo thời gian phản hồi.
     */
    fun verifyPin(entered: String, saved: String): Boolean {
        val input = entered.trim()
        if (input.isEmpty()) return false
        val stored = saved.trim().ifEmpty { DEFAULT_PIN }
        val isHashed = stored.length == 64 && stored.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
        val candidate = if (isHashed) sha256(input) else input
        return MessageDigest.isEqual(
            candidate.lowercase().toByteArray(),
            stored.lowercase().toByteArray()
        )
    }

    fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
