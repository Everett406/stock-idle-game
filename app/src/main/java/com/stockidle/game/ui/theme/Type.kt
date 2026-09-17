package com.stockidle.game.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 排版：正文用系统默认无衬线，数字统一等宽字体（金融终端习惯，跳动的数字更稳定）。
 */
val NumStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
)

val AppTypography = Typography()
