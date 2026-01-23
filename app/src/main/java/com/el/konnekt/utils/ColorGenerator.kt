package com.el.konnekt.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

fun generateColorFromId(id: String): Color {
    val colors = listOf(
        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF42A5F5),
        Color(0xFF26A69A), Color(0xFFFF7043), Color(0xFF66BB6A),
        Color(0xFFFFCA28), Color(0xFF7E57C2)
    )
    val index = (id.hashCode().absoluteValue) % colors.size
    return colors[index]
}
