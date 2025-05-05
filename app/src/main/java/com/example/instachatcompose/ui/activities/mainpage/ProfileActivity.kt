package com.example.instachatcompose.ui.activities.mainpage

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import coil.compose.rememberAsyncImagePainter
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val friendId = intent.getStringExtra("friendId")
                    val groupId = intent.getStringExtra("groupId")

                    if (groupId != null) {
                        GroupProfileScreen(groupId)
                    } else {
                        FriendProfileScreen(friendId ?: "")
                    }
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

    ProfileScreen(
        title = username,
        subtitle = email,
        bio = bio,
        profileImage = profileImage,
        showFriendButton = true,
        isFriend = isFriend,
        onFriendButtonClick = {
            val currentUserId = currentUser?.uid ?: return@ProfileScreen
            val userFriendsRef = database.child("users").child(currentUserId).child("friends").push()
            val friendFriendsRef = database.child("users").child(friendId).child("friends").push()

            if (isFriend) {
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
        }
    )
}

@Composable
fun GroupProfileScreen(groupId: String) {
    val context = LocalContext.current
    val storageRef = FirebaseStorage.getInstance().reference
    val database = FirebaseDatabase.getInstance().reference
    val usersRef = database.child("users")
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var groupName by remember { mutableStateOf("Loading group...") }
    var groupImage by remember { mutableStateOf<String?>(null) }
    var members by remember { mutableStateOf<List<String>>(emptyList()) }

    var showEditDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val imageRef = storageRef.child("group_photos/group_$groupId.jpg")
            CoroutineScope(Dispatchers.IO).launch {
                imageRef.putFile(uri).await()
                val downloadUrl = imageRef.downloadUrl.await().toString()
                database.child("chats").child("group_$groupId").child("groupImage").setValue(downloadUrl)
                groupImage = downloadUrl
            }
        }
    }

    LaunchedEffect(groupId) {
        val groupSnapshot = database.child("chats").child("group_$groupId").get().await()
        groupName = groupSnapshot.child("groupName").getValue(String::class.java) ?: "Unnamed Group"
        groupImage = groupSnapshot.child("groupImage").getValue(String::class.java)

        val memberIds = groupSnapshot.child("members").children.mapNotNull { it.key }

        val usernames = mutableListOf<String>()
        for (memberId in memberIds) {
            val usernameSnapshot = usersRef.child(memberId).child("username").get().await()
            val username = usernameSnapshot.getValue(String::class.java) ?: memberId
            usernames.add(username)
        }
        members = usernames
    }

    val bioText = if (members.isEmpty()) {
        "No members found."
    } else {
        "Members: ${members.joinToString(", ")}"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileScreen(
            title = groupName,
            subtitle = "",
            bio = bioText,
            profileImage = groupImage,
            showFriendButton = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showEditDialog = true },
            colors = ButtonDefaults.buttonColors(Color(0xFF2F9ECE)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Edit Group Name", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Change Group Image
        Button(
            onClick = { imageLauncher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(Color(0xFF2F9ECE)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Change Group Photo", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (currentUserId != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        database.child("chats").child("group_$groupId").child("members").child(currentUserId).removeValue()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(Color.Red),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Leave Group", color = Color.White)
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        database.child("chats").child("group_$groupId").child("groupName").setValue(newGroupName)
                        groupName = newGroupName
                    }
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Edit Group Name") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("New Group Name") }
                )
            }
        )
    }
}


@Composable
fun ProfileScreen(
    title: String,
    subtitle: String,
    bio: String,
    profileImage: String?,
    showFriendButton: Boolean = false,
    isFriend: Boolean = false,
    onFriendButtonClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
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

            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 16.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
            }

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

            if (showFriendButton && onFriendButtonClick != null) {
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onFriendButtonClick,
                    colors = ButtonDefaults.buttonColors(if (isFriend) Color.Red else Color(0xFF2F9ECE)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(if (isFriend) "Remove Friend" else "Add Friend", color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}
