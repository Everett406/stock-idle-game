package com.stockidle.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockidle.game.ui.theme.LocalPal
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * v0.2 专业图表：
 *  - 分时模式：折线 + 渐变 + 均值回归基准虚线 + 末端呼吸点
 *  - K线模式：15 秒/根蜡烛（由秒级分时聚合），红涨绿跌实心/空心
 *  - 共有：4×8 网格、右侧价格刻度、底部成交量柱、按住显示十字线与读数
 */
object ProChart {

    const val MODE_LINE = 0
    const val MODE_CANDLE = 1

    /** 由秒级价格序列聚合 15s 蜡烛：List of (open, high, low, close, volume) */
    fun aggregateCandles(prices: List<Double>, volumes: List<Double>, perCandle: Int = 15): List<List<Double>> {
        if (prices.size < perCandle) return emptyList()
        val out = mutableListOf<List<Double>>()
        val start = prices.size % perCandle   // 丢掉不完整的头
        var i = start
        while (i + perCandle <= prices.size) {
            var hi = -Double.MAX_VALUE
            var lo = Double.MAX_VALUE
            var vol = 0.0
            for (k in i until i + perCandle) {
                hi = max(hi, prices[k]); lo = min(lo, prices[k])
                vol += volumes.getOrElse(k) { 0.0 }
            }
            out.add(listOf(prices[i], hi, lo, prices[i + perCandle - 1], vol))
            i += perCandle
        }
        return out
    }
}

@Composable
fun ProChart(
    prices: List<Double>,
    volumes: List<Double>,
    mode: Int,
    basePrice: Double,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    var touchX by remember { mutableStateOf(Float.NaN) }

    val candles = remember(prices, mode) {
        if (mode == ProChart.MODE_CANDLE) ProChart.aggregateCandles(prices, volumes) else emptyList()
    }

    Canvas(
        modifier
            .pointerInput(prices.size, mode) {
                detectDragGestures(
                    onDragStart = { touchX = it.x },
                    onDragEnd = { touchX = Float.NaN },
                    onDragCancel = { touchX = Float.NaN },
                ) { change, _ ->
                    touchX = change.position.x
                }
            },
    ) {
        if (mode == ProChart.MODE_CANDLE && candles.size >= 2) {
            drawCandles(candles, pal, basePrice)
        } else if (mode == ProChart.MODE_LINE && prices.size >= 2) {
            drawLineMode(prices, volumes, pal, basePrice)
        } else {
            drawEmpty(pal)
        }

        // 十字线读数
        if (!touchX.isNaN() && touchX >= 0 && prices.size >= 2) {
            drawCrosshair(touchX, prices, volumes, mode, candles, pal)
        }
    }
}

