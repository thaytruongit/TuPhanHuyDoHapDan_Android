package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AgeGroup
import com.example.model.DecayLevelMode
import com.example.model.SessionState
import com.example.model.TargetPlatform
import com.example.ui.components.VisualDecayHelper
import com.example.ui.components.applyScreenDecayEffect
import com.example.ui.components.applyVisualCoolDown
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimulatorScreen(
    sessionState: SessionState,
    onSelectAgeGroup: (AgeGroup) -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleTargetPlatform: (TargetPlatform) -> Unit,
    onSelectPreviewPlatform: (TargetPlatform, Boolean) -> Unit,
    onToggleExemptYoutubeEducation: (Boolean) -> Unit,
    onToggleBackgroundMonitor: (Boolean) -> Unit,
    onToggleFastForward30x: (Boolean) -> Unit,
    onSelectDecayLevelMode: (DecayLevelMode) -> Unit,
    onSendTestNotification: () -> Unit,
    onPlayLullabyPreview: () -> Unit,
    onTriggerMovementStationDirectly: () -> Unit,
    onTriggerReflectionDirectly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = sessionState.progress
    val isExempted = sessionState.isGrayscaleExemptedNow
    val blurRadius = VisualDecayHelper.calculateVisualCoolDownBlur(progress, isExempted)

    val coroutineScope = rememberCoroutineScope()
    var scrollFrictionNotice by remember { mutableStateOf(false) }
    var isLoadingMoreFeed by remember { mutableStateOf(false) }
    var extraFeedCount by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(sessionState.currentDecayLevel) {
                if (sessionState.infiniteScrollDelayMs > 0L) {
                    detectVerticalDragGestures { _, _ ->
                        scrollFrictionNotice = true
                        coroutineScope.launch {
                            delay(sessionState.infiniteScrollDelayMs)
                            scrollFrictionNotice = false
                        }
                    }
                }
            },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. PHÂN KHÚC ĐỘ TUỔI (3–12 TUỔI)
        item {
            Text(
                text = "1. PHÂN KHÚC ĐỘ TUỔI TRẺ EM (3 – 12 TUỔI)",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgeGroup.values().forEach { group ->
                    val isSelected = sessionState.ageGroup == group
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectAgeGroup(group) },
                        label = { Text("${group.title} (${group.ageRange})") },
                        leadingIcon = {
                            when (group) {
                                AgeGroup.TODDLER -> Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                                AgeGroup.PRIMARY -> Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                                AgeGroup.MIDDLE_SCHOOL -> Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("chip_age_group_${group.name.lowercase()}")
                    )
                }
            }
        }

        // 2. KIỂM SOÁT CHẠY NỀN 4 NHÓM MỤC TIÊU (Giống hệt hình 1)
        item {
            TargetPlatformsControlCard(
                sessionState = sessionState,
                onToggleTargetPlatform = onToggleTargetPlatform,
                onToggleExemptYoutubeEducation = onToggleExemptYoutubeEducation,
                onToggleBackgroundMonitor = onToggleBackgroundMonitor,
                onSendTestNotification = onSendTestNotification
            )
        }

        // 3. CHẾ ĐỘ TRÌNH DIỄN GIÁM KHẢO KHKT (DEMO) (Giống hệt hình 2)
        item {
            JudgeKhktDemoCard(
                sessionState = sessionState,
                onToggleFastForward30x = onToggleFastForward30x,
                onSelectDecayLevelMode = onSelectDecayLevelMode
            )
        }

        // 4. BẢNG CHỈ SỐ TỰ PHÂN HỦY ĐỘ HẤP DẪN THỜI GIAN THỰC
        item {
            ScientificHudCard(
                sessionState = sessionState,
                onToggleRunning = onToggleRunning,
                onReset = onReset
            )
        }

        // Cảnh báo trễ cuộn vô tận 900ms
        item {
            AnimatedVisibility(visible = scrollFrictionNotice) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ma sát nhận thức: Đang làm trễ cuộn vô tận (${sessionState.infiniteScrollDelayMs} ms) để cắt vòng lặp Dopamine!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // 5. KHÔNG GIAN MÔ PHỎNG TRỰC QUAN 4 NHÓM MỤC TIÊU & BÀI GIẢNG YOUTUBE
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Màn Hình Mô Phỏng Tự Phân Hủy:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = when (sessionState.ageGroup) {
                        AgeGroup.TODDLER -> "Mở Bạn Gấu (15p)"
                        AgeGroup.PRIMARY -> "Mở Trạm VĐ (25p)"
                        AgeGroup.MIDDLE_SCHOOL -> "Gõ Mục Tiêu (30p)"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            when (sessionState.ageGroup) {
                                AgeGroup.TODDLER -> onPlayLullabyPreview()
                                AgeGroup.PRIMARY -> onTriggerMovementStationDirectly()
                                AgeGroup.MIDDLE_SCHOOL -> onTriggerReflectionDirectly()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("btn_quick_trigger_mechanism")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thanh chọn kênh mô phỏng để Giám khảo kiểm tra trực tiếp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TargetPlatform.values().forEach { platform ->
                    val isSelected = sessionState.selectedPreviewPlatform == platform && !sessionState.isViewingYoutubeLecture
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPreviewPlatform(platform, false) },
                        label = { Text("${platform.emoji} ${platform.title}") }
                    )
                }
                // Tab Bài giảng học tập YouTube để kiểm chứng tính năng miễn giảm làm xám
                FilterChip(
                    selected = sessionState.isViewingYoutubeLecture,
                    onClick = { onSelectPreviewPlatform(TargetPlatform.YOUTUBE_SHORTS, true) },
                    label = { Text("🎓 Bài giảng học tập YouTube") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF065F46),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("chip_preview_youtube_lecture")
                )
            }
        }

        // Vùng hiển thị nội dung áp dụng trực tiếp bộ lọc tự phân hủy (Trắng đen 100% / Sepia / Ẩn Like-View-Thông báo đỏ)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .applyScreenDecayEffect(
                        ageGroup = sessionState.ageGroup,
                        progress = progress,
                        isExempted = isExempted
                    )
            ) {
                SimulatedTargetPlatformFeed(
                    sessionState = sessionState,
                    blurRadius = blurRadius,
                    extraFeedCount = extraFeedCount,
                    isLoadingMoreFeed = isLoadingMoreFeed,
                    onLoadMoreInfiniteScroll = {
                        if (!isLoadingMoreFeed) {
                            coroutineScope.launch {
                                isLoadingMoreFeed = true
                                val delayMs = sessionState.infiniteScrollDelayMs
                                if (delayMs > 0L) {
                                    scrollFrictionNotice = true
                                    delay(delayMs)
                                    scrollFrictionNotice = false
                                }
                                extraFeedCount++
                                isLoadingMoreFeed = false
                            }
                        }
                    },
                    onOpenLearningGoalModal = onTriggerReflectionDirectly
                )
            }
        }
    }
}

