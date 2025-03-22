package com.example.instachatcompose.ui.activities.mainpage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

        database.child("friends").child(currentUser?.uid ?: "").child(friendId).get().addOnSuccessListener { snapshot ->
            isFriend = snapshot.exists()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberAsyncImagePainter(profileImage ?: R.drawable.nopfp),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.Gray)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(username, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(email, fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(bio, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (isFriend) {
                    database.child("friends").child(currentUser?.uid ?: "").child(friendId).removeValue()
                    isFriend = false
                } else {
                    database.child("friends").child(currentUser?.uid ?: "").child(friendId).setValue(true)
                    isFriend = true
                }
            },
            colors = ButtonDefaults.buttonColors(if (isFriend) Color.Red else Color(0xFF2F9ECE))
        ) {
            Text(if (isFriend) "Remove Friend" else "Add Friend", color = Color.White)
        }
    }
}
