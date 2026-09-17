package com.stockidle.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockidle.game.ui.theme.AppShape
import com.stockidle.game.ui.theme.LocalPal

/**
 * v0.2 新手引导：5 步全屏教学（首次启动自动弹出，完成后可在设置中重看）。
 * 硬朗终端风：黑遮罩 + 直角卡片 + 步骤指示。
 */
object TutorialSteps {
    data class Step(
        val icon: String,          // AppIcons key
        val title: String,
        val lines: List<String>,
        val cta: String,
        val jumpTab: Int?,         // 需要切到的 tab（null=不切）
    )

    val steps = listOf(
        Step(
            icon = "chart",
            title = "欢迎来到《股海拾金》",
            lines = listOf(
                "这是一款单机放置炒股游戏：市场 24 小时持续模拟，你随时来、随时走。",
                "目标很简单——低买高卖赚差价，持仓收分红，把总资产做大。",
                "总资产达标后可以「转生」，换取永久加成，无限变强。",
            ),
            cta = "开始学习",
            jumpTab = null,
        ),
        Step(
            icon = "timeline",
            title = "行情页 · 看懂市场",
            lines = listOf(
                "顶部滚动条是实时行情；卡片右侧的迷你折线是近期走势。",
                "红 = 上涨，绿 = 下跌（中国市场习惯），颜色边框实时闪烁。",
                "名字旁边有金色圆点 = 你持有这只股票。点击卡片进入交易。",
                "未解锁的股票会显示解锁条件，总资产达标自动解锁。",
            ),
            cta = "去行情页看看",
            jumpTab = 0,
        ),
        Step(
            icon = "order",
            title = "买入 · 两种方式",
            lines = listOf(
                "进入股票详情页：选比例（25%~全仓），点「买入」立即成交。",
                "想等低价？用「限价委托」：挂一个目标价，跌到就自动买入。",
                "卖出同理：挂高价，涨到自动卖出。委托会冻结对应资金/持仓，可随时撤销。",
                "新手建议：先用 25% 仓位买「稳盛银行」，感受价格跳动。",
            ),
            cta = "下一步",
            jumpTab = 0,
        ),
        Step(
            icon = "coin",
            title = "分红 · 睡后收入",
            lines = listOf(
                "只要持仓，每秒都在累计分红（资产页显示「待领取分红」）。",
                "关掉游戏也在赚！离线收益 = 离线时长 × 持仓市值 × 分红率，上限 8 小时。",
                "下次打开会弹出离线结算报告，一次性到账。",
                "每天 12:00-13:00 和 20:00-21:00 是波动翻倍时段，差价更好做。",
            ),
            cta = "下一步",
            jumpTab = 1,
        ),
        Step(
            icon = "trophy",
            title = "成长 · 越滚越大",
            lines = listOf(
                "「成长页」有三样宝贝：每日任务、永久升级、成就奖励。",
                "升级：手续费折扣 / 分红倍率 / 量化机器人 / 内幕雷达等。",
                "总资产到 ¥100万 可「转生」：清空重来，换经理等级永久加成。",
                "节奏参考：首笔交易 1 分钟，1 天解锁 5 只股票，1 周可转生。",
            ),
            cta = "领 ¥3000 启动补贴，开始炒股",
            jumpTab = 2,
        ),
    )
}

@Composable
fun TutorialOverlay(
    visible: Boolean,
    stepIndex: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    val step = TutorialSteps.steps.getOrNull(stepIndex) ?: return

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .background(pal.surface)
                    .border(1.dp, pal.border, AppShape.Card)
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcons.Icon(kind = step.icon, color = pal.gold, size = 26.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        step.title,
                        color = pal.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${stepIndex + 1}/${TutorialSteps.steps.size}",
                        color = pal.textDim,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                }
                Spacer(Modifier.height(12.dp))
                step.lines.forEach { line ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Box(
                            Modifier
                                .padding(top = 7.dp)
                                .width(8.dp)
                                .height(2.dp)
                                .background(pal.gold),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(line, color = pal.text, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "跳过引导",
                        color = pal.textDim,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { onSkip() }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pal.gold,
                            contentColor = Color(0xFF2B1D00),
                        ),
                        shape = AppShape.Button,
                        modifier = Modifier.height(44.dp),
                    ) {
                        Text(step.cta, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                // 步骤指示条
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TutorialSteps.steps.forEachIndexed { i, _ ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(if (i <= stepIndex) pal.gold else pal.surfaceHigh),
                        )
                    }
                }
            }
        }
    }
}
