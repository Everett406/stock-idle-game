package com.stockidle.game.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt

/**
 * 资产页：总资产 + 现金 / 待领分红 / 今日盈亏 + 持仓列表。
 */
@Composable
fun AssetsScreen(
    state: GameState,
    onCollectDividends: () -> Unit,
    onOpenDetail: (String) -> Unit,
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(pal.surface)
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
                        Text("现金", color = pal.textDim, fontSize = 11.sp)
                        AnimatedNumber(value = state.cash, format = { Fmt.money(it) }, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
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
            }
        }
        item {
            // 待领分红
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(pal.goldDim)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(pal.gold.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) { Text("💰", fontSize = 16.sp) }
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    Text("收取", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(pal.surface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("📭", fontSize = 30.sp)
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
                    .clip(RoundedCornerShape(14.dp))
                    .background(pal.surface)
                    .padding(14.dp),
            ) {
                StatRow("累计交易次数", state.totalTrades.toString())
                StatRow("累计已实现盈亏", Fmt.money(state.realizedPnL))
                StatRow("历史最高总资产", Fmt.money(state.maxTotalAssets))
                StatRow("经理等级", "Lv." + state.managerLevel)
            }
            Spacer(Modifier.height(90.dp))
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
            .clip(RoundedCornerShape(12.dp))
            .background(pal.surface)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.2f)) {
            Text(s.def.name, color = pal.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
