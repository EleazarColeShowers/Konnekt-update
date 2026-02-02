package com.el.konnekt.ui.activities.mainpage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import com.el.konnekt.R


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

data class MemberInfo(
    val memberId: String,
    val name: String,
    val isAdmin: Boolean
)

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
    var members by remember { mutableStateOf<List<MemberInfo>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var adminId by remember { mutableStateOf<String?>(null) }

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
        adminId = groupSnapshot.child("adminId").getValue(String::class.java)

        val memberIds = groupSnapshot.child("members").children.mapNotNull { it.key }

        val membersList = mutableListOf<MemberInfo>()
        for (memberId in memberIds) {
            val usernameSnapshot = usersRef.child(memberId).child("username").get().await()
            val username = usernameSnapshot.getValue(String::class.java) ?: memberId
            membersList.add(MemberInfo(memberId, username, memberId == adminId))
        }
        members = membersList
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileScreen(
            title = groupName,
            subtitle = "${members.size} members",
            bio = null,
            members = members,
            profileImage = groupImage,
            showFriendButton = false,
            isCurrentUserAdmin = currentUserId == adminId,
            currentUserId = currentUserId,
            onMemberClick = { memberId ->
                // Navigate to member's profile
                val intent = Intent(context, ProfileActivity::class.java)
                intent.putExtra("friendId", memberId)
                context.startActivity(intent)
            },
            onRemoveMember = { memberId ->
                CoroutineScope(Dispatchers.IO).launch {
                    database.child("chats").child("group_$groupId")
                        .child("members").child(memberId).removeValue().await()
                    // Refresh members list
                    val updatedSnapshot = database.child("chats").child("group_$groupId").get().await()
                    val updatedMemberIds = updatedSnapshot.child("members").children.mapNotNull { it.key }
                    val updatedMembersList = mutableListOf<MemberInfo>()
                    for (id in updatedMemberIds) {
                        val usernameSnapshot = usersRef.child(id).child("username").get().await()
                        val username = usernameSnapshot.getValue(String::class.java) ?: id
                        updatedMembersList.add(MemberInfo(id, username, id == adminId))
                    }
                    withContext(Dispatchers.Main) {
                        members = updatedMembersList
                    }
                }
            },
            onMakeAdmin = { memberId ->
                CoroutineScope(Dispatchers.IO).launch {
                    database.child("chats").child("group_$groupId")
                        .child("adminId").setValue(memberId).await()
                    // Update adminId and refresh members
                    adminId = memberId
                    val updatedMembersList = members.map { member ->
                        member.copy(isAdmin = member.memberId == memberId)
                    }
                    withContext(Dispatchers.Main) {
                        members = updatedMembersList
                    }
                }
            }
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
        Spacer(modifier = Modifier.height(16.dp))
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
    bio: String? = null,
    members: List<MemberInfo>? = null,
    profileImage: String?,
    showFriendButton: Boolean = false,
    isFriend: Boolean = false,
    onFriendButtonClick: (() -> Unit)? = null,
    isCurrentUserAdmin: Boolean = false,
    currentUserId: String? = null,
    onMemberClick: ((String) -> Unit)? = null,
    onRemoveMember: ((String) -> Unit)? = null,
    onMakeAdmin: ((String) -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Profile Picture
        AsyncImage(
            model = profileImage ?: R.drawable.nopfp,
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(3.dp, Color(0xFF2F9ECE), CircleShape)
                .background(Color.LightGray)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Subtitle
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, fontSize = 16.sp, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bio Section
        bio?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = it,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Members Section with improved UI
        members?.let { memberList ->
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Members (${memberList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    memberList.forEach { member ->
                        MemberItem(
                            member = member,
                            isCurrentUserAdmin = isCurrentUserAdmin,
                            currentUserId = currentUserId,
                            onMemberClick = onMemberClick,
                            onRemoveMember = onRemoveMember,
                            onMakeAdmin = onMakeAdmin
                        )
                        if (member != memberList.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }

        // Friend Button
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

@Composable
fun MemberItem(
    member: MemberInfo,
    isCurrentUserAdmin: Boolean = false,
    currentUserId: String? = null,
    onMemberClick: ((String) -> Unit)? = null,
    onRemoveMember: ((String) -> Unit)? = null,
    onMakeAdmin: ((String) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onMemberClick != null) {
                onMemberClick?.invoke(member.memberId)
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2F9ECE).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF2F9ECE),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = member.name,
                fontSize = 16.sp,
                fontWeight = if (member.isAdmin) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Admin badge
            if (member.isAdmin) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Admin",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Admin",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB8860B)
                        )
                    }
                }
            }

            // Admin controls menu
            if (isCurrentUserAdmin && member.memberId != currentUserId) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!member.isAdmin) {
                            DropdownMenuItem(
                                text = { Text("Make Admin") },
                                onClick = {
                                    onMakeAdmin?.invoke(member.memberId)
                                    showMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Remove from Group", color = Color.Red) },
                            onClick = {
                                onRemoveMember?.invoke(member.memberId)
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}