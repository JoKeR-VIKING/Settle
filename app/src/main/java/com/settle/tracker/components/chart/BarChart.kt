package com.settle.tracker.components.chart

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.settle.tracker.utils.formatCurrency
import kotlin.math.max

data class BarData(
    val barValues: List<Float>,
    val barColors: List<Color>
)

@Composable
fun BarChart(
    modifier: Modifier = Modifier,
    xAxisData: List<String>? = null,
    bars: List<BarData> = emptyList(),
    yAxisBackground: Color = MaterialTheme.colorScheme.surface,
    gridColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    barWidth: Float = 80f,
    barRadius: Float = 16f,
    barSpacing: Float = 100f,
    tooltipBackground: Color = MaterialTheme.colorScheme.secondary,
    tooltipTextStyle: TextStyle = MaterialTheme.typography.labelMedium,
    tooltipFormatter: (Float) -> String = { it.toString() },
    steps: Int = 10,
    showAxes: Boolean = true,
    showGrid: Boolean = true,
    showTooltip: Boolean = true,
    startFromEnd: Boolean = true,
    referenceLine: ReferenceLineConfig? = null
) {
    if (bars.size <= 1) return

    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurface
    )

    val maxValue = bars.maxOf { it.barValues.sum() }.coerceAtLeast(1f)
    val progress = remember { Animatable(0f) }

    val paddingLeft = 40f
    val paddingBottom = 20f
    val xStartOffset = 80f
    val dashedEffect = PathEffect.dashPathEffect(
        floatArrayOf(16f, 10f),
        phase = 0f
    )

    var selectedIndex by remember { mutableStateOf(Pair(-1, -1)) }

    LaunchedEffect(bars.size) {
        if (startFromEnd) scrollState.animateScrollTo(scrollState.maxValue)

        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            )
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        val contentWidthPx = (barSpacing + barWidth) * (bars.size - 1)
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val finalWidthPx = max(contentWidthPx, availableWidthPx)

        Box(
            modifier = Modifier.horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = modifier
                    .width(with(density) { finalWidthPx.toDp() })
                    .pointerInput(bars) {
                        detectTapGestures { tapOffset ->
                            val chartWidth = size.width - paddingLeft - xStartOffset
                            val chartHeight = size.height
                            val offsetWidth = chartWidth - xStartOffset

                            bars.forEachIndexed { i, barData ->
                                val fraction = i / (bars.size - 1).toFloat()
                                val centerX = paddingLeft + xStartOffset + offsetWidth * fraction

                                val left = centerX - barWidth / 2f
                                val right = centerX + barWidth / 2f

                                Log.d("TapOffset", "$i, $tapOffset, $left, $right")
                                if (tapOffset.x !in left..right) return@forEachIndexed
                                var accumulatedHeight = 0f

                                barData.barValues.forEachIndexed { j, value ->
                                    val segmentHeight = chartHeight * (value / maxValue)
                                    val segmentTop = chartHeight - accumulatedHeight - segmentHeight
                                    val segmentBottom = chartHeight - accumulatedHeight

                                    Log.d("TapOffset", "$segmentTop, $segmentBottom")
                                    if (tapOffset.y in segmentTop..segmentBottom) {
                                        Log.d("TapOffset", "hit here")
                                        selectedIndex = Pair(i, j)
                                        return@detectTapGestures
                                    }

                                    accumulatedHeight += segmentHeight
                                }
                            }

                            selectedIndex = Pair(-1, -1)
                        }
                    }
            ) {
                val chartWidth = size.width - paddingLeft - xStartOffset
                val chartHeight = size.height
                val offsetWidth = chartWidth - xStartOffset

                if (showAxes) {
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, chartHeight),
                        end = Offset(chartWidth + paddingLeft + xStartOffset, chartHeight),
                        strokeWidth = 1f
                    )
                }

                for (i in 0..<steps) {
                    val fraction = i / (steps - 1).toFloat()
                    val y = chartHeight * (1f - fraction)

                    if (showGrid && i < steps - 1 && i > 0) {
                        drawLine(
                            color = gridColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(chartWidth + paddingLeft + xStartOffset, y),
                            strokeWidth = 1f,
                            pathEffect = dashedEffect
                        )
                    }
                }

                if (referenceLine != null) {
                    val referenceY =
                        chartHeight - chartHeight * (referenceLine.referenceLineValue / maxValue)
                    val referencePadding = 8f

                    drawLine(
                        color = referenceLine.referenceLineColor,
                        start = Offset(
                            x = paddingLeft,
                            y = referenceY
                        ),
                        end = Offset(
                            x = chartWidth + paddingLeft + xStartOffset,
                            y = referenceY
                        ),
                        strokeWidth = 2f,
                        pathEffect = dashedEffect
                    )

                    val referenceLineText = textMeasurer.measure(
                        text = referenceLine.referenceLineLabel,
                        style = referenceLine.referenceLineTextStyle
                    )

                    drawText(
                        textLayoutResult = referenceLineText,
                        topLeft = Offset(
                            x = paddingLeft + xStartOffset,
                            y = referenceY - referenceLineText.size.height - referencePadding
                        )
                    )
                }

                for (i in bars.indices) {
                    val fraction = i / (bars.size - 1).toFloat()
                    val x = paddingLeft + xStartOffset + offsetWidth * fraction

                    val left = x - barWidth / 2f
                    val right = left + barWidth
                    val maxLabelWidth = (barWidth * 2f).toInt()

                    val label = xAxisData?.get(i) ?: i.toString()
                    val text = textMeasurer.measure(
                        text = label,
                        style = labelStyle.copy(
                            lineHeight = 10.sp,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 2,
                        constraints = Constraints(
                            maxWidth = maxLabelWidth
                        )
                    )

                    val labelX = x - text.size.width / 2f
                    val labelY = chartHeight + paddingBottom

                    drawText(
                        textLayoutResult = text,
                        topLeft = Offset(
                            x = labelX,
                            y = labelY
                        )
                    )

                    if (bars[i].barValues.sum() <= 0) continue

                    var bottom = chartHeight

                    bars[i].barValues.forEachIndexed { j, _ ->
                        val segmentHeight = chartHeight * (bars[i].barValues[j] / maxValue)
                        val top = bottom - segmentHeight

                        val path = Path().apply {
                            moveTo(left, bottom)

                            if (j == bars[i].barValues.lastIndex) {
                                lineTo(left, top + barRadius)
                                quadraticTo(
                                    left,
                                    top,
                                    left + barRadius,
                                    top
                                )
                                lineTo(right - barRadius, top)
                                quadraticTo(
                                    right,
                                    top,
                                    right,
                                    top + barRadius
                                )
                            } else {
                                lineTo(left, top)
                                lineTo(right, top)
                            }

                            lineTo(right, bottom)
                            close()
                        }

                        drawPath(
                            path = path,
                            color = bars[i].barColors[j]
                        )

                        bottom = top
                    }

                    bottom = chartHeight

                    bars[i].barValues.forEachIndexed { j,  _ ->
                        val segmentHeight = chartHeight * (bars[i].barValues[j] / maxValue)
                        val top = bottom - segmentHeight

                        if (showTooltip && selectedIndex.first == i && selectedIndex.second == j) {
                            val tooltipLabel = tooltipFormatter(bars[i].barValues[j])
                            val tooltipText = textMeasurer.measure(
                                text = tooltipLabel,
                                style = tooltipTextStyle
                            )

                            val padding = 16f
                            val tooltipWidth = tooltipText.size.width + padding * 2
                            val tooltipHeight = tooltipText.size.height + padding * 2

                            var tooltipX = x - tooltipWidth / 2f
                            val tooltipY = top - tooltipHeight - 12f

                            tooltipX = tooltipX.coerceIn(
                                paddingLeft,
                                size.width - tooltipWidth
                            )

                            val tooltipTopLeft = Offset(tooltipX, tooltipY)

                            drawRoundRect(
                                color = tooltipBackground,
                                topLeft = tooltipTopLeft,
                                size = Size(tooltipWidth, tooltipHeight),
                                cornerRadius = CornerRadius(16f, 16f)
                            )

                            drawText(
                                textLayoutResult = tooltipText,
                                topLeft = Offset(
                                    x = tooltipTopLeft.x + padding,
                                    y = tooltipTopLeft.y + padding
                                )
                            )
                        }

                        bottom = top
                    }
                }
            }
        }

        Canvas(
            modifier = modifier
        ) {
            val chartHeight = size.height

            drawRect(
                color = yAxisBackground,
                topLeft = Offset(0f, 0f),
                size = Size(paddingLeft, chartHeight)
            )

            if (showAxes) {
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, 0f),
                    end = Offset(paddingLeft, chartHeight),
                    strokeWidth = 1f
                )
            }

            for (i in 0..<steps) {
                val fraction = i / (steps - 1).toFloat()
                val y = chartHeight * (1f - fraction)

                val label = formatCurrency(
                    amount = (maxValue * fraction).toDouble(),
                    compact = true
                )
                val text = textMeasurer.measure(label, labelStyle)

                drawText(
                    textLayoutResult = text,
                    topLeft = Offset(
                        x = paddingLeft - text.size.width - 10f,
                        y = y - text.size.height / 2
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
}
