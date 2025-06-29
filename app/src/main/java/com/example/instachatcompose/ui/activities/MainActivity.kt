@file:Suppress("DEPRECATION")

package com.example.instachatcompose.ui.activities

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.instachatcompose.ui.activities.data.ChatManager
import com.example.instachatcompose.ui.activities.data.ChatViewModel
import com.example.instachatcompose.ui.activities.mainpage.MessageActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainActivity : ComponentActivity() {
    private val splashScreenDuration = 3000L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Handler().postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
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
