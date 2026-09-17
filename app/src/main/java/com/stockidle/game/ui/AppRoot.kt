package com.stockidle.game.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockidle.game.GameViewModel
import com.stockidle.game.game.ActiveEvent
import com.stockidle.game.game.GameEvent
import com.stockidle.game.game.OfflineReport
import com.stockidle.game.game.ThemeMode
import com.stockidle.game.sound.SoundManager
import com.stockidle.game.ui.components.BreakingBanner
import com.stockidle.game.ui.components.CoinRain
import com.stockidle.game.ui.components.FloatingAmount
import com.stockidle.game.ui.components.MoodBackground
import com.stockidle.game.ui.components.OfflineReportOverlay
import com.stockidle.game.ui.components.PrestigeOverlay
import com.stockidle.game.ui.components.TradeBurst
import com.stockidle.game.ui.components.UnlockOverlay
import com.stockidle.game.ui.screens.AssetsScreen
import com.stockidle.game.ui.screens.DetailScreen
import com.stockidle.game.ui.screens.GrowthScreen
import com.stockidle.game.ui.screens.MarketScreen
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt

/**
 * 应用根：底部三 Tab + 详情覆盖层 + 全部特效覆盖层 + 事件消费中枢。
 */
@Composable
fun AppRoot(vm: GameViewModel) {
    val context = LocalContext.current
    val pal = LocalPal.current
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 导航
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var confirmPrestige by remember { mutableStateOf(false) }

    // 特效触发器（自增计数）
    var newsVisible by remember { mutableStateOf(false) }
    var collectTrigger by remember { mutableIntStateOf(0) }
    var burstTrigger by remember { mutableIntStateOf(0) }
    var burstGood by remember { mutableStateOf(true) }
    var floatTrigger by remember { mutableIntStateOf(0) }
    var floatText by remember { mutableStateOf("") }
    var unlockId by remember { mutableIntStateOf(0) }
    var unlockName by remember { mutableStateOf("") }
    var unlockSymbol by remember { mutableStateOf("") }
    var prestigeId by remember { mutableIntStateOf(0) }
    var prestigeOld by remember { mutableStateOf(0.0) }
    var prestigeLevel by remember { mutableIntStateOf(0) }
    var offlineReport by remember { mutableStateOf<OfflineReport?>(null) }

    val sound = remember { SoundManager(context) }
    DisposableEffect(Unit) { onDispose { sound.release() } }

    fun vibrate(ms: Long) {
        if (!state.vibrateOn) return
        try {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                mgr.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
        }
    }

    // 引擎事件消费：声音 / 震动 / 特效 / 提示
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is GameEvent.Trade -> {
                    if (ev.ok) {
                        burstGood = ev.buy
                        burstTrigger++
                        sound.play(SoundManager.Sfx.TRADE)
                    } else {
                        sound.play(SoundManager.Sfx.ERROR)
                    }
                    snackbarHostState.showSnackbar(ev.msg)
                }
                is GameEvent.DividendsCollected -> {
                    collectTrigger++
                    floatText = "+" + Fmt.money(ev.amount)
                    floatTrigger++
                    sound.play(SoundManager.Sfx.COIN)
                }
                is GameEvent.News -> {
                    newsVisible = true
                    sound.play(SoundManager.Sfx.NEWS)
                    vibrate(220)
                }
                is GameEvent.EventHint -> {
                    sound.play(SoundManager.Sfx.CLICK, 0.6f)
                    snackbarHostState.showSnackbar(
                        "📡 内幕雷达：" + ev.stockName + (if (ev.good) " 即将有利好" else " 即将有利空") + "（${ev.leadSeconds}秒后）"
                    )
                }
                is GameEvent.StockUnlocked -> {
                    unlockId++
                    unlockName = ev.name
                    unlockSymbol = state.stocks.firstOrNull { it.def.id == ev.stockId }?.def?.symbol ?: ""
                    sound.play(SoundManager.Sfx.UNLOCK)
                    vibrate(120)
                }
                is GameEvent.OfflineReportReady -> {
                    offlineReport = ev.report
                    sound.play(SoundManager.Sfx.COIN)
                }
                is GameEvent.PrestigeDone -> {
                    prestigeId++
                    prestigeOld = ev.oldTotal
                    prestigeLevel = ev.newLevel
                    sound.play(SoundManager.Sfx.PRESTIGE)
                    vibrate(400)
                }
                is GameEvent.UpgradeBought -> {
                    if (ev.newLevel > 0) {
                        sound.play(SoundManager.Sfx.CLICK)
                        snackbarHostState.showSnackbar("升级成功")
                    } else {
                        sound.play(SoundManager.Sfx.ERROR)
                        snackbarHostState.showSnackbar("现金不足")
                    }
                }
                is GameEvent.TaskClaimed -> {
                    collectTrigger++
                    sound.play(SoundManager.Sfx.COIN)
                    snackbarHostState.showSnackbar("任务完成：${ev.title}，奖励 " + Fmt.money(ev.reward))
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        MoodBackground(state.mood, Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("股海拾金", color = pal.gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("  单机放置 · 模拟市场", color = pal.textDim, fontSize = 10.sp)
                    Box(Modifier.weight(1f))
                    Text(
                        "⚙️",
                        fontSize = 17.sp,
                        modifier = Modifier
                            .clickable { showSettings = true }
                            .padding(6.dp),
                    )
                }
            },
            bottomBar = {
                NavigationBar(containerColor = pal.surface) {
                    val tabs = listOf("行情", "资产", "成长")
                    tabs.forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Text(listOf("📈", "💼", "🚀")[i], fontSize = 18.sp) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedTextColor = pal.gold,
                                indicatorColor = pal.goldDim,
                            ),
                        )
                    }
                }
            },
        ) { pad ->
            Box(Modifier.padding(pad)) {
                Crossfade(targetState = tab, label = "tabCross") { t ->
                    when (t) {
                        0 -> MarketScreen(
                            state = state,
                            onOpenDetail = { detailId = it },
                            onToggleFavorite = vm::toggleFavorite,
                        )
                        1 -> AssetsScreen(
                            state = state,
                            onCollectDividends = vm::collectDividends,
                            onOpenDetail = { detailId = it },
                        )
                        else -> GrowthScreen(
                            state = state,
                            onBuyUpgrade = vm::buyUpgrade,
                            onClaimTask = vm::claimTask,
                            onPrestige = { confirmPrestige = true },
                        )
                    }
                }
            }
        }

        // 突发新闻横幅（使用引擎内的真实事件数据）
        Box(Modifier.align(Alignment.TopCenter)) {
            BreakingBanner(
                event = if (newsVisible) state.activeEvent else null,
                onDismiss = { newsVisible = false },
            )
        }

        // 金币雨 / 成交粒子 / 飘字
        CoinRain(trigger = collectTrigger, modifier = Modifier.fillMaxSize())
        TradeBurst(trigger = burstTrigger, good = burstGood, modifier = Modifier.fillMaxSize())
        FloatingAmount(
            trigger = floatTrigger,
            text = floatText,
            color = pal.gold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 110.dp),
        )

        // 详情页（覆盖整屏，含底栏；带滑入滑出动画）
        val shownDetail = remember { mutableStateOf<String?>(null) }
        if (detailId != null) shownDetail.value = detailId
        androidx.compose.animation.AnimatedVisibility(
            visible = detailId != null,
            enter = androidx.compose.animation.slideInHorizontally { it } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutHorizontally { it } + androidx.compose.animation.fadeOut(),
        ) {
            val id = shownDetail.value
            if (id != null) {
                DetailScreen(
                    state = state,
                    stockId = id,
                    onBack = { detailId = null },
                    onBuy = vm::buy,
                    onSell = vm::sell,
                )
            }
        }
        androidx.activity.compose.BackHandler(enabled = detailId != null) { detailId = null }

        // 解锁特效
        UnlockOverlay(
            triggerId = unlockId,
            stockName = unlockName,
            symbol = unlockSymbol,
            modifier = Modifier.fillMaxSize(),
        )

        // 离线结算
        OfflineReportOverlay(
            report = offlineReport,
            onClaim = {
                offlineReport = null
                collectTrigger++
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 转生特效
        PrestigeOverlay(
            triggerId = prestigeId,
            oldTotal = prestigeOld,
            newLevel = prestigeLevel,
            modifier = Modifier.fillMaxSize(),
        )
    }

    // 设置弹窗
    if (showSettings) {
        SettingsDialog(
            themeMode = state.themeMode,
            soundOn = state.soundOn,
            vibrateOn = state.vibrateOn,
            version = state.versionName,
            onTheme = vm::setThemeMode,
            onSound = vm::setSound,
            onVibrate = vm::setVibrate,
            onReset = {
                vm.resetAll()
                showSettings = false
            },
            onDismiss = { showSettings = false },
        )
    }

    // 转生确认
    if (confirmPrestige) {
        AlertDialog(
            onDismissRequest = { confirmPrestige = false },
            title = { Text("确认转生？") },
            text = {
                Text(
                    "转生将清空现金、持仓与待领分红，股价重置。\n" +
                        "你将获得：经理 Lv." + (state.managerLevel + 1) + "，永久分红 +30%，初始资金提升至 " +
                        Fmt.money(10000.0 + 6000.0 * (state.managerLevel + 1)) + "。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmPrestige = false
                    vm.prestige()
                }) { Text("转生", color = pal.gold, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmPrestige = false }) { Text("再等等") }
            },
        )
    }
}

