package com.stockidle.game.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * v0.2 视觉规范：硬朗金融终端风。
 *
 * 全局弱化圆角：
 *  - 卡片 / 按钮 / 输入：直角（0dp）
 *  - 小徽章 / 角标：2dp（仅用于尺寸 < 24dp 的微型元素，肉眼近似直角）
 * 层级靠「1dp 细边框 + 表面色差」表达，不靠圆角和阴影。
 */
object AppShape {
    val Card = RoundedCornerShape(0.dp)
    val Button = RoundedCornerShape(0.dp)
    val Chip = RoundedCornerShape(0.dp)
    val Dialog = RoundedCornerShape(0.dp)
    val Mini = RoundedCornerShape(2.dp)
}

/**
 * 边框宽度规范：所有卡片 1dp 细线；强调元素（事件/转生）1.5dp。
 */
object AppStroke {
    const val Thin = 1f      // dp
    const val Strong = 1.5f
}
