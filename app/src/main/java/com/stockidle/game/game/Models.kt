package com.stockidle.game.game

/**
 * 股票静态定义（数值全部与 docs/GDD.md 中的参数表一一对应）。
 *
 * @param id           唯一标识
 * @param symbol       六位虚拟代码
 * @param name         股票名称
 * @param sector       所属板块
 * @param basePrice    初始价 / 均值回归锚定价
 * @param driftPerSec  每秒对数漂移率 μ（由日漂移换算：ln(1+日漂移)/86400）
 * @param volPerSec    每秒波动率 σ
 * @param divPerSec    每秒分红率（持仓市值每秒产生的分红比例，由日分红率/86400）
 * @param unlockAt     解锁所需总资产阈值（0 表示开局解锁）
 */
data class StockDef(
    val id: String,
    val symbol: String,
    val name: String,
    val sector: String,
    val basePrice: Double,
    val driftPerSec: Double,
    val volPerSec: Double,
    val divPerSec: Double,
    val unlockAt: Double,
) {
    val divDailyPct: Double get() = divPerSec * 86400.0 * 100.0
}

/** 单只股票的运行时状态 */
class StockState(val def: StockDef) {
    var price: Double = def.basePrice
    var prevPrice: Double = def.basePrice
    var sessionOpen: Double = def.basePrice
    val history = ArrayDeque<Double>()   // 最多 720 个点（约 12 分钟）
    var unlocked: Boolean = false
    var unlockedAnnounced: Boolean = false

    /** 当前生效事件（对数漂移增量 / 每秒） */
    var eventDriftPerSec: Double = 0.0
    var eventUntilMs: Long = 0
    var eventId: Int = 0
    var eventGood: Boolean = true
    var eventTitle: String = ""
    var eventBody: String = ""

    val tickChange: Double get() = price - prevPrice
    val sessionChangePct: Double
        get() = if (sessionOpen > 1e-9) (price / sessionOpen - 1.0) * 100.0 else 0.0

    fun pushHistory(p: Double) {
        history.addLast(p)
        while (history.size > HISTORY_MAX) history.removeFirst()
    }

    companion object { const val HISTORY_MAX = 720 }
}

/** 持仓（ shares + 成本总额，成本含买入手续费，用于计算盈亏） */
data class HoldingState(
    var shares: Double = 0.0,
    var costBasis: Double = 0.0,
) {
    val avgCost: Double get() = if (shares > 1e-9) costBasis / shares else 0.0
}

/** 升级项定义 */
data class UpgradeDef(
    val id: String,
    val name: String,
    val icon: String,
    val desc: String,
    val baseCost: Double,
    val costGrowth: Double,
    val maxLevel: Int,
) {
    fun costAt(level: Int): Double = baseCost * Math.pow(costGrowth, level.toDouble())
}

/** 每日任务类型 */
enum class TaskType { PROFIT_TODAY, TRADES, DIVIDEND_CLAIMS }

/** 每日任务运行时 */
data class TaskState(
    val type: TaskType,
    val target: Double,
    val reward: Double,
    var claimed: Boolean = false,
) {
    fun title(totalAssets: Double): String = when (type) {
        TaskType.PROFIT_TODAY -> "今日盈利 ¥${com.stockidle.game.util.Fmt.qty(target)}"
        TaskType.TRADES -> "完成 ${target.toInt()} 笔交易"
        TaskType.DIVIDEND_CLAIMS -> "领取 ${target.toInt()} 次分红"
    }
}

/** 随机事件（用于横幅展示） */
data class ActiveEvent(
    val id: Int,
    val stockId: String,
    val stockName: String,
    val good: Boolean,
    val magnitudePct: Double,
    val untilMs: Long,
    val title: String,
    val body: String,
)

/** 内幕提示（新闻推送升级解锁后，事件发生前给出） */
data class EventHint(
    val stockName: String,
    val good: Boolean,
    val leadSeconds: Int,
)

/** 离线收益报告 */
data class OfflineReport(
    val seconds: Long,
    val items: List<Pair<String, Double>>, // 股票名 -> 分红金额
    val total: Double,
)

/** 引擎 -> UI 的一次性事件 */
sealed interface GameEvent {
    data class Trade(
        val stockId: String, val symbol: String, val qty: Double,
        val price: Double, val buy: Boolean, val ok: Boolean, val msg: String,
    ) : GameEvent

    data class DividendsCollected(val amount: Double) : GameEvent
    data class News(val title: String, val body: String, val good: Boolean) : GameEvent
    data class EventHint(val stockName: String, val good: Boolean, val leadSeconds: Int) : GameEvent
    data class StockUnlocked(val stockId: String, val name: String) : GameEvent
    data class OfflineReportReady(val report: OfflineReport) : GameEvent
    data class PrestigeDone(val oldTotal: Double, val newLevel: Int, val startCapital: Double) : GameEvent
    data class UpgradeBought(val id: String, val newLevel: Int) : GameEvent
    data class TaskClaimed(val title: String, val reward: Double) : GameEvent
}

/** 主题模式：跟随系统 / 亮色 / 暗色 */
object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}
