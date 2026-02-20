package com.el.konnekt.ui.activities.signup

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.el.konnekt.R
import com.el.konnekt.ui.activities.mainpage.MessageActivity
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

@Suppress("DEPRECATION")
class ProfileSetUp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProfileSetUpPage(onBackPressed = { onBackPressed() })
                }
            }
        }
    }
}

@Composable
fun ProfileSetUpPage(onBackPressed: () -> Unit) {
    val activity = LocalContext.current as? ComponentActivity
    val username: String? = activity?.intent?.getStringExtra("username")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        ProfileSetUpProgress(onBackPressed = onBackPressed)
        username?.let {
            ProfileFill(username = it)
        }
    }
}

@Composable
fun ProfileSetUpProgress(onBackPressed: () -> Unit) {
    val returnArrow = painterResource(id = R.drawable.returnarrow)
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onBackPressed() },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = returnArrow,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF2F9ECE),
                                    Color(0xFF4DB8E8),
                                    Color(0xFF5FD4A0)
                                )
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Step 2 of 2 · Almost done!",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2F9ECE)
            )
        }
    }
}

@SuppressLint("InvalidColorHexValue")
@Composable
fun ProfileFill(username: String) {
    val context = LocalContext.current
    var bio by remember { mutableStateOf("") }
    val maxBioLength = 139
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Header
    Text(
        text = "Set Up Your Profile",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Add a photo and bio to help friends recognize you.",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(40.dp))

    // Avatar picker — centered
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder initials
                Text(
                    text = username.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2F9ECE)
                )
            }
        }

        // Camera badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.BottomCenter)
                .offset(x = 36.dp, y = (-4).dp)
                .clip(CircleShape)
                .background(Color(0xFF2F9ECE))
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📷", fontSize = 14.sp)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "@$username",
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    if (selectedImageUri == null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap to add a profile photo",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(36.dp))

    // Bio field
    Text(
        text = "Bio",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "Optional",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = if (bio.isNotEmpty())
                    Color(0xFF2F9ECE).copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            )
            .height(100.dp)
    ) {
        BasicTextField(
            value = bio,
            onValueChange = { if (it.length <= maxBioLength) bio = it },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                lineHeight = 21.sp
            ),
            singleLine = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
        if (bio.isEmpty()) {
            Text(
                text = "Write a short bio about yourself...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
        Text(
            text = "${bio.length}/$maxBioLength",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 8.dp)
        )
    }

    Spacer(modifier = Modifier.height(40.dp))

    // Get Started button
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isLoading) Color(0xFF2F9ECE).copy(alpha = 0.6f) else Color(0xFF2F9ECE),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(enabled = !isLoading) {
                isLoading = true
                if (selectedImageUri != null) {
                    uploadProfileImageToFirebaseStorage(
                        imageUri = selectedImageUri!!,
                        onSuccess = { downloadUrl ->
                            updateUserProfileInDatabase(username, downloadUrl, bio)
                            context.startActivity(
                                Intent(context, MessageActivity::class.java).apply {
                                    putExtra("username", username)
                                    putExtra("profileUri", selectedImageUri.toString())
                                    putExtra("bio", bio)
                                }
                            )
                        },
                        onFailure = { exception ->
                            isLoading = false
                            Toast.makeText(context, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    updateUserProfileInDatabase(username, "", bio)
                    context.startActivity(
                        Intent(context, MessageActivity::class.java).apply {
                            putExtra("username", username)
                            putExtra("profileUri", "")
                            putExtra("bio", bio)
                        }
                    )
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = "Get Started",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(40.dp))
}

fun uploadProfileImageToFirebaseStorage(
    imageUri: Uri,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val storage: FirebaseStorage = FirebaseStorage.getInstance()
    val storageRef: StorageReference = storage.reference
    val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val profileImageRef = storageRef.child("profile_images/$userUid/profile_image.jpg")

    profileImageRef.putFile(imageUri)
        .addOnSuccessListener {
            profileImageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                onSuccess(downloadUrl.toString())
            }
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}

fun updateUserProfileInDatabase(username: String, profileImageUri: String, bio: String) {
    val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val database = Firebase.database.reference

    val userData = mutableMapOf(
        "username" to username,
        "bio" to bio
    )
    if (profileImageUri.isNotBlank()) {
        userData["profileImageUri"] = profileImageUri
    }

    database.child("users").child(userUid).updateChildren(userData as Map<String, Any>)
        .addOnSuccessListener { Log.d("Firebase", "User profile updated successfully") }
        .addOnFailureListener { Log.e("Firebase", "Failed to update user profile: ${it.message}") }
}