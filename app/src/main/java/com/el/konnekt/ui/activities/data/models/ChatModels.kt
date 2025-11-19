package com.el.konnekt.ui.activities.data.models

import com.el.konnekt.ui.activities.mainpage.Friend
import com.el.konnekt.ui.activities.mainpage.GroupChat

//data class Friend(
//    val friendId: String = "",
//    val timestamp: Long = 0L
//)
//
//data class GroupChat(
//    val groupId: String,
//    val groupName: String,
//    val members: List<String>,
//    val groupImage: String = ""
//)

sealed class ChatItem {
    data class FriendItem(
        val friend: Friend,
        val details: Map<String, String>,
        val timestamp: Long
    ) : ChatItem()

    data class GroupItem(
        val group: GroupChat,
        val timestamp: Long
    ) : ChatItem()
}

object TempGroupIdHolder {
    var groupId: String = " "
}

enum class BottomAppBarItem {
    Messages,
    Calls,
    AddFriends
}