package com.stockidle.game.ui.components

import com.stockidle.game.ui.theme.LocalPal
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max

/**
 * 详情页分时图：网格 + 折线 + 渐变填充 + 末端呼吸光点。
 */
@Composable
fun LineChart(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    val pulse = rememberInfiniteTransition(label = "chartPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val pulseR by pulse.animateFloat(
        initialValue = 7f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pulseR",
    )

    Canvas(modifier) {
        if (data.size < 2) return@Canvas
        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        for (d in data) {
            if (d < min) min = d
            if (d > max) max = d
        }
        val range = if (max - min > 1e-9) max - min else 1.0
        val pad = size.height * 0.08f
        val h = size.height - pad * 2
        val n = data.size
        val stepX = size.width / max(1, n - 1)

        fun yOf(v: Double): Float {
            val t = ((v - min) / range).coerceIn(0.0, 1.0)
            return (pad + h * (1.0 - t)).toFloat()
        }

        // 横向网格
        val gridColor = if (pal.isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.07f)
        for (i in 0..4) {
            val gy = pad + h * i / 4f
            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2f)
        }

        val line = Path()
        val fill = Path()
        data.forEachIndexed { i, v ->
            val x = i * stepX
            val y = yOf(v)
            if (i == 0) {
                line.moveTo(x.toFloat(), y); fill.moveTo(x.toFloat(), y)
            } else {
                line.lineTo(x.toFloat(), y); fill.lineTo(x.toFloat(), y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.lineTo(0f, size.height)
        fill.close()

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.28f), Color.Transparent),
                startY = 0f, endY = size.height,
            ),
        )
        drawPath(path = line, color = color, style = Stroke(width = 5f))

        // 末端呼吸点
        val lx = size.width - stepX * 0.2f
        val ly = yOf(data.last())
        drawCircle(color.copy(alpha = pulseAlpha * 0.35f), radius = pulseR * 2.2f, center = Offset(lx, ly))
        drawCircle(color.copy(alpha = pulseAlpha), radius = pulseR, center = Offset(lx, ly))
        drawCircle(Color.White.copy(alpha = 0.9f * pulseAlpha), radius = pulseR * 0.45f, center = Offset(lx, ly))
    }
}
