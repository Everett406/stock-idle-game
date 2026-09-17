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
    var sessionOpen: Double = def.basePrice   // 分时涨跌基准
    var dayOpen: Double = def.basePrice       // 涨跌停基准（每日重置）
    val history = ArrayDeque<Double>()        // 最多 720 个点（约 12 分钟分时）
    val volumes = ArrayDeque<Double>()        // 对应每秒成交量（虚拟手数）
    var unlocked: Boolean = false
    var unlockedAnnounced: Boolean = false

    /** 当前生效事件（对数漂移增量 / 每秒） */
    var eventDriftPerSec: Double = 0.0
    var eventUntilMs: Long = 0
    var eventId: Int = 0
    var eventGood: Boolean = true
    var eventTitle: String = ""
    var eventBody: String = ""

    /** 涨跌停状态：1 涨停 / -1 跌停 / 0 正常 */
    var limitState: Int = 0

    val limitUp: Double get() = (dayOpen * LIMIT_PCT).round4()
    val limitDown: Double get() = (dayOpen * (2.0 - LIMIT_PCT)).round4()

    val tickChange: Double get() = price - prevPrice
    val sessionChangePct: Double
        get() = if (sessionOpen > 1e-9) (price / sessionOpen - 1.0) * 100.0 else 0.0
    val dayChangePct: Double
        get() = if (dayOpen > 1e-9) (price / dayOpen - 1.0) * 100.0 else 0.0

    fun pushHistory(p: Double, v: Double) {
        history.addLast(p)
        volumes.addLast(v)
        while (history.size > HISTORY_MAX) history.removeFirst()
        while (volumes.size > HISTORY_MAX) volumes.removeFirst()
    }

    private fun Double.round4(): Double = Math.round(this * 10000.0) / 10000.0

    companion object {
        const val HISTORY_MAX = 720
        const val LIMIT_PCT = 1.10   // ±10% 涨跌停
    }
}

/** 持仓（ shares + 成本总额，成本含买入手续费，用于计算盈亏） */
data class HoldingState(
    var shares: Double = 0.0,
    var costBasis: Double = 0.0,
) {
    val avgCost: Double get() = if (shares > 1e-9) costBasis / shares else 0.0
}

/** 限价委托单（挂单冻结资金/持仓，成交或撤单释放） */
data class PendingOrder(
    val id: Long,
    val stockId: String,
    val buy: Boolean,
    val limitPrice: Double,
    val fraction: Double,     // 挂单时选定的比例（买:当时现金比例；卖:当时持仓比例）
    var frozenCash: Double,   // 买单冻结现金
    var frozenShares: Double, // 卖单冻结股数
    val createdAtMs: Long,
)

/** 委托单 UI 快照 */
data class OrderUi(
    val id: Long,
    val stockId: String,
    val symbol: String,
    val name: String,
    val buy: Boolean,
    val limitPrice: Double,
    val fraction: Double,
    val currentPrice: Double,
    val frozenCash: Double,
    val frozenShares: Double,
)

/** 升级项定义（icon 为 AppIcons 的 key，全应用禁用 emoji） */
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
    val sector: String? = null,   // 板块事件时非空
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

    /** 限价单成交 */
    data class OrderFilled(
        val stockId: String, val symbol: String, val buy: Boolean,
        val price: Double, val amount: Double,
    ) : GameEvent

    /** 委托单操作反馈 */
    data class OrderPlaced(val ok: Boolean, val msg: String) : GameEvent

    /** 成就达成 */
    data class AchievementUnlocked(val name: String, val reward: Double) : GameEvent

    /** 新手引导完成（含一次性启动补贴） */
    data class TutorialDone(val rewarded: Boolean) : GameEvent
}

/** 主题模式：跟随系统 / 亮色 / 暗色 */
object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}

/** 市场活跃度（价格波动与事件频率的全局节奏，可在设置中调整） */
object MarketPace {
    const val CALM = 0      // 悠闲：波动 ×0.7，事件更少
    const val STANDARD = 1  // 标准：原版节奏
    const val RAPID = 2     // 疾速：波动 ×1.5，事件更密

    fun volFactor(pace: Int): Double = when (pace) {
        CALM -> 0.7
        RAPID -> 1.5
        else -> 1.0
    }

    fun intervalFactor(pace: Int): Double = when (pace) {
        CALM -> 1.5
        RAPID -> 0.6
        else -> 1.0
    }

    fun label(pace: Int): String = when (pace) {
        CALM -> "悠闲"
        RAPID -> "疾速"
        else -> "标准"
    }
}
