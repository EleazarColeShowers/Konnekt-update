package com.el.konnekt.data.models

data class GroupChat(
    val groupId: String,
    val groupName: String,
    val members: List<String>,
    val groupImage: String = ""
)