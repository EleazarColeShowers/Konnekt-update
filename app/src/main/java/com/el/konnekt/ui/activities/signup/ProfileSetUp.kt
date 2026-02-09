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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.el.konnekt.R
import com.el.konnekt.ui.activities.mainpage.MessageActivity
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

@Suppress("DEPRECATION")
class ProfileSetUp: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier= Modifier.fillMaxSize()) {
                        ProfileSetUpPage(onBackPressed = {
                            onBackPressed()
                        })
                    }
                }
            }
        }

    }
}

@Composable
fun ProfileSetUpPage(onBackPressed: () -> Unit){
    val activity = LocalContext.current as? ComponentActivity
    val username: String? = activity?.intent?.getStringExtra("username")

    Column (
        modifier= Modifier.padding(horizontal = 15.dp)
    ){
        ProfileSetUpProgress(onBackPressed ={
            onBackPressed()
        } )
        username?.let {
            ProfileFill(username = it, bio = it)
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

    Spacer(modifier = Modifier.height(15.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onBackPressed() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = returnArrow,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Completed progress bar with success gradient
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE0E0E0))
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
                                    Color(0xFF5FD4A0) // Success green at the end
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Completion text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Step 2 of 2 - Almost done!",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight(500),
                        color = Color(0xFF2F9ECE)
                    )
                )
            }
        }
    }
}

@SuppressLint("InvalidColorHexValue")
@Composable
fun ProfileFill(username: String, bio: String){
    val context= LocalContext.current
    val  noPfp= painterResource(id = R.drawable.nopfp)
    val addPfp= painterResource(id = R.drawable.addpfp)
    val defaultCam= painterResource(id = R.drawable.nopfpcam)
    var bio by remember {
        mutableStateOf("")
    }
    val maxBioLength = 139
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Replace GetContent with PickVisualMedia (Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            selectedImageUri = selectedUri
        }
    }

    val painter by rememberUpdatedState(
        if (selectedImageUri != null) {
            rememberAsyncImagePainter(selectedImageUri!!)
        } else {
            painterResource(id = R.drawable.nopfp)
        }
    )
    Column{
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Set Up Profile",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight(500),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Set up your profile and start connecting",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(400),
                color = MaterialTheme.colorScheme.onBackground,
            )
        )
        Spacer(modifier = Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center

        ) {
            if (selectedImageUri != null) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .scale(1.5f)

                )
            } else {
                Image(
                    painter = noPfp,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                )

                Image(
                    painter = defaultCam,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .offset(0.dp, 0.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)

                )

            }

            Image(
                painter = addPfp,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .offset(30.dp, 30.dp)
                    .clickable {
                        // Launch Photo Picker for images only
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "@$username",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight(400),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "You can select a picture from your gallery",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(400),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Bio(Optional)",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(400),
                color = MaterialTheme.colorScheme.onBackground,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(size = 12.dp)
                )
                .height(86.dp)
                .fillMaxWidth()
        ) {
            BasicTextField(
                value = bio,
                onValueChange = {
                    if (it.length <= maxBioLength) {
                        bio = it
                    }
                },
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 14.dp)
            )

            if (bio.isEmpty()) {
                Text(
                    text = "Write a short bio about yourself",
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 24.dp,top=14.dp)
                )
            }
            Text(
                text = "${bio.length}/$maxBioLength",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
    Spacer(modifier = Modifier.height(28.dp))
    GetStarted(
        onClick = {
            if (selectedImageUri != null) {
                uploadProfileImageToFirebaseStorage(
                    imageUri = selectedImageUri!!,
                    onSuccess = { downloadUrl ->
                        updateUserProfileInDatabase(username, downloadUrl, bio)
                        val intent = Intent(context, MessageActivity::class.java)
                        intent.putExtra("username", username)
                        intent.putExtra("profileUri", selectedImageUri?.toString() ?: "")
                        intent.putExtra("bio", bio)
                        context.startActivity(intent)
                    },
                    onFailure = { exception ->
                        Toast.makeText(context, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    )


}

@Composable
fun GetStarted(
    onClick: () -> Unit,
) {

    Surface(
        shape = RoundedCornerShape(25.dp),
        color = Color(0xFF2F9ECE),
        modifier = Modifier
            .height(54.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                onClick()
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            Alignment.CenterHorizontally
        ) {
            Text(
                text = "Get Started",
                color = Color(0xFFFFFFFF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

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

    val uploadTask = profileImageRef.putFile(imageUri)
    uploadTask.addOnSuccessListener {
        // Get the download URL to store in the Realtime Database
        profileImageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
            onSuccess(downloadUrl.toString()) // Call the success callback with the download URL
        }
    }.addOnFailureListener { exception ->
        onFailure(exception) // Handle the failure
    }
}

fun updateUserProfileInDatabase(username: String, profileImageUri: String, bio: String) {
    val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val database = Firebase.database.reference

    // Create a map of user data including bio
    val userData = mapOf(
        "username" to username,
        "profileImageUri" to profileImageUri,
        "bio" to bio
    )

    // Save the data to the Realtime Database
    database.child("users").child(userUid).updateChildren(userData)
        .addOnSuccessListener {
            Log.d("Firebase", "User profile updated successfully")
        }
        .addOnFailureListener { exception ->
            Log.e("Firebase", "Failed to update user profile: ${exception.message}")
        }
}