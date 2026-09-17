package com.stockidle.game.game

/**
 * 成就系统 v0.2：长期目标 + 现金奖励，达成自动入账。
 * 检查所需全部统计由引擎的 StatsSnapshot 提供。
 */
object Achievements {

    /** 引擎每秒汇总给成就检查的统计快照 */
    data class Stats(
        val totalTrades: Int,
        val realizedPnL: Double,
        val totalAssets: Double,
        val holdingCount: Int,
        val managerLevel: Int,
        val divClaimTotal: Int,
        val orderFilledTotal: Int,
        val unlockedCount: Int,
    )

    data class AchievementDef(
        val id: String,
        val name: String,
        val desc: String,
        val reward: Double,
        val check: (Stats) -> Boolean,
    )

    val defs: List<AchievementDef> = listOf(
        AchievementDef("first_trade", "开门红", "完成第一笔交易", 800.0) { it.totalTrades >= 1 },
        AchievementDef("ten_trades", "小试牛刀", "累计完成 10 笔交易", 2_000.0) { it.totalTrades >= 10 },
        AchievementDef("fifty_trades", "交易常客", "累计完成 50 笔交易", 8_000.0) { it.totalTrades >= 50 },
        AchievementDef("realized_10k", "落袋为安", "累计已实现盈利 ¥1万", 5_000.0) { it.realizedPnL >= 10_000 },
        AchievementDef("realized_100k", "职业操盘", "累计已实现盈利 ¥10万", 30_000.0) { it.realizedPnL >= 100_000 },
        AchievementDef("five_stocks", "分散投资", "同时持有 5 只不同股票", 6_000.0) { it.holdingCount >= 5 },
        AchievementDef("assets_100k", "十万俱乐部", "总资产达到 ¥10万", 10_000.0) { it.totalAssets >= 100_000 },
        AchievementDef("assets_1m", "百万之路", "总资产达到 ¥100万", 60_000.0) { it.totalAssets >= 1_000_000 },
        AchievementDef("div_50", "收租达人", "累计领取分红 50 次", 5_000.0) { it.divClaimTotal >= 50 },
        AchievementDef("order_1", "挂单高手", "第一次限价委托成交", 3_000.0) { it.orderFilledTotal >= 1 },
        AchievementDef("prestige_1", "涅槃重生", "完成第一次转生", 50_000.0) { it.managerLevel >= 1 },
        AchievementDef("manager_3", "金牌经理", "经理等级达到 Lv.3", 200_000.0) { it.managerLevel >= 3 },
    )

    fun byId(id: String): AchievementDef? = defs.firstOrNull { it.id == id }
}
