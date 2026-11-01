package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

@Composable
fun MovementStationDialog(
    currentDeltaA: Float,
    threshold: Float = 6.5f,
    repsDone: Int,
    targetReps: Int = 10,
    onSimulateRep: () -> Unit,
    onParentOverride: () -> Unit
) {
    val progress = (repsDone.toFloat() / targetReps.toFloat()).coerceIn(0f, 1f)
    val isThresholdReached = currentDeltaA >= threshold
    val deltaColor by animateColorAsState(
        targetValue = if (isThresholdReached) Color(0xFF10B981) else Color(0xFF3B82F6),
        label = "delta_color"
    )

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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B),
                            Color(0xFF0B132B)
                        )
                    )
                ),
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
                // Header Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06B6D4).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Trạm vận động",
                        tint = Color(0xFF06B6D4),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "TRẠM VẬN ĐỘNG & VƯƠN VAI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Cơ chế: Hạ nhiệt thị giác & Cảm biến gia tốc (Phút 25)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF94A3B8)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise illustration card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(200.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.stretch_station_kids_1793542742750),
                        contentDescription = "Bé tập vươn vai vận động",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Repetition counter card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF06B6D4), Color(0xFF10B981))
                        )
                    ),
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Mục tiêu: Đạt 10 nhịp vươn vai",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big Rep Circle
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(110.dp),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF334155),
                                strokeWidth = 10.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$repsDone/$targetReps",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "nhịp",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Real Accelerometer Gauge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = deltaColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gia tốc tức thời: Δa = ${"%.2f".format(currentDeltaA)} m/s²",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = deltaColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (currentDeltaA / 10f).coerceIn(0f, 1f) },
                            color = deltaColor,
                            trackColor = Color(0xFF334155),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Text(
                            text = if (isThresholdReached)
                                "✓ Tuyệt vời! Đã vượt ngưỡng Δa > 6.5 m/s² (Đếm nhịp)"
                            else
                                "Cần lực vươn vai: Δa > 6.5 m/s² để ghi nhận nhịp",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isThresholdReached) Color(0xFF34D399) else Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Physical action instruction
                Text(
                    text = "Hãy cầm điện thoại, đứng thẳng lưng, vươn 2 tay lên cao qua đầu rồi hạ xuống dứt khoát 10 lần nhé!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSimulateRep,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_movement_rep_button")
                    ) {
                        Text(text = "Kiểm thử (+1 nhịp)", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onParentOverride,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("movement_parent_override_button")
                    ) {
                        Text(text = "Phụ huynh", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
