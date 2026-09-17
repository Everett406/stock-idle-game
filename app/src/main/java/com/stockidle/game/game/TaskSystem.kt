package com.stockidle.game.game

import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

/**
 * 每日任务系统：每天 3 个任务，目标由“日初总资产”动态生成，
 * 保证任何阶段都只要几分钟就能完成。
 */
object TaskSystem {

    data class DailyTasks(val dayKey: String, val tasks: List<TaskState>)

    /** 生成当日任务（目标、奖励） */
    fun generate(dayKey: String, totalAssets: Double): DailyTasks {
        val rng = Random(dayKey.hashCode().toLong())
        val profitTarget = niceRound((totalAssets * 0.025).coerceAtLeast(1500.0), rng)
        val profitReward = profitTarget * 0.6
        val tradeReward = (profitTarget * 0.15).coerceAtLeast(300.0)
        val divReward = 800.0
        val tasks = listOf(
            TaskState(TaskType.PROFIT_TODAY, profitTarget, profitReward),
            TaskState(TaskType.TRADES, 6.0, tradeReward),
            TaskState(TaskType.DIVIDEND_CLAIMS, 3.0, divReward),
        )
        return DailyTasks(dayKey, tasks)
    }

    /** 把目标凑成好看的整数 */
    private fun niceRound(v: Double, rng: Random): Double {
        val magnitude = Math.pow(10.0, Math.floor(Math.log10(v)) - 1.0)
        val steps = listOf(1.0, 1.5, 2.0, 2.5, 3.0, 5.0)
        val base = v / magnitude
        val chosen = steps[rng.nextInt(steps.size)]
        return chosen * magnitude
    }

    /** 当前日 key：yyyy-MM-dd（本地时区） */
    fun todayKey(): String {
        val c = Calendar.getInstance()
        return String.format(
            Locale.CHINA, "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** 当前时间是否处于波动翻倍窗口（12:00-13:00 / 20:00-21:00） */
    fun windowActive(): Boolean {
        val c = Calendar.getInstance()
        val m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        return (m >= 720 && m < 780) || (m >= 1200 && m < 1260)
    }

    /** 距离下一个波动窗口开始的分钟数；若正在窗口中返回 -1 */
    fun nextWindowInMinutes(): Int {
        val c = Calendar.getInstance()
        val m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        val windows = listOf(720, 1200)
        for (w in windows) if (m < w) return w - m
        // 明天 12:00
        return 1440 - m + 720
    }
}