// ---------------------------------------------------------------- 分时
private fun DrawScope.drawLineMode(prices: List<Double>, volumes: List<Double>, pal: com.stockidle.game.ui.theme.Pal, basePrice: Double) {
    var lo = Double.MAX_VALUE
    var hi = -Double.MAX_VALUE
    for (d in prices) { if (d < lo) lo = d; if (d > hi) hi = d }
    lo = min(lo, basePrice); hi = max(hi, basePrice)
    val range = if (hi - lo > 1e-9) hi - lo else 1.0

    val priceH = size.height * 0.74f
    val volTop = size.height * 0.80f
    val padL = 0f
    val chartW = size.width
    val n = prices.size
    val stepX = chartW / max(1, n - 1)

    fun yOf(v: Double): Float = (priceH * (1.0 - (v - lo) / range)).toFloat()

    drawGrid(pal, priceH, chartW)

    // 基准虚线（均值回归锚定价）
    if (basePrice in lo..hi) {
        drawDashedLine(
            color = pal.textDim,
            startX = 0f, y = yOf(basePrice) + 12f, stopX = chartW,
        )
    }

    // 分时线颜色按整体涨跌（相对基准价）
    val lineColor = if (prices.last() >= basePrice) pal.up else pal.down

    // 成交量柱
    val vMax = volumes.maxOrNull() ?: 1.0
    for (i in prices.indices) {
        val v = volumes.getOrElse(i) { 0.0 }
        if (v <= 0) continue
        val bh = ((size.height - volTop) * (v / max(1.0, vMax))).toFloat()
        val up = i == 0 || prices[i] >= prices[i - 1]
        drawRect(
            color = (if (up) pal.up else pal.down).copy(alpha = 0.55f),
            topLeft = Offset(i * stepX, size.height - bh),
            size = androidx.compose.ui.geometry.Size(max(1.5f, stepX * 0.7f), bh),
        )
    }

    // 价格折线 + 渐变
    val line = Path()
    val fill = Path()
    prices.forEachIndexed { i, v ->
        val x = i * stepX
        val y = yOf(v)
        if (i == 0) { line.moveTo(x, y); fill.moveTo(x, y) }
        else { line.lineTo(x, y); fill.lineTo(x, y) }
    }
    fill.lineTo(chartW, volTop); fill.lineTo(0f, volTop); fill.close()
    drawPath(
        path = fill,
        brush = Brush.verticalGradient(
            listOf(lineColor.copy(alpha = 0.20f), Color.Transparent),
            startY = 0f, endY = volTop,
        ),
    )
    drawPath(path = line, color = lineColor, style = Stroke(width = 3f))

    // 末端点
    val ly = yOf(prices.last())
    drawCircle(lineColor.copy(alpha = 0.30f), radius = 9f, center = Offset(chartW - 2f, ly))
    drawCircle(lineColor, radius = 4.5f, center = Offset(chartW - 2f, ly))
    drawCircle(Color.White.copy(alpha = 0.85f), radius = 2f, center = Offset(chartW - 2f, ly))

    drawPriceAxis(lo, hi, priceH, chartW, pal)
}

// ---------------------------------------------------------------- K线
private fun DrawScope.drawCandles(candles: List<List<Double>>, pal: com.stockidle.game.ui.theme.Pal, basePrice: Double) {
    var lo = Double.MAX_VALUE
    var hi = -Double.MAX_VALUE
    for (c in candles) { lo = min(lo, c[2]); hi = max(hi, c[1]) }
    lo = min(lo, basePrice); hi = max(hi, basePrice)
    val range = if (hi - lo > 1e-9) hi - lo else 1.0

    val priceH = size.height * 0.74f
    val chartW = size.width
    val n = candles.size
    val slot = chartW / n
    val bodyW = max(3f, slot * 0.62f)

    fun yOf(v: Double): Float = (priceH * (1.0 - (v - lo) / range)).toFloat()

    drawGrid(pal, priceH, chartW)

    if (basePrice in lo..hi) {
        drawDashedLine(color = pal.textDim, startX = 0f, y = yOf(basePrice), stopX = chartW)
    }

    candles.forEachIndexed { i, c ->
        val open = c[0]; val close = c[3]
        val up = close >= open
        val color = if (up) pal.up else pal.down
        val cx = i * slot + slot / 2f
        // 影线
        drawLine(color, Offset(cx, yOf(c[1])), Offset(cx, yOf(c[2])), strokeWidth = 2f)
        // 实体
        val top = yOf(max(open, close))
        val bottom = yOf(min(open, close))
        val h = max(2f, bottom - top)
        if (up) {
            drawRect(color, topLeft = Offset(cx - bodyW / 2f, top), size = androidx.compose.ui.geometry.Size(bodyW, h))
        } else {
            drawRect(color, topLeft = Offset(cx - bodyW / 2f, top), size = androidx.compose.ui.geometry.Size(bodyW, h), style = Stroke(2f))
        }
    }

    // 成交量
    val volTop = size.height * 0.80f
    val vMax = candles.maxOf { it[4] }.coerceAtLeast(1.0)
    candles.forEachIndexed { i, c ->
        val bh = ((size.height - volTop) * (c[4] / vMax)).toFloat()
        val color = if (c[3] >= c[0]) pal.up else pal.down
        drawRect(
            color.copy(alpha = 0.55f),
            topLeft = Offset(i * slot + (slot - bodyW) / 2f, size.height - bh),
            size = androidx.compose.ui.geometry.Size(max(3f, bodyW), bh),
        )
    }

    drawPriceAxis(lo, hi, priceH, chartW, pal)
}

