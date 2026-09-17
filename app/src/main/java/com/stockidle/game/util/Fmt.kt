package com.stockidle.game.util

import java.util.Locale
import kotlin.math.abs

/**
 * 数字格式化工具。
 * 中文习惯单位：万（1e4）、亿（1e8）、万亿（1e12）。
 * 所有金额展示统一走这里，保证全局风格一致。
 */
object Fmt {

    private fun core(x: Double): String = when {
        x >= 100.0 -> String.format(Locale.CHINA, "%.1f", x)
        else -> String.format(Locale.CHINA, "%.2f", x)
    }

    /** ¥1.23万 / ¥4567.89 / -¥3.20亿 */
    fun money(v: Double): String {
        val sign = if (v < 0) "-" else ""
        val a = abs(v)
        val body = when {
            a >= 1e12 -> core(a / 1e12) + "万亿"
            a >= 1e8 -> core(a / 1e8) + "亿"
            a >= 1e4 -> core(a / 1e4) + "万"
            else -> core(a)
        }
        return sign + "¥" + body
    }

    /** 不带货币符号，用于紧凑位置（行情条等） */
    fun price(v: Double): String = when {
        v >= 1e4 -> core(v / 1e4) + "万"
        v >= 1000.0 -> String.format(Locale.CHINA, "%.1f", v)
        else -> String.format(Locale.CHINA, "%.2f", v)
    }

    /** 带符号百分比：+12.34% / -3.05% */
    fun pct(v: Double): String {
        val sign = if (v > 0) "+" else ""
        return sign + String.format(Locale.CHINA, "%.2f", v) + "%"
    }

    /** 数量：小数两位以内，大数量走紧凑单位 */
    fun qty(v: Double): String = when {
        v >= 1e8 -> core(v / 1e8) + "亿"
        v >= 1e4 -> core(v / 1e4) + "万"
        else -> String.format(Locale.CHINA, "%.2f", v)
    }

    /** 时长：x天x小时 / x小时x分钟 / x分x秒 */
    fun duration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val d = s / 86400
        val h = (s % 86400) / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return when {
            d > 0 -> "${d}天${h}小时"
            h > 0 -> "${h}小时${m}分钟"
            m > 0 -> "${m}分${sec}秒"
            else -> "${sec}秒"
        }
    }

    /** mm:ss 倒计时 */
    fun clock(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        return String.format(Locale.CHINA, "%02d:%02d", s / 60, s % 60)
    }
}
