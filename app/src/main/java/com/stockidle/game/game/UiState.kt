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
    val history: List<Double>,
    val locked: Boolean,
    val shares: Double,
    val avgCost: Double,
    val pnl: Double,
    val favorite: Boolean,
    val hasEvent: Boolean,
    val eventGood: Boolean,
) {
    val positionValue: Double get() = shares * price
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

data class GameState(
    val cash: Double,
    val pendingDividends: Double,
    val totalAssets: Double,
    val maxTotalAssets: Double,
    val todayPnL: Double,
    val stocks: List<StockUi>,
    val marketIndex: Double,
    val mood: Double,
    val upgrades: List<UpgradeUi>,
    val tasks: List<TaskUi>,
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
    val versionName: String,
) {
    val positionMarketValue: Double get() = stocks.sumOf { it.positionValue }
    val holdingCount: Int get() = stocks.count { it.shares > 1e-9 }
}
