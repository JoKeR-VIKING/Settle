package com.settle.tracker.components.chart

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import java.util.Locale

data class PieData(
    val label: String = "",
    val value: Float,
    val color: Color
)

@Composable
fun PieChart(
    modifier: Modifier = Modifier,
    data: List<PieData>,
    start: Float = -90f,
    donutMode: Boolean = false,
    donutHole: Float = 0.5f,
    showPercentage: Boolean = false,
    minPercentageToShow: Float = 5f,
    showValue: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    )
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()

    val total = data.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val diameter = size.minDimension
        val radius = diameter / 2f
        val center = Offset(
            size.width / 2,
            size.height / 2
        )

        var startAngle = start

        data.forEach { pieData ->
            val sweepAngle = pieData.value / total * 360f

            drawArc(
                color = pieData.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(
                    x = center.x - radius,
                    y = center.y - radius
                ),
                size = Size(
                    diameter,
                    diameter
                )
            )

            if (showPercentage || showValue) {
                val midAngle = startAngle + sweepAngle / 2f
                val angleRad = Math.toRadians(midAngle.toDouble())

                val labelRadius = if (donutMode) {
                    radius * (1f + donutHole) / 2f
                } else {
                    radius * 0.65f
                }

                val textX = center.x + labelRadius * kotlin.math.cos(angleRad).toFloat()
                val textY = center.y + labelRadius * kotlin.math.sin(angleRad).toFloat()

                val percentage = (pieData.value / total * 100f)
                val percentageFormatted = String.format(
                    locale = Locale.getDefault(),
                    format = "%.2f",
                    percentage
                )
                val valueFormatted = String.format(
                    locale = Locale.getDefault(),
                    format = "%.2f",
                    pieData.value
                )

                val label = when {
                    showPercentage && showValue && percentage >= minPercentageToShow ->
                        "${percentageFormatted}%\n${valueFormatted}"
                    showPercentage && percentage >= minPercentageToShow ->
                        "${percentageFormatted}%"
                    showValue ->
                        valueFormatted
                    else ->
                        ""
                }

                val textLayout = textMeasurer.measure(
                    label,
                    style = textStyle
                )

                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        x = textX - textLayout.size.width / 2,
                        y = textY - textLayout.size.height / 2
                    )
                )
            }

            startAngle += sweepAngle
        }

        if (donutMode) {
            drawCircle(
                color = Color.Transparent,
                radius = radius * donutHole,
                center = center,
                blendMode = BlendMode.Clear
            )
        }
    }
}
