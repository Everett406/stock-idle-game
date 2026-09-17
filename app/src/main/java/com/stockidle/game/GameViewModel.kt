package com.stockidle.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockidle.game.game.GameEngine
import com.stockidle.game.game.GameEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 唯一的 ViewModel：包着 GameEngine，向 UI 暴露状态流与命令。
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = GameEngine(viewModelScope, app.filesDir)

    val state: StateFlow<com.stockidle.game.game.GameState> = engine.state
    val events: SharedFlow<GameEvent> = engine.events

    fun start() = engine.start()
    fun pause() = engine.pause()

    fun buy(stockId: String, fraction: Double) = engine.buy(stockId, fraction)
    fun sell(stockId: String, fraction: Double) = engine.sell(stockId, fraction)
    fun placeOrder(stockId: String, buy: Boolean, limitPrice: Double, fraction: Double) =
        engine.placeOrder(stockId, buy, limitPrice, fraction)
    fun cancelOrder(orderId: Long) = engine.cancelOrder(orderId)
    fun toggleFavorite(stockId: String) = engine.toggleFavorite(stockId)
    fun collectDividends() = engine.collectDividends()
    fun buyUpgrade(id: String) = engine.buyUpgrade(id)
    fun claimTask(index: Int) = engine.claimTask(index)
    fun prestige() = engine.prestige()

    fun setThemeMode(mode: Int) = engine.setThemeMode(mode)
    fun setSound(on: Boolean) = engine.setSound(on)
    fun setVibrate(on: Boolean) = engine.setVibrate(on)
    fun setPace(pace: Int) = engine.setPace(pace)
    fun setTutorialDone() = engine.setTutorialDone()
    fun resetAll() = engine.resetAll()
}
