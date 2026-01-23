@file:Suppress("DEPRECATION")

package com.el.konnekt.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.el.konnekt.data.local.AppDatabase
import com.el.konnekt.data.core.ChatManager
import com.el.konnekt.data.repository.ChatRepository
import com.el.konnekt.data.ChatViewModel
import com.el.konnekt.data.ChatViewModelFactory
import com.el.konnekt.data.remote.FirebaseDataSource
import com.el.konnekt.data.local.LocalDataSource
import com.el.konnekt.ui.activities.mainpage.MessageActivity
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : ComponentActivity() {
    private val splashScreenDuration = 100L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        // Splash screen content removed
                    }
                }
            }
        }

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
                                val appDb = AppDatabase.getDatabase(applicationContext)
                                val localDataSource = LocalDataSource(appDb)
                                val firebaseDataSource = FirebaseDataSource()
                                val repo = ChatRepository(firebaseDataSource, localDataSource)
                                val factory = ChatViewModelFactory(application, repo)
                                val chatViewModel = ViewModelProvider(this@MainActivity, factory)[ChatViewModel::class.java]
                                ChatManager.startListeningForMessages(
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
                // Not logged in
                startActivity(Intent(this@MainActivity, JoinActivity::class.java))
                finish()
            }
        }, splashScreenDuration)
    }
}