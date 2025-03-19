package com.example.instachatcompose.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.instachatcompose.ui.activities.login.LoginActivity
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.log

class Settings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsPage()
                }
            }
        }
    }
}

@Composable
fun SettingsPage() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var showDialog by remember { mutableStateOf(false) }

    fun logout() {
        auth.signOut()
        val intent = Intent(context, LoginActivity::class.java)
        context.startActivity(intent)
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Settings",
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2F9ECE)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        SettingOption(text = "Change Username") {
            showDialog = true
        }

        SettingOption(text = "Change Profile Picture") {}
        SettingOption(text = "Privacy Settings") {}

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, Color(0xFF2F9ECE), RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Save Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { logout() }
                .background(Color(0xFF2F9ECE), RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Logout", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showDialog) {
        ChangeUsernameDialog(
            onDismiss = { showDialog = false },
            onConfirm = { newUsername ->
                updateUsername(newUsername)
                showDialog = false
            }
        )
    }
}

@Composable
fun SettingOption(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ChangeUsernameDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newUsername by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Change Username") },
        text = {
            Column {
                Text("Enter your new username:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("New Username") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(newUsername) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

fun updateUsername(newUsername: String) {
    val user = FirebaseAuth.getInstance().currentUser
    val database = FirebaseDatabase.getInstance().reference

    user?.let {
        val profileUpdates = userProfileChangeRequest {
            displayName = newUsername
        }

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    database.child("users").child(user.uid).child("username")
                        .setValue(newUsername)
                        .addOnSuccessListener {
                            Log.d("UpdateUsername", "Username updated successfully in Realtime DB")
                        }
                        .addOnFailureListener { e ->
                            Log.e("UpdateUsername", "Error updating username in Realtime DB", e)
                        }
                }
            }
    }
}
