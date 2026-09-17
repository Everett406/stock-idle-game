package com.stockidle.game.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.stockidle.game.ui.theme.LocalPal
import kotlin.math.abs

/**
 * 市场氛围背景：随市场整体状态缓慢变化。
 * - 牛市（mood>0）泛金；熊市（mood<0）泛蓝。
 * - 亮色模式下是微妙的背景色渐变，暗色模式下更深沉。
 * 变化周期用 2 秒的颜色动画，肉眼是“缓慢呼吸”的感觉。
 */
@Composable
fun MoodBackground(
    mood: Double,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    val m = mood.coerceIn(-1.0, 1.0)
    val strength = abs(m)

    val bullTint = if (pal.isDark) Color(0xFF8A6A1C) else Color(0xFFF0DFAE)
    val bearTint = if (pal.isDark) Color(0xFF14243E) else Color(0xFFB8CFE6)
    val tint = if (m >= 0) bullTint else bearTint
    val alpha = (if (pal.isDark) 0.22f else 0.30f) * strength.toFloat()

    val animatedTint by animateColorAsState(
        targetValue = tint.copy(alpha = alpha),
        animationSpec = tween(2000),
        label = "moodTint",
    )
    val base = pal.bg

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(base, animatedTint, 0.9f),
                        base,
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY,
                )
            ),
    )
}
