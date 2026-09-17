package com.stockidle.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.stockidle.game.game.GameState
import com.stockidle.game.game.StockUi
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt
import kotlin.math.ceil

/**
 * 顶部横向滚动的实时行情条：
 * 内容按“循环宽度”复制多份，用无限动画平移，视觉上无缝循环。
 * 点击任意项跳转股票详情。
 */
@Composable
fun TickerTape(
    stocks: List<StockUi>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    val items = stocks.filter { !it.locked }
    if (items.isEmpty()) {
        BoxWithConstraints(modifier = modifier.height(38.dp)) {}
        return
    }
    val itemWidth = 132.dp
    val loopWidthDp = itemWidth * items.size
    val density = LocalDensity.current

    // 速度约 34dp/s
    val durationMs = (loopWidthDp.value / 34.0 * 1000.0).toInt().coerceIn(4000, 60000)
    val transition = rememberInfiniteTransition(label = "tape")
    val frac by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing)),
        label = "tapeFrac",
    )

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(pal.surfaceHigh),
    ) {
        val loopPx = with(density) { loopWidthDp.toPx() }
        val groups = ceil(constraints.maxWidth / loopPx).toInt() + 1
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .graphicsLayer { translationX = -frac * loopPx },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(groups) {
                items.forEach { s ->
                    TapeItem(s, itemWidth, onClick)
                }
            }
        }
    }
}

@Composable
private fun TapeItem(s: StockUi, width: androidx.compose.ui.unit.Dp, onClick: (String) -> Unit) {
    val pal = LocalPal.current
    val color = if (s.sessionChangePct > 1e-9) pal.up
    else if (s.sessionChangePct < -1e-9) pal.down
    else pal.textDim
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clickable { onClick(s.def.id) }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = s.def.symbol,
                style = MaterialTheme.typography.labelSmall,
                color = pal.textDim,
                maxLines = 1,
            )
            if (s.shares > 1e-9) {
                Text(
                    text = " 持",
                    style = MaterialTheme.typography.labelSmall,
                    color = pal.gold,
                    maxLines = 1,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Fmt.price(s.price),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = color,
                maxLines = 1,
            )
            Text(
                text = "  " + Fmt.pct(s.sessionChangePct),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = color,
                maxLines = 1,
            )
        }
    }
}
