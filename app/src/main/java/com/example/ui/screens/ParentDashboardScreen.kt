package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActivityRecord
import com.example.model.ParentalConfig
import com.example.util.CryptoUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ParentDashboardScreen(
    isUnlocked: Boolean,
    config: ParentalConfig,
    logs: List<ActivityRecord>,
    onVerifyPin: (String) -> Boolean,
    onLock: () -> Unit,
    onSaveConfig: (ParentalConfig) -> Unit,
    onChangePin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isUnlocked) {
        ParentLockedState(onVerifyPin = onVerifyPin, modifier = modifier)
    } else {
        ParentUnlockedDashboard(
            config = config,
            logs = logs,
            onLock = onLock,
            onSaveConfig = onSaveConfig,
            onChangePin = onChangePin,
            modifier = modifier
        )
    }
}

@Composable
fun ParentLockedState(
    onVerifyPin: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val liveHash = remember(enteredPin) {
        if (enteredPin.isEmpty()) "" else CryptoUtils.sha256(enteredPin)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Khu Vực Quản Trị Của Phụ Huynh",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "Nhập mã PIN phụ huynh để mở khóa và tùy chỉnh các tham số khoa học.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = enteredPin,
            onValueChange = {
                enteredPin = it
                errorMessage = null
            },
            label = { Text("Mã PIN (Mặc định: 1234)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag("parent_dashboard_pin_field")
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (liveHash.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "SHA-256 Hash:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = liveHash,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val ok = onVerifyPin(enteredPin)
                if (!ok) {
                    errorMessage = "Mã PIN không đúng. Vui lòng thử lại!"
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(50.dp)
                .testTag("btn_parent_unlock")
        ) {
            Icon(Icons.Default.LockOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mở Khóa Bảng Điều Khiển", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ParentUnlockedDashboard(
    config: ParentalConfig,
    logs: List<ActivityRecord>,
    onLock: () -> Unit,
    onSaveConfig: (ParentalConfig) -> Unit,
    onChangePin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var toddlerMin by remember(config) { mutableFloatStateOf(config.toddlerMinutes.toFloat()) }
    var primaryMin by remember(config) { mutableFloatStateOf(config.primaryMinutes.toFloat()) }
    var middleMin by remember(config) { mutableFloatStateOf(config.middleSchoolMinutes.toFloat()) }
    var accelThreshold by remember(config) { mutableFloatStateOf(config.accelerometerThreshold) }
    var stretchReps by remember(config) { mutableFloatStateOf(config.requiredStretchReps.toFloat()) }
    var reflectionSentence by remember(config) { mutableStateOf(config.reflectionSentence) }

    var newPinText by remember { mutableStateOf("") }
    var showSavedMessage by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Unlocked Header with Lock Button
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bảng Cấu Hình Phụ Huynh",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Đã xác thực mã hóa SHA-256 an toàn",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                    }

                    OutlinedButton(
                        onClick = onLock,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_lock_parent_dashboard")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Khóa lại", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section: Time limit configuration
        item {
            OutlinedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Giới Hạn Thời Gian Từng Nhóm Tuổi",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Toddler slider (3-6)
                    Text("Nhóm 1 (3–6t): ${toddlerMin.toInt()} phút (Khuyến nghị: 15p)")
                    Slider(
                        value = toddlerMin,
                        onValueChange = { toddlerMin = it },
                        valueRange = 5f..30f,
                        steps = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Primary slider (7-9)
                    Text("Nhóm 2 (7–9t): ${primaryMin.toInt()} phút (Khuyến nghị: 25p)")
                    Slider(
                        value = primaryMin,
                        onValueChange = { primaryMin = it },
                        valueRange = 10f..45f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Middle School slider (10-12)
                    Text("Nhóm 3 (10–12t): ${middleMin.toInt()} phút (Khuyến nghị: 30p)")
                    Slider(
                        value = middleMin,
                        onValueChange = { middleMin = it },
                        valueRange = 15f..60f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section: Sensor & Physical Stretch Config
        item {
            OutlinedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Thông Số Cảm Biến & Vận Động (Nhóm 2)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Ngưỡng gia tốc vươn vai: Δa = ${"%.1f".format(accelThreshold)} m/s² (Chuẩn nghiên cứu: 6.5 m/s²)")
                    Slider(
                        value = accelThreshold,
                        onValueChange = { accelThreshold = it },
                        valueRange = 4.0f..10.0f,
                        steps = 11,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Số nhịp vươn vai yêu cầu: ${stretchReps.toInt()} nhịp (Chuẩn: 10 nhịp)")
                    Slider(
                        value = stretchReps,
                        onValueChange = { stretchReps = it },
                        valueRange = 5f..20f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section: Reflection quote config
        item {
            OutlinedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Câu Tâm Niệm Phản Tư Chống Dán (Nhóm 3)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reflectionSentence,
                        onValueChange = { reflectionSentence = it },
                        label = { Text("Nội dung trẻ cần tự tay gõ để nhận thức") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }
            }
        }

        // Section: Change PIN
        item {
            OutlinedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đổi Mã PIN Phụ Huynh Mới",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPinText,
                            onValueChange = { newPinText = it },
                            label = { Text("Mã PIN mới") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (newPinText.length >= 4) {
                                    onChangePin(newPinText)
                                    newPinText = ""
                                    showSavedMessage = true
                                }
                            },
                            enabled = newPinText.length >= 4,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Đổi")
                        }
                    }
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val updated = config.copy(
                        toddlerMinutes = toddlerMin.toInt(),
                        primaryMinutes = primaryMin.toInt(),
                        middleSchoolMinutes = middleMin.toInt(),
                        accelerometerThreshold = accelThreshold,
                        requiredStretchReps = stretchReps.toInt(),
                        reflectionSentence = reflectionSentence
                    )
                    onSaveConfig(updated)
                    showSavedMessage = true
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_parent_config")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu Toàn Bộ Cấu Hình Khoa Học", fontWeight = FontWeight.Bold)
            }

            if (showSavedMessage) {
                Text(
                    text = "✓ Đã cập nhật và lưu cấu hình thành công!",
                    color = Color(0xFF10B981),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Section: Activity Logs
        item {
            Text(
                text = "Nhật Ký Sử Dụng & Tuân Thủ Của Trẻ:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (logs.isEmpty()) {
            item {
                Text(
                    text = "Chưa có nhật ký ghi nhận.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        } else {
            items(logs) { record ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = record.ageGroupName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = dateFormat.format(Date(record.timestamp)),
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Thời lượng: ${record.minutesSpent} phút | ${record.notes}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
