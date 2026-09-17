package com.stockidle.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * v0.2 图标系统：全应用禁用 emoji，统一使用自绘 24dp 线条图标。
 * 线宽 1.8dp，直角转折 + 少量斜切，匹配硬朗金融终端风。
 */
object AppIcons {

    /** 通用包装：固定尺寸 + 居中 */
    @Composable
    fun Icon(
        kind: String,
        modifier: Modifier = Modifier,
        size: Dp = 20.dp,
        color: Color,
        filled: Boolean = false,
    ) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val s = this.size.minDimension / 24f   // 归一化到 24 坐标
                val w = 1.8f * s
                drawInto(kind, color, w, filled, s)
            }
        }
    }
}

private fun DrawScope.drawInto(kind: String, color: Color, w: Float, filled: Boolean, s: Float) {
    when (kind) {
        // 行情：折线上扬
        "chart" -> {
            val p = Path().apply {
                moveTo(2f * s, 19f * s); lineTo(8f * s, 12f * s)
                lineTo(13f * s, 16f * s); lineTo(22f * s, 4f * s)
            }
            drawPath(p, color, style = Stroke(w))
            drawLine(color, Offset(21f * s, 4f * s), Offset(15.5f * s, 4f * s), w)
            drawLine(color, Offset(21f * s, 4f * s), Offset(21f * s, 9.5f * s), w)
        }
        // 资产：钱包
        "wallet" -> {
            drawRect(color, topLeft = Offset(2.5f * s, 6f * s), size = androidx.compose.ui.geometry.Size(19f * s, 14f * s), style = Stroke(w))
            drawLine(color, Offset(2.5f * s, 10f * s), Offset(21.5f * s, 10f * s), w)
            if (filled) {
                drawRect(color, topLeft = Offset(15f * s, 12.5f * s), size = androidx.compose.ui.geometry.Size(4.5f * s, 3.5f * s))
            } else {
                drawRect(color, topLeft = Offset(15f * s, 12.5f * s), size = androidx.compose.ui.geometry.Size(4.5f * s, 3.5f * s), style = Stroke(w * 0.8f))
            }
        }
        // 成长：三段柱状上升 + 箭头
        "growth" -> {
            drawLine(color, Offset(4f * s, 21f * s), Offset(4f * s, 14f * s), w)
            drawLine(color, Offset(11f * s, 21f * s), Offset(11f * s, 9f * s), w)
            drawLine(color, Offset(18f * s, 21f * s), Offset(18f * s, 13f * s), w)
            drawLine(color, Offset(2f * s, 21f * s), Offset(22f * s, 21f * s), w)
            val p = Path().apply { moveTo(5f * s, 9f * s); lineTo(11f * s, 4f * s); lineTo(15f * s, 7f * s); lineTo(21f * s, 2.5f * s) }
            drawPath(p, color, style = Stroke(w * 0.9f))
        }
        // 设置：齿轮
        "gear" -> {
            val cx = 12f * s; val cy = 12f * s
            val rOut = 8.2f * s; val rIn = 6.4f * s
            val p = Path()
            for (i in 0 until 8) {
                val a0 = Math.toRadians((i * 45 - 11.25))
                val a1 = Math.toRadians((i * 45 + 11.25))
                val b0 = Math.toRadians((i * 45 + 22.5 - 11.25))
                val b1 = Math.toRadians((i * 45 + 22.5 + 11.25))
                if (i == 0) p.moveTo((cx + rOut * cos(a0)).toFloat(), (cy + rOut * sin(a0)).toFloat())
                p.lineTo((cx + rOut * cos(a1)).toFloat(), (cy + rOut * sin(a1)).toFloat())
                p.lineTo((cx + rIn * cos(b0)).toFloat(), (cy + rIn * sin(b0)).toFloat())
                p.lineTo((cx + rIn * cos(b1)).toFloat(), (cy + rIn * sin(b1)).toFloat())
                p.lineTo((cx + rOut * cos(b0 + Math.toRadians(0.0))).toFloat(), (cy + rOut * sin(b0)).toFloat())
            }
            p.close()
            drawPath(p, color, style = Stroke(w * 0.9f))
            drawCircle(color, radius = 3f * s, center = Offset(cx, cy), style = Stroke(w * 0.9f))
        }
        // 星（自选）
        "star" -> star(color, w, s, filled, 12f * s, 12f * s, 9.2f * s)
        // 锁（未解锁）
        "lock" -> {
            val p = Path().apply {
                moveTo(7f * s, 11f * s); lineTo(7f * s, 8f * s)
                arcTo(androidx.compose.ui.geometry.Rect(7f * s, 3f * s, 17f * s, 13f * s), 180f, 180f, false)
                lineTo(17f * s, 11f * s)
            }
            drawPath(p, color, style = Stroke(w * 0.9f))
            drawRect(color, topLeft = Offset(5f * s, 11f * s), size = androidx.compose.ui.geometry.Size(14f * s, 10f * s), style = Stroke(w))
        }
        // 破锁（解锁特效）
        "unlock" -> {
            val p = Path().apply {
                moveTo(7f * s, 11f * s); lineTo(7f * s, 7.5f * s)
                arcTo(androidx.compose.ui.geometry.Rect(7f * s, 2.5f * s, 17f * s, 12.5f * s), 180f, 140f, false)
            }
            drawPath(p, color, style = Stroke(w * 0.9f))
            drawRect(color, topLeft = Offset(5f * s, 11f * s), size = androidx.compose.ui.geometry.Size(14f * s, 10f * s), style = Stroke(w))
        }
        // 闪电（事件）
        "bolt" -> {
            val p = Path().apply {
                moveTo(13f * s, 2f * s); lineTo(5f * s, 13.5f * s); lineTo(11f * s, 13.5f * s)
                lineTo(9.5f * s, 22f * s); lineTo(19f * s, 9.5f * s); lineTo(12.5f * s, 9.5f * s); close()
            }
            if (filled) drawPath(p, color) else drawPath(p, color, style = Stroke(w * 0.9f))
        }
        // 时钟
        "clock" -> {
            drawCircle(color, radius = 9f * s, center = Offset(12f * s, 12f * s), style = Stroke(w))
            drawLine(color, Offset(12f * s, 12f * s), Offset(12f * s, 6.5f * s), w * 0.9f)
            drawLine(color, Offset(12f * s, 12f * s), Offset(16.5f * s, 14f * s), w * 0.9f)
        }
        // 金币
        "coin" -> {
            drawCircle(color, radius = 9f * s, center = Offset(12f * s, 12f * s), style = Stroke(w))
            drawCircle(color, radius = 6f * s, center = Offset(12f * s, 12f * s), style = Stroke(w * 0.7f))
            drawLine(color, Offset(12f * s, 8f * s), Offset(12f * s, 16f * s), w * 0.8f)
            drawLine(color, Offset(9.8f * s, 10f * s), Offset(14.2f * s, 10f * s), w * 0.8f)
            drawLine(color, Offset(9.8f * s, 14f * s), Offset(14.2f * s, 14f * s), w * 0.8f)
        }
        // 空托盘（无持仓）
        "tray" -> {
            val p = Path().apply {
                moveTo(3f * s, 13f * s); lineTo(7.5f * s, 13f * s); lineTo(9.5f * s, 16f * s)
                lineTo(14.5f * s, 16f * s); lineTo(16.5f * s, 13f * s); lineTo(21f * s, 13f * s)
                lineTo(19f * s, 20f * s); lineTo(5f * s, 20f * s); close()
            }
            drawPath(p, color, style = Stroke(w))
            drawLine(color, Offset(8f * s, 9f * s), Offset(16f * s, 9f * s), w * 0.8f)
        }
        // 雷达（内幕）
        "radar" -> {
            drawCircle(color, radius = 9f * s, center = Offset(12f * s, 12f * s), style = Stroke(w * 0.8f))
            drawCircle(color, radius = 5f * s, center = Offset(12f * s, 12f * s), style = Stroke(w * 0.7f))
            drawLine(color, Offset(12f * s, 12f * s), Offset(18.5f * s, 5.5f * s), w)
            drawCircle(color, radius = 1.6f * s, center = Offset(18.5f * s, 5.5f * s))
        }
        // 机器人
        "bot" -> {
            drawRect(color, topLeft = Offset(5f * s, 8f * s), size = androidx.compose.ui.geometry.Size(14f * s, 11f * s), style = Stroke(w))
            drawCircle(color, radius = 1.7f * s, center = Offset(9.5f * s, 13f * s))
            drawCircle(color, radius = 1.7f * s, center = Offset(14.5f * s, 13f * s))
            drawLine(color, Offset(12f * s, 8f * s), Offset(12f * s, 4.5f * s), w * 0.8f)
            drawCircle(color, radius = 1.3f * s, center = Offset(12f * s, 3.5f * s))
            drawLine(color, Offset(8f * s, 16.5f * s), Offset(16f * s, 16.5f * s), w * 0.7f)
        }
        // 盾牌（保险）
        "shield" -> {
            val p = Path().apply {
                moveTo(12f * s, 2.5f * s); lineTo(20f * s, 5.5f * s); lineTo(20f * s, 12f * s)
                quadraticBezierTo(20f * s, 18f * s, 12f * s, 21.5f * s)
                quadraticBezierTo(4f * s, 18f * s, 4f * s, 12f * s)
                lineTo(4f * s, 5.5f * s); close()
            }
            drawPath(p, color, style = Stroke(w))
            drawLine(color, Offset(8.5f * s, 11.5f * s), Offset(11f * s, 14.5f * s), w)
            drawLine(color, Offset(11f * s, 14.5f * s), Offset(15.5f * s, 8.5f * s), w)
        }
        // 报纸（资讯）
        "news" -> {
            drawRect(color, topLeft = Offset(3.5f * s, 5f * s), size = androidx.compose.ui.geometry.Size(14f * s, 15f * s), style = Stroke(w))
            drawLine(color, Offset(17.5f * s, 9f * s), Offset(20.5f * s, 9f * s), w * 0.8f)
            drawLine(color, Offset(17.5f * s, 13f * s), Offset(20.5f * s, 13f * s), w * 0.8f)
            drawLine(color, Offset(17.5f * s, 17f * s), Offset(20.5f * s, 17f * s), w * 0.8f)
            drawLine(color, Offset(6.5f * s, 9f * s), Offset(14.5f * s, 9f * s), w * 0.7f)
            drawLine(color, Offset(6.5f * s, 12.5f * s), Offset(14.5f * s, 12.5f * s), w * 0.7f)
            drawLine(color, Offset(6.5f * s, 16f * s), Offset(11f * s, 16f * s), w * 0.7f)
        }
        // 循环箭头（复投/转生）
        "cycle" -> {
            val p1 = Path().apply {
                arcTo(androidx.compose.ui.geometry.Rect(4f * s, 4f * s, 20f * s, 20f * s), 200f, 250f, false)
            }
            drawPath(p1, color, style = Stroke(w))
            val p2 = Path().apply {
                arcTo(androidx.compose.ui.geometry.Rect(4f * s, 4f * s, 20f * s, 20f * s), 20f, 250f, false)
            }
            drawPath(p2, color, style = Stroke(w))
            drawLine(color, Offset(5.2f * s, 5.2f * s), Offset(8.8f * s, 5.4f * s), w)
            drawLine(color, Offset(5.2f * s, 5.2f * s), Offset(5.6f * s, 8.9f * s), w)
            drawLine(color, Offset(18.8f * s, 18.8f * s), Offset(15.2f * s, 18.6f * s), w)
            drawLine(color, Offset(18.8f * s, 18.8f * s), Offset(18.4f * s, 15.1f * s), w)
        }
        // 奖杯（成就）
        "trophy" -> {
            drawPath(
                Path().apply {
                    moveTo(7f * s, 3.5f * s); lineTo(17f * s, 3.5f * s); lineTo(17f * s, 10f * s)
                    quadraticBezierTo(17f * s, 15f * s, 12f * s, 15f * s)
                    quadraticBezierTo(7f * s, 15f * s, 7f * s, 10f * s); close()
                },
                color, style = Stroke(w),
            )
            drawLine(color, Offset(7f * s, 5.5f * s), Offset(3.5f * s, 5.5f * s), w * 0.8f)
            drawLine(color, Offset(3.5f * s, 5.5f * s), Offset(3.5f * s, 8.5f * s), w * 0.8f)
            drawLine(color, Offset(17f * s, 5.5f * s), Offset(20.5f * s, 5.5f * s), w * 0.8f)
            drawLine(color, Offset(20.5f * s, 5.5f * s), Offset(20.5f * s, 8.5f * s), w * 0.8f)
            drawLine(color, Offset(12f * s, 15f * s), Offset(12f * s, 18f * s), w)
            drawLine(color, Offset(8f * s, 20.5f * s), Offset(16f * s, 20.5f * s), w)
        }
        // 返回箭头
        "back" -> {
            drawLine(color, Offset(14f * s, 4f * s), Offset(6f * s, 12f * s), w)
            drawLine(color, Offset(6f * s, 12f * s), Offset(14f * s, 20f * s), w)
        }
        // 关闭
        "close" -> {
            drawLine(color, Offset(5f * s, 5f * s), Offset(19f * s, 19f * s), w)
            drawLine(color, Offset(19f * s, 5f * s), Offset(5f * s, 19f * s), w)
        }
        // 加号
        "plus" -> {
            drawLine(color, Offset(12f * s, 4.5f * s), Offset(12f * s, 19.5f * s), w)
            drawLine(color, Offset(4.5f * s, 12f * s), Offset(19.5f * s, 12f * s), w)
        }
        // 减号
        "minus" -> drawLine(color, Offset(4.5f * s, 12f * s), Offset(19.5f * s, 12f * s), w)
        // 书（教程）
        "book" -> {
            drawPath(
                Path().apply {
                    moveTo(12f * s, 5f * s)
                    quadraticBezierTo(8f * s, 2.8f * s, 3.5f * s, 4f * s)
                    lineTo(3.5f * s, 18.5f * s)
                    quadraticBezierTo(8f * s, 17.3f * s, 12f * s, 19.5f * s)
                    quadraticBezierTo(16f * s, 17.3f * s, 20.5f * s, 18.5f * s)
                    lineTo(20.5f * s, 4f * s)
                    quadraticBezierTo(16f * s, 2.8f * s, 12f * s, 5f * s)
                    close()
                },
                color, style = Stroke(w),
            )
            drawLine(color, Offset(12f * s, 5f * s), Offset(12f * s, 19.5f * s), w * 0.8f)
        }
        // 委托单（列表+勾）
        "order" -> {
            drawRect(color, topLeft = Offset(4f * s, 3f * s), size = androidx.compose.ui.geometry.Size(16f * s, 18f * s), style = Stroke(w))
            drawLine(color, Offset(7.5f * s, 8f * s), Offset(16.5f * s, 8f * s), w * 0.7f)
            drawLine(color, Offset(7.5f * s, 12f * s), Offset(16.5f * s, 12f * s), w * 0.7f)
            drawLine(color, Offset(7.5f * s, 16f * s), Offset(12f * s, 16f * s), w * 0.7f)
        }
        // 烛台图（K线）
        "candle" -> {
            // 左蜡烛
            drawLine(color, Offset(7f * s, 3f * s), Offset(7f * s, 21f * s), w * 0.7f)
            drawRect(color, topLeft = Offset(4.8f * s, 8f * s), size = androidx.compose.ui.geometry.Size(4.4f * s, 7f * s), style = Stroke(w * 0.9f))
            // 右蜡烛
            drawLine(color, Offset(16f * s, 5f * s), Offset(16f * s, 19f * s), w * 0.7f)
            drawRect(color, topLeft = Offset(13.8f * s, 10f * s), size = androidx.compose.ui.geometry.Size(4.4f * s, 6f * s), style = Stroke(w * 0.9f))
        }
        // 分时（曲线）
        "timeline" -> {
            val p = Path().apply {
                moveTo(2.5f * s, 18f * s)
                quadraticBezierTo(7f * s, 8f * s, 12f * s, 12f * s)
                quadraticBezierTo(16f * s, 15f * s, 21.5f * s, 5f * s)
            }
            drawPath(p, color, style = Stroke(w))
        }
        // 箭头向下（撤单/展开）
        "chevronDown" -> {
            drawLine(color, Offset(5f * s, 9f * s), Offset(12f * s, 16f * s), w)
            drawLine(color, Offset(12f * s, 16f * s), Offset(19f * s, 9f * s), w)
        }
    }
}

private fun DrawScope.star(color: Color, w: Float, s: Float, filled: Boolean, cx: Float, cy: Float, r: Float) {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.42f
        val a = Math.toRadians(-90.0 + i * 36.0)
        val x = cx + rr * cos(a).toFloat()
        val y = cy + rr * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    if (filled) drawPath(p, color) else drawPath(p, color, style = Stroke(w * 0.9f))
}
