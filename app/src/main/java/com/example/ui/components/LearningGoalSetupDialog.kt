package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LearningGoalType
import com.example.model.ParentalConfig

/**
 * Dialog thiết lập mục tiêu học tập (ví dụ: giải 3 bài toán) mà trẻ cần hoàn thành
 * để 'tự phân hủy' độ hấp dẫn và mở khóa ứng dụng sau khi đạt giới hạn thời gian.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearningGoalSetupDialog(
    currentConfig: ParentalConfig,
    onDismiss: () -> Unit,
    onSaveGoal: (ParentalConfig, Boolean) -> Unit
) {
    var goalTitle by remember(currentConfig) { mutableStateOf(currentConfig.learningGoalTitle) }
    var selectedType by remember(currentConfig) { mutableStateOf(currentConfig.learningGoalType) }
    var mathProblemsCount by remember(currentConfig) {
        mutableFloatStateOf(currentConfig.requiredMathProblems.toFloat().coerceIn(1f, 5f))
    }
    var reflectionSentence by remember(currentConfig) {
        mutableStateOf(currentConfig.reflectionSentence)
    }

    val presets = listOf(
        Triple(
            "🧮 Giải 3 bài toán tư duy",
            LearningGoalType.MATH_CHALLENGE,
            3
        ),
        Triple(
            "📚 Giải 3 bài toán & gõ cam kết",
            LearningGoalType.COMBINED,
            3
        ),
        Triple(
            "🏆 Giải 5 bài toán nâng cao",
            LearningGoalType.MATH_CHALLENGE,
            5
        ),
        Triple(
            "✍️ Tự gõ mục tiêu học tập",
            LearningGoalType.TYPING_REFLECTION,
            3
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Thiết Lập Mục Tiêu Học Tập",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = "Nhiệm vụ trẻ cần hoàn thành để tự phân hủy độ hấp dẫn & mở khóa sau khi hết giờ",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Gợi ý chọn nhanh
                Column {
                    Text(
                        text = "Chọn nhanh mẫu mục tiêu:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (label, type, count) ->
                            FilterChip(
                                selected = goalTitle == label && selectedType == type,
                                onClick = {
                                    goalTitle = label
                                    selectedType = type
                                    mathProblemsCount = count.toFloat()
                                },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Nhập tên mục tiêu học tập
                OutlinedTextField(
                    value = goalTitle,
                    onValueChange = { goalTitle = it },
                    label = { Text("Tên mục tiêu học tập cần hoàn thành") },
                    placeholder = { Text("Ví dụ: Giải 3 bài toán tư duy...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_learning_goal_title")
                )

                // Chọn phương thức thử thách học tập
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Hình thức thử thách mở khóa ứng dụng:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    LearningGoalType.values().forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = type }
                                .testTag("goal_type_${type.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected)
                                        Icons.Default.RadioButtonChecked
                                    else
                                        Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = type.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = type.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Cấu hình số lượng bài toán nếu có bao gồm giải toán
                if (selectedType == LearningGoalType.MATH_CHALLENGE ||
                    selectedType == LearningGoalType.COMBINED
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Số bài toán trẻ cần giải đúng:",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${mathProblemsCount.toInt()} bài toán",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Slider(
                                value = mathProblemsCount,
                                onValueChange = { mathProblemsCount = it },
                                valueRange = 1f..5f,
                                steps = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Câu cam kết nếu có bao gồm gõ phản tư
                if (selectedType == LearningGoalType.TYPING_REFLECTION ||
                    selectedType == LearningGoalType.COMBINED
                ) {
                    OutlinedTextField(
                        value = reflectionSentence,
                        onValueChange = { reflectionSentence = it },
                        label = { Text("Câu cam kết học tập cần gõ (chống dán)") },
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_learning_goal_sentence")
                    )
                }

                // Nút hành động: Lưu & Thử thách mở khóa ngay / Lưu / Đóng
                Button(
                    onClick = {
                        val updated = currentConfig.copy(
                            learningGoalTitle = goalTitle.ifBlank { "Giải ${mathProblemsCount.toInt()} bài toán tư duy" },
                            learningGoalType = selectedType,
                            requiredMathProblems = mathProblemsCount.toInt(),
                            reflectionSentence = reflectionSentence.ifBlank { currentConfig.reflectionSentence }
                        )
                        onSaveGoal(updated, true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_and_test_learning_goal")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lưu & Mở Thử Thách Mở Khóa Ngay",
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_cancel_learning_goal_setup")
                    ) {
                        Text("Hủy")
                    }

                    Button(
                        onClick = {
                            val updated = currentConfig.copy(
                                learningGoalTitle = goalTitle.ifBlank { "Giải ${mathProblemsCount.toInt()} bài toán tư duy" },
                                learningGoalType = selectedType,
                                requiredMathProblems = mathProblemsCount.toInt(),
                                reflectionSentence = reflectionSentence.ifBlank { currentConfig.reflectionSentence }
                            )
                            onSaveGoal(updated, false)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_learning_goal_setup")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lưu Mục Tiêu", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
