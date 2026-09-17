package com.stockidle.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stockidle.game.ui.theme.LocalPal
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 金币雨：分红到账 / 任务奖励时，屏幕顶部飘落金币。
 * 触发方式：传入一个不断自增的 trigger 值。
 */
@Composable
fun CoinRain(trigger: Int, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    data class Coin(
        val x0: Float, val delayMs: Float, val speed: Float,
        val radius: Float, val sway: Float, val seed: Float,
    )
    val coins = remember { mutableStateListOf<Coin>() }
    val running = remember { mutableStateOf(0L) } // 开始时间戳，0 表示未运行

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        val rng = Random(trigger)
        coins.clear()
        repeat(20) {
            coins.add(
                Coin(
                    x0 = rng.nextFloat(),
                    delayMs = rng.nextFloat() * 350f,
                    speed = 0.9f + rng.nextFloat() * 0.8f,
                    radius = 5f + rng.nextFloat() * 7f,
                    sway = 12f + rng.nextFloat() * 26f,
                    seed = rng.nextFloat() * 6.28f,
                )
            )
        }
        running.value = withFrameNanos { it }
        val totalMs = 1650L
        while (true) {
            withFrameNanos { now ->
                if (now - running.value > totalMs * 1_000_000) {
                    coins.clear()
                }
            }
            if (coins.isEmpty()) break
        }
        running.value = 0
    }

    if (coins.isNotEmpty()) {
        Canvas(modifier) {
            val start = running.value
            if (start == 0L) return@Canvas
            val elapsedMs = (System.nanoTime() - start) / 1_000_000f
            for (c in coins) {
                val t = ((elapsedMs - c.delayMs) / 1400f)
                if (t <= 0f || t >= 1f) continue
                val y = -40f + t * (size.height + 120f) * c.speed
                if (y < -40f || y > size.height + 40f) continue
                val x = c.x0 * size.width + sin(c.seed + t * 5f) * c.sway
                val alpha = (1f - t * 0.85f).coerceIn(0f, 1f)
                val gold = Color(0xFFF5C451).copy(alpha = alpha)
                val rim = Color(0xFFB8860B).copy(alpha = alpha)
                drawCircle(rim, radius = c.radius, center = Offset(x, y))
                drawCircle(gold, radius = c.radius * 0.78f, center = Offset(x, y))
                drawCircle(Color.White.copy(alpha = alpha * 0.5f), radius = c.radius * 0.28f, center = Offset(x, y))
            }
        }
    }
}

/**
 * 成交粒子：买卖成交时从屏幕中下方爆开的彩色粒子。
 */
@Composable
fun TradeBurst(trigger: Int, good: Boolean, modifier: Modifier = Modifier) {
    data class P(val angle: Float, val speed: Float, val size: Float, val colorIdx: Int, val delayMs: Float)
    val parts = remember { mutableStateListOf<P>() }
    val running = remember { mutableLongStateOf(0L) }
    val pal = LocalPal.current

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        val rng = Random(trigger)
        parts.clear()
        repeat(26) {
            parts.add(
                P(
                    angle = rng.nextFloat() * 6.2832f,
                    speed = 0.35f + rng.nextFloat() * 0.9f,
                    size = 3f + rng.nextFloat() * 6f,
                    colorIdx = rng.nextInt(3),
                    delayMs = rng.nextFloat() * 120f,
                )
            )
        }
        running.longValue = withFrameNanos { it }
        val totalMs = 900L
        while (true) {
            withFrameNanos { now ->
                if (now - running.longValue > totalMs * 1_000_000) parts.clear()
            }
            if (parts.isEmpty()) break
        }
        running.longValue = 0
    }

    if (parts.isNotEmpty()) {
        val base = if (good) pal.up else pal.down
        val palette = listOf(base, pal.gold, Color.White)
        Canvas(modifier) {
            val start = running.longValue
            if (start == 0L) return@Canvas
            val elapsedMs = (System.nanoTime() - start) / 1_000_000f
            val origin = Offset(size.width / 2f, size.height * 0.62f)
            for (p in parts) {
                val t = ((elapsedMs - p.delayMs) / 850f)
                if (t <= 0f || t >= 1f) continue
                val dist = p.speed * t * 240f
                val x = origin.x + cos(p.angle) * dist
                val y = origin.y + sin(p.angle) * dist + t * t * 120f // 重力
                val alpha = (1f - t).coerceIn(0f, 1f)
                drawCircle(
                    palette[p.colorIdx % palette.size].copy(alpha = alpha * 0.9f),
                    radius = p.size * (1f - t * 0.5f),
                    center = Offset(x, y),
                )
            }
        }
    }
}

/**
 * "+¥xx" 飘字：从给定位置向上飘并淡出。
 */
@Composable
fun FloatingAmount(
    trigger: Int,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val visible = remember { mutableStateOf(false) }
    val progress = remember { mutableStateOf(0f) }
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        visible.value = true
        val start = withFrameNanos { it }
        val durMs = 1300.0
        var t = 0.0
        while (t < 1.0) {
            withFrameNanos { now ->
                t = ((now - start) / 1e6 / durMs).coerceIn(0.0, 1.0)
                progress.value = t.toFloat()
            }
        }
        visible.value = false
    }
    if (visible.value) {
        val t = progress.value
        val alpha = (1f - t).coerceIn(0f, 1f)
        Text(
            text = text,
            color = color.copy(alpha = alpha),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = modifier.graphicsLayer { translationY = -t * 90f },
        )
    }
}
