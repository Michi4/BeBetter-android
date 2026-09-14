package at.websters.bebetter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Exact replica of frontend Logo.vue: 5x5 grid of rounded squares,
 * base #21c55d with varying alpha (full / 0xAA / 0x30).
 */
@Composable
fun BeBetterLogo(size: Dp = 28.dp, light: Boolean = false) {
    Canvas(Modifier.size(size)) {
        val s = size.toPx()
        // background rounded rect (bb-bg 85% + border)
        drawRoundRect(
            color = if (light) Color(0xFFF2F0EA) else Color(0xFF0B0C0F).copy(alpha = 0.85f),
            topLeft = Offset.Zero, size = Size(s, s), cornerRadius = CornerRadius(s * 0.044f)
        )
        drawRoundRect(
            color = if (light) Color(0xFFD8DDE3) else Color(0xFF232833),
            topLeft = Offset.Zero, size = Size(s, s), cornerRadius = CornerRadius(s * 0.044f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = s * 0.012f)
        )
        val base = Color(0xFF21C55D)
        // alpha pattern from Logo.vue (row-major)
        val alphas = listOf(
            1f, 1f, 0.67f, 1f, 0.19f,
            1f, 0.19f, 1f, 0.19f, 1f,
            1f, 1f, 0.67f, 1f, 0.19f,
            1f, 1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f, 1f
        )
        val cell = s / 5f
        val pad = cell * 0.08f
        val sq = cell * 0.84f
        for (r in 0 until 5) for (c in 0 until 5) {
            val a = alphas[r * 5 + c]
            drawRoundRect(
                color = base.copy(alpha = a),
                topLeft = Offset(c * cell + pad / 2 + s * 0.04f, r * cell + pad / 2 + s * 0.04f),
                size = Size(sq * 0.92f, sq * 0.92f),
                cornerRadius = CornerRadius(sq * 0.27f)
            )
        }
    }
}
