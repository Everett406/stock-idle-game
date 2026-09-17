package com.stockidle.game.game

/**
 * 12 只虚拟股票的参数表。
 *
 * 换算公式（与 GDD 一致）：
 *   漂移：driftPerSec = ln(1 + 日漂移) / 86400
 *   分红：divPerSec   = 日分红率 / 86400（线性累计，不复利）
 *
 * 数值设计目标：
 *   - 开局 3 只低波动蓝筹，靠差价 + 分红熟悉玩法
 *   - 解锁阶梯 ×3~4 倍总资产，配合主动交易 1 天解锁第 5 只
 *   - 高波动股差价空间大但风险高，留给中期博弈权重
 */
object StockUniverse {

    private fun drift(dailyPct: Double): Double = ln1p(dailyPct / 100.0) / 86400.0
    private fun div(dailyPct: Double): Double = dailyPct / 100.0 / 86400.0

    val stocks: List<StockDef> = listOf(
        StockDef(
            id = "wsbk", symbol = "WS104", name = "稳盛银行", sector = "金融",
            basePrice = 8.60, driftPerSec = drift(0.40), volPerSec = 0.00060,
            divPerSec = div(2.40), unlockAt = 0.0,
        ),
        StockDef(
            id = "klye", symbol = "KL208", name = "凯乐饮料", sector = "消费",
            basePrice = 12.00, driftPerSec = drift(0.80), volPerSec = 0.00090,
            divPerSec = div(1.40), unlockAt = 0.0,
        ),
        StockDef(
            id = "tddl", symbol = "TD315", name = "天电能源", sector = "公用",
            basePrice = 6.80, driftPerSec = drift(0.30), volPerSec = 0.00070,
            divPerSec = div(2.80), unlockAt = 0.0,
        ),
        StockDef(
            id = "rjsj", symbol = "RJ420", name = "瑞机软件", sector = "科技",
            basePrice = 42.00, driftPerSec = drift(2.50), volPerSec = 0.00160,
            divPerSec = div(0.35), unlockAt = 30_000.0,
        ),
        StockDef(
            id = "xnyx", symbol = "XN517", name = "新耀新能源", sector = "周期",
            basePrice = 18.50, driftPerSec = drift(3.00), volPerSec = 0.00180,
            divPerSec = div(0.50), unlockAt = 80_000.0,
        ),
        StockDef(
            id = "swsw", symbol = "SW633", name = "深物生物", sector = "医药",
            basePrice = 65.00, driftPerSec = drift(2.00), volPerSec = 0.00220,
            divPerSec = div(0.25), unlockAt = 200_000.0,
        ),
        StockDef(
            id = "wlkj", symbol = "WL741", name = "未来科技", sector = "科技",
            basePrice = 88.00, driftPerSec = drift(4.00), volPerSec = 0.00220,
            divPerSec = div(0.20), unlockAt = 500_000.0,
        ),
        StockDef(
            id = "htkg", symbol = "HT852", name = "华天航天", sector = "军工",
            basePrice = 130.00, driftPerSec = drift(3.50), volPerSec = 0.00260,
            divPerSec = div(0.35), unlockAt = 1_200_000.0,
        ),
        StockDef(
            id = "szhb", symbol = "SZ966", name = "数子货币", sector = "矿企",
            basePrice = 25.00, driftPerSec = drift(-0.50), volPerSec = 0.00400,
            divPerSec = div(0.0), unlockAt = 3_000_000.0,
        ),
        StockDef(
            id = "rgzn", symbol = "RG117", name = "人极智能", sector = "科技",
            basePrice = 210.00, driftPerSec = drift(5.00), volPerSec = 0.00280,
            divPerSec = div(0.15), unlockAt = 8_000_000.0,
        ),
        StockDef(
            id = "lzjs", symbol = "LZ258", name = "量子计算", sector = "前沿",
            basePrice = 320.00, driftPerSec = drift(4.50), volPerSec = 0.00340,
            divPerSec = div(0.10), unlockAt = 20_000_000.0,
        ),
        StockDef(
            id = "xjkx", symbol = "XJ369", name = "星际科考", sector = "前沿",
            basePrice = 520.00, driftPerSec = drift(6.00), volPerSec = 0.00420,
            divPerSec = div(0.30), unlockAt = 50_000_000.0,
        ),
    )

    private fun ln1p(x: Double): Double = Math.log1p(x)

    fun byId(id: String): StockDef = stocks.first { it.id == id }
}
