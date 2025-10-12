@file:Suppress("DEPRECATION")

package com.example.instachatcompose.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.activities.data.local.AppDatabase
import com.example.instachatcompose.ui.activities.data.core.ChatManager
import com.example.instachatcompose.ui.activities.data.repository.ChatRepository
import com.example.instachatcompose.ui.activities.data.ChatViewModel
import com.example.instachatcompose.ui.activities.data.ChatViewModelFactory
import com.example.instachatcompose.ui.activities.data.remote.FirebaseDataSource
import com.example.instachatcompose.ui.activities.data.local.LocalDataSource
import com.example.instachatcompose.ui.activities.mainpage.MessageActivity
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : ComponentActivity() {
    private val splashScreenDuration = 5000L // 10 seconds in milliseconds

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
                        AnimatedPreloader()
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
                                // You’ll need a ViewModel instance here
                                val appDb = AppDatabase.getDatabase(applicationContext)
                                val localDataSource = LocalDataSource(appDb)
                                val firebaseDataSource = FirebaseDataSource()

// Build the repository with the correct constructor
                                val repo = ChatRepository(firebaseDataSource, localDataSource)

// Now provide it to the ViewModel
                                val factory = ChatViewModelFactory(application, repo)
                                val chatViewModel = ViewModelProvider(this@MainActivity, factory)[ChatViewModel::class.java]


                                ChatManager.startListeningForMessages(
                                    applicationContext,
                                    chatId,
                                    currentUserId,
                                    chatViewModel
                                )
                            }
                            // Go to the home or login screen
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

@Composable
fun AnimatedPreloader(modifier: Modifier = Modifier) {
    val preloaderLottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            R.raw.connections
        )
    )

    val preloaderProgress by animateLottieCompositionAsState(
        composition = preloaderLottieComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )

    Column(
        modifier= Modifier.padding(horizontal = 20.dp)
    ) {
        LottieAnimation(
            composition = preloaderLottieComposition,
            progress = preloaderProgress,
            modifier = modifier
        )
    }
}