/**
 * Giao diện Kiểm soát chạy nền 4 nhóm mục tiêu & Miễn giảm làm xám bài giảng YouTube (Giống hình 1)
 */
@Composable
fun TargetPlatformsControlCard(
    sessionState: SessionState,
    onToggleTargetPlatform: (TargetPlatform) -> Unit,
    onToggleExemptYoutubeEducation: (Boolean) -> Unit,
    onToggleBackgroundMonitor: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "2. KIỂM SOÁT CHẠY NỀN 4 NHÓM MỤC TIÊU",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                // Nút gửi thử Local Notification ngay lập tức
                Surface(
                    color = Color(0xFF1E3A8A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { onSendTestNotification() }
                        .testTag("btn_test_local_notification")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF7DD3FC),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bắn Notification",
                            color = Color(0xFFE0F2FE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Hàng 1: YouTube & Shorts | TikTok Web
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TargetPlatformPill(
                    platform = TargetPlatform.YOUTUBE_SHORTS,
                    checked = sessionState.enabledTargets.contains(TargetPlatform.YOUTUBE_SHORTS),
                    onToggle = { onToggleTargetPlatform(TargetPlatform.YOUTUBE_SHORTS) },
                    modifier = Modifier.weight(1f)
                )
                TargetPlatformPill(
                    platform = TargetPlatform.TIKTOK_WEB,
                    checked = sessionState.enabledTargets.contains(TargetPlatform.TIKTOK_WEB),
                    onToggle = { onToggleTargetPlatform(TargetPlatform.TIKTOK_WEB) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Hàng 2: Facebook & Reels | Web Game
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TargetPlatformPill(
                    platform = TargetPlatform.FACEBOOK_REELS,
                    checked = sessionState.enabledTargets.contains(TargetPlatform.FACEBOOK_REELS),
                    onToggle = { onToggleTargetPlatform(TargetPlatform.FACEBOOK_REELS) },
                    modifier = Modifier.weight(1f)
                )
                TargetPlatformPill(
                    platform = TargetPlatform.WEB_GAME,
                    checked = sessionState.enabledTargets.contains(TargetPlatform.WEB_GAME),
                    onToggle = { onToggleTargetPlatform(TargetPlatform.WEB_GAME) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Hàng 3: Miễn giảm làm xám cho bài giảng học tập YouTube
            Surface(
                color = Color(0xFF0B2926),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExemptYoutubeEducation(!sessionState.exemptYoutubeEducation) }
                    .testTag("toggle_exempt_youtube_education")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "🎓", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Miễn giảm làm xám cho bài giảng học tập YouTube",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Checkbox(
                        checked = sessionState.exemptYoutubeEducation,
                        onCheckedChange = { onToggleExemptYoutubeEducation(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF3B82F6),
                            checkmarkColor = Color.White,
                            uncheckedColor = Color(0xFF64748B)
                        )
                    )
                }
            }

            // Trạng thái dịch vụ chạy nền trên hệ thống ngay cả khi tắt ứng dụng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (sessionState.isBackgroundMonitorActive)
                            "🟢 Chạy nền hệ thống (Hoạt động ngay cả khi tắt ứng dụng)"
                        else
                            "⚪ Dịch vụ chạy nền hệ thống: Đang tạm dừng",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (sessionState.isBackgroundMonitorActive) Color(0xFF34D399) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Foreground Service (START_STICKY) giám sát ${sessionState.enabledTargets.size}/4 nhóm mục tiêu",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    )
                }
                Switch(
                    checked = sessionState.isBackgroundMonitorActive,
                    onCheckedChange = onToggleBackgroundMonitor,
                    modifier = Modifier.testTag("switch_bg_monitor")
                )
            }
        }
    }
}

