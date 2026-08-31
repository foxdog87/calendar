package com.foxdog.strucalendar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class SpotlightShape { ROUNDED_RECT, PILL, OVAL }

data class SpotlightStep(
    val targetKey: String,
    val title: String,
    val description: String,
    val shape: SpotlightShape = SpotlightShape.ROUNDED_RECT,
    val highlightCornerRadius: Dp = 12.dp,
    val highlightPadding: Dp = 6.dp
)

private enum class OnboardingPhase { IDLE, INTRO, STEP }

/**
 * スポットライト型オンボーディング。
 */
@Composable
fun SpotlightOnboardingOverlay(
    steps: List<SpotlightStep>,
    targetRects: Map<String, Rect>,
    introTitle: String = "ようこそ！",
    introDescription: String = "使い方を簡単にご紹介します。",
    onSkip: () -> Unit,
    onShowLater: () -> Unit,
    onFinish: () -> Unit,
    onStepShown: (SpotlightStep) -> Unit = {} // ステップ表示時に通知するコールバック
) {
    var phase by remember { mutableStateOf(OnboardingPhase.IDLE) }
    var stepIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(500)
        phase = OnboardingPhase.INTRO
    }

    // STEPフェーズになったら、現在のステップを通知する
    LaunchedEffect(phase, stepIndex) {
        if (phase == OnboardingPhase.STEP) {
            steps.getOrNull(stepIndex)?.let { step ->
                onStepShown(step)
            }
        }
    }

    AnimatedVisibility(
        visible = phase != OnboardingPhase.IDLE,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(150))
    ) {
        when (phase) {
            OnboardingPhase.INTRO -> {
                IntroScrim(
                    title = introTitle,
                    description = introDescription,
                    onStart = { phase = OnboardingPhase.STEP; stepIndex = 0 },
                    onSkip = onSkip
                )
            }

            OnboardingPhase.STEP -> {
                val step = steps.getOrNull(stepIndex)
                val rect = step?.let { targetRects[it.targetKey] }
                if (step != null && rect != null) {
                    StepScrim(
                        step = step,
                        rect = rect,
                        stepIndex = stepIndex,
                        stepCount = steps.size,
                        isLastStep = stepIndex == steps.lastIndex,
                        onAdvance = {
                            if (stepIndex < steps.lastIndex) stepIndex++ else onFinish()
                        },
                        onSkip = onSkip,
                        onShowLater = onShowLater,
                        onFinish = onFinish
                    )
                }
            }

            OnboardingPhase.IDLE -> Unit
        }
    }
}

// ============================================================
// 導入カード（スポットライトなし、全面暗転＋中央カード）
// ============================================================
@Composable
private fun IntroScrim(
    title: String,
    description: String,
    onStart: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            // 空のタップ判定を置くことで、背面の画面へのタップをブロックする
            .pointerInput(Unit) { detectTapGestures {} },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("はじめる")
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onSkip) {
                    Text("スキップ", fontSize = 12.sp)
                }
            }
        }
    }
}

// ============================================================
// ステップ表示（暗転＋くり抜き＋説明カード）
// ============================================================
@Composable
private fun StepScrim(
    step: SpotlightStep,
    rect: Rect,
    stepIndex: Int,
    stepCount: Int,
    isLastStep: Boolean,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    onShowLater: () -> Unit,
    onFinish: () -> Unit
) {
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp

    val targetIsInLowerHalf = with(density) { rect.center.y.toDp().value } > screenHeightDp / 2f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(stepIndex) {
                // isLastStep の判定を detectTapGestures の「中」に移動する。
                // こうすることで、全ステップでタップを消費しつつ、最終ステップ以外は onAdvance() が呼ばれる。
                detectTapGestures {
                    if (!isLastStep) {
                        onAdvance()
                    }
                }
            }
    ) {
        // --- 暗転＋くり抜き ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color.Black.copy(alpha = 0.72f))

                val paddingPx = step.highlightPadding.toPx()
                val holeTopLeft = Offset(
                    x = (rect.left - paddingPx).coerceAtLeast(0f),
                    y = (rect.top - paddingPx).coerceAtLeast(0f)
                )
                val holeSize = Size(
                    width = rect.width + paddingPx * 2,
                    height = rect.height + paddingPx * 2
                )

                when (step.shape) {
                    SpotlightShape.OVAL -> {
                        drawOval(
                            color = Color.Transparent,
                            topLeft = holeTopLeft,
                            size = holeSize,
                            blendMode = BlendMode.Clear
                        )
                    }
                    SpotlightShape.PILL -> {
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = holeTopLeft,
                            size = holeSize,
                            cornerRadius = CornerRadius(holeSize.height / 2f),
                            blendMode = BlendMode.Clear,
                            style = Fill
                        )
                    }
                    SpotlightShape.ROUNDED_RECT -> {
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = holeTopLeft,
                            size = holeSize,
                            cornerRadius = CornerRadius(step.highlightCornerRadius.toPx()),
                            blendMode = BlendMode.Clear,
                            style = Fill
                        )
                    }
                }
            }
        }

        if (!isLastStep) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "任意の場所をタップで次へ",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // --- 説明カード ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 72.dp),
            contentAlignment = if (targetIsInLowerHalf) Alignment.TopCenter else Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(step.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(step.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stepIndex + 1} / $stepCount",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (isLastStep) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onShowLater) {
                                Text("後でもう一度表示する", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = onFinish,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("わかった！", fontSize = 13.sp)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onSkip,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Text("スキップ", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}