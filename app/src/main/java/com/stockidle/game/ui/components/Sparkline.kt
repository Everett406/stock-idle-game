package com.stockidle.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 迷你走势图（股票卡片用）：折线 + 底部渐变填充。
 */
@Composable
fun Sparkline(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidthPx: Float = 4f,
) {
    Canvas(modifier) {
        if (data.size < 2) return@Canvas
        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        for (d in data) {
            if (d < min) min = d
            if (d > max) max = d
        }
        val range = if (max - min > 1e-9) max - min else 1.0
        val n = data.size
        val stepX = size.width / (n - 1)
        fun yOf(v: Double): Float {
            val t = ((v - min) / range).coerceIn(0.0, 1.0)
            return (size.height * (1.0 - t)).toFloat()
        }

        val line = Path()
        val fill = Path()
        data.forEachIndexed { i, v ->
            val x = i * stepX
            val y = yOf(v)
            if (i == 0) {
                line.moveTo(x.toFloat(), y)
                fill.moveTo(x.toFloat(), y)
            } else {
                line.lineTo(x.toFloat(), y)
                fill.lineTo(x.toFloat(), y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.lineTo(0f, size.height)
        fill.close()

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
                startY = 0f, endY = size.height,
            ),
        )
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = strokeWidthPx),
        )
        // 末端点
        val lastX = size.width
        val lastY = yOf(data.last())
        drawCircle(color = color, radius = strokeWidthPx * 1.6f, center = Offset(lastX - strokeWidthPx, lastY))
    }
}
