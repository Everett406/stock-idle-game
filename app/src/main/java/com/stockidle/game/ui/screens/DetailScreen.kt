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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockidle.game.game.GameState
import com.stockidle.game.game.StockUi
import com.stockidle.game.ui.components.AnimatedNumber
import com.stockidle.game.ui.components.AppIcons
import com.stockidle.game.ui.components.ChangeText
import com.stockidle.game.ui.components.ProChart
import com.stockidle.game.ui.theme.AppShape
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt
import java.util.Locale
import kotlin.math.abs

/**
 * 股票详情 v0.2：分时/K线双图表 + 五档盘口 + 市价/限价双交易面板。
 */
@Composable
fun DetailScreen(
    state: GameState,
    stockId: String,
    onBack: () -> Unit,
    onBuy: (String, Double) -> Unit,
    onSell: (String, Double) -> Unit,
    onPlaceOrder: (String, Boolean, Double, Double) -> Unit,
) {
    val pal = LocalPal.current
    val s = state.stocks.firstOrNull { it.def.id == stockId } ?: return
    var fraction by rememberSaveable(stockId) { mutableStateOf(0.5f) }
    var chartMode by rememberSaveable(stockId) { mutableIntStateOf(ProChart.MODE_LINE) }
    var tradeMode by rememberSaveable(stockId) { mutableIntStateOf(0) } // 0市价 1限价
    var limitPct by rememberSaveable(stockId) { mutableStateOf(-1f) }   // 相对现价的挂单偏差 %
    var limitInput by rememberSaveable(stockId) { mutableStateOf("") }  // 手动输入的挂单价

    val changeColor = if (s.sessionChangePct > 1e-9) pal.up
    else if (s.sessionChangePct < -1e-9) pal.down
    else pal.textDim

    // 限价单价格解析：手动输入优先，否则按偏差计算
    val parsedInput = limitInput.replace(",", "").toDoubleOrNull()
    val limitPrice = parsedInput ?: (s.price * (1.0 + limitPct / 100.0))

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
                .background(pal.surface)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcons.Icon(
                kind = "back",
                color = pal.text,
                size = 20.dp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(10.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(s.def.name, color = pal.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    s.def.symbol + " · " + s.def.sector,
                    color = pal.textDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            if (s.limitState != 0) {
                Text(
                    if (s.limitState > 0) "涨停" else "跌停",
                    color = if (s.limitState > 0) pal.up else pal.down,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background((if (s.limitState > 0) pal.up else pal.down).copy(alpha = 0.12f), AppShape.Mini)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
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
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.width(10.dp))
                ChangeText(pct = s.sessionChangePct, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    "日内 " + Fmt.pct(s.dayChangePct),
                    color = changeColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            // 图表：分时 / K线 切换
            Row(
                Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("分时" to ProChart.MODE_LINE, "K线" to ProChart.MODE_CANDLE).forEach { (label, m) ->
                    val selected = chartMode == m
                    Box(
                        Modifier
                            .background(if (selected) pal.gold else pal.surface)
                            .border(1.dp, if (selected) pal.gold else pal.border, AppShape.Card)
                            .clickable { chartMode = m }
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(
                            label,
                            color = if (selected) Color(0xFF2B1D00) else pal.textDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (chartMode == ProChart.MODE_LINE) "近12分钟 · 按住查看" else "15秒/根 · 按住查看",
                    color = pal.textDim,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 4.dp),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .height(240.dp)
                    .background(pal.surface)
                    .border(1.dp, pal.border, AppShape.Card)
                    .padding(6.dp),
            ) {
                ProChart(
                    prices = s.history.takeLast(720),
                    volumes = s.volumes.takeLast(720),
                    mode = chartMode,
                    basePrice = s.def.basePrice,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // 五档盘口 + 关键指标
            OrderBook(s, state)

            // 指标行
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InfoCell("日分红率", String.format(Locale.CHINA, "%.2f", s.def.divDailyPct) + "%", pal.teal)
                InfoCell("波动性", volatilityLabel(s.def.volPerSec), pal.text)
                InfoCell("涨跌停", Fmt.price(s.limitDown) + "~" + Fmt.price(s.limitUp), pal.textDim)
                InfoCell("事件", if (s.hasEvent) (if (s.eventGood) "利好中" else "利空中") else "无", if (s.hasEvent) (if (s.eventGood) pal.gold else pal.down) else pal.textDim)
            }

            // 持仓信息
            if (s.shares > 1e-9) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .background(pal.surface)
                        .border(1.dp, pal.gold, AppShape.Card)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("持仓 ${Fmt.qty(s.shares)} 股", color = pal.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "成本 ${Fmt.price(s.avgCost)} · 市值 " + Fmt.money(s.positionValue) +
                                (if (s.frozenShares > 1e-9) " · 委托冻结 ${Fmt.qty(s.frozenShares)} 股" else ""),
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
            }
            Spacer(Modifier.height(8.dp))
        }

        // 买卖面板（市价/限价）
        TradePanel(
            state = state,
            s = s,
            tradeMode = tradeMode,
            onTradeMode = { tradeMode = it },
            fraction = fraction,
            onFraction = { fraction = it },
            limitPrice = limitPrice,
            limitInput = limitInput,
            hasManualInput = parsedInput != null,
            onLimitInput = { limitInput = it },
            onLimitPct = { limitPct = it; limitInput = "" },
            limitPct = limitPct,
            onBuy = onBuy,
            onSell = onSell,
            onPlaceOrder = onPlaceOrder,
        )
    }
}

// ---------------------------------------------------------------- 盘口
@Composable
private fun OrderBook(s: StockUi, state: GameState) {
    val pal = LocalPal.current
    // 用成交量做活跃度，档位价围绕现价生成，量做确定性伪随机
    val activeVol = s.volumes.lastOrNull()?.takeIf { it > 0 } ?: 42_000.0
    val recentVol = s.volumes.takeLast(60).filter { it > 0 }.maxOrNull() ?: activeVol
    val heat = (activeVol / recentVol.coerceAtLeast(1.0)).coerceIn(0.3, 2.0)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .background(pal.surface)
            .border(1.dp, pal.border, AppShape.Card)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("五档盘口", color = pal.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("价格 / 委托量（模拟）", color = pal.textDim, fontSize = 9.sp)
        }
        Spacer(Modifier.height(4.dp))
        // 卖 5 → 卖 1
        for (lvl in 5 downTo 1) {
            val p = s.price * (1.0 + 0.004 * lvl + lvl * 0.0004)
            OrderBookRow(
                label = "卖$lvl", price = p, amount = bookAmount(s.def.id, lvl, heat),
                color = pal.down, priceWidth = 0.18f, pal = pal,
            )
        }
        // 现价行
        Row(
            Modifier
                .fillMaxWidth()
                .background(pal.surfaceHigh)
                .padding(vertical = 2.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("现价", color = pal.textDim, fontSize = 10.sp, modifier = Modifier.width(52.dp))
            Text(
                Fmt.price(s.price),
                color = if (s.sessionChangePct >= 0) pal.up else pal.down,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "量 " + Fmt.qty(activeVol),
                color = pal.textDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        for (lvl in 1..5) {
            val p = s.price * (1.0 - 0.004 * lvl - lvl * 0.0004)
            OrderBookRow(
                label = "买$lvl", price = p, amount = bookAmount(s.def.id, -lvl, heat),
                color = pal.up, priceWidth = 0.18f, pal = pal,
            )
        }
    }
}

@Composable
private fun OrderBookRow(label: String, price: Double, amount: Double, color: Color, priceWidth: Float, pal: com.stockidle.game.ui.theme.Pal) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = pal.textDim, fontSize = 10.sp, modifier = Modifier.width(34.dp))
        Text(
            String.format(Locale.CHINA, "%.2f", price),
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(64.dp),
        )
        // 量条
        val frac = ((amount % 90000.0) / 90000.0).toFloat().coerceIn(0.08f, 1f)
        Box(Modifier.weight(1f).height(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(8.dp)
                    .background(color.copy(alpha = 0.22f)),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            Fmt.qty(amount),
            color = pal.textDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** 确定性伪随机量：同一档位稳定，不同档位/股票不同 */
private fun bookAmount(stockId: String, level: Int, heat: Double): Double {
    var h = 7L
    for (c in stockId) h = h * 31 + c.code
    h = h * 31 + level * 97
    val r = kotlin.math.abs(h % 1000) / 1000.0
    return (18_000.0 + r * 66_000.0) * heat
}

// ---------------------------------------------------------------- 交易面板
@Composable
private fun TradePanel(
    state: GameState,
    s: StockUi,
    tradeMode: Int,
    onTradeMode: (Int) -> Unit,
    fraction: Float,
    onFraction: (Float) -> Unit,
    limitPrice: Double,
    limitInput: String,
    hasManualInput: Boolean,
    onLimitInput: (String) -> Unit,
    onLimitPct: (Float) -> Unit,
    limitPct: Float,
    onBuy: (String, Double) -> Unit,
    onSell: (String, Double) -> Unit,
    onPlaceOrder: (String, Boolean, Double, Double) -> Unit,
) {
    val pal = LocalPal.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(pal.surface)
            .border(1.dp, pal.border, AppShape.Card)
            .padding(12.dp),
    ) {
        // 模式切换
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("市价交易" to 0, "限价委托" to 1).forEach { (label, m) ->
                val selected = tradeMode == m
                Box(
                    Modifier
                        .background(if (selected) pal.text else pal.surfaceHigh)
                        .clickable { onTradeMode(m) }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                ) {
                    Text(
                        label,
                        color = if (selected) pal.surface else pal.textDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "手续费 " + String.format(Locale.CHINA, "%.3f%%", state.feeRate * 100),
                color = pal.textDim,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Spacer(Modifier.height(8.dp))

        // 比例选择
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("金额比例", color = pal.textDim, fontSize = 11.sp)
            Spacer(Modifier.width(8.dp))
            val options = listOf(0.25f, 0.5f, 0.75f, 1.0f)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { f ->
                    val selected = abs(f - fraction) < 0.01f
                    Box(
                        Modifier
                            .background(if (selected) pal.gold else pal.surfaceHigh)
                            .border(1.dp, if (selected) pal.gold else pal.border, AppShape.Card)
                            .clickable { onFraction(f) }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
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

        if (tradeMode == 1) {
            Spacer(Modifier.height(8.dp))
            // 限价：手动输入 + 快捷偏差
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("委托价", color = pal.textDim, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { v -> onLimitInput(v.filter { it.isDigit() || it == '.' }) },
                    placeholder = { Text(String.format(Locale.CHINA, "%.2f", limitPrice), fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = AppShape.Card,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = pal.gold,
                        unfocusedBorderColor = pal.border,
                        focusedTextColor = pal.text,
                        unfocusedTextColor = pal.text,
                        cursorColor = pal.gold,
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = pal.text,
                    ),
                    modifier = Modifier.width(120.dp).height(52.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "现价 " + Fmt.price(s.price) +
                        (if (hasManualInput) " (" + Fmt.pct((limitPrice / s.price - 1.0) * 100.0) + ")" else ""),
                    color = pal.textDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(-3f, -2f, -1f, 1f, 2f, 3f).forEach { pct ->
                    val selected = limitInput.isEmpty() && abs(limitPct - pct) < 0.01f
                    Box(
                        Modifier
                            .background(if (selected) pal.gold else pal.surfaceHigh)
                            .border(1.dp, pal.border, AppShape.Card)
                            .clickable { onLimitPct(pct) }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Text(
                            (if (pct > 0) "+" else "") + pct.toInt() + "%",
                            color = if (selected) Color(0xFF2B1D00) else pal.textDim,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "委托冻结资金/持仓，成交或撤销后释放；委托长期有效。",
                color = pal.textDim,
                fontSize = 9.sp,
            )
        } else {
            Spacer(Modifier.height(8.dp))
            val budget = state.cash * fraction
            val buyQty = budget * (1.0 - state.feeRate) / s.price.coerceAtLeast(0.01)
            val sellQty = s.tradableShares * fraction
            Text(
                "预计买入 ${Fmt.qty(buyQty)} 股 · 费 ${Fmt.money(budget * state.feeRate)}" +
                    (if (s.shares > 1e-9) " ｜ 卖出 ${Fmt.qty(sellQty)} 股 · 入账约 " + Fmt.money(sellQty * s.price * (1.0 - state.feeRate)) else ""),
                color = pal.textDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (tradeMode == 0) onBuy(s.def.id, fraction.toDouble())
                    else onPlaceOrder(s.def.id, true, limitPrice, fraction.toDouble())
                },
                colors = ButtonDefaults.buttonColors(containerColor = pal.up, contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = AppShape.Button,
            ) {
                Text(if (tradeMode == 0) "买入" else "挂买单", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    if (tradeMode == 0) onSell(s.def.id, fraction.toDouble())
                    else onPlaceOrder(s.def.id, false, limitPrice, fraction.toDouble())
                },
                colors = ButtonDefaults.buttonColors(containerColor = pal.down, contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = AppShape.Button,
                enabled = if (tradeMode == 0) s.shares > 1e-9 else s.tradableShares > 1e-9,
            ) {
                Text(if (tradeMode == 0) "卖出" else "挂卖单", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, valueColor: Color) {
    val pal = LocalPal.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = pal.textDim, fontSize = 10.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun volatilityLabel(vol: Double): String = when {
    vol < 0.0008 -> "低"
    vol < 0.0015 -> "中"
    vol < 0.0028 -> "高"
    else -> "极高"
}
