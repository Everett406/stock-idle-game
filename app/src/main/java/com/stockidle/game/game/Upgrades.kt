package com.stockidle.game.game

/**
 * 升级系统 v0.2：7 个升级项，成本 = baseCost × growth^level。
 * 效果函数由 GameEngine 调用。
 * icon 字段为 AppIcons 的 key（全应用禁用 emoji）。
 */
object Upgrades {

    val defs: List<UpgradeDef> = listOf(
        UpgradeDef(
            id = "fee", name = "VIP通道", icon = "coin",
            desc = "降低买卖手续费",
            baseCost = 800.0, costGrowth = 1.9, maxLevel = 10,
        ),
        UpgradeDef(
            id = "div", name = "红利基金", icon = "chart",
            desc = "提升分红收益",
            baseCost = 2000.0, costGrowth = 1.75, maxLevel = 20,
        ),
        UpgradeDef(
            id = "news", name = "资讯通", icon = "news",
            desc = "突发新闻更频繁，行情机会更多",
            baseCost = 12_000.0, costGrowth = 2.20, maxLevel = 5,
        ),
        UpgradeDef(
            id = "bot", name = "量化机器人", icon = "bot",
            desc = "持仓每秒自动套利",
            baseCost = 50_000.0, costGrowth = 2.10, maxLevel = 10,
        ),
        UpgradeDef(
            id = "ins", name = "止损保险", icon = "shield",
            desc = "下跌时按比例赔付",
            baseCost = 15_000.0, costGrowth = 2.00, maxLevel = 8,
        ),
        UpgradeDef(
            id = "radar", name = "内幕雷达", icon = "radar",
            desc = "提前预告即将发生的事件",
            baseCost = 30_000.0, costGrowth = 2.40, maxLevel = 5,
        ),
        UpgradeDef(
            id = "reinvest", name = "自动复投", icon = "cycle",
            desc = "分红自动领取并按持仓比例再买入",
            baseCost = 100_000.0, costGrowth = 1.0, maxLevel = 1,
        ),
    )

    fun byId(id: String): UpgradeDef = defs.first { it.id == id }

    /** 手续费率：0.1% × 0.8^等级，下限 0.01% */
    fun feeRate(feeLevel: Int): Double =
        (0.001 * Math.pow(0.8, feeLevel.toDouble())).coerceAtLeast(0.0001)

    /** 分红倍率：×(1 + 0.12×等级) */
    fun dividendMult(divLevel: Int): Double = 1.0 + 0.12 * divLevel

    /** 事件间隔缩短：×(1 - 0.10×等级) */
    fun newsIntervalMult(newsLevel: Int): Double = (1.0 - 0.10 * newsLevel).coerceAtLeast(0.5)

    /** 量化机器人每秒收益率（占持仓市值比例） */
    fun botRatePerSec(botLevel: Int): Double = 0.00006 * botLevel

    /** 保险赔付比例（占每秒下跌损失） */
    fun insuranceRatio(insLevel: Int): Double = 0.08 * insLevel

    /** 内幕雷达提前量（秒） */
    fun radarLead(radarLevel: Int): Int = if (radarLevel <= 0) 0 else (8 + 6 * radarLevel).coerceAtMost(30)

    /** 升级项当前效果描述 */
    fun effectText(id: String, level: Int): String = when (id) {
        "fee" -> String.format(java.util.Locale.CHINA, "手续费 %.3f%%", feeRate(level) * 100)
        "div" -> "分红 ×${String.format(java.util.Locale.CHINA, "%.2f", dividendMult(level))}"
        "news" -> if (level > 0) "新闻间隔 -${(level * 10)}%" else "未启用"
        "bot" -> if (level > 0) "套利 ${String.format(java.util.Locale.CHINA, "%.3f", botRatePerSec(level) * 100)}%/秒" else "未启用"
        "ins" -> if (level > 0) "赔付 ${(insuranceRatio(level) * 100).toInt()}% 跌损" else "未启用"
        "radar" -> if (level > 0) "提前 ${radarLead(level)} 秒预警" else "未启用"
        "reinvest" -> if (level > 0) "已启用" else "未启用"
        else -> ""
    }
}
