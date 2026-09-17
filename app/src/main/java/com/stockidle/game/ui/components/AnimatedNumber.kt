package com.stockidle.game.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.stockidle.game.ui.theme.FlashDown
import com.stockidle.game.ui.theme.FlashUp
import com.stockidle.game.ui.theme.LocalPal
import kotlin.math.abs

/**
 * 带动画的数字文本：
 * - 数值变化时按缓动函数滚动（Double 精度，金额大也不抖动）
 * - 变化瞬间短暂变红（涨）/变绿（跌），然后回落
 * 全游戏的数字一律用这个组件，禁止瞬间跳变。
 */
@Composable
fun AnimatedNumber(
    value: Double,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    prefix: String = "",
    suffix: String = "",
    format: (Double) -> String,
    flash: Boolean = true,
) {
    val pal = LocalPal.current
    val display = remember { mutableDoubleStateOf(value) }
    var flashColorState = remember { mutableStateOf(Color.Transparent) }
    var lastValue = remember { mutableDoubleStateOf(value) }

    // 滚动动画
    LaunchedEffect(value) {
        val from = display.doubleValue
        val to = value
        if (abs(to - from) < 1e-12) return@LaunchedEffect
        val durMs = 320.0
        val startNanos = withFrameNanos { it }
        var t = 0.0
        while (t < 1.0) {
            withFrameNanos { now ->
                t = ((now - startNanos) / 1e6 / durMs).coerceIn(0.0, 1.0)
                val e = 1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t) // easeOutCubic
                display.doubleValue = from + (to - from) * e
            }
        }
        display.doubleValue = to
    }

    // 涨跌闪色
    LaunchedEffect(value) {
        if (flash) {
            val prev = lastValue.doubleValue
            if (value > prev + 1e-9) flashColorState.value = if (pal.isDark) FlashUp else FlashUp
            else if (value < prev - 1e-9) flashColorState.value = FlashDown
            lastValue.doubleValue = value
            kotlinx.coroutines.delay(450)
            flashColorState.value = Color.Transparent
        } else {
            lastValue.doubleValue = value
        }
    }

    val flashColor by animateColorAsState(
        targetValue = flashColorState.value,
        animationSpec = tween(220),
        label = "numFlash",
    )
    val effective = when {
        flashColor == Color.Transparent -> color
        color == Color.Unspecified -> flashColor
        else -> flashColor
    }

    Box(modifier) {
        Text(
            text = prefix + format(display.doubleValue) + suffix,
            style = style.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            color = if (effective == Color.Unspecified) MaterialTheme.colorScheme.onBackground else effective,
            maxLines = 1,
        )
    }
}

/**
 * 纯变化率文本（+x.xx%），红涨绿跌，无滚动。
 */
@Composable
fun ChangeText(
    pct: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium,
) {
    val pal = LocalPal.current
    val color = if (pct > 1e-9) pal.up else if (pct < -1e-9) pal.down else pal.textDim
    Text(
        text = com.stockidle.game.util.Fmt.pct(pct),
        style = style.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
        color = color,
        modifier = modifier,
        maxLines = 1,
    )
}
