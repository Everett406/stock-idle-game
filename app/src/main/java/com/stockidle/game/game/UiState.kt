package com.stockidle.game.game

/**
 * UI 层每秒收到的完整快照。所有界面只读这个对象渲染。
 */
data class StockUi(
    val def: StockDef,
    val price: Double,
    val tickUp: Boolean,
    val tickFlat: Boolean,
    val sessionChangePct: Double,
    val dayChangePct: Double,
    val history: List<Double>,
    val volumes: List<Double>,
    val locked: Boolean,
    val shares: Double,
    val avgCost: Double,
    val pnl: Double,
    val favorite: Boolean,
    val hasEvent: Boolean,
    val eventGood: Boolean,
    val limitState: Int,          // 1 涨停 / -1 跌停 / 0 正常
    val dayOpen: Double,          // 涨跌停基准价
    val pendingOrders: Int,       // 该股待成交委托数
    val frozenShares: Double,     // 卖单冻结股数
) {
    val positionValue: Double get() = shares * price
    val tradableShares: Double get() = (shares - frozenShares).coerceAtLeast(0.0)
    val limitUp: Double get() = dayOpen * 1.10
    val limitDown: Double get() = dayOpen * 0.90
}

data class UpgradeUi(
    val def: UpgradeDef,
    val level: Int,
    val cost: Double,
    val affordable: Boolean,
    val maxed: Boolean,
)

data class TaskUi(
    val title: String,
    val target: Double,
    val progress: Double,
    val reward: Double,
    val done: Boolean,
    val claimed: Boolean,
)

data class AchievementUi(
    val def: Achievements.AchievementDef,
    val unlocked: Boolean,
)

data class GameState(
    val cash: Double,
    val pendingDividends: Double,
    val totalAssets: Double,
    val maxTotalAssets: Double,
    val todayPnL: Double,
    val stocks: List<StockUi>,
    val marketIndex: Double,          // 市场情绪指数（已解锁股平均涨跌幅 %）
    val indexValue: Double,           // 大盘指数（基准 1000）
    val indexHistory: List<Double>,   // 大盘指数分时
    val mood: Double,
    val upgrades: List<UpgradeUi>,
    val tasks: List<TaskUi>,
    val achievements: List<AchievementUi>,
    val achievementUnlockedCount: Int,
    val orders: List<OrderUi>,        // 进行中的限价委托
    val managerLevel: Int,
    val prestigeThreshold: Double,
    val canPrestige: Boolean,
    val windowActive: Boolean,
    val nextWindowMinutes: Int,
    val activeEvent: ActiveEvent?,
    val hint: EventHint?,
    val feeRate: Double,
    val dividendMult: Double,
    val dayKey: String,
    val totalTrades: Int,
    val realizedPnL: Double,
    val themeMode: Int,
    val soundOn: Boolean,
    val vibrateOn: Boolean,
    val pace: Int,                    // 市场活跃度（MarketPace）
    val tutorialDone: Boolean,
    val versionName: String,
) {
    val positionMarketValue: Double get() = stocks.sumOf { it.positionValue }
    val holdingCount: Int get() = stocks.count { it.shares > 1e-9 }
    val frozenCash: Double get() = orders.filter { it.buy }.sumOf { it.frozenCash }
    val availableCash: Double get() = (cash - frozenCash).coerceAtLeast(0.0)
}
