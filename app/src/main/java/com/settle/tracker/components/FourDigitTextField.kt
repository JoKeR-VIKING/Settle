package com.settle.tracker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

import androidx.compose.ui.unit.dp

@Composable
fun FourDigitTextField(
    lastFourDigits: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    helperText: String,
) {
    val focusRequester = remember { FocusRequester() }

    val customTextSelectionColors = TextSelectionColors(
        handleColor = Color.Transparent,
        backgroundColor = Color.Transparent,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isVisible) 1f else 0f)
    ) {
        CompositionLocalProvider(
            LocalTextSelectionColors provides customTextSelectionColors,
        ) {
            BasicTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .alpha(0f)
                    .matchParentSize()
                    .drawWithContent {},
                value = lastFourDigits,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        onValueChange(it)
                    }
                },
                enabled = isVisible,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(Color.Transparent),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Transparent
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) { index ->
                    val char = when {
                        index >= lastFourDigits.length -> ""
                        else -> lastFourDigits[index].toString()
                    }

                    val isFocused = (lastFourDigits.length == index ||
                        (lastFourDigits.length == 4 && index == 3))

                    DigitBox(char = char, isFocused = isFocused)
                }
            }

            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .alpha(if (isVisible) 1f else 0f)
            )
        }
    }
}

@Composable
fun DigitBox(char: String, isFocused: Boolean) {
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(45.dp)
    ) {
        Text(
            text = char,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(4.dp)
                .fillMaxWidth()
                .background(
                    color = borderColor,
                    shape = RoundedCornerShape(10)
                )
        )
    }
}
