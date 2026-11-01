package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AgeGroup

object VisualDecayHelper {

    /**
     * Tạo ColorFilter theo trạng thái suy giảm của từng nhóm tuổi:
     * - Nhóm 1: Tông vàng ấm Sepia tăng dần
     * - Nhóm 3: Thang độ xám (Grayscale) 100% theo Holte & Ferraro 2020 & PMC10159421
     */
    fun createDecayColorFilter(ageGroup: AgeGroup, progress: Float): ColorFilter? {
        return when (ageGroup) {
            AgeGroup.TODDLER -> {
                // Tông Sepia tăng dần theo thời gian (0f -> 1f)
                val s = progress.coerceIn(0f, 1f)
                val sepiaArray = floatArrayOf(
                    0.393f * s + (1f - s), 0.769f * s,             0.189f * s,             0f, 0f,
                    0.349f * s,             0.686f * s + (1f - s), 0.168f * s,             0f, 0f,
                    0.272f * s,             0.534f * s,             0.131f * s + (1f - s), 0f, 0f,
                    0f,                     0f,                     0f,                     1f, 0f
                )
                ColorFilter.colorMatrix(ColorMatrix(sepiaArray))
            }
            AgeGroup.MIDDLE_SCHOOL -> {
                // Thang độ xám 100% (Saturation chuyển từ 1.0 về 0.0)
                val sat = (1f - progress).coerceIn(0f, 1f)
                val matrix = ColorMatrix()
                matrix.setToSaturation(sat)
                ColorFilter.colorMatrix(matrix)
            }
            AgeGroup.PRIMARY -> {
                // Giảm nhẹ độ bão hòa màu để hạ nhiệt thị giác
                val sat = (1f - (progress * 0.45f)).coerceIn(0.55f, 1f)
                val matrix = ColorMatrix()
                matrix.setToSaturation(sat)
                ColorFilter.colorMatrix(matrix)
            }
        }
    }

    /**
     * Tính độ mờ (Blur) của các thành phần gợi ý hấp dẫn cho Nhóm 2 (Hạ nhiệt thị giác)
     */
    fun calculateVisualCoolDownBlur(progress: Float): Dp {
        // Blur tăng từ 0.dp đến 16.dp khi sắp đến phút thứ 25
        return (progress * 16f).dp
    }
}

/**
 * Modifier wrapper áp dụng làm mờ gợi ý theo cơ chế Nhóm 2
 */
fun Modifier.applyVisualCoolDown(blurRadius: Dp): Modifier {
    return if (blurRadius > 0.5.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(blurRadius)
    } else {
        this
    }
}
