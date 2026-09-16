package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

enum class ExpenseCategory(
    val stringResId: Int,
    val icon: ImageVector,
    val colorHex: Long
) {
    FOOD(R.string.category_food, Icons.Default.Restaurant, 0xFFF97316),
    GROCERIES(R.string.category_groceries, Icons.Default.ShoppingCart, 0xFF10B981),
    TRANSPORT(R.string.category_transport, Icons.Default.DirectionsCar, 0xFF3B82F6),
    HOUSING(R.string.category_housing, Icons.Default.Home, 0xFF8B5CF6),
    SHOPPING(R.string.category_shopping, Icons.Default.ShoppingBag, 0xFFEC4899),
    HEALTH(R.string.category_health, Icons.Default.LocalPharmacy, 0xFFEF4444),
    LEISURE(R.string.category_leisure, Icons.Default.SportsEsports, 0xFFF59E0B),
    OTHER(R.string.category_other, Icons.Default.Category, 0xFF64748B);

    companion object {
        fun fromName(name: String): ExpenseCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}
