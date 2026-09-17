package com.stockidle.game.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 配色体系（与 GDD「视觉规范」一致）：
 * - 暗色：深色金融终端风，黑/深蓝底 + 金色、青绿点缀
 * - 亮色：干净白底 + 证券红绿
 * - 通用：红涨绿跌（中国市场习惯）
 */

// ---------- 暗色主题 ----------
val DarkBg = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF111826)
val DarkSurfaceHigh = Color(0xFF1A2334)
val DarkBorder = Color(0xFF233047)
val DarkText = Color(0xFFE8ECF1)
val DarkTextDim = Color(0xFF8A94A6)
val DarkGold = Color(0xFFF5C451)
val DarkTeal = Color(0xFF2DD4BF)
val DarkUp = Color(0xFFF6465D)      // 红涨
val DarkDown = Color(0xFF26A69A)    // 绿跌
val DarkUpDim = Color(0x33F6465D)
val DarkDownDim = Color(0x3326A69A)
val DarkGoldDim = Color(0x26F5C451)

// ---------- 亮色主题 ----------
val LightBg = Color(0xFFF7F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceHigh = Color(0xFFF0F2F5)
val LightBorder = Color(0xFFE3E6EB)
val LightText = Color(0xFF1B2230)
val LightTextDim = Color(0xFF7A8494)
val LightGold = Color(0xFFC89211)
val LightTeal = Color(0xFF0E9488)
val LightUp = Color(0xFFE0302F)     // 红涨
val LightDown = Color(0xFF009E7A)   // 绿跌
val LightUpDim = Color(0x1AE0302F)
val LightDownDim = Color(0x1A009E7A)
val LightGoldDim = Color(0x1AC89211)

/** 涨跌闪烁用的临时高亮色 */
val FlashUp = Color(0xFFF6465D)
val FlashDown = Color(0xFF26A69A)
