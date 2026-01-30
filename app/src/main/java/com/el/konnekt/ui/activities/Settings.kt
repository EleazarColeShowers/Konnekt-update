package com.el.konnekt.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.el.konnekt.ui.activities.login.LoginActivity
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.el.konnekt.R
import com.el.konnekt.services.MyFirebaseMessagingService


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
    var showBioDialog by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            uploadImageToFirebase(uri, user, database, storageReference) { downloadUrl ->
                // Update state after upload
                if (downloadUrl != null) {
                    profileImageUri = downloadUrl
                }
            }
        }
    }

    // Real-time database listener with proper cleanup
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                username = snapshot.child("username").value as? String ?: "User"
                bio = snapshot.child("bio").value as? String ?: "No bio yet"
                profileImageUri = snapshot.child("profileImageUri").value as? String
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SettingsPage", "Database error: ${error.message}")
            }
        }

        database.addValueEventListener(listener)

        onDispose {
            database.removeEventListener(listener)
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
                model = profileImageUri ?: R.drawable.nopfp,
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
        SettingOption("Change Bio") { showBioDialog = true }
        SettingOption("Change Profile Picture") { pickImageLauncher.launch("image/*") }
        SettingOption("Privacy Settings") {
            android.widget.Toast.makeText(context, "Privacy settings coming soon", android.widget.Toast.LENGTH_SHORT).show()
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        Button(
            onClick = {
                auth.signOut()
                context.stopService(Intent(context, MyFirebaseMessagingService::class.java))

                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
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
                updateUsername(newUsername) { success ->
                    if (success) {
                        username = newUsername
                        android.widget.Toast.makeText(context, "Username updated", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Failed to update username", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                showDialog = false
            }
        )
    }

    if (showBioDialog) {
        ChangeBioDialog(
            onDismiss = { showBioDialog = false },
            onConfirm = { newBio ->
                updateBio(newBio) { success ->
                    if (success) {
                        bio = newBio
                        android.widget.Toast.makeText(context, "Bio updated", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Failed to update bio", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                showBioDialog = false
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
                    label = { Text("New Username") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newUsername.isNotBlank()) {
                        onConfirm(newUsername)
                    }
                },
                enabled = newUsername.isNotBlank()
            ) {
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

@Composable
fun ChangeBioDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newBio by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Change Bio") },
        text = {
            Column {
                Text("Enter your new bio:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newBio,
                    onValueChange = { newBio = it },
                    label = { Text("New Bio") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newBio.isNotBlank()) {
                        onConfirm(newBio)
                    }
                },
                enabled = newBio.isNotBlank()
            ) {
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

fun updateUsername(newUsername: String, onComplete: (Boolean) -> Unit) {
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
                            Log.d("UpdateUsername", "Username updated successfully")
                            onComplete(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e("UpdateUsername", "Error updating username", e)
                            onComplete(false)
                        }
                } else {
                    Log.e("UpdateUsername", "Error updating profile", task.exception)
                    onComplete(false)
                }
            }
    } ?: run {
        Log.e("UpdateUsername", "User is null")
        onComplete(false)
    }
}

fun uploadImageToFirebase(
    uri: Uri,
    user: FirebaseUser?,
    database: DatabaseReference,
    storageReference: StorageReference,
    onComplete: (String?) -> Unit
) {
    user?.uid?.let { uid ->
        val profileImageRef = storageReference.child("profile_images/$uid/profile_image.jpg")

        profileImageRef.putFile(uri)
            .addOnSuccessListener {
                profileImageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()

                    database.child("profileImageUri")
                        .setValue(imageUrl)
                        .addOnSuccessListener {
                            Log.d("ProfileUpdate", "Profile picture URL saved successfully")
                            onComplete(imageUrl)
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfileUpdate", "Error saving profile picture URL in DB", e)
                            onComplete(null)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpload", "Error uploading image", e)
                onComplete(null)
            }
    } ?: run {
        Log.e("UploadImage", "User is null")
        onComplete(null)
    }
}

fun updateBio(newBio: String, onComplete: (Boolean) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val database = FirebaseDatabase.getInstance().reference

    user?.let {
        database.child("users").child(user.uid).child("bio")
            .setValue(newBio)
            .addOnSuccessListener {
                Log.d("UpdateBio", "Bio updated successfully in Realtime DB")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("UpdateBio", "Error updating bio in Realtime DB", e)
                onComplete(false)
            }
    } ?: run {
        Log.e("UpdateBio", "User is null")
        onComplete(false)
    }
}