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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.stockidle.game.ui.components.LineChart
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt

/**
 * 股票详情：分时折线实时延伸 + 持仓信息 + 买卖面板。
 */
@Composable
fun DetailScreen(
    state: GameState,
    stockId: String,
    onBack: () -> Unit,
    onBuy: (String, Double) -> Unit,
    onSell: (String, Double) -> Unit,
) {
    val pal = LocalPal.current
    val s = state.stocks.firstOrNull { it.def.id == stockId } ?: return
    var fraction by rememberSaveable(stockId) { mutableStateOf(0.5f) }

    val changeColor = if (s.sessionChangePct > 1e-9) pal.up
    else if (s.sessionChangePct < -1e-9) pal.down
    else pal.textDim

    Column(
        Modifier
            .fillMaxSize()
            .background(pal.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // 顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹ 返回",
                color = pal.textDim,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(8.dp),
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.def.name, color = pal.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(s.def.symbol, color = pal.textDim, fontSize = 11.sp)
            }
            Spacer(Modifier.width(56.dp))
        }

        // 价格头部
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            AnimatedNumber(
                value = s.price,
                format = { Fmt.price(it) },
                color = changeColor,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.width(10.dp))
            ChangeText(pct = s.sessionChangePct, style = MaterialTheme.typography.titleMedium)
        }

        // 图表
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(210.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pal.surface)
                .border(1.dp, pal.border, RoundedCornerShape(14.dp))
                .padding(8.dp),
        ) {
            LineChart(
                data = s.history.takeLast(600),
                color = changeColor,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                "分时 · 近12分钟",
                color = pal.textDim,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
            )
        }

        // 指标行
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            InfoCell("日分红率", String.format(java.util.Locale.CHINA, "%.2f", s.def.divDailyPct) + "%", pal.teal)
            InfoCell("波动性", volatilityLabel(s.def.volPerSec), pal.text)
            InfoCell("板块", s.def.sector, pal.text)
            InfoCell("事件", if (s.hasEvent) (if (s.eventGood) "利好中" else "利空中") else "无", if (s.hasEvent) (if (s.eventGood) pal.gold else pal.down) else pal.textDim)
        }

        // 持仓信息
        if (s.shares > 1e-9) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(pal.goldDim)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("持仓 ${Fmt.qty(s.shares)} 股", color = pal.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "成本 ${Fmt.price(s.avgCost)} · 市值 " + Fmt.money(s.positionValue),
                        color = pal.textDim,
                        fontSize = 11.sp,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("浮动盈亏", color = pal.textDim, fontSize = 10.sp)
                    AnimatedNumber(
                        value = s.pnl,
                        format = { Fmt.money(it) },
                        color = if (s.pnl >= 0) pal.up else pal.down,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        } else {
            Text(
                "暂无持仓，买入后可持续收取分红",
                color = pal.textDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        // 买卖面板
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(pal.surface)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("金额比例", color = pal.textDim, fontSize = 12.sp)
                Spacer(Modifier.width(10.dp))
                val options = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { f ->
                        val selected = kotlin.math.abs(f - fraction) < 0.01f
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) pal.gold else pal.surfaceHigh)
                                .clickable { fraction = f }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                if (f >= 0.99f) "全仓" else "${(f * 100).toInt()}%",
                                color = if (selected) Color(0xFF2B1D00) else pal.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val budget = state.cash * fraction
            val buyQty = budget * (1.0 - state.feeRate) / s.price.coerceAtLeast(0.01)
            val sellQty = s.shares * fraction
            Text(
                "预计买入 ${Fmt.qty(buyQty)} 股 · 手续费 ${Fmt.money(budget * state.feeRate)}",
                color = pal.textDim,
                fontSize = 11.sp,
            )
            if (s.shares > 1e-9) {
                Text(
                    "预计卖出 ${Fmt.qty(sellQty)} 股 · 入账约 ${Fmt.money(sellQty * s.price * (1.0 - state.feeRate))}",
                    color = pal.textDim,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onBuy(s.def.id, fraction.toDouble()) },
                    colors = ButtonDefaults.buttonColors(containerColor = pal.up, contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("买入", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onSell(s.def.id, fraction.toDouble()) },
                    colors = ButtonDefaults.buttonColors(containerColor = pal.down, contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = s.shares > 1e-9,
                ) {
                    Text("卖出", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = LocalPal.current.textDim, fontSize = 10.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun volatilityLabel(vol: Double): String = when {
    vol < 0.0008 -> "低"
    vol < 0.0015 -> "中"
    vol < 0.0028 -> "高"
    else -> "极高"
}
