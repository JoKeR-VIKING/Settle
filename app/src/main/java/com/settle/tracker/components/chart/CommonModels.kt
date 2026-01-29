package com.settle.tracker.components.chart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

data class ReferenceLineConfig(
    val referenceLineValue: Float,
    val referenceLineColor: Color,
    val referenceLineLabel: String,
    val referenceLineTextStyle: TextStyle
)
