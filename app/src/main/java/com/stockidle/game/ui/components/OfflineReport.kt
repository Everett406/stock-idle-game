package com.stockidle.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockidle.game.game.OfflineReport
import com.stockidle.game.ui.theme.LocalPal
import com.stockidle.game.util.Fmt
import kotlin.math.pow

/**
 * 离线收益结算：全屏报告从底部滑入，
 * 各收益项数字逐项跳动累加，总额最后大跳 + 金光，然后金币雨收尾。
 * 这是玩家“回游”的情绪高潮。
 */
@Composable
fun OfflineReportOverlay(
    report: OfflineReport?,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    // 每一项的已“滚出”金额与阶段
    val itemProgress = remember { mutableStateOf(0) }      // 当前滚到第几项
    val itemValues = remember { mutableStateOf(listOf<Double>()) }
    val totalDone = remember { mutableStateOf(false) }
    val totalValue = remember { mutableStateOf(0.0) }
    val glow = remember { mutableStateOf(0f) }

    LaunchedEffect(report?.seconds, report?.total) {
        val r = report ?: return@LaunchedEffect
        itemProgress.value = 0
        itemValues.value = List(r.items.size) { 0.0 }
        totalDone.value = false
        totalValue.value = 0.0
        glow.value = 0f

        suspend fun countTo(target: Double, durMs: Long, onTick: (Double) -> Unit) {
            val start = withFrameNanos { it }
            var t = 0.0
            while (t < 1.0) {
                withFrameNanos { now ->
                    t = ((now - start) / 1e6 / durMs).coerceIn(0.0, 1.0)
                    val e = 1.0 - (1.0 - t).pow(3.0)
                    onTick(target * e)
                }
            }
            onTick(target)
        }

        r.items.forEachIndexed { i, (_, amt) ->
            kotlinx.coroutines.delay(260)
            countTo(amt, 480) { v ->
                val list = itemValues.value.toMutableList()
                list[i] = v
                itemValues.value = list
            }
            itemProgress.value = i + 1
        }
        kotlinx.coroutines.delay(200)
        countTo(r.total, 620) { totalValue.value = it }
        totalDone.value = true
        // 金光脉冲
        val start2 = withFrameNanos { it }
        var t2 = 0.0
        while (t2 < 1.0) {
            withFrameNanos { now ->
                t2 = ((now - start2) / 1e6 / 900.0).coerceIn(0.0, 1.0)
                glow.value = (1.0 - t2).toFloat()
            }
        }
    }

    AnimatedVisibility(
        visible = report != null,
        enter = fadeIn(tween(150)) + slideInVertically(tween(420)) { it },
        exit = fadeOut(tween(260)) + slideOutVertically(tween(320)) { it },
        modifier = modifier,
    ) {
        val r = report ?: return@AnimatedVisibility
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(pal.surface)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("离线收益报告", color = pal.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    "您离开 ${Fmt.duration(r.seconds)}，持仓持续为您赚取分红",
                    color = pal.textDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(14.dp))

                r.items.forEachIndexed { i, (name, amt) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(6.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(pal.gold.copy(alpha = if (itemProgress.value > i) 1f else 0.25f)),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(name, color = pal.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            "+" + Fmt.money(itemValues.value.getOrNull(i) ?: 0.0),
                            color = if (itemProgress.value > i) pal.gold else pal.textDim,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                // 合计行（金光）
                Box {
                    if (glow.value > 0.01f) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(pal.gold.copy(alpha = 0.45f * glow.value), Color.Transparent),
                                    )
                                ),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("分红合计", color = pal.textDim, fontSize = 12.sp)
                        Text(
                            "+" + Fmt.money(totalValue.value),
                            color = if (totalDone.value) pal.gold else pal.text,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.graphicsLayer {
                                val s = if (totalDone.value) 1.12f else 1f
                                scaleX = s; scaleY = s
                            },
                        )
                    }
                }
                Text(
                    "已自动存入现金账户",
                    color = pal.textDim,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = pal.gold, contentColor = Color(0xFF2B1D00)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("收 下", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
