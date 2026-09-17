package com.stockidle.game.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockidle.game.game.GameState
import com.stockidle.game.game.OrderUi
import com.stockidle.game.game.StockUi
import com.stockidle.game.ui.components.AnimatedNumber
import com.stockidle.game.ui.components.AppIcons
import com.stockidle.game.ui.theme.AppShape
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt

/**
 * 资产页 v0.2：总资产 + 现金（区分冻结）/ 待领分红 / 今日盈亏 + 委托单 + 持仓列表。
 */
@Composable
fun AssetsScreen(
    state: GameState,
    onCollectDividends: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onCancelOrder: (Long) -> Unit,
) {
    val pal = LocalPal.current
    val positions = state.stocks.filter { it.shares > 1e-9 }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            // 总览卡片
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(pal.surface)
                    .border(1.dp, pal.border, AppShape.Card)
                    .padding(16.dp),
            ) {
                Text("总资产", color = pal.textDim, fontSize = 12.sp)
                AnimatedNumber(
                    value = state.totalAssets,
                    format = { Fmt.money(it) },
                    color = pal.text,
                    style = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("可用现金", color = pal.textDim, fontSize = 11.sp)
                        AnimatedNumber(value = state.availableCash, format = { Fmt.money(it) }, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("持仓市值", color = pal.textDim, fontSize = 11.sp)
                        AnimatedNumber(value = state.positionMarketValue, format = { Fmt.money(it) }, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("今日盈亏", color = pal.textDim, fontSize = 11.sp)
                        AnimatedNumber(
                            value = state.todayPnL,
                            format = { Fmt.money(it) },
                            color = if (state.todayPnL >= 0) pal.up else pal.down,
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        )
                    }
                }
                if (state.frozenCash > 0.5) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "委托冻结现金 " + Fmt.money(state.frozenCash),
                        color = pal.teal,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        item {
            // 待领分红
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(pal.goldDim)
                    .border(1.dp, pal.gold, AppShape.Card)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcons.Icon(kind = "coin", color = pal.gold, size = 26.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("待领取分红", color = pal.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "持仓每秒持续累计，分红倍率 ×${String.format(java.util.Locale.CHINA, "%.2f", state.dividendMult)}",
                        color = pal.textDim,
                        fontSize = 10.sp,
                    )
                }
                AnimatedNumber(
                    value = state.pendingDividends,
                    format = { Fmt.money(it) },
                    color = pal.gold,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onCollectDividends,
                    enabled = state.pendingDividends >= 0.01,
                    colors = ButtonDefaults.buttonColors(containerColor = pal.gold, contentColor = Color(0xFF2B1D00)),
                    shape = AppShape.Button,
                    modifier = Modifier.height(36.dp),
                ) {
                    Text("收取", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 委托单
        if (state.orders.isNotEmpty()) {
            item {
                Row(
                    Modifier.padding(top = 4.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcons.Icon(kind = "order", color = pal.teal, size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "限价委托（${state.orders.size}）",
                        color = pal.textDim,
                        fontSize = 12.sp,
                    )
                }
            }
            items(state.orders, key = { "order_" + it.id }) { o ->
                OrderRow(o, onCancel = { onCancelOrder(o.id) })
            }
        }

        item {
            Text(
                "持仓列表（${positions.size}）",
                color = pal.textDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }

        if (positions.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(pal.surface)
                        .border(1.dp, pal.border, AppShape.Card)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppIcons.Icon(kind = "tray", color = pal.textDim, size = 34.dp)
                    Text(
                        "还没有持仓\n去行情页挑一只蓝筹开始吧",
                        color = pal.textDim,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        items(positions, key = { it.def.id }) { s ->
            PositionRow(s, onClick = { onOpenDetail(s.def.id) })
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(pal.surface)
                    .border(1.dp, pal.border, AppShape.Card)
                    .padding(14.dp),
            ) {
                StatRow("累计交易次数", state.totalTrades.toString())
                StatRow("累计已实现盈亏", Fmt.money(state.realizedPnL))
                StatRow("历史最高总资产", Fmt.money(state.maxTotalAssets))
                StatRow("成就进度", "${state.achievementUnlockedCount} 项达成")
                StatRow("经理等级", "Lv." + state.managerLevel)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OrderRow(o: OrderUi, onCancel: () -> Unit) {
    val pal = LocalPal.current
    val diffPct = if (o.currentPrice > 1e-9) (o.limitPrice / o.currentPrice - 1.0) * 100.0 else 0.0
    Row(
        Modifier
            .fillMaxWidth()
            .background(pal.surface)
            .border(1.dp, pal.border, AppShape.Card)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .background((if (o.buy) pal.up else pal.down).copy(alpha = 0.12f), AppShape.Mini)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                if (o.buy) "买入" else "卖出",
                color = if (o.buy) pal.up else pal.down,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(o.name, color = pal.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "委托价 " + String.format(java.util.Locale.CHINA, "%.2f", o.limitPrice) +
                    "（现价偏离 " + Fmt.pct(diffPct) + "）" +
                    if (o.buy) " · 冻结 " + Fmt.money(o.frozenCash) else " · 冻结 " + Fmt.qty(o.frozenShares) + " 股",
                color = pal.textDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        TextButton(
            onClick = onCancel,
            colors = ButtonDefaults.textButtonColors(contentColor = pal.textDim),
        ) {
            Text("撤单", fontSize = 12.sp)
        }
    }
}

@Composable
private fun PositionRow(s: StockUi, onClick: () -> Unit) {
    val pal = LocalPal.current
    val changeColor = if (s.sessionChangePct > 1e-9) pal.up
    else if (s.sessionChangePct < -1e-9) pal.down
    else pal.textDim
    Row(
        Modifier
            .fillMaxWidth()
            .background(pal.surface)
            .border(1.dp, pal.border, AppShape.Card)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.2f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.def.name, color = pal.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (s.limitState != 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (s.limitState > 0) "涨停" else "跌停",
                        color = if (s.limitState > 0) pal.up else pal.down,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                Fmt.qty(s.shares) + " 股 · 成本 " + Fmt.price(s.avgCost),
                color = pal.textDim,
                fontSize = 10.sp,
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text("市值 " + Fmt.money(s.positionValue), color = pal.text, fontSize = 12.sp)
            AnimatedNumber(
                value = s.pnl,
                format = { (if (it >= 0) "+" else "") + Fmt.money(it) },
                color = if (s.pnl >= 0) pal.up else pal.down,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            Fmt.pct(s.sessionChangePct),
            color = changeColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val pal = LocalPal.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(label, color = pal.textDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = pal.text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