@Composable
private fun TargetPlatformPill(
    platform: TargetPlatform,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF162032),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (checked) Color(0xFF334155) else Color(0xFF1E293B)),
        modifier = modifier
            .clickable { onToggle() }
            .testTag("target_pill_${platform.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = platform.emoji, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = platform.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (checked) Color(0xFF60A5FA) else Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3. CHẾ ĐỘ TRÌNH DIỄN GIÁM KHẢO KHKT (DEMO) (Giống hệt hình 2)
 */
@Composable
fun JudgeKhktDemoCard(
    sessionState: SessionState,
    onToggleFastForward30x: (Boolean) -> Unit,
    onSelectDecayLevelMode: (DecayLevelMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "3. CHẾ ĐỘ TRÌNH DIỄN GIÁM KHẢO KHKT (DEMO)",
            style = MaterialTheme.typography.labelLarge.copy(
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162032)),
            border = BorderStroke(1.dp, Color(0xFF283548)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hàng: ⚡ Tua nhanh thời gian (1s = 30s): [Checkbox]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFastForward30x(!sessionState.isFastForward30x) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tua nhanh thời gian (1s = 30s):",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Checkbox(
                        checked = sessionState.isFastForward30x,
                        onCheckedChange = { onToggleFastForward30x(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF7DD3FC),
                            checkmarkColor = Color(0xFF0F172A),
                            uncheckedColor = Color.White
                        ),
                        modifier = Modifier.testTag("checkbox_fast_forward_30x")
                    )
                }

                Text(
                    text = "Hoặc bấm chọn thử ngay từng Cấp độ suy giảm:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8)
                    )
                )

                // Dải 6 nút: Tự động | Mức 0 | Mức 1 | Mức 2 | Mức 3 | Mức 4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DecayLevelMode.values().forEach { mode ->
                        val isSelected = sessionState.decayLevelMode == mode
                        Surface(
                            color = if (isSelected) Color(0xFF7DD3FC) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF7DD3FC) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .clickable { onSelectDecayLevelMode(mode) }
                                .testTag("btn_decay_mode_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                // Mô tả ngắn gọn trạng thái cấp độ hiện tại
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Đang áp dụng: Cấp độ Mức ${sessionState.currentDecayLevel} (${(sessionState.progress * 100).toInt()}% suy giảm) – " +
                                when (sessionState.currentDecayLevel) {
                                    0 -> "Màu gốc 100%, hiện đủ Like/View/Thông báo đỏ, cuộn 0ms."
                                    1 -> "Ẩn chấm thông báo đỏ, bắt đầu hạ nhiệt màu sắc."
                                    2 -> "Ẩn số Like/View, làm mờ gợi ý 8dp, xám 55%, trễ cuộn 450ms."
                                    3 -> "Ẩn toàn bộ Like/View/Thông báo đỏ, trắng đen 85%, trễ cuộn vô tận 900ms."
                                    else -> "Trắng đen 100%, phát Notification & yêu cầu hoàn thành nhiệm vụ độ tuổi!"
                                },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScientificHudCard(
    sessionState: SessionState,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit
) {
    val progress = sessionState.progress
    val group = sessionState.ageGroup
    val effectiveElapsed = sessionState.totalSeconds * progress
    val elapsedMin = (effectiveElapsed / 60).toInt()
    val elapsedSec = (effectiveElapsed % 60).toInt()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${group.mechanismName} (Mức ${sessionState.currentDecayLevel}/4)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = group.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "%02d:%02d / %02d:00".format(elapsedMin, elapsedSec, sessionState.targetMinutes),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when (group) {
                    AgeGroup.TODDLER -> Color(0xFFD97706)
                    AgeGroup.PRIMARY -> Color(0xFF0284C7)
                    AgeGroup.MIDDLE_SCHOOL -> Color(0xFF475569)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Các chỉ số Ma sát nhận thức & Hạ nhiệt thị giác trực tiếp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HudMetricPill(
                    icon = if (sessionState.hideLikeAndViewCounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    label = "Like/View/Đỏ",
                    value = when {
                        sessionState.hideLikeAndViewCounts -> "Đã ẩn 100%"
                        sessionState.hideRedNotifications -> "Ẩn chấm đỏ"
                        else -> "Hiển thị"
                    }
                )
                HudMetricPill(
                    icon = Icons.Default.Psychology,
                    label = if (group == AgeGroup.TODDLER) "Tông Sepia" else "Trắng đen",
                    value = if (sessionState.isGrayscaleExemptedNow) "Miễn xám (YT)" else "${(progress * 100).toInt()}%"
                )
                HudMetricPill(
                    icon = Icons.Default.Speed,
                    label = "Trễ cuộn",
                    value = "${sessionState.infiniteScrollDelayMs} ms"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onToggleRunning,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_toggle_running")
                    ) {
                        Icon(
                            imageVector = if (sessionState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (sessionState.isRunning) "Tạm dừng" else "Chạy bộ đếm")
                    }

                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_reset_session")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đặt lại")
                    }
                }

                Text(
                    text = if (sessionState.isFastForward30x) "Tốc độ: 30x" else "Tốc độ: 1x",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun HudMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

/**
 * Luồng nội dung mô phỏng cho 4 nhóm mục tiêu (YouTube & Shorts, TikTok Web, Facebook & Reels, Web Game)
 * và Bài giảng học tập YouTube, thể hiện rõ cơ chế Ma sát nhận thức:
 * - Ẩn số Like/View/Thông báo đỏ
 * - Giảm độ bão hòa màu về trắng đen 100% (trừ khi xem Bài giảng học tập YouTube có bật miễn giảm)
 * - Làm trễ cuộn vô tận (900ms) và yêu cầu gõ mục tiêu học tập.
 */
@Composable
fun SimulatedTargetPlatformFeed(
    sessionState: SessionState,
    blurRadius: androidx.compose.ui.unit.Dp,
    extraFeedCount: Int,
    isLoadingMoreFeed: Boolean,
    onLoadMoreInfiniteScroll: () -> Unit,
    onOpenLearningGoalModal: () -> Unit
) {
    val platform = sessionState.selectedPreviewPlatform
    val isLecture = sessionState.isViewingYoutubeLecture

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Thanh tiêu đề ứng dụng giả lập kèm chấm thông báo đỏ (Red notification badge)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLecture) Color(0xFFECFDF5) else Color(0xFFFFF1F2)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLecture) {
                            "🎓 Bài Giảng Học Tập YouTube (Toán & Khoa Học)"
                        } else {
                            "${platform.emoji} Luồng ${platform.title} (Đang giám sát)"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLecture) Color(0xFF065F46) else Color(0xFF9F1239)
                        )
                    )
                    Text(
                        text = if (sessionState.isGrayscaleExemptedNow) {
                            "✓ Đang được miễn giảm làm xám để học sinh quan sát hình vẽ minh họa bài giảng."
                        } else {
                            "Độ bão hòa màu: ${((1f - sessionState.progress) * 100).toInt()}% | Cấp độ suy giảm: Mức ${sessionState.currentDecayLevel}"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isLecture) Color(0xFF047857) else Color(0xFF881337)
                        )
                    )
                }

                // Biểu tượng thông báo đỏ (Bị ẩn từ Mức 1+)
                if (!sessionState.hideRedNotifications) {
                    Surface(
                        color = Color(0xFFEF4444),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "99+ Mới",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                } else {
                    Surface(
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Đã ẩn TB đỏ",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Danh sách bài đăng / video mô phỏng
        val baseItems = remember(platform, isLecture, extraFeedCount) {
            val initial = if (isLecture) {
                listOf(
                    Triple("Bài giảng Hình học Không gian & Quang học lớp 6", "Thầy Nguyễn Văn A • Minh họa tia sáng và phổ màu cầu vồng", "45.2K ❤️ • 310K lượt xem"),
                    Triple("Thí nghiệm Hóa học vui: Phản ứng đổi màu chỉ thị pH", "Cô Trần Thị B • Quan sát sự biến đổi màu sắc dung dịch", "32.1K ❤️ • 195K lượt xem")
                )
            } else {
                when (platform) {
                    TargetPlatform.YOUTUBE_SHORTS -> listOf(
                        Triple("Shorts: Thử thách 24h sinh tồn siêu kịch tính #trending", "Kênh Giải Trí Siêu Tốc • Video ngắn gây nghiện", "248.5K ❤️ • 3.8M lượt xem"),
                        Triple("Top 10 khoảnh khắc hài hước không thể rời mắt!", "Shorts Viral Mỗi Ngày • Cuộn liên tục", "95.4K ❤️ • 1.2M lượt xem")
                    )
                    TargetPlatform.TIKTOK_WEB -> listOf(
                        Triple("Trend nhảy biến hình cực cuốn #fyp #xuhuong", "TikTok Creator • Nhạc nền bắt tai lặp vô tận", "512.0K ❤️ • 6.4M lượt xem"),
                        Triple("Review đồ chơi công nghệ phát sáng rực rỡ", "Unbox Channel • Hiệu ứng màu neon kích thích", "184.2K ❤️ • 2.1M lượt xem")
                    )
                    TargetPlatform.FACEBOOK_REELS -> listOf(
                        Triple("Reels: Mẹo vặt cuộc sống 15 giây cực bất ngờ", "ộng Đồng Sáng Tạo • Xem tiếp tự động", "76.8K ❤️ • 980K lượt xem"),
                        Triple("Tổng hợp clip thú cưng siêu đáng yêu", "Pet Lovers VN • Thuật toán giữ chân người xem", "134.9K ❤️ • 1.7M lượt xem")
                    )
                    TargetPlatform.WEB_GAME -> listOf(
                        Triple("Đấu Trường Kẹo Ngọt Siêu Cấp (Candy Blitz)", "Combo nổ màu sắc liên hoàn & phần thưởng vòng quay", "890.2K ❤️ • 12.5M lượt chơi"),
                        Triple("Đua Xe Neon Tốc Độ Cao 3D", "Bảng xếp hạng trực tuyến & rương vật phẩm lấp lánh", "410.6K ❤️ • 5.3M lượt chơi")
                    )
                }
            }
            val extras = (1..extraFeedCount).map { idx ->
                Triple(
                    "Nội dung cuộn vô tận #$idx (${if (isLecture) "Bài giảng" else platform.title})",
                    "Đã tải sau độ trễ ma sát nhận thức ${sessionState.infiniteScrollDelayMs}ms",
                    "${10 + idx * 7}K ❤️ • ${100 + idx * 45}K lượt xem"
                )
            }
            initial + extras
        }

        baseItems.forEachIndexed { index, (title, subtitle, metrics) ->
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (index % 2 == 0) R.drawable.stretch_station_kids_1793542742750
                                else R.drawable.sleepy_bear_lullaby_1793542723612
                            ),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .applyVisualCoolDown(blurRadius)
                        )

                        // Huy hiệu trạng thái màu / mờ trên ảnh
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (sessionState.isGrayscaleExemptedNow) {
                                    "🎓 Giữ nguyên màu học tập"
                                } else if (sessionState.currentDecayLevel == 4) {
                                    "Trắng đen 100% (Mức 4)"
                                } else {
                                    "Mức ${sessionState.currentDecayLevel} • Xám ${(sessionState.progress * 100).toInt()}%"
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hàng hiển thị hoặc ẩn số Like / View (Cơ chế Ma sát nhận thức)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!sessionState.hideLikeAndViewCounts) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = metrics,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color(0xFFE11D48),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Đã ẩn số Like / View (Chống hiệu ứng đám đông)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Nút thử nghiệm Cuộn vô tận (Infinite Scroll) có độ trễ 900ms & nút Gõ mục tiêu học tập
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onLoadMoreInfiniteScroll,
                enabled = !isLoadingMoreFeed,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_infinite_scroll_load_more")
            ) {
                if (isLoadingMoreFeed) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trễ ${sessionState.infiniteScrollDelayMs}ms...", fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sessionState.infiniteScrollDelayMs > 0)
                            "Cuộn vô tận (Trễ ${sessionState.infiniteScrollDelayMs}ms)"
                        else
                            "Cuộn tải thêm (0ms)",
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = onOpenLearningGoalModal,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_open_learning_goal_modal")
            ) {
                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gõ Mục Tiêu Học Tập", fontSize = 12.sp)
            }
        }
    }
}
