package com.stockidle.game.game

import com.stockidle.game.BuildConfig
import com.stockidle.game.util.Fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * 游戏引擎：市场模拟 + 玩家状态 + 存档，全部单机本地运行。
 *
 * 市场模型 v0.2（几何布朗运动 + 均值回归 + 事件冲击 + 涨跌停）：
 *   S(t+dt) = S(t) · exp( (μ_eff − σ²/2)·dt + σ·√dt·Z )
 *   μ_eff   = μ + E(t) − k·ln( S(t)/B )        —— k=2.5e-5，B=初始价
 *   E(t)    = ln(1 ± m) / T                    —— 事件期额外漂移，m∈[3%,18%]，T∈[40,150]s
 *   涨跌停  = 日开盘价 ±10%，触板后锁价直至首笔反向波动
 *   大盘指数 = 1000 × Π(Sᵢ/Bᵢ)^(1/n)（已解锁股等权几何平均）
 *   成交量   = 基数 × (0.5+|Z|) × (1+事件放大)，虚拟手数
 */
class GameEngine(
    private val scope: CoroutineScope,
    private val filesDir: File,
) {

    // ---------- 市场状态 ----------
    private val stockStates: List<StockState> = StockUniverse.stocks.map { StockState(it) }
    private val stockMap: Map<String, StockState> = stockStates.associateBy { it.def.id }

    // ---------- 大盘指数 ----------
    private val indexHistory = ArrayDeque<Double>()
    private var indexValue = INDEX_BASE

    // ---------- 玩家状态 ----------
    private val holdings = mutableMapOf<String, HoldingState>()
    private val upgradeLevels = mutableMapOf<String, Int>()
    private val favorites = mutableSetOf<String>()
    private val orders = mutableListOf<PendingOrder>()
    private val achievementsUnlocked = mutableSetOf<String>()
    private var cash = START_CAPITAL_BASE
    private var pendingDividends = 0.0
    private var maxTotalAssets = START_CAPITAL_BASE
    private var managerLevel = 0

    // ---------- 统计 / 任务 ----------
    private var dayKey = TaskSystem.todayKey()
    private var dayBaseline = START_CAPITAL_BASE
    private var tradesToday = 0
    private var divClaimsToday = 0
    private var divClaimTotal = 0
    private var orderFilledTotal = 0
    private var totalTradeCount = 0
    private var realizedPnLTotal = 0.0
    private var tasks: TaskSystem.DailyTasks = TaskSystem.generate(dayKey, START_CAPITAL_BASE)

    // ---------- 事件调度 ----------
    private var activeEvent: ActiveEvent? = null
    private var pendingEvent: PendingEvent? = null
    private var nextEventAtMs = 0L
    private var eventIdSeq = 0
    private var hint: EventHint? = null
    private var orderIdSeq = 0L

    // ---------- 设置 ----------
    private var themeMode = ThemeMode.SYSTEM
    private var soundOn = true
    private var vibrateOn = true
    private var pace = MarketPace.STANDARD
    private var tutorialDone = false
    private var tutorialRewarded = false

    // ---------- 运行时 ----------
    private val rng = Random()
    private var tickCount = 0L
    private var lastTickRealMs = 0L
    private var lastSavedMs = 0L
    private var lastReinvestMs = 0L
    private var initialized = false
    private var tickJob: Job? = null

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 32, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _state = MutableStateFlow(buildState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private data class PendingEvent(
        val stockId: String, val good: Boolean, val magnitudePct: Double,
        val durationSec: Long, val fireAtMs: Long, val title: String, val body: String,
        val sector: String? = null,
    )

    // =====================================================================
    // 生命周期
    // =====================================================================

    fun start() {
        if (!initialized) {
            initialized = true
            if (!load()) initFresh()
        }
        val now = System.currentTimeMillis()
        maybeGrantOfflineEarnings(now)
        lastTickRealMs = now
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                val t = System.currentTimeMillis()
                val dt = ((t - lastTickRealMs) / 1000.0).coerceIn(0.2, 5.0)
                lastTickRealMs = t
                step(dt)
            }
        }
        saveNow()
    }

    fun pause() {
        tickJob?.cancel()
        tickJob = null
        if (initialized) saveNow()
    }

    private fun initFresh() {
        cash = START_CAPITAL_BASE
        dayKey = TaskSystem.todayKey()
        dayBaseline = cash
        maxTotalAssets = cash
        tasks = TaskSystem.generate(dayKey, cash)
        stockStates.forEach { resetStock(it, warmup = true) }
        rebuildIndexHistory()
        nextEventAtMs = System.currentTimeMillis() + FIRST_EVENT_DELAY_MS
        lastSavedMs = System.currentTimeMillis()
    }

    private fun resetStock(st: StockState, warmup: Boolean) {
        st.price = st.def.basePrice
        st.prevPrice = st.def.basePrice
        st.sessionOpen = st.def.basePrice
        st.dayOpen = st.def.basePrice
        st.history.clear()
        st.volumes.clear()
        st.eventDriftPerSec = 0.0
        st.eventUntilMs = 0
        st.limitState = 0
        st.unlocked = st.def.unlockAt <= maxTotalAssets + 1e-6
        st.unlockedAnnounced = st.unlocked
        if (warmup) {
            // 预热 120 秒历史，保证首屏就有走势图
            repeat(120) {
                val v = stepPrice(st, 1.0, window = false, volFactor = 1.0)
                st.pushHistory(st.price, v)
            }
        }
    }

    // =====================================================================
    // 每秒主循环
    // =====================================================================

    private fun step(dt: Double) {
        val now = System.currentTimeMillis()

        // 1) 跨天处理：重置任务 / 今日盈亏基准 / 涨跌停基准
        val tk = TaskSystem.todayKey()
        if (tk != dayKey) {
            dayKey = tk
            dayBaseline = computeTotalAssets()
            tradesToday = 0
            divClaimsToday = 0
            tasks = TaskSystem.generate(dayKey, dayBaseline)
            // 新交易日：以当前价格为开盘基准
            stockStates.forEach { st ->
                st.sessionOpen = st.price
                st.dayOpen = st.price
                st.limitState = 0
            }
            saveNow()
        }

        // 2) 事件调度（含内幕雷达提示）
        handleEventSchedule(now)

        // 3) 价格演化 + 成交量
        val window = TaskSystem.windowActive()
        val volFactor = MarketPace.volFactor(pace)
        for (st in stockStates) {
            st.prevPrice = st.price
            if (st.eventUntilMs in 1 until now) {
                st.eventDriftPerSec = 0.0
                st.eventUntilMs = 0
                if (activeEvent?.stockId == st.def.id) activeEvent = null
            }
            stepPrice(st, dt, window, volFactor).let { v -> st.pushHistory(st.price, v) }
        }

        // 4) 大盘指数演化
        updateIndex()

        // 5) 分红累计
        val divMult = Upgrades.dividendMult(upgradeLevels.getOrDefault("div", 0)) * (1.0 + 0.30 * managerLevel)
        var divAcc = 0.0
        for ((id, h) in holdings) {
            if (h.shares <= 1e-9) continue
            val st = stockMap[id] ?: continue
            divAcc += h.shares * st.price * st.def.divPerSec * divMult
        }
        pendingDividends += divAcc * dt

        // 6) 自动复投：每 30 秒把待领分红自动收取并按市值比例再买入
        val reinvestOn = upgradeLevels.getOrDefault("reinvest", 0) > 0
        if (reinvestOn && pendingDividends >= 1.0 && now - lastReinvestMs >= REINVEST_PERIOD_MS) {
            lastReinvestMs = now
            autoReinvest()
        }

        // 7) 量化机器人
        val botLevel = upgradeLevels.getOrDefault("bot", 0)
        if (botLevel > 0) {
            var pv = 0.0
            for ((id, h) in holdings) {
                val st = stockMap[id] ?: continue
                pv += h.shares * st.price
            }
            cash += pv * Upgrades.botRatePerSec(botLevel) * dt
        }

        // 8) 止损保险：按每秒下跌损失赔付
        val insLevel = upgradeLevels.getOrDefault("ins", 0)
        if (insLevel > 0) {
            val ratio = Upgrades.insuranceRatio(insLevel)
            for ((id, h) in holdings) {
                val st = stockMap[id] ?: continue
                val loss = h.shares * (st.prevPrice - st.price)
                if (loss > 0) cash += loss * ratio
            }
        }

        // 9) 限价委托撮合
        matchOrders(now)

        // 10) 解锁检查
        val total = computeTotalAssets()
        if (total > maxTotalAssets) maxTotalAssets = total
        for (st in stockStates) {
            if (!st.unlocked && maxTotalAssets >= st.def.unlockAt) {
                st.unlocked = true
                if (!st.unlockedAnnounced) {
                    st.unlockedAnnounced = true
                    _events.tryEmit(GameEvent.StockUnlocked(st.def.id, st.def.name))
                }
            }
        }

        // 11) 成就检查
        checkAchievements()

        // 12) 周期性存档
        tickCount++
        if (tickCount % 30L == 0L) saveNow()

        _state.value = buildState()
    }

    private fun stepPrice(st: StockState, dt: Double, window: Boolean, volFactor: Double): Double {
        // 涨停/跌停锁价：触板后冻结，直到「事件结束或出现足够强的反向力量」
        // 简化模型：贴板时仍按 GBM 计算，但结果被 clamp；当被 clamp 且方向反转时自然开板
        if (st.limitState != 0 && st.eventUntilMs == 0L) {
            // 无事件贴板：每天有概率开板
            val openProb = if (st.limitState > 0) 0.04 else 0.08
            if (rng.nextDouble() < openProb * dt) st.limitState = 0
        }
        val sigma = st.def.volPerSec * (if (window) 2.0 else 1.0) * volFactor
        val mu = st.def.driftPerSec + st.eventDriftPerSec
        val revert = -REVERT_K * Math.log(st.price / st.def.basePrice)
        val muEff = mu + revert
        val z = rng.nextGaussian()
        val next = st.price * Math.exp((muEff - sigma * sigma / 2.0) * dt + sigma * Math.sqrt(dt) * z)

        // 模拟成交量：基准 × (0.5 + |Z|) × 事件放大 × 贴板封单萎缩
        val eventBoost = if (st.eventUntilMs > 0L) 2.5 else 1.0
        val limitBoost = if (st.limitState != 0) 0.15 else 1.0   // 贴板时成交萎缩
        val v = VOLUME_BASE * (0.5 + kotlin.math.abs(z)) * eventBoost * limitBoost * (1.0 + (rng.nextDouble() - 0.5) * 0.4)

        // 涨跌停 clamp
        val up = st.dayOpen * StockState.LIMIT_PCT
        val down = st.dayOpen * (2.0 - StockState.LIMIT_PCT)
        st.price = when {
            next >= up -> { st.limitState = 1; up }
            next <= down -> { st.limitState = -1; down }
            else -> { st.limitState = 0; next }
        }
        if (st.price < st.def.basePrice * 0.05) st.price = st.def.basePrice * 0.05
        st.price = st.price.coerceAtLeast(0.01)
        return max(1.0, v)
    }

    // =====================================================================
    // 大盘指数
    // =====================================================================

    private fun computeIndex(): Double {
        val unlocked = stockStates.filter { it.unlocked }
        if (unlocked.isEmpty()) return INDEX_BASE
        var logSum = 0.0
        for (st in unlocked) {
            logSum += Math.log(max(0.01, st.price) / st.def.basePrice)
        }
        return INDEX_BASE * Math.exp(logSum / unlocked.size)
    }

    private fun updateIndex() {
        indexValue = computeIndex()
        indexHistory.addLast(indexValue)
        while (indexHistory.size > INDEX_HISTORY_MAX) indexHistory.removeFirst()
    }

    /** 用已解锁股票的分时历史重建指数历史（启动/重置时调用，保证图表完整） */
    private fun rebuildIndexHistory() {
        indexHistory.clear()
        val unlocked = stockStates.filter { it.unlocked }
        if (unlocked.isEmpty()) {
            repeat(INDEX_HISTORY_MAX) { indexHistory.addLast(INDEX_BASE) }
            return
        }
        val minLen = unlocked.minOf { it.history.size }
        for (i in 0 until minLen) {
            var logSum = 0.0
            for (st in unlocked) {
                val p = st.history.elementAtOrElse(i) { st.price }
                logSum += Math.log(max(0.01, p) / st.def.basePrice)
            }
            indexHistory.addLast(INDEX_BASE * Math.exp(logSum / unlocked.size))
        }
        // 不足时补齐
        while (indexHistory.size < INDEX_HISTORY_MAX) indexHistory.addFirst(indexHistory.firstOrNull() ?: INDEX_BASE)
        indexValue = computeIndex()
    }

    // =====================================================================
    // 事件系统（个股 / 板块 / 财报三类）
    // =====================================================================

    private fun handleEventSchedule(now: Long) {
        // 到点触发挂起事件
        val pe = pendingEvent
        if (pe != null && now >= pe.fireAtMs) {
            pendingEvent = null
            fireNewsEvent(pe, now)
        }
        // 无事件时按节奏安排
        if (pendingEvent == null && activeEvent == null && now >= nextEventAtMs) {
            val newsLvl = upgradeLevels.getOrDefault("news", 0)
            val intervalMult = MarketPace.intervalFactor(pace) * Upgrades.newsIntervalMult(newsLvl)

            val roll = rng.nextDouble()
            when {
                // 板块事件（18%）：同板块所有已解锁股联动
                roll < 0.18 -> scheduleSectorEvent(now)
                // 财报事件（15%）：单股大幅波动
                roll < 0.33 -> scheduleEarningsEvent(now)
                else -> scheduleStockEvent(now)
            }
            nextEventAtMs = now +
                ((EVENT_INTERVAL_BASE_MS + (rng.nextDouble() * EVENT_INTERVAL_RAND_MS).toLong()) * intervalMult).toLong()
        }
    }

    private fun scheduleStockEvent(now: Long) {
        val stock = pickEventStock()
        val good = rng.nextDouble() < 0.55
        val magnitude = 3.0 + rng.nextDouble() * 15.0
        val duration = (40 + rng.nextDouble() * 110).toLong()
        val tmpl = if (good) GOOD_TEMPLATES[rng.nextInt(GOOD_TEMPLATES.size)]
                   else BAD_TEMPLATES[rng.nextInt(BAD_TEMPLATES.size)]
        val title = if (good) "利好 · ${stock.name}" else "利空 · ${stock.name}"
        val body = tmpl.replace("%S", stock.name)
        dispatch(stock.id, good, magnitude, duration, title, body, null, now)
    }

    private fun scheduleEarningsEvent(now: Long) {
        val stock = pickEventStock()
        val good = rng.nextDouble() < 0.6
        val magnitude = 8.0 + rng.nextDouble() * 12.0
        val duration = (60 + rng.nextDouble() * 90).toLong()
        val q = 1 + rng.nextInt(4)
        val tmpl = if (good) EARNINGS_GOOD[rng.nextInt(EARNINGS_GOOD.size)]
                   else EARNINGS_BAD[rng.nextInt(EARNINGS_BAD.size)]
        val title = if (good) "财报超预期 · ${stock.name}" else "财报暴雷 · ${stock.name}"
        val body = tmpl.replace("%S", stock.name).replace("%Q", "Q$q")
        dispatch(stock.id, good, magnitude, duration, title, body, null, now)
    }

    private fun scheduleSectorEvent(now: Long) {
        val seed = pickEventStock()
        val sector = seed.sector
        val good = rng.nextDouble() < 0.55
        val base = 5.0 + rng.nextDouble() * 10.0
        val duration = (60 + rng.nextDouble() * 100).toLong()
        val tmpl = if (good) SECTOR_GOOD[rng.nextInt(SECTOR_GOOD.size)]
                   else SECTOR_BAD[rng.nextInt(SECTOR_BAD.size)]
        val title = if (good) "板块利好 · $sector" else "板块利空 · $sector"
        val body = tmpl.replace("%T", sector)
        // 同板块全部解锁股按 ±20% 随机化幅度各自受力
        val affected = stockStates.filter { it.unlocked && it.def.sector == sector }
        if (affected.isEmpty()) { scheduleStockEvent(now); return }
        val first = affected.first()
        affected.forEach { st ->
            val m = base * (0.8 + rng.nextDouble() * 0.4)
            val dur = duration + rng.nextLong(20)
            if (st.def.id == first.def.id) {
                dispatch(st.def.id, good, m, dur, title, body, sector, now)
            } else {
                st.eventDriftPerSec = (if (good) 1.0 else -1.0) * Math.log(1.0 + m / 100.0) / dur
                st.eventUntilMs = now + dur * 1000L
            }
        }
    }

    private fun dispatch(
        stockId: String, good: Boolean, magnitude: Double, durationSec: Long,
        title: String, body: String, sector: String?, now: Long,
    ) {
        val radarLevel = upgradeLevels.getOrDefault("radar", 0)
        val lead = Upgrades.radarLead(radarLevel)
        val pe = PendingEvent(stockId, good, magnitude, durationSec, now, title, body, sector)
        if (lead > 0) {
            // 内幕雷达：先预告，再引爆
            pendingEvent = pe.copy(fireAtMs = now + lead * 1000L)
            val stockName = stockMap[stockId]?.def?.name ?: ""
            hint = EventHint(stockName, good, lead)
            _events.tryEmit(GameEvent.EventHint(stockName, good, lead))
        } else {
            fireNewsEvent(pe, now)
        }
    }

    private fun fireNewsEvent(pe: PendingEvent, now: Long) {
        val st = stockMap[pe.stockId] ?: return
        val totalLn = if (pe.good) Math.log(1.0 + pe.magnitudePct / 100.0)
                      else -Math.log(1.0 + pe.magnitudePct / 100.0)
        st.eventDriftPerSec = totalLn / pe.durationSec
        st.eventUntilMs = now + pe.durationSec * 1000L
        st.eventId = ++eventIdSeq
        st.eventGood = pe.good
        st.eventTitle = pe.title
        st.eventBody = pe.body
        activeEvent = ActiveEvent(
            id = eventIdSeq, stockId = st.def.id, stockName = st.def.name,
            good = pe.good, magnitudePct = pe.magnitudePct,
            untilMs = st.eventUntilMs, title = pe.title, body = pe.body, sector = pe.sector,
        )
        _events.tryEmit(GameEvent.News(pe.title, pe.body, pe.good))
    }

    private fun pickEventStock(): StockDef {
        val unlocked = stockStates.filter { it.unlocked }
        val pool = if (unlocked.isNotEmpty() && rng.nextDouble() < 0.8) unlocked else stockStates
        return pool[rng.nextInt(pool.size)].def
    }

    // =====================================================================
    // 交易（市价）
    // =====================================================================

    fun buy(stockId: String, fraction: Double) {
        val st = stockMap[stockId] ?: return
        if (!st.unlocked) return
        val holding = holdings.getOrPut(stockId) { HoldingState() }
        val avail = availableCash()
        val budget = avail * fraction.coerceIn(0.0, 1.0)
        if (budget < 1.0) {
            _events.tryEmit(GameEvent.Trade(stockId, st.def.symbol, 0.0, st.price, true, false, "可用资金不足"))
            return
        }
        val feeRate = Upgrades.feeRate(upgradeLevels.getOrDefault("fee", 0))
        val qty = budget * (1.0 - feeRate) / st.price
        if (qty <= 1e-9) {
            _events.tryEmit(GameEvent.Trade(stockId, st.def.symbol, 0.0, st.price, true, false, "金额太小"))
            return
        }
        cash -= budget
        holding.shares += qty
        holding.costBasis += budget
        totalTradeCount++; tradesToday++
        saveNow()
        _events.tryEmit(GameEvent.Trade(stockId, st.def.symbol, qty, st.price, true, true,
            "买入 ${Fmt.qty(qty)} 股 @ ${Fmt.price(st.price)}"))
    }

    fun sell(stockId: String, fraction: Double) {
        val st = stockMap[stockId] ?: return
        val holding = holdings[stockId] ?: return
        val f = fraction.coerceIn(0.0, 1.0)
        val tradable = tradableShares(stockId)
        if (tradable <= 1e-9) {
            _events.tryEmit(GameEvent.Trade(stockId, st.def.symbol, 0.0, st.price, false, false, "没有可卖持仓（挂单冻结中）"))
            return
        }
        val qty = if (f >= 0.999) tradable else tradable * f
        if (qty * st.price < 1.0) {
            _events.tryEmit(GameEvent.Trade(stockId, st.def.symbol, 0.0, st.price, false, false, "金额太小"))
            return
        }
        val feeRate = Upgrades.feeRate(upgradeLevels.getOrDefault("fee", 0))
        val gross = qty * st.price
        val fee = gross * feeRate
        val net = gross - fee
        val basisPart = holding.costBasis * (qty / holding.shares)
        val realized = net - basisPart
        realizedPnLTotal += realized
        cash += net
        holding.shares -= qty
        holding.costBasis -= basisPart
        if (holding.shares < 1e-9) {
            holding.shares = 0.0
            holding.costBasis = 0.0
        }
        totalTradeCount++; tradesToday++
        saveNow()
        _events.tryEmit(GameEvent.Trade(stockId, st.def.symbol, qty, st.price, false, true,
            "卖出 ${Fmt.qty(qty)} 股，入账 ${Fmt.money(net)}"))
    }

    // =====================================================================
    // 限价委托单
    // =====================================================================

    fun placeOrder(stockId: String, buy: Boolean, limitPrice: Double, fraction: Double) {
        val st = stockMap[stockId] ?: return
        if (!st.unlocked) return
        val f = fraction.coerceIn(0.05, 1.0)
        if (limitPrice <= 0.0) {
            _events.tryEmit(GameEvent.OrderPlaced(false, "委托价无效"))
            return
        }
        if (buy) {
            val budget = availableCash() * f
            if (budget < 1.0) {
                _events.tryEmit(GameEvent.OrderPlaced(false, "可用资金不足"))
                return
            }
            if (limitPrice >= st.price) {
                // 直接高于现价：按市价立即买入更划算
                buy(stockId, f)
                return
            }
            orders.add(
                PendingOrder(++orderIdSeq, stockId, true, limitPrice, f, budget, 0.0, System.currentTimeMillis())
            )
            _events.tryEmit(GameEvent.OrderPlaced(true, "买单已挂出：${Fmt.price(limitPrice)}，冻结 ${Fmt.money(budget)}"))
        } else {
            val holding = holdings[stockId]
            val tradable = tradableShares(stockId)
            if (holding == null || tradable <= 1e-9) {
                _events.tryEmit(GameEvent.OrderPlaced(false, "没有可卖持仓"))
                return
            }
            if (limitPrice <= st.price) {
                sell(stockId, f)
                return
            }
            val shares = if (f >= 0.999) tradable else tradable * f
            orders.add(
                PendingOrder(++orderIdSeq, stockId, false, limitPrice, f, 0.0, shares, System.currentTimeMillis())
            )
            _events.tryEmit(GameEvent.OrderPlaced(true, "卖单已挂出：${Fmt.price(limitPrice)}，冻结 ${Fmt.qty(shares)} 股"))
        }
        saveNow()
        _state.value = buildState()
    }

    fun cancelOrder(orderId: Long) {
        val o = orders.firstOrNull { it.id == orderId } ?: return
        orders.remove(o)
        saveNow()
        _state.value = buildState()
        _events.tryEmit(GameEvent.OrderPlaced(true, "委托已撤销，冻结释放"))
    }

    private fun matchOrders(now: Long) {
        if (orders.isEmpty()) return
        val feeRate = Upgrades.feeRate(upgradeLevels.getOrDefault("fee", 0))
        val filled = mutableListOf<PendingOrder>()
        for (o in orders) {
            val st = stockMap[o.stockId] ?: continue
            val hit = if (o.buy) st.price <= o.limitPrice else st.price >= o.limitPrice
            if (!hit) continue
            if (o.buy) {
                val qty = o.frozenCash * (1.0 - feeRate) / st.price
                if (qty <= 1e-9) continue
                val h = holdings.getOrPut(o.stockId) { HoldingState() }
                cash -= o.frozenCash
                h.shares += qty
                h.costBasis += o.frozenCash
                _events.tryEmit(GameEvent.OrderFilled(o.stockId, st.def.symbol, true, st.price, o.frozenCash))
            } else {
                val h = holdings[o.stockId] ?: continue
                if (h.shares < o.frozenShares - 1e-9) continue  // 持仓异常，放弃
                val gross = o.frozenShares * st.price
                val net = gross * (1.0 - feeRate)
                val basisPart = h.costBasis * (o.frozenShares / h.shares)
                realizedPnLTotal += net - basisPart
                cash += net
                h.shares -= o.frozenShares
                h.costBasis -= basisPart
                if (h.shares < 1e-9) { h.shares = 0.0; h.costBasis = 0.0 }
                _events.tryEmit(GameEvent.OrderFilled(o.stockId, st.def.symbol, false, st.price, net))
            }
            orderFilledTotal++
            totalTradeCount++; tradesToday++
            filled.add(o)
        }
        if (filled.isNotEmpty()) {
            orders.removeAll(filled)
            saveNow()
        }
    }

    private fun availableCash(): Double {
        val frozen = orders.filter { it.buy }.sumOf { it.frozenCash }
        return (cash - frozen).coerceAtLeast(0.0)
    }

    private fun tradableShares(stockId: String): Double {
        val h = holdings[stockId] ?: return 0.0
        val frozen = orders.filter { !it.buy && it.stockId == stockId }.sumOf { it.frozenShares }
        return (h.shares - frozen).coerceAtLeast(0.0)
    }

    private fun autoReinvest() {
        val amount = pendingDividends
        if (amount < 0.01) return
        pendingDividends = 0.0
        divClaimsToday++; divClaimTotal++
        val totalPv = holdings.entries.sumOf { (id, h) ->
            (stockMap[id]?.price ?: 0.0) * h.shares
        }
        if (totalPv > 1e-9) {
            val feeRate = Upgrades.feeRate(upgradeLevels.getOrDefault("fee", 0))
            for ((id, h) in holdings) {
                if (h.shares <= 1e-9) continue
                val st = stockMap[id] ?: continue
                val share = h.shares * st.price / totalPv
                val budget = amount * share
                if (budget < 0.01) continue
                val qty = budget * (1.0 - feeRate) / st.price
                h.shares += qty
                h.costBasis += budget
            }
            cash -= amount
        } else {
            cash += amount
        }
        _events.tryEmit(GameEvent.DividendsCollected(amount))
        _state.value = buildState()
    }

    fun toggleFavorite(stockId: String) {
        if (!favorites.add(stockId)) favorites.remove(stockId)
        saveNow()
        _state.value = buildState()
    }

    // =====================================================================
    // 分红 / 升级 / 任务 / 成就 / 转生
    // =====================================================================

    fun collectDividends() {
        if (pendingDividends < 0.01) return
        val amount = pendingDividends
        cash += amount
        pendingDividends = 0.0
        divClaimsToday++; divClaimTotal++
        saveNow()
        _events.tryEmit(GameEvent.DividendsCollected(amount))
        _state.value = buildState()
    }

    fun buyUpgrade(id: String) {
        val def = Upgrades.byId(id)
        val level = upgradeLevels.getOrDefault(id, 0)
        if (level >= def.maxLevel) return
        val cost = def.costAt(level)
        if (availableCash() < cost) {
            _events.tryEmit(GameEvent.UpgradeBought(id, -1))
            return
        }
        cash -= cost
        upgradeLevels[id] = level + 1
        saveNow()
        _events.tryEmit(GameEvent.UpgradeBought(id, level + 1))
        _state.value = buildState()
    }

    fun claimTask(index: Int) {
        val list = tasks.tasks
        if (index !in list.indices) return
        val t = list[index]
        if (t.claimed) return
        if (taskProgress(t) < 1.0) return
        t.claimed = true
        cash += t.reward
        saveNow()
        _events.tryEmit(GameEvent.TaskClaimed(t.title(dayBaseline), t.reward))
        _state.value = buildState()
    }

    private fun checkAchievements() {
        val stats = Achievements.Stats(
            totalTrades = totalTradeCount,
            realizedPnL = realizedPnLTotal,
            totalAssets = computeTotalAssets(),
            holdingCount = holdings.count { it.value.shares > 1e-9 },
            managerLevel = managerLevel,
            divClaimTotal = divClaimTotal,
            orderFilledTotal = orderFilledTotal,
            unlockedCount = stockStates.count { it.unlocked },
        )
        var changed = false
        for (def in Achievements.defs) {
            if (def.id in achievementsUnlocked) continue
            if (def.check(stats)) {
                achievementsUnlocked.add(def.id)
                cash += def.reward
                changed = true
                _events.tryEmit(GameEvent.AchievementUnlocked(def.name, def.reward))
            }
        }
        if (changed) saveNow()
    }

    fun prestige() {
        val threshold = prestigeThreshold()
        if (maxTotalAssets < threshold) return
        val oldTotal = computeTotalAssets()
        managerLevel++
        val startCapital = startCapital(managerLevel)
        cash = startCapital
        holdings.clear()
        orders.clear()
        pendingDividends = 0.0
        maxTotalAssets = startCapital
        dayBaseline = startCapital
        activeEvent = null
        pendingEvent = null
        hint = null
        nextEventAtMs = System.currentTimeMillis() + FIRST_EVENT_DELAY_MS
        stockStates.forEach { resetStock(it, warmup = true) }
        rebuildIndexHistory()
        saveNow()
        _events.tryEmit(GameEvent.PrestigeDone(oldTotal, managerLevel, startCapital))
        _state.value = buildState()
    }

    // ---------- 设置 ----------

    fun setThemeMode(mode: Int) { themeMode = mode; saveNow(); _state.value = buildState() }
    fun setSound(on: Boolean) { soundOn = on; saveNow(); _state.value = buildState() }
    fun setVibrate(on: Boolean) { vibrateOn = on; saveNow(); _state.value = buildState() }
    fun setPace(p: Int) { pace = p.coerceIn(0, 2); saveNow(); _state.value = buildState() }
    fun setTutorialDone() {
        tutorialDone = true
        var rewarded = false
        if (!tutorialRewarded) {
            tutorialRewarded = true
            cash += TUTORIAL_BONUS
            rewarded = true
        }
        saveNow()
        _state.value = buildState()
        _events.tryEmit(GameEvent.TutorialDone(rewarded))
    }

    fun resetAll() {
        val f = saveFile()
        if (f.exists()) f.delete()
        holdings.clear(); upgradeLevels.clear(); favorites.clear(); orders.clear()
        achievementsUnlocked.clear()
        cash = START_CAPITAL_BASE; pendingDividends = 0.0; maxTotalAssets = START_CAPITAL_BASE
        managerLevel = 0; realizedPnLTotal = 0.0; totalTradeCount = 0
        tradesToday = 0; divClaimsToday = 0; divClaimTotal = 0; orderFilledTotal = 0
        themeMode = ThemeMode.SYSTEM; soundOn = true; vibrateOn = true
        pace = MarketPace.STANDARD
        tutorialDone = false; tutorialRewarded = false
        initFresh()
        _state.value = buildState()
    }

    // =====================================================================
    // 离线收益（上限 8 小时）
    // =====================================================================

    private fun maybeGrantOfflineEarnings(now: Long) {
        val gapSec = (now - lastSavedMs) / 1000.0
        if (gapSec < OFFLINE_MIN_SECONDS) return
        val capped = gapSec.coerceAtMost(OFFLINE_CAP_SECONDS)
        val divMult = Upgrades.dividendMult(upgradeLevels.getOrDefault("div", 0)) * (1.0 + 0.30 * managerLevel)
        val items = mutableListOf<Pair<String, Double>>()
        var total = 0.0
        for ((id, h) in holdings) {
            if (h.shares <= 1e-9) continue
            val st = stockMap[id] ?: continue
            val amt = h.shares * st.price * st.def.divPerSec * divMult * capped
            if (amt > 0.5) { items.add(st.def.name to amt); total += amt }
        }
        if (total > 0.5) {
            cash += total
            items.sortByDescending { it.second }
            _events.tryEmit(GameEvent.OfflineReportReady(OfflineReport(capped.toLong(), items, total)))
        }
    }

    // =====================================================================
    // 派生计算 / 快照构建
    // =====================================================================

    private fun computeTotalAssets(): Double {
        var pv = 0.0
        for ((id, h) in holdings) {
            val st = stockMap[id] ?: continue
            pv += h.shares * st.price
        }
        return cash + pv + pendingDividends
    }

    fun prestigeThreshold(): Double = PRESTIGE_BASE * Math.pow(8.0, managerLevel.toDouble())
    private fun startCapital(level: Int): Double = START_CAPITAL_BASE + START_CAPITAL_STEP * level

    private fun taskProgress(t: TaskState): Double = when (t.type) {
        TaskType.PROFIT_TODAY -> (max(0.0, computeTotalAssets() - dayBaseline) / t.target)
        TaskType.TRADES -> tradesToday / t.target
        TaskType.DIVIDEND_CLAIMS -> divClaimsToday / t.target
    }

    private fun buildState(): GameState {
        val total = computeTotalAssets()
        val unlocked = stockStates.filter { it.unlocked }
        val index = if (unlocked.isEmpty()) 0.0 else unlocked.sumOf { it.sessionChangePct } / unlocked.size
        val mood = (index / 2.0).coerceIn(-1.0, 1.0)
        val feeRate = Upgrades.feeRate(upgradeLevels.getOrDefault("fee", 0))
        val frozenByStock = orders.filter { !it.buy }.groupBy { it.stockId }
            .mapValues { (_, v) -> v.sumOf { it.frozenShares } }
        val orderCountByStock = orders.groupBy { it.stockId }.mapValues { (_, v) -> v.size }

        val stockUis = stockStates.map { st ->
            val h = holdings[st.def.id]
            StockUi(
                def = st.def,
                price = st.price,
                tickUp = st.tickChange > 1e-9,
                tickFlat = kotlin.math.abs(st.tickChange) <= 1e-9,
                sessionChangePct = st.sessionChangePct,
                dayChangePct = st.dayChangePct,
                history = st.history.toList(),
                volumes = st.volumes.toList(),
                locked = !st.unlocked,
                shares = h?.shares ?: 0.0,
                avgCost = h?.avgCost ?: 0.0,
                pnl = if (h != null) h.shares * st.price - h.costBasis else 0.0,
                favorite = st.def.id in favorites,
                hasEvent = st.eventUntilMs > 0 && st.eventDriftPerSec != 0.0,
                eventGood = st.eventGood,
                limitState = st.limitState,
                dayOpen = st.dayOpen,
                pendingOrders = orderCountByStock[st.def.id] ?: 0,
                frozenShares = frozenByStock[st.def.id] ?: 0.0,
            )
        }

        val upgradeUis = Upgrades.defs.map { d ->
            val lvl = upgradeLevels.getOrDefault(d.id, 0)
            val cost = if (lvl >= d.maxLevel) 0.0 else d.costAt(lvl)
            UpgradeUi(d, lvl, cost, availableCash() >= cost, lvl >= d.maxLevel)
        }

        val taskUis = tasks.tasks.map { t ->
            val p = taskProgress(t)
            TaskUi(
                title = t.title(dayBaseline), target = t.target,
                progress = p.coerceIn(0.0, 1.0), reward = t.reward,
                done = p >= 1.0, claimed = t.claimed,
            )
        }

        val orderUis = orders.map { o ->
            val st = stockMap[o.stockId]
            OrderUi(
                id = o.id,
                stockId = o.stockId,
                symbol = st?.def?.symbol ?: "",
                name = st?.def?.name ?: "",
                buy = o.buy,
                limitPrice = o.limitPrice,
                fraction = o.fraction,
                currentPrice = st?.price ?: 0.0,
                frozenCash = o.frozenCash,
                frozenShares = o.frozenShares,
            )
        }

        val achvUis = Achievements.defs.map { d ->
            AchievementUi(d, d.id in achievementsUnlocked)
        }

        val threshold = prestigeThreshold()
        return GameState(
            cash = cash,
            pendingDividends = pendingDividends,
            totalAssets = total,
            maxTotalAssets = maxTotalAssets,
            todayPnL = total - dayBaseline,
            stocks = stockUis,
            marketIndex = index,
            indexValue = indexValue,
            indexHistory = indexHistory.toList(),
            mood = mood,
            upgrades = upgradeUis,
            tasks = taskUis,
            achievements = achvUis,
            achievementUnlockedCount = achievementsUnlocked.size,
            orders = orderUis,
            managerLevel = managerLevel,
            prestigeThreshold = threshold,
            canPrestige = maxTotalAssets >= threshold,
            windowActive = TaskSystem.windowActive(),
            nextWindowMinutes = TaskSystem.nextWindowInMinutes(),
            activeEvent = activeEvent,
            hint = hint,
            feeRate = feeRate,
            dividendMult = Upgrades.dividendMult(upgradeLevels.getOrDefault("div", 0)) * (1.0 + 0.30 * managerLevel),
            dayKey = dayKey,
            totalTrades = totalTradeCount,
            realizedPnL = realizedPnLTotal,
            themeMode = themeMode,
            soundOn = soundOn,
            vibrateOn = vibrateOn,
            pace = pace,
            tutorialDone = tutorialDone,
            versionName = BuildConfig.VERSION_NAME,
        )
    }

    // =====================================================================
    // 存档（JSON 文件，原子写入；v2）
    // =====================================================================

    private fun saveFile(): File = File(filesDir, "save.json")

    fun saveNow() {
        try {
            lastSavedMs = System.currentTimeMillis()
            val root = JSONObject()
            root.put("v", SAVE_VERSION)
            root.put("cash", cash)
            root.put("pending", pendingDividends)
            root.put("maxAssets", maxTotalAssets)
            root.put("managerLevel", managerLevel)
            root.put("dayKey", dayKey)
            root.put("dayBaseline", dayBaseline)
            root.put("tradesToday", tradesToday)
            root.put("divClaimsToday", divClaimsToday)
            root.put("divClaimTotal", divClaimTotal)
            root.put("orderFilledTotal", orderFilledTotal)
            root.put("totalTrades", totalTradeCount)
            root.put("realized", realizedPnLTotal)
            root.put("lastSaved", lastSavedMs)
            root.put("themeMode", themeMode)
            root.put("soundOn", soundOn)
            root.put("vibrateOn", vibrateOn)
            root.put("pace", pace)
            root.put("tutorialDone", tutorialDone)
            root.put("tutorialRewarded", tutorialRewarded)
            root.put("orderIdSeq", orderIdSeq)

            val favArr = JSONArray()
            favorites.forEach { favArr.put(it) }
            root.put("favorites", favArr)

            val achArr = JSONArray()
            achievementsUnlocked.forEach { achArr.put(it) }
            root.put("achievements", achArr)

            val upObj = JSONObject()
            upgradeLevels.forEach { (k, v) -> upObj.put(k, v) }
            root.put("upgrades", upObj)

            val holdObj = JSONObject()
            for ((id, h) in holdings) {
                if (h.shares <= 1e-9) continue
                holdObj.put(id, JSONObject().put("s", h.shares).put("c", h.costBasis))
            }
            root.put("holdings", holdObj)

            val stockObj = JSONObject()
            for (st in stockStates) {
                val o = JSONObject()
                o.put("p", st.price)
                o.put("o", st.sessionOpen)
                o.put("d", st.dayOpen)
                val arr = JSONArray()
                st.history.takeLast(SAVED_HISTORY).forEach { arr.put(it) }
                o.put("h", arr)
                stockObj.put(st.def.id, o)
            }
            root.put("stocks", stockObj)

            val idxArr = JSONArray()
            indexHistory.takeLast(SAVED_HISTORY).forEach { idxArr.put(it) }
            root.put("index", idxArr)

            val orderArr = JSONArray()
            for (o in orders) {
                orderArr.put(
                    JSONObject()
                        .put("id", o.id).put("sid", o.stockId).put("b", o.buy)
                        .put("lp", o.limitPrice).put("f", o.fraction)
                        .put("fc", o.frozenCash).put("fs", o.frozenShares)
                        .put("at", o.createdAtMs)
                )
            }
            root.put("orders", orderArr)

            val taskObj = JSONObject()
            taskObj.put("day", tasks.dayKey)
            val claimedArr = JSONArray()
            tasks.tasks.forEachIndexed { i, t -> if (t.claimed) claimedArr.put(i) }
            taskObj.put("claimed", claimedArr)
            root.put("tasks", taskObj)

            val tmp = File(filesDir, "save.json.tmp")
            tmp.writeText(root.toString())
            val f = saveFile()
            if (f.exists()) f.delete()
            tmp.renameTo(f)
        } catch (_: Exception) {
            // 存档失败不打断游戏
        }
    }

    private fun load(): Boolean {
        return try {
            val f = saveFile()
            if (!f.exists()) return false
            val root = JSONObject(f.readText())
            cash = root.optDouble("cash", START_CAPITAL_BASE)
            pendingDividends = root.optDouble("pending", 0.0)
            maxTotalAssets = root.optDouble("maxAssets", cash)
            managerLevel = root.optInt("managerLevel", 0)
            dayKey = root.optString("dayKey", TaskSystem.todayKey())
            dayBaseline = root.optDouble("dayBaseline", cash)
            tradesToday = root.optInt("tradesToday", 0)
            divClaimsToday = root.optInt("divClaimsToday", 0)
            divClaimTotal = root.optInt("divClaimTotal", divClaimsToday)
            orderFilledTotal = root.optInt("orderFilledTotal", 0)
            totalTradeCount = root.optInt("totalTrades", 0)
            realizedPnLTotal = root.optDouble("realized", 0.0)
            lastSavedMs = root.optLong("lastSaved", System.currentTimeMillis())
            themeMode = root.optInt("themeMode", ThemeMode.SYSTEM)
            soundOn = root.optBoolean("soundOn", true)
            vibrateOn = root.optBoolean("vibrateOn", true)
            pace = root.optInt("pace", MarketPace.STANDARD)
            tutorialDone = root.optBoolean("tutorialDone", false)
            tutorialRewarded = root.optBoolean("tutorialRewarded", tutorialDone)
            orderIdSeq = root.optLong("orderIdSeq", 0L)

            favorites.clear()
            val favArr = root.optJSONArray("favorites") ?: JSONArray()
            for (i in 0 until favArr.length()) favorites.add(favArr.getString(i))

            achievementsUnlocked.clear()
            val achArr = root.optJSONArray("achievements") ?: JSONArray()
            for (i in 0 until achArr.length()) achievementsUnlocked.add(achArr.getString(i))

            upgradeLevels.clear()
            val upObj = root.optJSONObject("upgrades") ?: JSONObject()
            upObj.keys().forEach { k -> upgradeLevels[k] = upObj.optInt(k, 0) }

            holdings.clear()
            val holdObj = root.optJSONObject("holdings") ?: JSONObject()
            holdObj.keys().forEach { k ->
                val o = holdObj.getJSONObject(k)
                holdings[k] = HoldingState(o.optDouble("s", 0.0), o.optDouble("c", 0.0))
            }

            val stockObj = root.optJSONObject("stocks") ?: JSONObject()
            for (st in stockStates) {
                val o = stockObj.optJSONObject(st.def.id) ?: continue
                st.price = o.optDouble("p", st.def.basePrice).coerceAtLeast(0.01)
                st.prevPrice = st.price
                st.sessionOpen = o.optDouble("o", st.def.basePrice)
                st.dayOpen = o.optDouble("d", st.sessionOpen)
                st.history.clear()
                val arr = o.optJSONArray("h") ?: JSONArray()
                for (i in 0 until arr.length()) st.history.addLast(arr.optDouble(i, st.price))
                while (st.history.size > StockState.HISTORY_MAX) st.history.removeFirst()
                // 成交量历史不持久化：用占位补齐，图表显示为空柱
                repeat(st.history.size) { st.volumes.addLast(0.0) }
            }
            // 校正开/收盘基准
            stockStates.forEach {
                if (it.sessionOpen <= 1e-9) it.sessionOpen = it.def.basePrice
                if (it.dayOpen <= 1e-9) it.dayOpen = it.sessionOpen
            }

            indexHistory.clear()
            val idxArr = root.optJSONArray("index")
            if (idxArr != null) {
                for (i in 0 until idxArr.length()) indexHistory.addLast(idxArr.optDouble(i, INDEX_BASE))
            }
            if (indexHistory.size < 2) rebuildIndexHistory()
            indexValue = computeIndex()

            orders.clear()
            val orderArr = root.optJSONArray("orders")
            if (orderArr != null) {
                for (i in 0 until orderArr.length()) {
                    val o = orderArr.optJSONObject(i) ?: continue
                    val sid = o.optString("sid")
                    if (stockMap[sid] == null) continue
                    orders.add(
                        PendingOrder(
                            id = o.optLong("id", ++orderIdSeq),
                            stockId = sid,
                            buy = o.optBoolean("b", true),
                            limitPrice = o.optDouble("lp", 0.0),
                            fraction = o.optDouble("f", 0.5),
                            frozenCash = o.optDouble("fc", 0.0),
                            frozenShares = o.optDouble("fs", 0.0),
                            createdAtMs = o.optLong("at", System.currentTimeMillis()),
                        )
                    )
                }
            }

            maxTotalAssets = max(maxTotalAssets, computeTotalAssets())
            stockStates.forEach {
                it.unlocked = it.def.unlockAt <= maxTotalAssets + 1e-6
                it.unlockedAnnounced = true
            }

            tasks = TaskSystem.generate(dayKey, max(dayBaseline, 100.0))
            val taskObj = root.optJSONObject("tasks")
            if (taskObj != null && taskObj.optString("day") == dayKey) {
                val claimed = taskObj.optJSONArray("claimed") ?: JSONArray()
                for (i in 0 until claimed.length()) {
                    val idx = claimed.optInt(i, -1)
                    if (idx in tasks.tasks.indices) tasks.tasks[idx].claimed = true
                }
            }

            nextEventAtMs = System.currentTimeMillis() + EVENT_INTERVAL_BASE_MS
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val TICK_MS = 1000L
        const val SAVE_VERSION = 2
        const val START_CAPITAL_BASE = 10_000.0
        const val START_CAPITAL_STEP = 6_000.0
        const val PRESTIGE_BASE = 1_000_000.0
        const val REVERT_K = 2.5e-5
        const val OFFLINE_CAP_SECONDS = 8.0 * 3600.0
        const val OFFLINE_MIN_SECONDS = 120.0
        const val EVENT_INTERVAL_BASE_MS = 60_000L
        const val EVENT_INTERVAL_RAND_MS = 120_000L
        const val FIRST_EVENT_DELAY_MS = 25_000L
        const val SAVED_HISTORY = 720
        const val INDEX_BASE = 1000.0
        const val INDEX_HISTORY_MAX = 720
        const val VOLUME_BASE = 42_000.0
        const val REINVEST_PERIOD_MS = 30_000L
        const val TUTORIAL_BONUS = 3_000.0

        private val GOOD_TEMPLATES = listOf(
            "%S发布重磅新品，市场热议", "%S斩获大额订单", "机构集体上调%S评级",
            "%S业绩大超预期", "政策东风：%S所在板块迎风口", "%S核心技术取得突破",
        )
        private val BAD_TEMPLATES = listOf(
            "%S遭监管问询", "%S核心高管突然离职", "%S业绩暴雷，机构出逃",
            "行业寒潮：%S板块承压", "%S大股东计划减持", "%S主力工厂意外停产",
        )
        private val EARNINGS_GOOD = listOf(
            "%Q财报：营收超预期，毛利率创新高", "%Q财报：净利润翻倍，分红上调",
            "%Q财报：订单饱满，上调全年指引",
        )
        private val EARNINGS_BAD = listOf(
            "%Q财报：营收不及预期，股价承压", "%Q财报：计提大额减值", "%Q财报：亏损扩大，下调指引",
        )
        private val SECTOR_GOOD = listOf(
            "政策利好：%T板块迎来重大支持", "需求爆发：%T板块景气度飙升", "资金抢筹：%T板块全线走强",
        )
        private val SECTOR_BAD = listOf(
            "监管收紧：%T板块集体承压", "需求退潮：%T板块景气下行", "资金撤离：%T板块全线走弱",
        )

        fun formatDayKey(c: Calendar): String =
            String.format(Locale.CHINA, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))

        fun clampIndex(v: Double): Double = min(1.0, max(-1.0, v))
    }
}
