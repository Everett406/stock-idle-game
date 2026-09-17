package com.stockidle.game.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockidle.game.game.GameState
import com.stockidle.game.game.StockUi
import com.stockidle.game.ui.components.AnimatedNumber
import com.stockidle.game.ui.components.ChangeText
import com.stockidle.game.ui.components.Sparkline
import com.stockidle.game.ui.components.TickerTape
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt
import kotlin.math.abs

/**
 * 行情页：顶部滚动行情条 + 市场概览 + 股票卡片列表（含未解锁占位）。
 */
@Composable
fun MarketScreen(
    state: GameState,
    onOpenDetail: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val pal = LocalPal.current
    Column(Modifier.fillMaxSize()) {
        TickerTape(stocks = state.stocks, onClick = onOpenDetail)

        // 市场概览
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("市场情绪指数", color = pal.textDim, fontSize = 11.sp)
                AnimatedNumber(
                    value = state.marketIndex,
                    format = { Fmt.pct(it) },
                    color = if (state.marketIndex > 0) pal.up else if (state.marketIndex < 0) pal.down else pal.text,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            MoodTag(state.mood)
            if (state.windowActive) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(pal.goldDim)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("⏱ 波动翻倍中", color = pal.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        val unlocked = state.stocks.filter { !it.locked }
        val locked = state.stocks.filter { it.locked }.sortedBy { it.def.unlockAt }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(unlocked, key = { it.def.id }) { s ->
                StockCard(
                    s = s,
                    locked = false,
                    onClick = { onOpenDetail(s.def.id) },
                    onStar = { onToggleFavorite(s.def.id) },
                )
            }
            items(locked, key = { "lock_" + it.def.id }) { s ->
                StockCard(
                    s = s,
                    locked = true,
                    onClick = {},
                    onStar = {},
                    maxAssets = state.maxTotalAssets,
                )
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@Composable
private fun MoodTag(mood: Double) {
    val pal = LocalPal.current
    val label = when {
        mood > 0.6 -> "亢奋"
        mood > 0.15 -> "偏多"
        mood > -0.15 -> "中性"
        mood > -0.6 -> "偏空"
        else -> "恐慌"
    }
    val color = when {
        mood > 0.15 -> pal.up
        mood < -0.15 -> pal.down
        else -> pal.textDim
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text("市场 $label", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * 股票卡片：
 * - 迷你实时走势图
 * - 涨跌时边框泛红/泛绿
 * - 持仓股金点标记 + 自选星标轻微跳动
 * - 未解锁：锁定占位 + 解锁进度
 */
@Composable
fun StockCard(
    s: StockUi,
    locked: Boolean,
    onClick: () -> Unit,
    onStar: () -> Unit,
    maxAssets: Double = 0.0,
) {
    val pal = LocalPal.current

    // 边框颜色：按最新 tick 方向泛红/泛绿；平盘时用中性色
    val borderColor by animateColorAsState(
        targetValue = when {
            locked -> pal.border
            s.tickFlat -> pal.border
            s.tickUp -> pal.up.copy(alpha = 0.65f)
            else -> pal.down.copy(alpha = 0.65f)
        },
        animationSpec = tween(280),
        label = "cardBorder",
    )

    // 持仓股 / 自选股：轻微呼吸
    val highlight = !locked && (s.shares > 1e-9 || s.favorite)
    val breathe = rememberInfiniteTransition(label = "breathe" + s.def.id)
    val breatheAlpha by breathe.animateFloat(
        initialValue = if (highlight) 0.35f else 0.0f,
        targetValue = if (highlight) 0.75f else 0.0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "breatheA",
    )

    val changeColor = if (s.sessionChangePct > 1e-9) pal.up
    else if (s.sessionChangePct < -1e-9) pal.down
    else pal.textDim

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) pal.surfaceHigh else pal.surface)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = !locked) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (!locked && s.hasEvent) {
            // 事件角标
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (s.eventGood) pal.goldDim else pal.downDim)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    if (s.eventGood) "利好" else "利空",
                    color = if (s.eventGood) pal.gold else pal.down,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (locked) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(pal.surfaceHigh),
                    contentAlignment = Alignment.Center,
                ) { Text("🔒", fontSize = 15.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("？？？", color = pal.textDim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "总资产达到 ${Fmt.money(s.def.unlockAt)} 解锁",
                        color = pal.textDim,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (s.def.unlockAt <= 0) 1f
                            else (maxAssets / s.def.unlockAt).toFloat().coerceIn(0f, 1f)
                        },
                        color = pal.gold,
                        trackColor = pal.surfaceHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }
                Text(
                    "${((maxAssets / (s.def.unlockAt.coerceAtLeast(1.0))) * 100).toInt().coerceIn(0, 100)}%",
                    color = pal.gold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(112.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.def.name, color = pal.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        if (s.shares > 1e-9) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(pal.gold.copy(alpha = breatheAlpha + 0.3f)),
                            )
                        }
                    }
                    Text(
                        s.def.symbol + " · " + s.def.sector,
                        color = pal.textDim,
                        fontSize = 10.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (s.favorite) {
                            Text("★", color = pal.gold, fontSize = 12.sp, modifier = Modifier.clickable { onStar() })
                        } else {
                            Text("☆", color = pal.textDim, fontSize = 12.sp, modifier = Modifier.clickable { onStar() })
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "日分红 " + String.format(java.util.Locale.CHINA, "%.2f", s.def.divDailyPct) + "%",
                            color = pal.teal,
                            fontSize = 10.sp,
                        )
                    }
                }
                Sparkline(
                    data = s.history.takeLast(120),
                    color = changeColor,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .padding(horizontal = 4.dp),
                )
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(86.dp)) {
                    AnimatedNumber(
                        value = s.price,
                        format = { Fmt.price(it) },
                        color = changeColor,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    ChangeText(pct = s.sessionChangePct)
                }
            }
        }
    }
}
