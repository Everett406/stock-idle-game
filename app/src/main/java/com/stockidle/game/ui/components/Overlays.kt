package com.stockidle.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.stockidle.game.ui.theme.AppShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockidle.game.game.ActiveEvent
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt
import kotlin.math.min
import kotlin.random.Random

/**
 * 突发新闻横幅：从顶部弹入的红色 BREAKING 横条。
 * 利好 = 金色光边；利空 = 暗红光边。自动 7 秒后收回。
 */
@Composable
fun BreakingBanner(
    event: ActiveEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = rememberInfiniteTransition(label = "newsGlow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.9f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "ga",
    )

    LaunchedEffect(event?.id) {
        if (event != null) {
            kotlinx.coroutines.delay(7000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = event != null,
        enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(280)) { -it } + fadeOut(),
        modifier = modifier,
    ) {
        val ev = event ?: return@AnimatedVisibility
        val good = ev.good
        val barBrush = if (good) Brush.horizontalGradient(listOf(Color(0xFF7A4D00), Color(0xFFB8860B), Color(0xFF7A4D00)))
                       else Brush.horizontalGradient(listOf(Color(0xFF5C0E14), Color(0xFF9A1B24), Color(0xFF5C0E14)))
        val edge = if (good) Color(0xFFF5C451).copy(alpha = glowAlpha) else Color(0xFF2B0508).copy(alpha = min(1f, glowAlpha + 0.3f))
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(AppShape.Card)
                .background(barBrush)
                .background(Color.Transparent)
                .padding(1.5.dp),
        ) {
            // 光边
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(AppShape.Card)
                    .background(Color.Black.copy(alpha = 0.0f))
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcons.Icon(kind = "bolt", color = if (good) Color(0xFFFFE9B0) else Color(0xFFFFC9CE), size = 18.dp, filled = true)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                ) {
                    Text(
                        text = "突发 BREAKING · ${if (good) "利好" else "利空"} ${Fmt.pct(if (good) ev.magnitudePct else -ev.magnitudePct)}",
                        color = if (good) Color(0xFFFFE9B0) else Color(0xFFFFC9CE),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = ev.body,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
                // 倒计时角标
                Text(
                    text = Fmt.clock((ev.untilMs - System.currentTimeMillis()).coerceAtLeast(0) / 1000),
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            // 光边框
            Box(
                Modifier
                    .matchParentSize()
                    .clip(AppShape.Card)
                    .background(Color.Transparent)
            )
            Canvas(Modifier.matchParentSize()) {
                drawRoundRect(
                    color = edge,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f),
                )
            }
        }
    }
}

/**
 * 解锁新股票：锁链破碎 + 闪光 + 新股票卡片弹入。
 */
@Composable
fun UnlockOverlay(
    triggerId: Int,
    stockName: String,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    data class Shard(val angle: Float, val speed: Float, val size: Float)
    val visible = remember { mutableStateOf(false) }
    val shards = remember { mutableStateListOf<Shard>() }
    val running = remember { mutableStateOf(0L) }
    val pal = LocalPal.current

    LaunchedEffect(triggerId) {
        if (triggerId <= 0) return@LaunchedEffect
        val rng = Random(triggerId * 7919)
        shards.clear()
        repeat(30) {
            shards.add(Shard(rng.nextFloat() * 6.2832f, 0.3f + rng.nextFloat(), 3f + rng.nextFloat() * 8f))
        }
        visible.value = true
        running.value = withFrameNanos { it }
        kotlinx.coroutines.delay(2000)
        visible.value = false
        shards.clear()
    }

    AnimatedVisibility(
        visible = visible.value,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(320)),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            // 碎片飞散
            Canvas(Modifier.fillMaxSize()) {
                val start = running.value
                if (start == 0L) return@Canvas
                val t = ((System.nanoTime() - start) / 1e9).toFloat().coerceIn(0f, 1f)
                val cx = size.width / 2f
                val cy = size.height / 2f
                for (s in shards) {
                    val d = s.speed * t * 320f
                    val a = (1f - t).coerceIn(0f, 1f)
                    drawRect(
                        color = pal.gold.copy(alpha = a * 0.9f),
                        topLeft = Offset(cx + kotlin.math.cos(s.angle) * d, cy + kotlin.math.sin(s.angle) * d - 40f),
                        size = androidx.compose.ui.geometry.Size(s.size, s.size),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    val t = ((System.nanoTime() - running.value) / 1e9).toFloat().coerceIn(0f, 1f)
                    val s = 0.6f + 0.4f * t
                    scaleX = s; scaleY = s
                    alpha = t
                },
            ) {
                AppIcons.Icon(kind = "unlock", color = pal.gold, size = 48.dp)
                Text(
                    "新股票解锁",
                    color = pal.gold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "$symbol · $stockName",
                    color = pal.text,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    "已加入市场，快去看看吧",
                    color = pal.textDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * 转生特效：金色粒子上升 + 资产数字倒流归零 + 经理徽章金光。
 * 全屏、约 3.6 秒，是全游戏最高规格的动画。
 */
@Composable
fun PrestigeOverlay(
    triggerId: Int,
    oldTotal: Double,
    newLevel: Int,
    modifier: Modifier = Modifier,
) {
    data class Spark(val x0: Float, val speed: Float, val size: Float, val delay: Float)
    val visible = remember { mutableStateOf(false) }
    val sparks = remember { mutableStateListOf<Spark>() }
    val running = remember { mutableStateOf(0L) }
    val rewinding = remember { mutableDoubleStateOf(oldTotal) }
    val pal = LocalPal.current

    LaunchedEffect(triggerId) {
        if (triggerId <= 0) return@LaunchedEffect
        val rng = Random(triggerId * 104729)
        sparks.clear()
        repeat(46) {
            sparks.add(Spark(rng.nextFloat(), 0.5f + rng.nextFloat() * 0.9f, 2f + rng.nextFloat() * 5f, rng.nextFloat() * 1200f))
        }
        visible.value = true
        running.value = withFrameNanos { it }

        // 数字倒流 0~1.6s
        val startN = withFrameNanos { it }
        var t = 0.0
        while (t < 1.0) {
            withFrameNanos { now ->
                t = ((now - startN) / 1e6 / 1600.0).coerceIn(0.0, 1.0)
                val e = 1.0 - (1.0 - t) * (1.0 - t)
                rewinding.doubleValue = oldTotal * (1.0 - e)
            }
        }
        rewinding.doubleValue = 0.0
        kotlinx.coroutines.delay(2000)
        visible.value = false
    }

    AnimatedVisibility(
        visible = visible.value,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(450)),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF4A3608).copy(alpha = 0.92f), Color.Black.copy(alpha = 0.94f)),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            // 上升金色粒子
            Canvas(Modifier.fillMaxSize()) {
                val start = running.value
                if (start == 0L) return@Canvas
                val elapsed = ((System.nanoTime() - start) / 1e9).toFloat()
                for (s in sparks) {
                    val life = ((elapsed - s.delay / 1000f) % 2.2f) / 2.2f
                    if (life < 0f) continue
                    val y = size.height * (1f - life)
                    val x = s.x0 * size.width + kotlin.math.sin(elapsed * 2f + s.x0 * 20f) * 14f
                    drawCircle(
                        pal.gold.copy(alpha = (1f - life) * 0.75f),
                        radius = s.size,
                        center = Offset(x, y),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("转生", color = pal.gold, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Text(
                    "总资产清盘",
                    color = pal.textDim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                // 倒流归零的资产数字
                Text(
                    Fmt.money(rewinding.doubleValue),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 14.dp),
                )
                // 经理徽章
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 26.dp)
                        .graphicsLayer {
                            val t = ((System.nanoTime() - running.value) / 1e6 / 1900f).toFloat().coerceIn(0f, 1f)
                            val s = if (t < 1f) 0.4f + 0.6f * min(1.3f, t * 1.6f) else 1f
                            scaleX = s; scaleY = s
                        },
                ) {
                    Box(
                        Modifier
                            .size(112.dp, 64.dp)
                            .clip(AppShape.Card)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF8A6A1C), Color(0xFFF5C451), Color(0xFF8A6A1C)))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("经理", color = Color(0xFF2B1D00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Lv.$newLevel", color = Color(0xFF2B1D00), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Text(
                    "永久加成：分红 +30% · 初始资金提升",
                    color = pal.gold.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}
