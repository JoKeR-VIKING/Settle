package com.settle.tracker.components.chart

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.settle.tracker.utils.formatCurrency
import kotlin.math.max

enum class LineType {
    STRAIGHT,
    CURVED
}

data class ReferenceLineConfig(
    val referenceLineValue: Float,
    val referenceLineColor: Color,
    val referenceLineLabel: String,
    val referenceLineTextStyle: TextStyle
)

@Composable
fun LineChart(
    modifier: Modifier = Modifier,
    xAxisData: List<String>? = null,
    points: List<Float> = emptyList(),
    yAxisBackground: Color = MaterialTheme.colorScheme.surface,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    pointColor: Color = MaterialTheme.colorScheme.primary,
    pointRadius: Float = 10f,
    tooltipBackground: Color = MaterialTheme.colorScheme.secondary,
    tooltipTextStyle: TextStyle = MaterialTheme.typography.labelMedium,
    tooltipFormatter: (Float) -> String = { it.toString() },
    steps: Int = 10,
    showPoints: Boolean = true,
    showAxes: Boolean = true,
    showHorizontalGrid: Boolean = true,
    showVerticalGrid: Boolean = true,
    showTooltip: Boolean = true,
    lineType: LineType = LineType.STRAIGHT,
    referenceLine: ReferenceLineConfig? = null
) {
    if (points.size <= 1) return

    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurface
    )

    val maxValue = points.maxOf { it }.coerceAtLeast(1f)
    val progress = remember { Animatable(0f) }

    val paddingLeft = 40f
    val paddingBottom = 20f
    val xStartOffset = 80f
    val dashedEffect = PathEffect.dashPathEffect(
        floatArrayOf(16f, 10f),
        phase = 0f
    )
    val pointSpacing = 150f

    var selectedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
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
        val contentWidthPx = pointSpacing * (points.size - 1)
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val finalWidthPx = max(contentWidthPx, availableWidthPx)

        Box(
            modifier = Modifier.horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = modifier
                    .width(with(density) { finalWidthPx.toDp() })
                    .pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            var hitIndex = -1
                            var minDistance = Float.MAX_VALUE

                            val chartWidth = size.width - paddingLeft - xStartOffset
                            val chartHeight = size.height
                            val offsetWidth = chartWidth - xStartOffset

                            points.forEachIndexed { index, point ->
                                val fraction = index / (points.size - 1).toFloat()
                                val x = paddingLeft + xStartOffset + offsetWidth * fraction
                                val y = chartHeight - chartHeight * (point / maxValue)

                                val distance = (tapOffset - Offset(x, y)).getDistance()
                                if (distance <= pointRadius * 4f && distance < minDistance) {
                                    minDistance = distance
                                    hitIndex = index
                                }
                            }

                            selectedIndex = hitIndex
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

                    if (showHorizontalGrid && i < steps - 1 && i > 0) {
                        drawLine(
                            color = gridColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(chartWidth + paddingLeft + xStartOffset, y),
                            strokeWidth = 1f,
                            pathEffect = dashedEffect
                        )
                    }
                }

                val path = Path().apply {
                    var prevX = 0f
                    var prevY = 0f

                    points.forEachIndexed { index, point ->
                        val fraction = index / (points.size - 1).toFloat()
                        val x = paddingLeft + xStartOffset + offsetWidth * fraction
                        val y = chartHeight - chartHeight * (point / maxValue)

                        if (index == 0) {
                            moveTo(
                                x = x,
                                y = y
                            )
                        } else {
                            if (lineType == LineType.STRAIGHT) {
                                lineTo(
                                    x = x,
                                    y = y
                                )
                            } else {
                                val controlX = (prevX + x) / 2f

                                cubicTo(
                                    controlX, prevY,
                                    controlX, y,
                                    x, y
                                )
                            }
                        }

                        prevX = x
                        prevY = y
                    }
                }

                val shadowPath = Path().apply {
                    addPath(path)

                    lineTo(
                        x = paddingLeft + xStartOffset + offsetWidth,
                        y = chartHeight
                    )

                    lineTo(
                        x = paddingLeft + xStartOffset,
                        y = chartHeight
                    )

                    close()
                }

                val animatedPath = Path()
                val pathMeasure = PathMeasure()

                pathMeasure.setPath(path, false)
                pathMeasure.getSegment(
                    startDistance = 0f,
                    stopDistance = pathMeasure.length * progress.value,
                    destination = animatedPath,
                    startWithMoveTo = true
                )

                val shadowRevealX = size.width * progress.value

                clipRect(
                    left = 0f,
                    top = 0f,
                    right = shadowRevealX,
                    bottom = chartHeight
                ) {
                    drawPath(
                        path = shadowPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.45f),
                                lineColor.copy(alpha = 0.25f)
                            ),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )
                }

                drawPath(
                    path = animatedPath,
                    color = lineColor,
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                for (i in points.indices) {
                    val fraction = i / (points.size - 1).toFloat()
                    val x = paddingLeft + xStartOffset + offsetWidth * fraction

                    if (showVerticalGrid) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, chartHeight),
                            strokeWidth = 1f,
                            pathEffect = dashedEffect
                        )
                    }

                    if (showPoints) {
                        drawCircle(
                            color = pointColor,
                            radius = pointRadius,
                            center = Offset(
                                x = x,
                                y = chartHeight - chartHeight * (points[i] / maxValue)
                            )
                        )
                    }

                    if (referenceLine != null) {
                        val referenceY = chartHeight - chartHeight * (referenceLine.referenceLineValue / maxValue)
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

                    val label = xAxisData?.get(i) ?: i.toString()
                    val text = textMeasurer.measure(label, labelStyle)

                    val maxLabelX = paddingLeft + chartWidth - text.size.width
                    val labelX = (x - text.size.width / 2).coerceIn(
                        paddingLeft,
                        maxLabelX
                    )

                    drawText(
                        textLayoutResult = text,
                        topLeft = Offset(
                            x = labelX,
                            y = chartHeight + paddingBottom
                        )
                    )
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

        Canvas(
            modifier = modifier
                .width(with(density) { finalWidthPx.toDp() })
        ) {
            if (selectedIndex == -1) return@Canvas

            val chartWidth = size.width - paddingLeft - xStartOffset
            val chartHeight = size.height
            val offsetWidth = chartWidth - xStartOffset

            for (i in points.indices) {
                val fraction = i / (points.size - 1).toFloat()
                val x = paddingLeft + xStartOffset + offsetWidth * fraction

                if (showTooltip) {
                    if (selectedIndex == i) {
                        val tooltipLabel = tooltipFormatter(points[i])
                        val tooltipText = textMeasurer.measure(
                            text = tooltipLabel,
                            style = tooltipTextStyle
                        )

                        val padding = 16f
                        val tooltipWidth = tooltipText.size.width + padding * 2
                        val tooltipHeight = tooltipText.size.height + padding * 2

                        val tooltipTopLeft = Offset(
                            x = x - tooltipWidth / 2,
                            y = chartHeight - chartHeight * (points[i] / maxValue) - tooltipHeight - padding * 2
                        )

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
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
}
