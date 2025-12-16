package com.el.konnekt.data.models

import com.el.konnekt.ui.activities.mainpage.Friend
import com.el.konnekt.ui.activities.mainpage.GroupChat



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