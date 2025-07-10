package com.example.instachatcompose.ui.activities.mainpage

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.activities.data.ChatViewModel
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.UUID

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
                        UserOrGroupProfileScreen(ProfileType.Group(groupId))
                    } else {
                        friendId?.let { ProfileType.Friend(it) }
                            ?.let { UserOrGroupProfileScreen(it) }
                    }
                }
            }
        }
    }
}
sealed class ProfileType {
    data class Friend(val id: String) : ProfileType()
    data class Group(val id: String) : ProfileType()
}

data class GroupMember(val id: String, val name: String)

@Composable
fun UserOrGroupProfileScreen(profileType: ProfileType) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val database = FirebaseDatabase.getInstance().reference
    val storageRef = FirebaseStorage.getInstance().reference
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val viewModel: ChatViewModel = viewModel()
    val friendList = remember { mutableStateListOf<Pair<Friend, Map<String, String>>>() }


    var title by remember { mutableStateOf("Loading...") }
    var subtitle by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf<String?>(null) }
    var profileImage by remember { mutableStateOf<String?>(null) }
    var isFriend by remember { mutableStateOf(false) }
//    var members by remember { mutableStateOf<List<String>>(emptyList()) }

    var showEditDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var adminId by remember { mutableStateOf<String?>(null) }
    var members by remember { mutableStateOf<List<GroupMember>>(emptyList()) }
    var selectedMember by remember { mutableStateOf<GroupMember?>(null) }
    var showMemberOptionsDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false)}

    LaunchedEffect(userId) {
        viewModel.loadFriendsWithDetails(userId) { friends ->
            friendList.clear()
            friendList.addAll(friends)
        }
    }


    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (profileType is ProfileType.Group) {
                val imageRef = storageRef.child("group_photos/group_${profileType.id}.jpg")
                CoroutineScope(Dispatchers.IO).launch {
                    imageRef.putFile(uri).await()
                    val downloadUrl = imageRef.downloadUrl.await().toString()
                    database.child("chats").child("group_${profileType.id}").child("groupImage").setValue(downloadUrl)
                    profileImage = downloadUrl
                }
            }
        }
    }

    LaunchedEffect(profileType) {
        when (profileType) {
            is ProfileType.Friend -> {
                database.child("users").child(profileType.id).get().addOnSuccessListener { snapshot ->
                    title = snapshot.child("username").value as? String ?: "Unknown"
                    subtitle = snapshot.child("email").value as? String ?: "No email"
                    bio = snapshot.child("bio").value as? String ?: "No bio"
                    profileImage = snapshot.child("profileImageUri").value as? String
                }

                database.child("users").child(currentUser?.uid ?: "").child("friends").get()
                    .addOnSuccessListener { snapshot ->
                        isFriend = snapshot.children.any { it.child("friendId").value == profileType.id }
                    }
            }

            is ProfileType.Group -> {
                val groupSnapshot = database.child("chats").child("group_${profileType.id}").get().await()
                title = groupSnapshot.child("groupName").getValue(String::class.java) ?: "Unnamed Group"
                profileImage = groupSnapshot.child("groupImage").getValue(String::class.java)
                adminId = groupSnapshot.child("adminId").getValue(String::class.java)
                val memberIds = groupSnapshot.child("members").children.mapNotNull { it.key }
                val usernames = mutableListOf<GroupMember>()
                for (memberId in memberIds) {
                    val usernameSnapshot = database.child("users").child(memberId).child("username").get().await()
                    var username = usernameSnapshot.getValue(String::class.java) ?: memberId
                    if (memberId == adminId) username += " - 👑"
                    usernames.add(GroupMember(id = memberId, name = username))
                }
                members = usernames

            }
        }
    }

    // Shared UI
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileScreen(
            title = title,
            subtitle = subtitle,
            bio = bio,
            members = members.takeIf { profileType is ProfileType.Group },
            profileImage = profileImage,
            showFriendButton = profileType is ProfileType.Friend,
            isFriend = isFriend,
            adminId = adminId,
            onMemberClick = { member ->
                if (currentUser?.uid == adminId) {
                    selectedMember = member
                    showMemberOptionsDialog = true
                }
            },
            onFriendButtonClick = if (profileType is ProfileType.Friend) {
                {
                    val friendId = profileType.id
                    val currentUserId = currentUser?.uid
                    val userFriendsRef =
                        currentUserId?.let { database.child("users").child(it).child("friends").push() }
                    val friendFriendsRef = database.child("users").child(friendId).child("friends").push()

                    if (isFriend) {
                        if (currentUserId != null) {
                            database.child("users").child(currentUserId).child("friends").get()
                                .addOnSuccessListener { snapshot ->
                                    snapshot.children.forEach { child ->
                                        if (child.child("friendId").value == friendId) {
                                            child.ref.removeValue()
                                        }
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

                        userFriendsRef?.setValue(friendshipData)?.addOnSuccessListener {
                            friendFriendsRef.setValue(reverseFriendshipData).addOnSuccessListener {
                                isFriend = true
                            }
                        }
                    }
                }
            } else null
        )

        if (profileType is ProfileType.Group) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showAddFriendDialog = true },
                colors = ButtonDefaults.buttonColors(Color(0xFF2F9ECE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Add a New Member", color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
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
                    currentUser?.uid?.let {
                        CoroutineScope(Dispatchers.IO).launch {
                            database.child("chats").child("group_${profileType.id}").child("members").child(it).removeValue()
                        }
                    }
                    val intent= Intent(context, MessageActivity::class.java)
                    context.startActivity(intent)

                },
                colors = ButtonDefaults.buttonColors(Color.Red),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Leave Group", color = Color.White)
            }
        }
    }
    if (showAddFriendDialog && profileType is ProfileType.Group) {
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    // Handle confirm action here
                    showAddFriendDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Add Friends") },
            text = {
                // Limit height to avoid AlertDialog overflow
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)) {

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(friendList, key = { pair ->
                            val friend = pair.first
                            friend.friendId.ifBlank { UUID.randomUUID().toString() }
                        }) { pair ->
                            val friend = pair.first
                            val details = pair.second

                            val friendId = friend.friendId
                            val username = details["username"] ?: "Unknown"
                            val profileImage = details["profileImageUri"] ?: ""

                            var isChecked by remember { mutableStateOf(false) }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (profileImage.isNotEmpty()) {
                                    AsyncImage(
                                        model = profileImage,
                                        contentDescription = "Profile Image",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.Gray)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = username,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 16.sp
                                )

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { isChecked = it }
                                )
                            }
                        }
                    }
                }
            }
        )
    }


    if (showEditDialog && profileType is ProfileType.Group) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        database.child("chats").child("group_${profileType.id}").child("groupName").setValue(newGroupName)
                        title = newGroupName
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
    if (showMemberOptionsDialog && selectedMember != null && profileType is ProfileType.Group) {
        AlertDialog(
            onDismissRequest = { showMemberOptionsDialog = false },
            title = { Text("Manage Member") },
            text = {
                Column {
                    Text("What would you like to do with ${selectedMember!!.name}?")
                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedMember!!.id != adminId) {
                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val groupAdminsRef = database.child("chats")
                                        .child("group_${profileType.id}")
                                        .child("adminId")

                                    groupAdminsRef.get().addOnSuccessListener { snapshot ->
                                        val value = snapshot.value
                                        val currentAdmins: MutableList<String> = when (value) {
                                            is String -> mutableListOf(value)
                                            is List<*> -> value.filterIsInstance<String>().toMutableList()
                                            else -> mutableListOf()
                                        }

                                        if (!currentAdmins.contains(selectedMember!!.id) && currentAdmins.size < 5) {
                                            groupAdminsRef.setValue(currentAdmins + selectedMember!!.id)
                                        }
                                    }

                                }
                                adminId = selectedMember!!.id
                                showMemberOptionsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Make Admin")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                database.child("chats")
                                    .child("group_${profileType.id}")
                                    .child("members")
                                    .child(selectedMember!!.id)
                                    .removeValue()
                            }
                            members = members.filterNot { it.id == selectedMember!!.id }
                            showMemberOptionsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove Member")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showMemberOptionsDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    title: String,
    subtitle: String,
    bio: String? = null,
    members: List<GroupMember>? = null,
    profileImage: String?,
    showFriendButton: Boolean = false,
    isFriend: Boolean = false,
    onFriendButtonClick: (() -> Unit)? = null,
    onMemberClick: ((GroupMember) -> Unit)? = null,
    adminId: String?
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
            Spacer(modifier = Modifier.height(20.dp))

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
                Column(modifier = Modifier.padding(16.dp)) {
                    bio?.let {
                        Text(
                            text = it,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (!members.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Group Members", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    members.forEach { member ->
                        AssistChip(
                            onClick = { onMemberClick?.invoke(member) },
                            label = { Text(text = member.name, fontSize = 14.sp) },
                            shape = RoundedCornerShape(20.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFDDEEFF),
                                labelColor = Color.Black
                            )
                        )
                    }
                }
            }
            if (showFriendButton && onFriendButtonClick != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onFriendButtonClick,
                    colors = ButtonDefaults.buttonColors(if (isFriend) Color.Red else Color(0xFF2F9ECE)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(
                        if (isFriend) "Remove Friend" else "Add Friend",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
