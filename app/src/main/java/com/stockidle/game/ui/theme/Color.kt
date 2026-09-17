package com.stockidle.game.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 配色体系 v0.2（与 GDD「视觉规范」一致）：
 * - 暗色：深色金融终端风，近黑底 + 金色、青绿点缀
 * - 亮色：v0.2 重做 —— 专业证券行情软件风格：
 *     浅灰底区分层级、白卡细灰线、沉稳正红/深绿、克制用金
 * - 通用：红涨绿跌（中国市场习惯）、等宽数字
 */

// ---------- 暗色主题 ----------
val DarkBg = Color(0xFF0A0D12)
val DarkSurface = Color(0xFF10151F)
val DarkSurfaceHigh = Color(0xFF181F2D)
val DarkBorder = Color(0xFF27324A)
val DarkText = Color(0xFFE8ECF1)
val DarkTextDim = Color(0xFF8A94A6)
val DarkGold = Color(0xFFF0C05A)
val DarkTeal = Color(0xFF2DD4BF)
val DarkUp = Color(0xFFF6465D)      // 红涨
val DarkDown = Color(0xFF26A69A)    // 绿跌
val DarkUpDim = Color(0x2EF6465D)
val DarkDownDim = Color(0x2E26A69A)
val DarkGoldDim = Color(0x24F0C05A)

// ---------- 亮色主题（v0.2 重做：专业行情软件风） ----------
val LightBg = Color(0xFFF3F4F6)           // 浅灰底，衬出白卡层级
val LightSurface = Color(0xFFFFFFFF)      // 纯白卡片
val LightSurfaceHigh = Color(0xFFE9EBEE)  // 强调面/选中态
val LightBorder = Color(0xFFD2D7DE)       // 清晰细线
val LightBorderStrong = Color(0xFFB7BEC8) // 强分割线（顶栏/底栏）
val LightText = Color(0xFF16191F)         // 近黑主文字
val LightTextDim = Color(0xFF667080)      // 次级文字
val LightGold = Color(0xFF9E6E00)         // 深琥珀：亮色下按钮/强调可读
val LightTeal = Color(0xFF0A7C6B)
val LightUp = Color(0xFFD3222B)           // 沉稳正红（涨）
val LightDown = Color(0xFF008A57)         // 深绿（跌）
val LightUpDim = Color(0x14D3222B)
val LightDownDim = Color(0x12008A57)
val LightGoldDim = Color(0x149E6E00)

/** 涨跌闪烁用的临时高亮色 */
val FlashUp = Color(0xFFF6465D)
val FlashDown = Color(0xFF26A69A)
