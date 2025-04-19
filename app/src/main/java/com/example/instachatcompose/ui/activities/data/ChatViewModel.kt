package com.example.instachatcompose.ui.activities.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {

    private val db = Firebase.database.reference

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isFriendTyping = MutableStateFlow(false)
    val isFriendTyping: StateFlow<Boolean> = _isFriendTyping

    private var chatListener: ValueEventListener? = null
    private var typingListener: ValueEventListener? = null

    fun observeMessages(chatId: String, currentUserId: String, isChatOpen: Boolean) {
        val messagesRef = db.child("chats").child(chatId).child("messages")

        chatListener?.let { messagesRef.removeEventListener(it) } // Remove previous listener if any

        chatListener = messagesRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Message>()
                for (child in snapshot.children) {
                    val message = child.getValue(Message::class.java)
                    if (message != null) {
                        if (message.deletedFor?.containsKey(currentUserId) == true) continue
                        if (message.receiverId == currentUserId && !message.seen && isChatOpen) {
                            child.ref.child("seen").setValue(true)
                        }
                        list.add(message)
                    }
                }
                _messages.value = list.sortedByDescending { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatVM", "Failed to fetch messages: ${error.message}")
            }
        })
    }

    fun observeTyping(chatId: String, receiverId: String) {
        val typingRef = db.child("chats").child(chatId).child("typing").child(receiverId)

        typingListener?.let { typingRef.removeEventListener(it) }

        typingListener = typingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isFriendTyping.value = snapshot.getValue(Boolean::class.java) == true
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatVM", "Typing listener failed: ${error.message}")
            }
        })
    }

    fun sendMessage(chatId: String, message: Message) {
        val messagesRef = db.child("chats").child(chatId).child("messages")
        val newMessageRef = messagesRef.push()
        newMessageRef.setValue(message)
    }

    fun setTypingStatus(chatId: String, currentUserId: String, isTyping: Boolean) {
        val typingRef = db.child("chats").child(chatId).child("typing").child(currentUserId)
        typingRef.setValue(isTyping)
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.let {
            db.removeEventListener(it)
        }
        typingListener?.let {
            db.removeEventListener(it)
        }
    }
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val seen: Boolean = false,
    val replyTo: String? = null,
    val edited: Boolean = false,
    val deletedFor: Map<String, Boolean>? = null

)