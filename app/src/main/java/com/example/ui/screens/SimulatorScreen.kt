package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import com.example.model.SessionState
import com.example.ui.components.VisualDecayHelper
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
    onPlayLullabyPreview: () -> Unit,
    onTriggerMovementStationDirectly: () -> Unit,
    onTriggerReflectionDirectly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = sessionState.progress
    val currentFilter = VisualDecayHelper.createDecayColorFilter(sessionState.ageGroup, progress)
    val blurRadius = VisualDecayHelper.calculateVisualCoolDownBlur(progress)

    val coroutineScope = rememberCoroutineScope()
    var scrollFrictionNotice by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(sessionState.ageGroup, progress) {
                // If Middle School and progress > 0.5, apply 900ms scroll friction feedback
                if (sessionState.ageGroup == AgeGroup.MIDDLE_SCHOOL && progress > 0.4f) {
                    detectVerticalDragGestures { _, _ ->
                        scrollFrictionNotice = true
                        coroutineScope.launch {
                            delay(900)
                            scrollFrictionNotice = false
                        }
                    }
                }
            },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Age group selector tabs
        item {
            Text(
                text = "Chọn Phân Khúc Độ Tuổi Trẻ (3 – 12 tuổi):",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
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
                        label = { Text(group.title) },
                        leadingIcon = {
                            when (group) {
                                AgeGroup.TODDLER -> Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                                AgeGroup.PRIMARY -> Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
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

        // Live HUD Panel (Scientific Decay Dashboard)
        item {
            ScientificHudCard(
                sessionState = sessionState,
                onToggleRunning = onToggleRunning,
                onReset = onReset,
                onSetSpeed = onSetSpeed
            )
        }

        // 900ms Scroll friction alert banner (Nhóm 3)
        item {
            AnimatedVisibility(visible = scrollFrictionNotice) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
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
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ma sát cuộn kích hoạt: Trễ 900ms ngăn chặn thói quen doomscrolling!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // Interactive Content Simulator based on selected Age Group
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Không Gian Trải Nghiệm & Tự Phân Hủy:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Quick test trigger button
                Text(
                    text = when (sessionState.ageGroup) {
                        AgeGroup.TODDLER -> "Xem Bạn Gấu"
                        AgeGroup.PRIMARY -> "Mở Trạm VĐ"
                        AgeGroup.MIDDLE_SCHOOL -> "Xem Phản Tư"
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
                )
            }
        }

        when (sessionState.ageGroup) {
            AgeGroup.TODDLER -> {
                item {
                    ToddlerContentSection(
                        progress = progress,
                        colorFilter = currentFilter
                    )
                }
            }
            AgeGroup.PRIMARY -> {
                item {
                    PrimaryContentSection(
                        progress = progress,
                        blurRadius = blurRadius,
                        colorFilter = currentFilter
                    )
                }
            }
            AgeGroup.MIDDLE_SCHOOL -> {
                item {
                    MiddleSchoolContentSection(
                        progress = progress,
                        colorFilter = currentFilter
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
    onReset: () -> Unit,
    onSetSpeed: (Float) -> Unit
) {
    val progress = sessionState.progress
    val group = sessionState.ageGroup
    val elapsedMin = (sessionState.elapsedSeconds / 60).toInt()
    val elapsedSec = (sessionState.elapsedSeconds % 60).toInt()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Mechanism Title & Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = group.mechanismName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = group.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                // Digital Timer
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

            // Main Progress Bar
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

            // Decay Metrics Live Indicators
            when (group) {
                AgeGroup.TODDLER -> {
                    // Toddler: Brightness 68%, Volume 12%, Sepia tone, Stop at 15m
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HudMetricPill(
                            icon = Icons.Default.WbSunny,
                            label = "Độ sáng",
                            value = "${sessionState.brightnessPercent}% → 68%"
                        )
                        HudMetricPill(
                            icon = Icons.Default.VolumeDown,
                            label = "Âm lượng",
                            value = "${(100 - (88 * progress)).toInt()}% → 12%"
                        )
                        HudMetricPill(
                            icon = Icons.Default.Bedtime,
                            label = "Tông Sepia",
                            value = "${(progress * 100).toInt()}%"
                        )
                    }
                }
                AgeGroup.PRIMARY -> {
                    // Primary: Blur thumbnails, Accelerometer station at 25m
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HudMetricPill(
                            icon = Icons.Default.AutoAwesome,
                            label = "Làm mờ gợi ý",
                            value = "${"%.1f".format(progress * 16)} dp"
                        )
                        HudMetricPill(
                            icon = Icons.Default.DirectionsRun,
                            label = "Ngưỡng gia tốc",
                            value = "Δa > 6.5 m/s²"
                        )
                        HudMetricPill(
                            icon = Icons.Default.HourglassBottom,
                            label = "Vận động",
                            value = "10 nhịp"
                        )
                    }
                }
                AgeGroup.MIDDLE_SCHOOL -> {
                    // Middle School: Grayscale 100%, 900ms scroll delay, SHA-256 & Reflection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HudMetricPill(
                            icon = Icons.Default.Psychology,
                            label = "Thang độ xám",
                            value = "${(progress * 100).toInt()}% (100% PMC)"
                        )
                        HudMetricPill(
                            icon = Icons.Default.Speed,
                            label = "Trễ cuộn",
                            value = if (progress > 0.4f) "900 ms" else "0 ms"
                        )
                        HudMetricPill(
                            icon = Icons.Default.HourglassBottom,
                            label = "Phản tư",
                            value = "Chống dán"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons & Speed Toggle
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
                        Text(if (sessionState.isRunning) "Tạm dừng" else "Bắt đầu")
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

                // Speed buttons for science demonstration
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                ) {
                    listOf(1f, 15f, 60f).forEach { speed ->
                        val isSelected = sessionState.speedMultiplier == speed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onSetSpeed(speed) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${speed.toInt()}x",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
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

// -------------------------------------------------------------
// NHÓM 1: MẦM NON (3–6 TUỔI) COSY STORYBOOK CONTENT
// -------------------------------------------------------------
@Composable
fun ToddlerContentSection(
    progress: Float,
    colorFilter: ColorFilter?
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFFFDE68A), Color(0xFFFBBF24)))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vườn Cổ Tích Của Bé – Thế Giới Bạn Gấu",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    )
                }
                Text(
                    text = "Khi bé đọc truyện hoặc xem phim, ánh sáng vàng ấm Sepia sẽ dịu dần để mắt bé không bị mỏi, đưa bé vào giấc ngủ ngon sau 15 phút.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF78350F)),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Storybook Items
        val stories = listOf(
            Triple("Bạn Gấu Tìm Đom Đóm Đêm", "Chuyến phiêu lưu êm đềm trong rừng thông...", R.drawable.sleepy_bear_lullaby_1793542723612),
            Triple("Bầu Trời Đầy Sao Của Sóc Nhỏ", "Khám phá dải ngân hà lấp lánh và dịu êm...", R.drawable.app_icon_fg_1793542708643),
            Triple("Khúc Hát Ru Vườn Cây", "Giai điệu du dương giúp các loài vật say giấc...", R.drawable.sleepy_bear_lullaby_1793542723612)
        )

        stories.forEach { (title, desc, drawableRes) ->
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Âm thanh ru êm",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFD97706))
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// NHÓM 2: TIỂU HỌC (7–9 TUỔI) DISCOVERY & VISUAL COOL-DOWN
// -------------------------------------------------------------
@Composable
fun PrimaryContentSection(
    progress: Float,
    blurRadius: androidx.compose.ui.unit.Dp,
    colorFilter: ColorFilter?
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF86EFAC), Color(0xFF22C55E)))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = Color(0xFF15803D)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Khám Phá Tri Thức & Hạ Nhiệt Thị Giác",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    )
                }
                Text(
                    text = "Hình ảnh gợi ý hấp dẫn sẽ mờ dần (Visual Cool-down) để tránh cuốn mắt quá đà. Đến phút 25, Trạm Vận Động sẽ yêu cầu 10 nhịp vươn vai qua cảm biến!",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF14532D)),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        val articles = listOf(
            Pair("Vũ Trụ Huyền Bí: Vì Sao Sao Hỏa Có Màu Đỏ?", "Khám phá các khoáng chất oxit sắt trên bề mặt hành tinh đỏ."),
            Pair("Vương Quốc Động Vật: Cá Heo Nói Chuyện Thế Nào?", "Sóng siêu âm và ngôn ngữ diệu kỳ dưới lòng biển sâu."),
            Pair("Sáng Chế Tuổi Thơ: Tự Làm Kính Thiên Văn Tại Nhà", "Các bước đơn giản từ thấu kính hội tụ và ống giấy các-tông.")
        )

        articles.forEachIndexed { idx, (title, desc) ->
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.stretch_station_kids_1793542742750),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            colorFilter = colorFilter,
                            modifier = Modifier
                                .fillMaxSize()
                                .applyVisualCoolDown(blurRadius)
                        )
                        // Blur indicator badge
                        if (blurRadius > 1.dp) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Đang hạ nhiệt mờ: ${"%.1f".format(blurRadius.value)}dp",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// NHÓM 3: LỚP 5–7 (10–12 TUỔI) COGNITIVE FRICTION & GRAYSCALE
// -------------------------------------------------------------
@Composable
fun MiddleSchoolContentSection(
    progress: Float,
    colorFilter: ColorFilter?
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF94A3B8), Color(0xFF475569)))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Diễn Đàn Khoa Học & Ma Sát Nhận Thức",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    )
                }
                Text(
                    text = "Áp dụng Thang độ xám 100% (Holte & Ferraro 2020) triệt tiêu dopamine gây nghiện, kèm trễ cuộn 900ms và gõ phản tư chống dán ở phút 30.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569)),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        val topics = listOf(
            Pair("Nguyên lý Mạng Nơ-ron Nhân tạo (Deep Learning)", "Cách các thuật toán máy tính mô phỏng kết nối tế bào thần kinh não bộ con người."),
            Pair("Vật Lý Thiên Văn: Sóng Hấp Dẫn & Sự Kiện Hợp Nhất Lỗ Đen", "Quan sát thực nghiệm từ đài thiên văn LIGO và giải Nobel Vật lý."),
            Pair("Tâm Lý Học Kỹ Thuật Số: Tại Sao Màn Hình Màu Gây Nghiện?", "Phân tích cơ chế giải phóng Dopamine khi xem video ngắn và tác dụng của Grayscale.")
        )

        topics.forEach { (title, desc) ->
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Bão hòa màu: ${( (1f - progress) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = if (progress > 0.4f) "Trễ cuộn: 900ms" else "Cuộn: Bình thường",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (progress > 0.4f) Color(0xFFDC2626) else Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
