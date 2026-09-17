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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockidle.game.game.GameState
import com.stockidle.game.game.StockUi
import com.stockidle.game.ui.components.AnimatedNumber
import com.stockidle.game.ui.components.AppIcons
import com.stockidle.game.ui.components.ChangeText
import com.stockidle.game.ui.components.Sparkline
import com.stockidle.game.ui.components.TickerTape
import com.stockidle.game.ui.theme.AppShape
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt
import kotlin.math.abs

/**
 * 行情页 v0.2：滚动行情条 + 大盘指数卡 + 排序筛选 + 股票列表（直角硬朗卡片）。
 */
@Composable
fun MarketScreen(
    state: GameState,
    onOpenDetail: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val pal = LocalPal.current
    var sortMode by rememberSaveable { mutableIntStateOf(0) } // 0默认 1涨幅 2跌幅 3自选

    Column(Modifier.fillMaxSize()) {
        TickerTape(stocks = state.stocks, onClick = onOpenDetail)

        // 大盘指数卡
        IndexCard(state)

        // 排序筛选
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val modes = listOf("默认" to 0, "涨幅" to 1, "跌幅" to 2, "自选" to 3)
            modes.forEach { (label, m) ->
                val selected = sortMode == m
                Box(
                    Modifier
                        .background(if (selected) pal.gold else pal.surface)
                        .border(1.dp, if (selected) pal.gold else pal.border, AppShape.Card)
                        .clickable { sortMode = m }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        label,
                        color = if (selected) Color(0xFF2B1D00) else pal.textDim,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (state.windowActive) {
                Row(
                    Modifier
                        .background(pal.goldDim)
                        .border(1.dp, pal.gold, AppShape.Mini)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcons.Icon(kind = "clock", color = pal.gold, size = 12.dp)
                    Spacer(Modifier.width(4.dp))
                    Text("波动翻倍", color = pal.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 列表数据（排序）
        val unlockedBase = state.stocks.filter { !it.locked }
        val unlocked = when (sortMode) {
            1 -> unlockedBase.sortedByDescending { it.sessionChangePct }
            2 -> unlockedBase.sortedBy { it.sessionChangePct }
            3 -> unlockedBase.sortedWith(compareByDescending<StockUi> { it.favorite }.thenByDescending { it.sessionChangePct })
            else -> unlockedBase
        }
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
            if (sortMode != 3) {
                items(locked, key = { "lock_" + it.def.id }) { s ->
                    StockCard(
                        s = s,
                        locked = true,
                        onClick = {},
                        onStar = {},
                        maxAssets = state.maxTotalAssets,
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

/** 大盘指数：数值 + 涨跌 + 迷你走势 */
@Composable
private fun IndexCard(state: GameState) {
    val pal = LocalPal.current
    val up = state.marketIndex > 1e-9
    val down = state.marketIndex < -1e-9
    val color = if (up) pal.up else if (down) pal.down else pal.textDim

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(pal.surface)
            .border(1.dp, pal.border, AppShape.Card)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(96.dp)) {
            Text("大盘指数", color = pal.textDim, fontSize = 10.sp)
            AnimatedNumber(
                value = state.indexValue,
                format = { String.format(java.util.Locale.CHINA, "%.1f", it) },
                color = color,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Column(Modifier.width(84.dp)) {
            Text("平均涨跌", color = pal.textDim, fontSize = 10.sp)
            ChangeText(pct = state.marketIndex, style = MaterialTheme.typography.titleSmall)
        }
        Sparkline(
            data = state.indexHistory.takeLast(240),
            color = color,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
        )
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
            .background(color.copy(alpha = 0.12f), AppShape.Mini)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text("市场 $label", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * 股票卡片 v0.2（直角终端风）：
 * - 迷你实时走势图 + 涨跌边框泛红/泛绿
 * - 涨跌停 / 委托中 / 事件角标
 * - 未解锁：锁图标 + 解锁进度
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
            .background(if (highlight) pal.surfaceHigh else pal.surface)
            .border(1.5.dp, borderColor, AppShape.Card)
            .clickable(enabled = !locked) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // 右上角标：涨跌停 > 事件 > 委托
        if (!locked && (s.limitState != 0 || s.hasEvent || s.pendingOrders > 0)) {
            Row(
                Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (s.limitState != 0) {
                    TagBadge(
                        text = if (s.limitState > 0) "涨停" else "跌停",
                        color = if (s.limitState > 0) pal.up else pal.down,
                    )
                }
                if (s.hasEvent) {
                    TagBadge(
                        text = if (s.eventGood) "利好" else "利空",
                        color = if (s.eventGood) pal.gold else pal.down,
                    )
                }
                if (s.pendingOrders > 0) {
                    TagBadge(text = "委托${s.pendingOrders}", color = pal.teal)
                }
            }
        }
        if (locked) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp),
                    contentAlignment = Alignment.Center,
                ) { AppIcons.Icon(kind = "lock", color = pal.textDim, size = 20.dp) }
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
                            .height(4.dp),
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
                        AppIcons.Icon(
                            kind = "star",
                            color = if (s.favorite) pal.gold else pal.textDim,
                            size = 14.dp,
                            filled = s.favorite,
                            modifier = Modifier.clickable { onStar() },
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "分红 " + String.format(java.util.Locale.CHINA, "%.2f", s.def.divDailyPct) + "%/日",
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

@Composable
private fun TagBadge(text: String, color: Color) {
    val pal = LocalPal.current
    Box(
        Modifier
            .background(color.copy(alpha = 0.14f), AppShape.Mini)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
