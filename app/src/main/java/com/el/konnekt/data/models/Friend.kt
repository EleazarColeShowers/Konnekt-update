package com.el.konnekt.data.models

data class Friend(
    val friendId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", System.currentTimeMillis())
}