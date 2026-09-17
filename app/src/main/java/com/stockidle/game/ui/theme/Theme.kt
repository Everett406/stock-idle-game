package com.stockidle.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * 扩展色板：Material 3 之外游戏需要的语义色（涨跌/金币/氛围）。
 */
class Pal(
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val border: Color,
    val text: Color,
    val textDim: Color,
    val gold: Color,
    val teal: Color,
    val up: Color,
    val down: Color,
    val upDim: Color,
    val downDim: Color,
    val goldDim: Color,
    val isDark: Boolean,
)

private val darkPal = Pal(
    bg = DarkBg, surface = DarkSurface, surfaceHigh = DarkSurfaceHigh, border = DarkBorder,
    text = DarkText, textDim = DarkTextDim, gold = DarkGold, teal = DarkTeal,
    up = DarkUp, down = DarkDown, upDim = DarkUpDim, downDim = DarkDownDim,
    goldDim = DarkGoldDim, isDark = true,
)

private val lightPal = Pal(
    bg = LightBg, surface = LightSurface, surfaceHigh = LightSurfaceHigh, border = LightBorder,
    text = LightText, textDim = LightTextDim, gold = LightGold, teal = LightTeal,
    up = LightUp, down = LightDown, upDim = LightUpDim, downDim = LightDownDim,
    goldDim = LightGoldDim, isDark = false,
)

val LocalPal = staticCompositionLocalOf { lightPal }

private val DarkScheme = darkColorScheme(
    primary = DarkGold,
    onPrimary = Color(0xFF1A1400),
    secondary = DarkTeal,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextDim,
    outline = DarkBorder,
    error = DarkUp,
)

private val LightScheme = lightColorScheme(
    primary = LightGold,
    onPrimary = Color.White,
    secondary = LightTeal,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextDim,
    outline = LightBorder,
    error = LightUp,
)

@Composable
fun StockIdleTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeModeConst.LIGHT -> false
        ThemeModeConst.DARK -> true
        else -> isSystemInDarkTheme()
    }
    val pal = if (dark) darkPal else lightPal
    CompositionLocalProvider(LocalPal provides pal) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

/** 避免与 game.ThemeMode 常量类混淆的本地常量 */
object ThemeModeConst {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}
