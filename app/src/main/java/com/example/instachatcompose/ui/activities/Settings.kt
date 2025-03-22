package com.example.instachatcompose.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.activities.login.LoginActivity
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

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
    val storageReference = FirebaseStorage.getInstance().reference
    val user = auth.currentUser
    val database = FirebaseDatabase.getInstance().reference.child("users").child(user?.uid ?: "")

    var username by remember { mutableStateOf("Loading...") }
    var bio by remember { mutableStateOf("No bio yet") }
    var email by remember { mutableStateOf(user?.email ?: "No email") }
    var profileImageUri by remember { mutableStateOf<String?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            uploadImageToFirebase(uri, user, database, storageReference)
        }
    }

    LaunchedEffect(Unit) {
        database.get().addOnSuccessListener { snapshot ->
            username = snapshot.child("username").value as? String ?: "User"
            bio = snapshot.child("bio").value as? String ?: "No bio yet"
            profileImageUri = snapshot.child("profileImageUri").value as? String
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Profile section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profileImageUri ?: R.drawable.nopfp, // Use a default image
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Gray, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(username, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(bio, fontSize = 14.sp, color = Color.Gray)
                Text(email, fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Settings Options
        SettingOption("Change Username") { showDialog = true }
        SettingOption("Change Profile Picture") { pickImageLauncher.launch("image/*") }
        SettingOption("Privacy Settings") { }

        Spacer(modifier = Modifier.height(20.dp))

        // Save Settings Button
        Button(
            onClick = { /* Save logic here */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F9ECE))
        ) {
            Text("Save Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Logout Button
        Button(
            onClick = {
                auth.signOut()
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

fun uploadImageToFirebase(
    uri: Uri,
    user: FirebaseUser?,
    database: DatabaseReference,
    storageReference: StorageReference
) {
    user?.uid?.let { uid ->
        val profileImageRef = storageReference.child("profile_images/$uid/profile_image.jpg")

        profileImageRef.putFile(uri)
            .addOnSuccessListener {
                profileImageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()

                    database.child("users").child(uid).child("profileImageUri")
                        .setValue(imageUrl)
                        .addOnSuccessListener {
                            Log.d("ProfileUpdate", "Profile picture URL saved successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfileUpdate", "Error saving profile picture URL in DB", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpload", "Error uploading image", e)
            }
    }
}

