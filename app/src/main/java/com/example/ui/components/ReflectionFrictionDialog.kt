package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ReflectionFrictionDialog(
    targetSentence: String,
    onSubmitReflection: (String) -> Boolean,
    onParentOverride: () -> Unit
) {
    var typedText by remember { mutableStateOf("") }
    var pasteWarning by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val matchRatio = remember(typedText, targetSentence) {
        val targetNorm = targetSentence.trim().lowercase()
        val userNorm = typedText.trim().lowercase()
        if (targetNorm.isEmpty()) 0f
        else {
            var common = 0
            for (i in 0 until minOf(userNorm.length, targetNorm.length)) {
                if (userNorm[i] == targetNorm[i]) common++
            }
            (common.toFloat() / targetNorm.length).coerceIn(0f, 1f)
        }
    }

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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E2E2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Ma sát nhận thức",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CƠ CHẾ MA SÁT NHẬN THỨC",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Nhóm 10–12 tuổi: Đạt giới hạn 30 phút sử dụng màn hình",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFAAAAAA)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4 Trụ cột Ma sát nhận thức
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier.fillMaxWidth(0.96f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TRẠNG THÁI TỰ PHÂN HỦY ĐỘ HẤP DẪN (MỨC 4):",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFBBBBBB),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        FrictionCheckRow("Đã ẩn toàn bộ số Like / View / Thông báo đỏ gây nghiện")
                        FrictionCheckRow("Giảm độ bão hòa màu về Trắng Đen 100% (Holte & Ferraro, 2020)")
                        FrictionCheckRow("Làm trễ cuộn vô tận (900 ms) cắt đứt vòng lặp lướt vô thức")
                        FrictionCheckRow("Yêu cầu tự tay gõ Mục tiêu học tập (Chống sao chép/dán)")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Learning Goal / Reflection Sentence
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF666666), Color(0xFF999999)))
                    ),
                    modifier = Modifier.fillMaxWidth(0.96f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MỤC TIÊU HỌC TẬP MẪU CẦN GÕ:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFCCCCCC),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "“$targetSentence”",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Anti-paste text input for learning goal
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
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.96f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tiến độ hoàn thành mục tiêu: ${minOf(100, (typedText.length * 100) / maxOf(20, targetSentence.length))}%",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFAAAAAA))
                    )
                    Text(
                        text = "${typedText.length} ký tự (Tối thiểu 20)",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFAAAAAA))
                    )
                }

                LinearProgressIndicator(
                    progress = { (typedText.length.toFloat() / 20f).coerceIn(0f, 1f) },
                    color = Color.White,
                    trackColor = Color(0xFF333333),
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .padding(top = 4.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                AnimatedVisibility(visible = pasteWarning != null) {
                    Text(
                        text = pasteWarning ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFFFAA00),
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
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

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val success = onSubmitReflection(typedText)
                        if (!success) {
                            errorMessage = "Vui lòng tự tay gõ mục tiêu học tập cụ thể (ít nhất 20 ký tự) hoặc khớp với câu mẫu."
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
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Xác Nhận Mục Tiêu Học Tập & Hoàn Tất",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE5E5E5))
        )
    }
}
