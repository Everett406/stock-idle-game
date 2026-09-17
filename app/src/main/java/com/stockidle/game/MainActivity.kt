package com.stockidle.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider
import com.stockidle.game.ui.AppRoot
import com.stockidle.game.ui.theme.StockIdleTheme
import com.stockidle.game.ui.theme.ThemeModeConst

class MainActivity : ComponentActivity() {

    private lateinit var vm: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)[GameViewModel::class.java]
        enableEdgeToEdge()

        setContent {
            val state by vm.state.collectAsState()
            val darkTheme = when (state.themeMode) {
                ThemeModeConst.LIGHT -> false
                ThemeModeConst.DARK -> true
                else -> isSystemInDarkTheme()
            }

            // 状态栏图标颜色跟随应用主题（而非系统主题）
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = if (darkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                )
            }

            StockIdleTheme(themeMode = state.themeMode) {
                AppRoot(vm = vm)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.start()
    }

    override fun onStop() {
        vm.pause()
        super.onStop()
    }
}