@Composable
private fun SettingsDialog(
    themeMode: Int,
    soundOn: Boolean,
    vibrateOn: Boolean,
    version: String,
    onTheme: (Int) -> Unit,
    onSound: (Boolean) -> Unit,
    onVibrate: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pal = LocalPal.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = pal.surface,
        title = { Text("设置", color = pal.text, fontWeight = FontWeight.Bold) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text("主题", color = pal.textDim, fontSize = 12.sp)
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    val modes = listOf("跟随系统" to ThemeMode.SYSTEM, "亮色" to ThemeMode.LIGHT, "暗色" to ThemeMode.DARK)
                    modes.forEach { (label, m) ->
                        val selected = themeMode == m
                        Box(
                            Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(if (selected) pal.gold else pal.surfaceHigh)
                                .clickable { onTheme(m) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                label,
                                color = if (selected) Color(0xFF2B1D00) else pal.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                SettingSwitch("音效", soundOn, onSound)
                SettingSwitch("震动反馈", vibrateOn, onVibrate)
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 4.dp))
                Text(
                    "股海拾金 v$version · 单机游戏，存档保存在本机",
                    color = pal.textDim,
                    fontSize = 11.sp,
                )
                Text(
                    "⚠️ 重置存档",
                    color = pal.up,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clickable { onReset() },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val pal = LocalPal.current
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = pal.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
