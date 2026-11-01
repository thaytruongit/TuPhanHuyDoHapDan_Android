package com.example.ui.components

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
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
     * - Nhóm 1 (3-6t): Tông vàng ấm Sepia tăng dần
     * - Nhóm 2 (7-9t): Hạ nhiệt thị giác & giảm độ rực màu
     * - Nhóm 3 (10-12t): Thang độ xám (Grayscale) 100% về trắng đen theo Holte & Ferraro 2020 & PMC10159421
     * Hỗ trợ miễn giảm làm xám cho bài giảng học tập YouTube (isExempted = true).
     */
    fun createDecayColorFilter(
        ageGroup: AgeGroup,
        progress: Float,
        isExempted: Boolean = false
    ): ColorFilter? {
        if (isExempted || progress <= 0.01f) return null

        return when (ageGroup) {
            AgeGroup.TODDLER -> {
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
                val sat = (1f - progress).coerceIn(0f, 1f)
                val matrix = ColorMatrix()
                matrix.setToSaturation(sat)
                ColorFilter.colorMatrix(matrix)
            }
            AgeGroup.PRIMARY -> {
                val sat = (1f - (progress * 0.85f)).coerceIn(0.15f, 1f)
                val matrix = ColorMatrix()
                matrix.setToSaturation(sat)
                ColorFilter.colorMatrix(matrix)
            }
        }
    }

    /**
     * Tính độ mờ (Blur) của các thành phần gợi ý hấp dẫn
     */
    fun calculateVisualCoolDownBlur(progress: Float, isExempted: Boolean = false): Dp {
        if (isExempted) return 0.dp
        return (progress * 16f).dp
    }
}

/**
 * Modifier áp dụng trực tiếp bộ lọc màu (Trắng đen 100% / Sepia / Hạ nhiệt màu) lên toàn bộ vùng giao diện
 */
fun Modifier.applyScreenDecayEffect(
    ageGroup: AgeGroup,
    progress: Float,
    isExempted: Boolean = false
): Modifier = this.drawWithContent {
    val filter = VisualDecayHelper.createDecayColorFilter(ageGroup, progress, isExempted)
    if (filter == null) {
        drawContent()
    } else {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                colorFilter = filter
            }
            canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            canvas.restore()
        }
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
