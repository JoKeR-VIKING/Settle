package com.settle.tracker.utils

import java.util.Locale

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalPlay
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Category

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

import java.text.NumberFormat

import com.settle.tracker.R
import com.settle.tracker.scheme.ExpenseCategory

fun formatTimestamp(
    timestamp: Long,
    format: String = "dd MMM"
): String {
    val formatter = DateTimeFormatter.ofPattern(format, Locale.getDefault())
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return dateTime.format(formatter)
}

fun formatCurrency(amount: Double): String {
    val indiaLocale = Locale("en", "IN")
    val formatter = NumberFormat.getCurrencyInstance(indiaLocale)
    return formatter.format(amount)
}

fun getExpenseCategoryIcon(expenseCategory: String): ImageVector {
    val category = try {
        ExpenseCategory.valueOf(expenseCategory)
    } catch (_: Exception) {
        ExpenseCategory.MISC
    }

    return when (category) {
        ExpenseCategory.FOOD -> Icons.Filled.Fastfood
        ExpenseCategory.GROCERY -> Icons.Filled.ShoppingBasket
        ExpenseCategory.DRINKS -> Icons.Filled.LocalBar
        ExpenseCategory.ENTERTAINMENT -> Icons.Filled.LocalPlay
        ExpenseCategory.SHOPPING -> Icons.Filled.ShoppingCart
        ExpenseCategory.SUBSCRIPTION -> Icons.Filled.Subscriptions
        ExpenseCategory.HEALTH -> Icons.Filled.MonitorHeart
        ExpenseCategory.TRAVEL -> Icons.Filled.Luggage
        ExpenseCategory.MISC -> Icons.Filled.Category
    }
}

fun getExpenseCategoryLargeIcon(expenseCategory: String): Int {
    val category = try {
        ExpenseCategory.valueOf(expenseCategory)
    } catch (_: Exception) {
        ExpenseCategory.MISC
    }

    return when (category) {
        ExpenseCategory.FOOD -> R.drawable.ic_food
        ExpenseCategory.GROCERY -> R.drawable.ic_grocery
        ExpenseCategory.DRINKS -> R.drawable.ic_drinks
        ExpenseCategory.ENTERTAINMENT -> R.drawable.ic_entertainment
        ExpenseCategory.SHOPPING -> R.drawable.ic_shopping
        ExpenseCategory.SUBSCRIPTION -> R.drawable.ic_subscription
        ExpenseCategory.HEALTH -> R.drawable.ic_health
        ExpenseCategory.TRAVEL -> R.drawable.ic_travel
        ExpenseCategory.MISC -> R.drawable.ic_misc
    }
}

fun getExpenseCategoryColor(expenseCategory: String): Color {
    val category = try {
        ExpenseCategory.valueOf(expenseCategory)
    } catch (_: Exception) {
        ExpenseCategory.MISC
    }

    return when (category) {
        ExpenseCategory.FOOD -> Color(0xFFFFA500)
        ExpenseCategory.GROCERY -> Color(0xFF2E7D32)
        ExpenseCategory.DRINKS -> Color(0xFF3949AB)
        ExpenseCategory.ENTERTAINMENT -> Color(0xFF8E24AA)
        ExpenseCategory.SHOPPING -> Color(0xFFFF69B4)
        ExpenseCategory.SUBSCRIPTION -> Color(0xFFFF0000)
        ExpenseCategory.HEALTH -> Color(0xFF1565C0)
        ExpenseCategory.TRAVEL -> Color(0xFF7B3F00)
        ExpenseCategory.MISC -> Color(0xFF546E7A)
    }
}
