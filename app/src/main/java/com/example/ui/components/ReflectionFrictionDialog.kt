package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.ui.text.font.FontFamily
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

    // Normalized match ratio
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
                .background(Color(0xFF121212)), // Strict Grayscale dark background
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
                // Psychology badge
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

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "MA SÁT NHẬN THỨC & PHẢN TƯ",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Cơ chế: Thang độ xám 100% & Trễ cuộn 900ms (Phút 30)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFAAAAAA)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scientific basis citation
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color(0xFFDDDDDD),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nghiên cứu Holte & Ferraro (2020) & PubMed PMC10159421 chứng minh: Thang độ xám 100% triệt tiêu thụ cảm dopamine ở não, giúp thanh thiếu niên dễ dàng thoát khỏi vòng lặp vô thức.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCCCCCC),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target quote card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF666666), Color(0xFF999999)))),
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CÂU TÂM NIỆM PHẢN TƯ CẦN GÕ:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF888888),
                                fontWeight = FontWeight.Bold
                            )
                        )
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

                Spacer(modifier = Modifier.height(16.dp))

                // Anti-paste text input
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { newText ->
                        // Anti-paste mechanism: if user suddenly pastes > 3 chars
                        val deltaLen = newText.length - typedText.length
                        if (deltaLen > 3) {
                            pasteWarning = "Tính năng chống dán (Anti-paste) đã kích hoạt! Bạn phải tự tay gõ từng từ để kích thích thùy trán trước."
                        } else {
                            pasteWarning = null
                            errorMessage = null
                            typedText = newText
                        }
                    },
                    label = { Text("Tự tay gõ câu suy ngẫm vào đây (chống dán)") },
                    placeholder = { Text("Gõ chính xác từng ký tự...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE0E0E0),
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF666666),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color(0xFFAAAAAA)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .testTag("reflection_input_field"),
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp)
                )

                // Match progress bar
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Độ chính xác: ${(matchRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFAAAAAA))
                    )
                    Text(
                        text = "${typedText.length}/${targetSentence.length} ký tự",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFAAAAAA))
                    )
                }

                LinearProgressIndicator(
                    progress = { matchRatio },
                    color = Color.White,
                    trackColor = Color(0xFF333333),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
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

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val success = onSubmitReflection(typedText)
                        if (!success) {
                            errorMessage = "Câu gõ chưa khớp với câu tâm niệm. Vui lòng hoàn thành chính xác để mở khóa."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(50.dp)
                        .testTag("submit_reflection_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Xác Nhận Đã Phản Tư & Rời Màn Hình",
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
                        .fillMaxWidth(0.95f)
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
