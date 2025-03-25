package com.example.instachatcompose.ui.activities.mainpage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val friendId = intent.getStringExtra("friendId") ?: ""
                    FriendProfileScreen(friendId)
                }
            }
        }
    }
}

@Composable
fun FriendProfileScreen(friendId: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val database = FirebaseDatabase.getInstance().reference

    var username by remember { mutableStateOf("Loading...") }
    var email by remember { mutableStateOf("Loading...") }
    var bio by remember { mutableStateOf("No bio available") }
    var profileImage by remember { mutableStateOf<String?>(null) }
    var isFriend by remember { mutableStateOf(false) }

    LaunchedEffect(friendId) {
        database.child("users").child(friendId).get().addOnSuccessListener { snapshot ->
            username = snapshot.child("username").value as? String ?: "Unknown"
            email = snapshot.child("email").value as? String ?: "No email"
            bio = snapshot.child("bio").value as? String ?: "No bio"
            profileImage = snapshot.child("profileImageUri").value as? String
        }
    }

    LaunchedEffect(friendId, currentUser?.uid) {
        database.child("users").child(currentUser?.uid ?: "").child("friends").get()
            .addOnSuccessListener { snapshot ->
                isFriend = snapshot.children.any { it.child("friendId").value == friendId }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Image(
                painter = rememberAsyncImagePainter(profileImage ?: R.drawable.nopfp),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(username, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(email, fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = bio,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val currentUserId = currentUser?.uid ?: return@Button
                    val userFriendsRef = database.child("users").child(currentUserId).child("friends").push()
                    val friendFriendsRef = database.child("users").child(friendId).child("friends").push()

                    if (isFriend) {
                        // Remove friend by finding and deleting the unique entry
                        database.child("users").child(currentUserId).child("friends").get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.children.forEach { child ->
                                    if (child.child("friendId").value == friendId) {
                                        child.ref.removeValue()
                                    }
                                }
                            }

                        database.child("users").child(friendId).child("friends").get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.children.forEach { child ->
                                    if (child.child("friendId").value == currentUserId) {
                                        child.ref.removeValue()
                                    }
                                }
                            }

                        isFriend = false
                    } else {
                        val friendshipData = mapOf("friendId" to friendId, "timestamp" to System.currentTimeMillis())
                        val reverseFriendshipData = mapOf("friendId" to currentUserId, "timestamp" to System.currentTimeMillis())

                        userFriendsRef.setValue(friendshipData).addOnSuccessListener {
                            friendFriendsRef.setValue(reverseFriendshipData).addOnSuccessListener {
                                isFriend = true
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(if (isFriend) Color.Red else Color(0xFF2F9ECE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(if (isFriend) "Remove Friend" else "Add Friend", color = Color.White, fontSize = 18.sp)
            }

        }
    }
}
