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
 * Pixel-perfect replica of frontend Logo.vue / favicon.svg:
 * viewBox 0 0 500, outer rx22 85% bb-bg + stroke 6 var(--bb-line),
 * 5x5 cells 80x80 rx22 at 20/115/210/305/400 with #21c55d alpha 1 / aa / 30.
 */
@Composable
fun BeBetterLogo(size: Dp = 28.dp, light: Boolean = false) {
    Canvas(Modifier.size(size)) {
        val s = size.toPx()
        val scale = s / 500f
        fun sx(v: Float) = v * scale
        // background 85% bb-bg
        drawRoundRect(
            color = if (light) Color(0xFFFAF9F6).copy(alpha = 0.85f) else Color(0xFF0B0C0F).copy(alpha = 0.85f),
            topLeft = Offset.Zero, size = Size(s, s), cornerRadius = CornerRadius(sx(22f))
        )
        drawRoundRect(
            color = if (light) Color(0xFFD8DDE3) else Color(0xFF232833),
            topLeft = Offset.Zero, size = Size(s, s), cornerRadius = CornerRadius(sx(22f)),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = sx(6f))
        )
        val base = Color(0xFF21C55D)
        val alphas = floatArrayOf(
            1f, 1f, 0.667f, 1f, 0.188f,
            1f, 0.188f, 1f, 0.188f, 1f,
            1f, 1f, 0.667f, 1f, 0.188f,
            1f, 1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f, 1f
        )
        // exact SVG coords: x in {20,115,210,305,400}, y same, size 80 rx22
        val xs = floatArrayOf(sx(20f), sx(115f), sx(210f), sx(305f), sx(400f))
        val ys = floatArrayOf(sx(20f), sx(115f), sx(210f), sx(305f), sx(400f))
        val cellSize = sx(80f)
        val cellRadius = sx(22f)
        for (r in 0 until 5) for (c in 0 until 5) {
            val a = alphas[r * 5 + c]
            drawRoundRect(
                color = base.copy(alpha = a),
                topLeft = Offset(xs[c], ys[r]),
                size = Size(cellSize, cellSize),
                cornerRadius = CornerRadius(cellRadius)
            )
        }
    }
}
