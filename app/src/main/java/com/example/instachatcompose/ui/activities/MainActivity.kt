@file:Suppress("DEPRECATION")

package com.example.instachatcompose.ui.activities

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.room.Room
import com.example.instachatcompose.ui.activities.data.AppDatabase
import com.example.instachatcompose.ui.activities.data.ChatManager
import com.example.instachatcompose.ui.activities.data.ChatViewModel
import com.example.instachatcompose.ui.activities.data.KeystoreHelper
import com.example.instachatcompose.ui.activities.mainpage.MessageActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val splashScreenDuration = 1000L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeystoreHelper.generateKeyIfNecessary()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val currentUserId = currentUser.uid
            val chatViewModel = ChatViewModel(application)

            // 🔥 Listen globally for friend requests!
            chatViewModel.listenForFriendRequests(this, currentUserId)

            // You can also set up message listening later inside Handler if needed
        }

        Handler().postDelayed({
            if (currentUser != null) {
                val currentUserId = currentUser.uid
                val dbRef = FirebaseDatabase.getInstance().reference
                dbRef.child("users").child(currentUserId).child("chatId")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val chatId = snapshot.getValue(String::class.java)
                            if (!chatId.isNullOrEmpty()) {
                                val chatViewModel = ChatViewModel(Application())
                                ChatManager.startListeningForMessages(
                                    applicationContext,
                                    chatId,
                                    currentUserId,
                                    chatViewModel
                                )
                            }
                            startActivity(Intent(this@MainActivity, MessageActivity::class.java))
                            finish()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Splash", "Failed to get chatId: ${error.message}")
                            startActivity(Intent(this@MainActivity, JoinActivity::class.java))
                            finish()
                        }
                    })
            } else {
                startActivity(Intent(this@MainActivity, JoinActivity::class.java))
                finish()
            }
        }, splashScreenDuration)
    }
}