// ---------------------------------------------------------------- 公共
private fun DrawScope.drawGrid(pal: com.stockidle.game.ui.theme.Pal, priceH: Float, chartW: Float) {
    val gridColor = if (pal.isDark) Color.White.copy(alpha = 0.055f) else Color.Black.copy(alpha = 0.07f)
    for (i in 0..4) {
        val gy = priceH * i / 4f
        drawLine(gridColor, Offset(0f, gy), Offset(chartW, gy), strokeWidth = 1.5f)
    }
    for (i in 1..7) {
        val gx = chartW * i / 8f
        drawLine(gridColor, Offset(gx, 0f), Offset(gx, priceH), strokeWidth = 1.5f)
    }
}

private fun DrawScope.drawPriceAxis(lo: Double, hi: Double, priceH: Float, chartW: Float, pal: com.stockidle.game.ui.theme.Pal) {
    val textPaint = android.graphics.Paint().apply {
        color = if (pal.isDark) 0xFF8A94A6.toInt() else 0xFF667080.toInt()
        textSize = 9.sp.toPx()
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.LEFT
        typeface = android.graphics.Typeface.MONOSPACE
    }
    val range = hi - lo
    for (i in 0..4) {
        val v = hi - range * i / 4.0
        val y = priceH * i / 4f
        val label = if (v >= 1000) String.format(java.util.Locale.CHINA, "%.0f", v)
                    else String.format(java.util.Locale.CHINA, "%.2f", v)
        drawContext.canvas.nativeCanvas.drawText(label, chartW - 2f, y.coerceAtLeast(9.sp.toPx()), textPaint)
    }
}

private fun DrawScope.drawDashedLine(color: Color, startX: Float, y: Float, stopX: Float) {
    val dash = 8f; val gap = 6f
    var x = startX
    while (x < stopX) {
        drawLine(color, Offset(x, y), Offset(min(x + dash, stopX), y), strokeWidth = 2f)
        x += dash + gap
    }
}

private fun DrawScope.drawEmpty(pal: com.stockidle.game.ui.theme.Pal) {
    drawLine(
        if (pal.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
        Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 2f,
    )
}

private fun DrawScope.drawCrosshair(
    touchX: Float,
    prices: List<Double>,
    volumes: List<Double>,
    mode: Int,
    candles: List<List<Double>>,
    pal: com.stockidle.game.ui.theme.Pal,
) {
    val priceH = size.height * 0.74f
    val n = if (mode == ProChart.MODE_CANDLE && candles.isNotEmpty()) candles.size else prices.size
    if (n < 2) return
    val slot = size.width / n
    val idx = ((touchX / slot).toInt()).coerceIn(0, n - 1)
    val cx = idx * slot + slot / 2f

    drawLine(pal.textDim.copy(alpha = 0.7f), Offset(cx, 0f), Offset(cx, priceH), strokeWidth = 1.5f)

    val value: Double = if (mode == ProChart.MODE_CANDLE && candles.isNotEmpty()) candles[idx][3] else prices.getOrElse(idx) { prices.last() }
    val vol = if (mode == ProChart.MODE_CANDLE && candles.isNotEmpty()) candles[idx][4] else volumes.getOrElse(idx) { 0.0 }

    // 读数气泡
    val text = String.format(java.util.Locale.CHINA, "%.2f  量:%s", value, com.stockidle.game.util.Fmt.qty(vol))
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 10.sp.toPx()
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val tw = paint.measureText(text)
    val pad = 6.dp.toPx()
    val boxW = tw + pad * 2
    val boxH = 15.sp.toPx() + 4.dp.toPx()
    val bx = (cx - boxW / 2f).coerceIn(0f, size.width - boxW)
    drawRect(pal.text, topLeft = Offset(bx, 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(boxW, boxH))
    drawContext.canvas.nativeCanvas.drawText(text, bx + boxW / 2f, 2.dp.toPx() + boxH - 3.dp.toPx(), paint)
}
