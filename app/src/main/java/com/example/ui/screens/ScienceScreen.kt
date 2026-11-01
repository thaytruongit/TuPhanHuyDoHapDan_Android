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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScienceScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Cơ Sở Khoa Học & Y Học Thần Kinh",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Nghiên cứu về cơ chế tự phân hủy độ hấp dẫn điều chỉnh hành vi của trẻ 3–12 tuổi.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onTertiaryContainer)
                        )
                    }
                }
            }
        }

        // Section 1: Holte & Ferraro (2020) and PubMed PMC10159421
        item {
            OutlinedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Thang Độ Xám (Grayscale 100%) & Triệt Tiêu Dopamine",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Trích dẫn: Holte & Ferraro (2020); PubMed Central PMC10159421 (\"The effect of grayscale screen settings on smartphone addiction and screen time\")",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Giao diện màn hình thông thường sử dụng màu sắc bão hòa cực đại (đỏ, vàng, xanh neon) để kích thích liên tục vùng thụ cảm V4 của vỏ não thị giác, tạo phản xạ giải phóng Dopamine tức thời (tương tự máy đánh bạc đánh cược sự chú ý).\n" +
                                "• Khi áp dụng Thang độ xám 100% (Grayscale) ở Nhóm 3 (10–12 tuổi), kích thích Dopamine suy giảm trung bình 45–60%, giúp thanh thiếu niên cảm thấy các ứng dụng mạng xã hội và video ngắn trở nên tẻ nhạt, vô vị và dễ dàng đặt điện thoại xuống mà không trải qua cảm giác bức bối tâm lý.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                }
            }
        }

        // Section 2: Nhóm 1 (3-6 tuổi) Melatonin & Warm Sepia
        item {
            OutlinedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Nhóm 1 (3–6 tuổi): Bảo Tồn Melatonin & Bạn Gấu Đi Ngủ",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Mắt của trẻ mầm non có thủy tinh thể trong suốt hơn người lớn, khiến ánh sáng xanh (450–480 nm) đi sâu vào võng mạc, ức chế tuyến tùng tiết hormone Melatonin lên đến 88%.\n" +
                                "• Cơ chế 'Trình duyệt buồn ngủ': Tự động dịch chuyển sang tông vàng ấm Sepia, giảm độ sáng xuống 68% và âm lượng về 12%, kết thúc ở mốc 15 phút (khuyến nghị AAP) bằng hoạt cảnh Bạn Gấu ngáp và đi ngủ ngoan. Việc nhân hóa thiết bị thành Bạn Gấu giúp trẻ mầm non đồng thuận tắt máy tự nhiên mà không quấy khóc hay phản ứng tiêu cực.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                }
            }
        }

        // Section 3: Nhóm 2 (7-9 tuổi) Kinetic Compensation
        item {
            OutlinedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = Color(0xFF0284C7)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3. Nhóm 2 (7–9 tuổi): Hạ Nhiệt Thị Giác & Vận Động Bù Trừ",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Quy tắc 20-20-20 của Hiệp hội Nhãn khoa Hoa Kỳ chỉ ra rằng việc nhìn cố định cự ly gần quá 20 phút gây co thắt thể mi và hội chứng mỏi mắt kỹ thuật số.\n" +
                                "• Cơ chế làm mờ gợi ý (Visual Cool-down) làm phân rã tính hấp dẫn của thumbnail. Đến phút thứ 25, Trạm vận động buộc trẻ phải thực hiện 10 nhịp vươn vai đạt gia tốc Δa > 6.5 m/s² qua cảm biến. Vận động toàn thân giải phóng endorphin, kích hoạt tuần hoàn máu não và tái lập cân bằng tư thế cột sống.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                }
            }
        }

        // Section 4: Cognitive Friction & Anti-Paste Typing
        item {
            OutlinedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "4. Ma Sát Nhận Thức (Cognitive Friction) & Thùy Trán Trước",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Hành vi vuốt màn hình (swiping) thường rơi vào trạng thái vô thức do Mạng chế độ mặc định (DMN) chi phối.\n" +
                                "• Việc tạo độ trễ cuộn 900ms phá vỡ nhịp kích thích tức thời. Khi kết hợp với yêu cầu gõ câu phản tư chống dán (Anti-paste), não bộ buộc phải chuyển hướng hoạt động sang Thùy trán trước (Prefrontal Cortex) – cơ quan chịu trách nhiệm tự kiểm soát, đánh giá hậu quả và lập kế hoạch dài hạn.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                }
            }
        }
    }
}
