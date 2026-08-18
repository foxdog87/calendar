package com.foxdog.strucalendar.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * タップ時のバウンス＆ショックウェーブ演出
 * @param waveExpansionSize 波紋が外側に広がる距離（デフォルト24.dp）
 */
fun Modifier.bounceClick(
    bounceScale: Float = 0.9f,
    showWave: Boolean = false,
    isWaveCircle: Boolean = true,
    waveCornerRadius: Dp = 0.dp,
    waveExpansionSize: Dp = 24.dp, // ★ 追加：広がる幅を調整可能に
    onClick: () -> Unit
) = composed {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    val waveExpansion = remember { Animatable(0f) }
    val waveAlpha = remember { Animatable(0f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val targetExpansionPx = with(density) { waveExpansionSize.toPx() } // ★ 追加した引数を使用

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .drawWithContent {
            drawContent()
            if (showWave && waveAlpha.value > 0f) {
                val strokeWidth = 3.dp.toPx()
                val expansion = waveExpansion.value

                if (isWaveCircle) {
                    drawCircle(
                        color = primaryColor,
                        radius = (size.maxDimension / 2) + expansion,
                        alpha = waveAlpha.value,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    drawRoundRect(
                        color = primaryColor,
                        size = Size(size.width + expansion * 2, size.height + expansion * 2),
                        topLeft = Offset(-expansion, -expansion),
                        cornerRadius = CornerRadius(waveCornerRadius.toPx() + expansion / 2),
                        alpha = waveAlpha.value,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                scope.launch {
                    launch {
                        scale.animateTo(bounceScale, animationSpec = tween(durationMillis = 50))
                        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                    }
                    if (showWave) {
                        launch {
                            waveExpansion.snapTo(0f)
                            waveAlpha.snapTo(0.8f)

                            launch {
                                waveExpansion.animateTo(
                                    targetValue = targetExpansionPx,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                )
                            }
                            launch {
                                waveAlpha.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(300, easing = LinearEasing)
                                )
                            }
                        }
                    }
                }
                onClick()
            }
        )
}