package com.stockidle.game.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockidle.game.game.GameState
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt

/**
 * 成长页：限时事件 / 每日任务 / 升级 / 转生。
 */
@Composable
fun GrowthScreen(
    state: GameState,
    onBuyUpgrade: (String) -> Unit,
    onClaimTask: (Int) -> Unit,
    onPrestige: () -> Unit,
) {
    val pal = LocalPal.current
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 限时事件
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (state.windowActive) pal.goldDim else pal.surface)
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏱", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "限时事件 · 波动翻倍时段",
                        color = pal.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (state.windowActive) "进行中" else "还有 ${state.nextWindowMinutes} 分钟",
                        color = if (state.windowActive) pal.gold else pal.textDim,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    "每天 12:00-13:00 与 20:00-21:00，全市场波动率 ×2，差价空间更大",
                    color = pal.textDim,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // 每日任务
        item {
            Text(
                "每日任务",
                color = pal.textDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
        taskItems(state, onClaimTask)

        // 升级
        item {
            Text(
                "永久升级",
                color = pal.textDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
        itemsUpgrades(state, onBuyUpgrade)

        // 转生
        item {
            PrestigeCard(state, onPrestige)
            Spacer(Modifier.height(90.dp))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.taskItems(
    state: GameState,
    onClaim: (Int) -> Unit,
) {
    state.tasks.forEachIndexed { i, t ->
        item(key = "task_$i") {
            TaskRow(
                title = t.title,
                progress = t.progress.toFloat(),
                reward = t.reward,
                done = t.done,
                claimed = t.claimed,
                onClaim = { onClaim(i) },
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsUpgrades(
    state: GameState,
    onBuyUpgrade: (String) -> Unit,
) {
    state.upgrades.forEach { u ->
        item(key = "up_" + u.def.id) {
            UpgradeRow(
                icon = u.def.icon,
                name = u.def.name,
                desc = u.def.desc,
                level = u.level,
                maxLevel = u.def.maxLevel,
                cost = u.cost,
                affordable = u.affordable,
                maxed = u.maxed,
                onBuy = { onBuyUpgrade(u.def.id) },
            )
        }
    }
}

@Composable
private fun TaskRow(
    title: String,
    progress: Float,
    reward: Double,
    done: Boolean,
    claimed: Boolean,
    onClaim: () -> Unit,
) {
    val pal = LocalPal.current
    val animProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "taskP",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pal.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = pal.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    "奖励 " + Fmt.money(reward),
                    color = pal.gold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animProgress },
                color = if (done) pal.gold else pal.teal,
                trackColor = pal.surfaceHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${(progress * 100).toInt()}%",
                color = pal.textDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onClaim,
            enabled = done && !claimed,
            colors = ButtonDefaults.buttonColors(
                containerColor = pal.gold,
                contentColor = Color(0xFF2B1D00),
            ),
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.height(34.dp),
        ) {
            Text(if (claimed) "已领取" else "领取", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UpgradeRow(
    icon: String,
    name: String,
    desc: String,
    level: Int,
    maxLevel: Int,
    cost: Double,
    affordable: Boolean,
    maxed: Boolean,
    onBuy: () -> Unit,
) {
    val pal = LocalPal.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pal.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(pal.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) { Text(icon, fontSize = 18.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = pal.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text("Lv.$level/$maxLevel", color = pal.teal, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Text(desc, color = pal.textDim, fontSize = 11.sp)
        }
        Button(
            onClick = onBuy,
            enabled = !maxed && affordable,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (maxed) pal.surfaceHigh else pal.gold,
                contentColor = if (maxed) pal.textDim else Color(0xFF2B1D00),
            ),
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.height(36.dp),
        ) {
            Text(
                if (maxed) "已满级" else Fmt.money(cost),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PrestigeCard(state: GameState, onPrestige: () -> Unit) {
    val pal = LocalPal.current
    val progress = if (state.prestigeThreshold <= 0) 1f
    else (state.maxTotalAssets / state.prestigeThreshold).toFloat().coerceIn(0f, 1f)
    val animProgress by animateFloatAsState(progress, tween(600), label = "prestigeP")

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(pal.goldDim, pal.surface),
                )
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔄 转生", color = pal.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(pal.gold.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("经理 Lv." + state.managerLevel, color = pal.gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "重置为初始资金，换取永久加成：分红 +30%/级 · 初始资金 +¥6000/级",
            color = pal.textDim,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { animProgress },
            color = pal.gold,
            trackColor = pal.surfaceHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Row {
            Text("历史最高 " + Fmt.money(state.maxTotalAssets), color = pal.textDim, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text(
                "门槛 " + Fmt.money(state.prestigeThreshold),
                color = pal.gold,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onPrestige,
            enabled = state.canPrestige,
            colors = ButtonDefaults.buttonColors(containerColor = pal.gold, contentColor = Color(0xFF2B1D00)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            Text(
                if (state.canPrestige) "立即转生" else "总资产达到门槛后可转生",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
