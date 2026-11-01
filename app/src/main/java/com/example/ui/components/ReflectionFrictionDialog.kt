package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LearningGoalType
import com.example.model.MathProblem
import com.example.model.ParentalConfig

@Composable
fun ReflectionFrictionDialog(
    config: ParentalConfig,
    onSubmitReflection: (String) -> Boolean,
    onOpenGoalSetup: () -> Unit,
    onParentOverride: () -> Unit
) {
    val requiresMath = config.learningGoalType == LearningGoalType.MATH_CHALLENGE ||
            config.learningGoalType == LearningGoalType.COMBINED
    val requiresTyping = config.learningGoalType == LearningGoalType.TYPING_REFLECTION ||
            config.learningGoalType == LearningGoalType.COMBINED
    val totalMathRequired = config.requiredMathProblems.coerceIn(1, 5)

    val mathProblems = remember(totalMathRequired) {
        listOf(
            MathProblem(1, "(12 × 3) + 14 = ?", 50, "Gợi ý: 12 × 3 = 36, sau đó cộng 14"),
            MathProblem(2, "85 - (9 × 5) = ?", 40, "Gợi ý: 9 × 5 = 45, lấy 85 trừ 45"),
            MathProblem(3, "(24 ÷ 4) × 7 = ?", 42, "Gợi ý: 24 ÷ 4 = 6, nhân với 7"),
            MathProblem(4, "15 × 4 + 25 = ?", 85, "Gợi ý: 15 × 4 = 60, cộng 25"),
            MathProblem(5, "(100 - 28) ÷ 8 = ?", 9, "Gợi ý: 100 - 28 = 72, chia cho 8")
        ).take(totalMathRequired)
    }

    var solvedMathCount by remember(totalMathRequired) { mutableIntStateOf(0) }
    var mathAnswerInput by remember { mutableStateOf("") }
    var mathFeedback by remember { mutableStateOf<String?>(null) }

    var typedText by remember { mutableStateOf("") }
    var pasteWarning by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isMathCompleted = !requiresMath || solvedMathCount >= totalMathRequired
    val currentProblem = if (!isMathCompleted) mathProblems[solvedMathCount] else null

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E2E2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Ma sát nhận thức",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "HOÀN THÀNH MỤC TIÊU HỌC TẬP ĐỂ MỞ KHÓA",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Mục tiêu đang áp dụng: ${config.learningGoalTitle}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFCCCCCC),
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Nút mở dialog thiết lập mục tiêu học tập ngay tại đây
                OutlinedButton(
                    onClick = onOpenGoalSetup,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF666666)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_open_goal_setup_from_reflection")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thiết lập / Đổi mục tiêu học tập (VD: Giải 3 bài toán)", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4 Trụ cột Ma sát nhận thức
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier.fillMaxWidth(0.96f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CƠ CHẾ TỰ PHÂN HỦY ĐỘ HẤP DẪN (MỨC 4):",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFBBBBBB),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        FrictionCheckRow("Ẩn toàn bộ số Like / View / Thông báo đỏ gây xao nhãng")
                        FrictionCheckRow("Giảm độ bão hòa màu về Trắng Đen 100% (Holte & Ferraro, 2020)")
                        FrictionCheckRow("Làm trễ cuộn vô tận (900 ms) cắt đứt vòng lặp lướt vô thức")
                        FrictionCheckRow("Giải đúng $totalMathRequired bài toán tư duy & cam kết học tập để mở khóa")
                    }
                }

                // PHẦN 1: GIẢI BÀI TOÁN TƯ DUY (VÍ DỤ: GIẢI 3 BÀI TOÁN)
                if (requiresMath) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                        border = BorderStroke(1.dp, Color(0xFF555555)),
                        modifier = Modifier.fillMaxWidth(0.96f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "THỬ THÁCH GIẢI TOÁN TƯ DUY",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }

                                Surface(
                                    color = if (isMathCompleted) Color(0xFFE5E5E5) else Color(0xFF3A3A3A),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Đã giải: $solvedMathCount / $totalMathRequired bài",
                                        color = if (isMathCompleted) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { solvedMathCount.toFloat() / totalMathRequired.toFloat() },
                                color = Color.White,
                                trackColor = Color(0xFF3A3A3A),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (currentProblem != null) {
                                Text(
                                    text = "Bài toán ${currentProblem.index}/$totalMathRequired:  ${currentProblem.question}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                                Text(
                                    text = currentProblem.hint,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFAAAAAA)
                                    ),
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = mathAnswerInput,
                                        onValueChange = {
                                            mathAnswerInput = it
                                            mathFeedback = null
                                        },
                                        label = { Text("Nhập đáp án số") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color.White,
                                            unfocusedBorderColor = Color(0xFF777777),
                                            focusedLabelColor = Color.White,
                                            unfocusedLabelColor = Color(0xFFAAAAAA)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_math_answer")
                                    )

                                    Button(
                                        onClick = {
                                            val ans = mathAnswerInput.trim().toIntOrNull()
                                            if (ans == currentProblem.correctAnswer) {
                                                solvedMathCount++
                                                mathAnswerInput = ""
                                                mathFeedback = "✓ Chính xác! Tuyệt vời!"
                                                errorMessage = null
                                            } else {
                                                mathFeedback = "✗ Chưa đúng! Em hãy tính lại nhé."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .height(54.dp)
                                            .testTag("btn_verify_math_answer")
                                    ) {
                                        Text("Kiểm tra", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (mathFeedback != null) {
                                    Text(
                                        text = mathFeedback ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = Color(0xFF333333),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "🎉 Xuất sắc! Em đã giải đúng đủ $totalMathRequired/$totalMathRequired bài toán tư duy!",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // PHẦN 2: GÕ MỤC TIÊU HỌC TẬP CHỐNG DÁN
                if (requiresTyping) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF666666), Color(0xFF999999)))
                        ),
                        modifier = Modifier.fillMaxWidth(0.96f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CAM KẾT MỤC TIÊU HỌC TẬP CẦN GÕ:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFCCCCCC),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "“${config.reflectionSentence}”",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { newText ->
                            val deltaLen = newText.length - typedText.length
                            if (deltaLen > 3) {
                                pasteWarning = "⚠️ Chống dán (Anti-paste) đã chặn thao tác dán! Hãy tự gõ từng chữ mục tiêu học tập của em."
                            } else {
                                pasteWarning = null
                                errorMessage = null
                                typedText = newText
                            }
                        },
                        label = { Text("Gõ mục tiêu học tập của em (chống dán)") },
                        placeholder = { Text("Gõ mục tiêu học tập cụ thể (tối thiểu 20 ký tự)...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE0E0E0),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0xFF666666),
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color(0xFFAAAAAA)
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .testTag("reflection_input_field"),
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp)
                    )

                    AnimatedVisibility(visible = pasteWarning != null) {
                        Text(
                            text = pasteWarning ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFFAA00),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!isMathCompleted) {
                            errorMessage = "Em cần giải đúng đủ $totalMathRequired bài toán tư duy phía trên trước khi mở khóa!"
                            return@Button
                        }
                        val payload = if (requiresTyping) {
                            typedText
                        } else {
                            "Đã giải đúng $totalMathRequired/$totalMathRequired bài toán: ${config.learningGoalTitle}"
                        }
                        val success = onSubmitReflection(payload)
                        if (!success) {
                            errorMessage = "Vui lòng tự tay gõ mục tiêu học tập cụ thể (ít nhất 20 ký tự) để hoàn tất mở khóa."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .height(50.dp)
                        .testTag("submit_reflection_button")
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hoàn Thành Mục Tiêu & Mở Khóa Ứng Dụng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onParentOverride,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFCCCCCC)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .testTag("reflection_parent_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Mở Khóa Khẩn Cấp Bằng Mã PIN Phụ Huynh (SHA-256)")
                }
            }
        }
    }
}

@Composable
private fun FrictionCheckRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFFD4D4D4),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE5E5E5))
        )
    }
}
