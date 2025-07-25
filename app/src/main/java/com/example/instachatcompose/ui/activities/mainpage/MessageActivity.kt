package com.example.instachatcompose.ui.activities.mainpage

import CameraPermissionWrapper
import CameraWithGallery
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.activities.Settings
import com.example.instachatcompose.ui.activities.konnekt.loadReceivedRequestsWithDetails
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import androidx.compose.runtime.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import coil.compose.rememberAsyncImagePainter
import com.android.identity.util.UUID
import com.example.instachatcompose.ui.activities.components.SharedBottomAppBar
import com.example.instachatcompose.ui.activities.data.AppDatabase
import com.example.instachatcompose.ui.activities.data.BottomAppBarItem
import com.example.instachatcompose.ui.activities.data.ChatViewModel
import com.example.instachatcompose.ui.activities.data.GroupEntity
import com.example.instachatcompose.ui.activities.data.KeystoreHelper
import com.example.instachatcompose.ui.activities.data.Message
import com.example.instachatcompose.ui.activities.data.MessageCrypto
import com.example.instachatcompose.ui.activities.data.NotificationHelper
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class MessageActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier= Modifier.fillMaxSize()) {
                        MessagePage()
                    }
                }
            }
        }
    }
}

@Composable
fun MessagePage() {
    val activity = LocalContext.current as? ComponentActivity
    val profilePic: Uri = Uri.parse(activity?.intent?.getStringExtra("profileUri") ?: "")
    val friendList = remember { mutableStateListOf<Pair<Friend, Map<String, String>>>() }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("Loading...") }
    var searchQuery by remember { mutableStateOf("") }
    val context= LocalContext.current
    val viewModel: ChatViewModel = viewModel()
    var isLoading by remember { mutableStateOf(true) }


    LaunchedEffect(userId) {
        viewModel.fetchUserProfile(context, userId) { fetchedUsername, fetchedProfilePicUrl ->
            username = fetchedUsername ?: "Unknown"
            profilePicUrl = fetchedProfilePicUrl
        }
    }

    LaunchedEffect(userId) {
        viewModel.loadFriendsWithDetails(userId) { friends ->
            friendList.clear()
            friendList.addAll(friends)
        }
    }
    LaunchedEffect(userId) {
        viewModel.loadGroupChats(userId)
    }

    LaunchedEffect(Unit) {
        viewModel.createNotificationChannel(context)
        delay(3000)
        isLoading = false
    }

    val navController = rememberNavController()
    Scaffold(
        topBar = {
            val currentBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = currentBackStackEntry?.destination?.route
            if (currentRoute != null && !currentRoute.startsWith("chat")) {
                User(username = username, profilePicUrl = profilePicUrl, userId = userId, searchQuery = searchQuery, onSearchQueryChange = {searchQuery= it}, navController, viewModel)
            }
        },
        bottomBar = {
            val currentBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = currentBackStackEntry?.destination?.route
            if (currentRoute != null && !currentRoute.startsWith("chat")) {
                SharedBottomAppBar(username = username, profilePic = profilePic, startingTab = BottomAppBarItem.Messages)
            }
        },
        floatingActionButton = {
            val currentBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = currentBackStackEntry?.destination?.route
            if (currentRoute == "friends" || currentRoute == "archives") {
                FloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    modifier = Modifier.padding(bottom = 6.dp),
                    containerColor = Color(0xFF2F9ECE)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Group", tint = Color.White)
                }
            }
        }

    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = if (friendList.isEmpty()) "message" else "friends",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("message") { MessageFrag(username = username) }
                composable("friends") {
                    FriendsListScreen(
                        friendList = friendList,
                        navController = navController,
                        currentUserId = userId,
                        searchQuery = searchQuery,
                        viewModel
                    )
                }
                composable("chat") {
                    ChatScreen(navController, viewModel)
                }

                composable("archives") {
                    ArchiveScreen(navController = navController, viewModel, currentUserId = userId)
                }

            }
        }
    }
    if (showCreateGroupDialog) {
        CreateGroupBottomSheet(onDismiss = { showCreateGroupDialog = false }, friendList = friendList)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(navController: NavController, viewModel: ChatViewModel, currentUserId: String) {
    val archivedFriends by viewModel.archivedFriends.collectAsState()
    val archivedGroups by viewModel.archivedGroups.collectAsState()
    val context= LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val friendDao = db.friendDao()
    var showUnarchiveDialog by remember { mutableStateOf(false) }
    var selectedArchivedFriend by remember { mutableStateOf<Friend?>(null) }
    var selectedArchivedGroup by remember { mutableStateOf<GroupChat?>(null) }
    var showRemoveFriendDialog by remember { mutableStateOf(false) }
    var showLeaveGroupDialog by remember { mutableStateOf(false) }
    var friendToRemove by remember { mutableStateOf<Friend?>(null) }
    var groupToLeave by remember { mutableStateOf<GroupChat?>(null) }


    LaunchedEffect(Unit) {
        viewModel.fetchArchivedChats(currentUserId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Archived Messages", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        if (archivedFriends.isEmpty() && archivedGroups.isEmpty()) {
            Text("No archived chats")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(archivedFriends) { friend ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                selectedArchivedFriend = friend
                                selectedArchivedGroup = null
                                showUnarchiveDialog = true
                                false

                            } else true
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                        content = {
                            FriendRow(friend, navController, currentUserId, viewModel)

                        }
                    )
                }

                items(archivedGroups) { group ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                selectedArchivedGroup = group
                                selectedArchivedFriend = null
                                showUnarchiveDialog = true
                                false

                            } else true
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                        content = {
                            GroupAsFriendRow(group, navController, currentUserId)
                        }
                    )
                }
            }
        }
    }
    ChatOptionsDialog(
        showDialog = showUnarchiveDialog,
        onDismiss = {
            showUnarchiveDialog = false
        },
        currentUserId = currentUserId,
        isArchived = true,
        friend = selectedArchivedFriend,
        group = selectedArchivedGroup,
        context = context,
        viewModel = viewModel,
        onConfirmRemoveFriend = {
            friendToRemove = selectedArchivedFriend
            showRemoveFriendDialog = true
        },
        onConfirmLeaveGroup = {
            groupToLeave = selectedArchivedGroup
            showLeaveGroupDialog = true
        }
    )

    ConfirmDialog(
        show = showRemoveFriendDialog && friendToRemove != null,
        title = "Remove Friend",
        message = "Are you sure you want to remove this friend?",
        onDismiss = { showRemoveFriendDialog = false },
        onConfirm = {
            friendToRemove?.let {
                viewModel.removeFriendFromDatabase(currentUserId, it.friendId, friendDao)
                Toast.makeText(context, "Friend removed", Toast.LENGTH_SHORT).show()
            }
            showRemoveFriendDialog = false
        }
    )

    ConfirmDialog(
        show = showLeaveGroupDialog && groupToLeave != null,
        title = "Leave Group",
        message = "Are you sure you want to leave '${groupToLeave?.groupName}'?",
        onDismiss = { showLeaveGroupDialog = false },
        onConfirm = {
            groupToLeave?.let {
                viewModel.leaveGroup(currentUserId, it.groupId)
                viewModel.removeGroupChat(it.groupId)
                Toast.makeText(context, "You left the group", Toast.LENGTH_SHORT).show()
            }
            showLeaveGroupDialog = false
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    friendList: List<Pair<Friend, Map<String, String>>>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var groupName by remember { mutableStateOf("") }
    val selectedFriends = remember { mutableStateListOf<String>() }
    var groupImageUri by remember { mutableStateOf<Uri?>(null) }

    val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "instachat_db"
    ).build()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        groupImageUri = uri
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("New Group", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (groupImageUri != null) {
                    AsyncImage(
                        model = groupImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))

                TextButton(onClick = { launcher.launch("image/*") }) {
                    Text(text = "Select Image", color = Color(0xFF2F9ECE))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Add Friends", fontWeight = FontWeight.SemiBold)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friendList, key = { (friend, _) ->
                    friend.friendId.ifBlank { UUID.randomUUID().toString() }
                }) { (friend, details) ->
                    val friendId = friend.friendId
                    val username = details["username"] ?: "Unknown"
                    val profileImage = details["profileImageUri"] ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Checkbox(
                            checked = selectedFriends.contains(friendId),
                            onCheckedChange = {
                                if (it) selectedFriends.add(friendId)
                                else selectedFriends.remove(friendId)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val currentUserId = currentUser?.uid
                    val currentUserName = currentUser?.displayName ?: "Someone"

                    if (selectedFriends.size < 2) {
                        Toast.makeText(context, "Select at least two members", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (groupName.isBlank() || currentUserId == null) {
                        Toast.makeText(context, "Enter group name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val groupId = "group_${UUID.randomUUID()}"
                    TempGroupIdHolder.groupId = groupId

                    val members = selectedFriends.toMutableList().apply {
                        if (!contains(currentUserId)) add(currentUserId)
                    }

                    val groupData = mapOf(
                        "groupName" to groupName,
                        "members" to members.associateWith { true },
                        "groupImage" to null,
                        "adminId" to currentUserId
                    )

                    fun notifyMembersAndDismiss() {
                        NotificationHelper.showNotification(
                            context,
                            title = "Group Created",
                            message = "You have now created \"$groupName\""
                        )

                        val usersRef = FirebaseDatabase.getInstance().getReference("users")
                        selectedFriends.filter { it != currentUserId }.forEach { memberId ->
                            usersRef.child(memberId).get().addOnSuccessListener { snapshot ->
                                val username = snapshot.child("username").getValue(String::class.java) ?: "Someone"
                                NotificationHelper.showNotification(
                                    context,
                                    title = "Added to Group",
                                    message = "$currentUserName added you to \"$groupName\""
                                )
                            }
                        }

                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    }

                    if (groupImageUri != null) {
                        val storageRef = FirebaseStorage.getInstance().reference
                            .child("group_images/$groupId/profile_image.jpg")
                        storageRef.putFile(groupImageUri!!)
                            .addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    val groupDataWithImage = groupData.toMutableMap().apply {
                                        this["groupImage"] = downloadUrl.toString()
                                    }

                                    FirebaseDatabase.getInstance().getReference("chats")
                                        .child(groupId)
                                        .setValue(groupDataWithImage)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Group created with image", Toast.LENGTH_SHORT).show()
                                            notifyMembersAndDismiss()
                                        }

                                    CoroutineScope(Dispatchers.IO).launch {
                                        val groupEntity = GroupEntity(
                                            groupId = groupId,
                                            userId = currentUserId,
                                            groupName = groupName,
                                            groupImageUri = downloadUrl.toString(),
                                            memberIds = members.joinToString(",")
                                        )
                                        db.groupDao().insertGroup(groupEntity)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        FirebaseDatabase.getInstance().getReference("chats")
                            .child(groupId)
                            .setValue(groupData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Group created", Toast.LENGTH_SHORT).show()
                                notifyMembersAndDismiss()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Group creation failed", Toast.LENGTH_SHORT).show()
                            }
                    }
                }) {
                    Text("Create")
                }
            }
        }
    }
}

object TempGroupIdHolder {
    var groupId: String = " "
}

@Composable
fun User(username: String, profilePicUrl: String?, userId: String,   searchQuery: String, onSearchQueryChange: (String) -> Unit, navController: NavController, viewModel: ChatViewModel) {
    val settingsIcon = painterResource(id = R.drawable.settings)
    val searchIcon = painterResource(id = R.drawable.searchicon)
    val context = LocalContext.current as ComponentActivity
    val requestCount = fetchReceivedRequestsCount(userId).value
    val count by viewModel.friendRequestCount.collectAsState()

    Column {
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                if (!profilePicUrl.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = profilePicUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(31.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .scale(1.5f)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.nopfp), // Add a default profile image
                        contentDescription = "Default Profile",
                        modifier = Modifier
                            .size(31.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = username,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight(400),
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                )
            }

            Row(modifier = Modifier.clickable {
                val intent = Intent(context, Settings::class.java)
                context.startActivity(intent)
            }) {
                Image(
                    painter = settingsIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Settings",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight(400),
                        color = Color(0xFF2F9ECE),
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Messages",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight(500),
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = Color(0x33333333),
                    shape = RoundedCornerShape(size = 20.dp)
                )
                .height(48.dp)
                .width(444.dp)
        ) {
            Image(
                painter = searchIcon,
                contentDescription = "Search",
                modifier = Modifier
                    .size(35.dp)
                    .padding(top = 15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, start = 27.dp)
            )
            if (searchQuery.isEmpty()) {
                Text(
                    text = "Search Friends",
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 27.dp, top = 14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Requests($count)",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight(400),
                    color = Color(0xFF2F9ECE),
                ),
            )
            Text(
                text = "Archives()",
                modifier = Modifier.clickable {
                    navController.navigate("archives")
                },
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight(400),
                    color = Color(0xFF2F9ECE),
                ),
            )
        }
    }
}


@Composable
fun fetchReceivedRequestsCount(userId: String): State<Int> {
    val requestCount = remember { mutableIntStateOf(0) }
    LaunchedEffect(userId) {
        loadReceivedRequestsWithDetails(userId) { receivedRequests ->
            requestCount.intValue = receivedRequests.size
        }
    }
    return requestCount
}

@Composable
fun MessageFrag(username: String){
    val messageConnected= painterResource(id = R.drawable.messagechats)

    Column(
        modifier= Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Image(
            painter = messageConnected,
            contentDescription = null,
            modifier= Modifier
                .width(160.95.dp)
                .height(160.18.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Welcome, $username",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight(600),
                color = Color(0xFF2F9ECE),
            ),

            )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You don’t have any friend yet, click on the “Konnekt” button below to add friends and start connecting.",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight(500),
                color = MaterialTheme.colorScheme.onBackground,
            ),
            modifier = Modifier.width(300.dp)
        )


    }

}

data class Friend(
    val friendId: String = "",
    val timestamp: Long = 0L
)

data class GroupChat(
    val groupId: String,
    val groupName: String,
    val members: List<String> = emptyList(),
    val groupImage: String = ""
){
    // Firebase needs this
    constructor() : this("", "", emptyList())
}


suspend fun fetchGroupChats(currentUserId: String): List<GroupChat> = suspendCoroutine { cont ->
    val dbRef = FirebaseDatabase.getInstance().reference.child("chats")
    val usersRef = FirebaseDatabase.getInstance().reference.child("users")

    dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            CoroutineScope(Dispatchers.IO).launch {
                val groupChats = snapshot.children.mapNotNull { groupSnapshot ->
                    val key = groupSnapshot.key ?: return@mapNotNull null
                    if (!key.startsWith("group_")) return@mapNotNull null

                    val membersSnapshot = groupSnapshot.child("members")
                    val memberIds = membersSnapshot.children.mapNotNull { it.key }

                    if (currentUserId !in memberIds) return@mapNotNull null

                    val groupName = groupSnapshot.child("groupName").getValue(String::class.java) ?: "Unnamed Group"
                    val groupId = key.removePrefix("group_")
                    val groupImage = groupSnapshot.child("groupImage").getValue(String::class.java) ?: ""

                    val members = memberIds.map { memberId ->
                        val usernameSnapshot = usersRef.child(memberId).child("username").get().await()
                        usernameSnapshot.getValue(String::class.java) ?: memberId
                    }

                    GroupChat(
                        groupId = groupId,
                        groupName = groupName,
                        members = members,
                        groupImage = groupImage
                    )
                }

                cont.resume(groupChats)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            cont.resume(emptyList())
        }
    })
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(friendList: List<Pair<Friend, Map<String, String>>>, navController: NavController, currentUserId: String, searchQuery: String, viewModel: ChatViewModel) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var friendToRemove by remember { mutableStateOf<Friend?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var groupToLeave by remember { mutableStateOf<GroupChat?>(null) }
    val db = AppDatabase.getDatabase(context)
    val friendDao = db.friendDao()
    val combinedList by viewModel.combinedChatList.collectAsState()
    var showActionDialog by remember { mutableStateOf(false) }
    var selectedFriend by remember { mutableStateOf<Friend?>(null) }
    var selectedGroup by remember { mutableStateOf<GroupChat?>(null) }
    val archivedFriends by viewModel.archivedFriends.collectAsState()
    val archivedGroups by viewModel.archivedGroups.collectAsState()
    val isArchiveInitialized by viewModel.isArchiveInitialized.collectAsState()
    val groupChats by viewModel.groupChats.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchArchivedChats(currentUserId)
    }
    LaunchedEffect(groupChats,isArchiveInitialized) {
        if (isArchiveInitialized) {
            viewModel.refreshCombinedChatList(
                currentUserId,
                friendList,
                searchQuery,
                context,
                groupChats
            )
        }
    }
    val filteredCombinedList = combinedList.filter {
        when (it) {
            is ChatItem.FriendItem -> !archivedFriends.any { archived -> archived.friendId == it.friend.friendId }
            is ChatItem.GroupItem -> !archivedGroups.any { archived -> archived.groupId == it.group.groupId }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredCombinedList) { item ->
            when (item) {
                is ChatItem.FriendItem -> {
                    val friend = item.friend
//                    val details = item.details

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                selectedFriend = friend
                                selectedGroup = null
                                showActionDialog = true
                                false
                            } else true
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                        content = {
                            FriendRow(friend, navController, currentUserId, viewModel)
                        }
                    )
                }
                is ChatItem.GroupItem -> {
                    val group = item.group
                    val groupDismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                selectedGroup = group
                                selectedFriend = null
                                showActionDialog = true
                                false
                            } else true
                        }
                    )
                    SwipeToDismissBox(
                        state = groupDismissState,
                        backgroundContent = {},
                        content = {
                            GroupAsFriendRow(group, navController, currentUserId)
                        }
                    )
                }
            }
        }
    }

    ChatOptionsDialog(
        showDialog = showActionDialog,
        onDismiss = { showActionDialog = false },
        currentUserId = currentUserId,
        isArchived = false,
        friend = selectedFriend,
        group = selectedGroup,
        context = context,
        viewModel = viewModel,
        onConfirmRemoveFriend = {
            friendToRemove = selectedFriend
            showDialog = true
        },
        onConfirmLeaveGroup = {
            groupToLeave = selectedGroup
            showGroupDialog = true
        }
    )

    ConfirmDialog(
        show = showDialog && friendToRemove != null,
        title = "Remove Friend",
        message = "Are you sure you want to remove this friend?",
        onDismiss = { showDialog = false },
        onConfirm = {
            friendToRemove?.let { friend ->
                viewModel.removeFriendFromDatabase(currentUserId, friend.friendId, friendDao)
                Toast.makeText(
                    context,
                    "Friend removed",
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.refreshCombinedChatList(
                    currentUserId,
                    friendList.filterNot { it.first.friendId == friend.friendId },
                    searchQuery,
                    context,
                    viewModel.groupChats.value
                )
            }
            showDialog = false
        }
    )

    ConfirmDialog(
        show = showGroupDialog && groupToLeave != null,
        title = "Leave Group",
        message = "Are you sure you want to leave '${groupToLeave?.groupName}'?",
        onDismiss = { showGroupDialog = false },
        onConfirm = {
            groupToLeave?.let { group ->
                viewModel.leaveGroup(currentUserId, group.groupId)
                viewModel.removeGroupChat(group.groupId)
                Toast.makeText(
                    context,
                    "You left '${group.groupName}'",
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.refreshCombinedChatList(
                    currentUserId,
                    friendList,
                    searchQuery,
                    context,
                    viewModel.groupChats.value.filterNot { it.groupId == group.groupId }
                )
            }
            showGroupDialog = false
        }
    )
}

@Composable
fun ChatOptionsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    currentUserId: String,
    isArchived: Boolean,
    friend: Friend? = null,
    group: GroupChat? = null,
    context: Context,
    viewModel: ChatViewModel,
    onConfirmRemoveFriend: () -> Unit,
    onConfirmLeaveGroup: () -> Unit
) {
    if (!showDialog) return

    val isFriend = friend != null
    val name = group?.groupName ?: "this friend"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat Options") },
        text = {
            Column {
                Text("What would you like to do with $name?")
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isArchived) {
                            friend?.let { viewModel.unarchiveItem(currentUserId, it) }
                            group?.let { viewModel.unarchiveItem(currentUserId, it) }
                        } else {
                            friend?.let {
                                viewModel.archiveItem(currentUserId, it)
                                Toast.makeText(context, "$name archived", Toast.LENGTH_SHORT).show()
                            }
                            group?.let {
                                viewModel.archiveItem(currentUserId, it)
                                Toast.makeText(context, "$name archived", Toast.LENGTH_SHORT).show()
                            }
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isArchived) "Unarchive" else "Archive")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isFriend) {
                    Button(
                        onClick = {
                            onConfirmRemoveFriend()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Remove Friend") }
                } else {
                    Button(
                        onClick = {
                            onConfirmLeaveGroup()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Leave Group") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel") }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun ConfirmDialog(
    show: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}


@Composable
fun FriendRow(
    friend: Friend,
    navController: NavController,
    currentUserId: String,
    viewModel: ChatViewModel
) {
    var details by remember { mutableStateOf<Map<String, String>?>(null) }

    val friendUsername = details?.get("username") ?: "Unknown"
    val friendProfileUri = details?.get("profileImageUri") ?: ""

    val chatId = if (currentUserId < friend.friendId) {
        "${currentUserId}_${friend.friendId}"
    } else {
        "${friend.friendId}_${currentUserId}"
    }

    var lastMessage by remember { mutableStateOf(" ") }
    var hasUnreadMessages by remember { mutableStateOf(false) }


    LaunchedEffect(friend.friendId) {
        viewModel.getFriendDetails(friend.friendId) {
            details = it
        }
    }


    DisposableEffect(chatId) {
        val db = Firebase.database.reference.child("chats").child(chatId).child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }

                if (messages.isNotEmpty()) {
                    val last = messages.last()

                    val decryptedText = try {
                        MessageCrypto.decrypt(last.text, last.iv)
                    } catch (e: Exception) {
                        "[error decrypting]"
                    }

                    lastMessage = decryptedText
                }

                hasUnreadMessages = messages.any {
                    it.receiverId == currentUserId && !it.seen
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FriendsListScreen", "Error fetching messages: ${error.message}")
            }
        }

        db.addValueEventListener(listener)

        onDispose {
            db.removeEventListener(listener)
        }
    }


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.apply {
                        set("friendId", friend.friendId)
                        set("username", friendUsername)
                        set("profileImageUri", friendProfileUri)
                        set("chatId", chatId)
                        set("currentUserId", currentUserId)
                        set("isGroupChat", false)
                    }
                navController.navigate("chat")
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (friendProfileUri.isNotEmpty()) {
            AsyncImage(
                model = friendProfileUri,
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = friendUsername,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = lastMessage,
                style = TextStyle(fontSize = 14.sp, color = Color.Gray),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hasUnreadMessages) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2F9ECE))
            )
        }
    }
}

@Composable
fun GroupAsFriendRow(group: GroupChat, navController: NavController, currentUserId: String) {
    var lastMessage by remember { mutableStateOf("Loading...") }
    var hasUnreadMessages by remember { mutableStateOf(false) }

    DisposableEffect(group.groupId) {
        val db = Firebase.database.reference
            .child(group.groupId)
            .child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                if (messages.isNotEmpty()) {
                    val latestMessage = messages.last()
                    val senderName = latestMessage.senderName
                    lastMessage = "$senderName: ${latestMessage.text}"

                    hasUnreadMessages = messages.any {
                        it.receiverId == currentUserId && !it.seen
                    }
                } else {
                    lastMessage = "No messages yet"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupRow", "Error fetching group messages: ${error.message}")
            }
        }

        db.addValueEventListener(listener)

        onDispose {
            db.removeEventListener(listener)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.apply {
                        set("groupId", group.groupId)
                        set("groupName", group.groupName)
                        set("groupImageUri", group.groupImage)
                        set("currentUserId", currentUserId)
                        set("chatId", group.groupId)
                        set("isGroupChat", true)
                    }
                navController.navigate("chat")
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (group.groupImage.isNotEmpty()) {
            AsyncImage(
                model = group.groupImage,
                contentDescription = "Group Image",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.groupName,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = lastMessage,
                style = TextStyle(fontSize = 14.sp, color = Color.Gray),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hasUnreadMessages) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2F9ECE))
            )
        }
    }
}

sealed class ChatItem {
    data class FriendItem(
        val friend: Friend,
        val details: Map<String, String>,
        val timestamp: Long
    ) : ChatItem()

    data class GroupItem(
        val group: GroupChat,
        val timestamp: Long,

    ) : ChatItem()
}

@Composable
fun ChatScreen(navController: NavController, viewModel: ChatViewModel) {
   lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    var showCamera by remember { mutableStateOf(false) }
    var currentUsername by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var showMentionDropdown by remember { mutableStateOf(false) }
    var mentionQuery by remember { mutableStateOf("") }
    var cursorPosition by remember { mutableIntStateOf(0) }
    var isChatOpen by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var groupMembers by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val isGroupChat = savedStateHandle?.get<Boolean>("isGroupChat") ?: false
    val username = if (isGroupChat) {
        savedStateHandle?.get<String>("groupName") ?: "Group Chat"
    } else {
        savedStateHandle?.get<String>("username") ?: "Unknown"
    }
    val profileImageUri = if (isGroupChat) {
        savedStateHandle?.get<String>("groupImageUri")?.let { Uri.parse(it) } ?: Uri.EMPTY
    } else {
        savedStateHandle?.get<String>("profileImageUri")?.let { Uri.parse(it) } ?: Uri.EMPTY
    }
    val chatId = savedStateHandle?.get<String>("chatId") ?: ""
    val currentUserId = savedStateHandle?.get<String>("currentUserId") ?: ""
    val receiverUserId = if (isGroupChat) "" else savedStateHandle?.get<String>("friendId") ?: ""
    val db = Firebase.database.reference
    val firebaseChatId = if (isGroupChat) chatId.removePrefix("group_") else chatId
    val messagesRef = db.child("chats").child(firebaseChatId).child("messages")
    val typingRef = db.child("chats").child(chatId).child("typing")
    val messages by viewModel.messages
        .map { it.sortedByDescending { msg -> msg.timestamp } }
        .collectAsState(initial = emptyList())
    val isFriendTyping by viewModel.isFriendTyping.collectAsState()
    val filteredMembers = groupMembers.filter {
        it.startsWith(mentionQuery, ignoreCase = true)
    }
    var hasPermission by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Handle image URI after selection
            // Show image with bottom text bar
        }
    }
    val capturedImageUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            capturedImageUri.value?.let { uri ->
                // Show image with bottom text bar
            }
        }
    }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }



    CameraPermissionWrapper {
        hasPermission = true
    }

    LaunchedEffect(currentUserId) {
        viewModel.fetchCurrentUserName(currentUserId) { name ->
            currentUsername = name ?: "Unknown"
        }
    }
    LaunchedEffect(chatId, isChatOpen) {
        if (!isChatOpen) return@LaunchedEffect
        messagesRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = mutableListOf<Message>()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        if (message.deletedFor?.containsKey(currentUserId) == true) continue

                        if (message.receiverId == currentUserId && !message.seen && isChatOpen) {
                            messageSnapshot.ref.child("seen").setValue(true)
                        }

                        if (!message.iv.isNullOrBlank() && !message.text.isNullOrBlank()) {
                            try {
                                message.text = MessageCrypto.decrypt(message.text, message.iv)
                            } catch (e: Exception) {
                                Log.e("ChatScreen", "Decryption failed: ${e.message}")
                                message.text = "[error decrypting]"
                            }
                        }
                        messageList.add(message)
                    }
                }
                viewModel.updateMessages(messageList)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatScreen", "Failed to load messages: ${error.message}")
            }
        })
    }

    DisposableEffect(Unit) {
        isChatOpen = true
        onDispose { isChatOpen = false }
    }
    LaunchedEffect(chatId) {
        viewModel.observeMessages( context, chatId, currentUserId,
            isChatOpen = true,
            requestNotificationPermission = {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        })
        if (isGroupChat) {
            val groupChatList = fetchGroupChats(currentUserId)
            val group = groupChatList.find { it.groupId == chatId.removePrefix("group_") }
            groupMembers = group?.members ?: emptyList()
        } else {
            viewModel.observeTyping(chatId, receiverUserId)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable {
                    val intent = Intent(context, ProfileActivity::class.java).apply {
                        if (isGroupChat) {
                            putExtra("groupId", chatId.removePrefix("group_"))
                        } else {
                            putExtra("friendId", receiverUserId)
                        }
                    }
                    context.startActivity(intent)
                },
                    verticalAlignment = Alignment.CenterVertically
        ) {
            if (profileImageUri != Uri.EMPTY) {
                AsyncImage(
                    model = profileImageUri.toString(),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFF2F9ECE), CircleShape)
                        .background(Color.LightGray)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = username,
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
                if (isGroupChat) {
                    Text(
                        text = groupMembers.joinToString(", "),
                        style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                    )
                }
                if (isFriendTyping) {
                    Text(
                        text = "typing...",
                        style = TextStyle(fontSize = 14.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        currentUserId = currentUserId,
                        onReply = { replyingTo = it },
                        isGroupChat = true,
                        onEdit ={ messageToEdit ->
                            messageText = messageToEdit.text
                            editingMessageId = messageToEdit.id
                        },
                        onDeleteForSelf = { msg ->
                            val specificMessageRef = db.child("chats").child(chatId).child("messages").child(msg.id)
                            specificMessageRef.child("deletedFor").child(currentUserId).setValue(true)
                        },
                        onDeleteForEveryone = { msg ->
                            if (msg.id.isNotBlank()) {
                                val specificMessageRef = db.child("chats").child(chatId).child("messages").child(msg.id)
                                specificMessageRef.removeValue()
                                    .addOnSuccessListener {
                                        Log.d("ChatScreen", "Message deleted for everyone")
                                    }
                                    .addOnFailureListener { error ->
                                        Log.e("ChatScreen", "Failed to delete message: ${error.message}")
                                    }
                            } else {
                                Log.e("ChatScreen", "Error: Message ID is blank")
                            }
                        }
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(8.dp)
                .imePadding()
        ) {
            replyingTo?.let { message ->
                val senderName = if (message.senderId == currentUserId) "You" else if(isGroupChat) message.senderName
                    else username
                val replyBackgroundColor = if (isSystemInDarkTheme()) Color.DarkGray else Color.LightGray
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(replyBackgroundColor, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = senderName,
                            color = Color(0xFF2F9ECE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = message.text,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp
                        )
                    }
                    IconButton(onClick = { replyingTo = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel Reply")
                    }
                }
            }
            editingMessageId?.let { messageId ->
                val replyBackgroundColor = if (isSystemInDarkTheme()) Color.DarkGray else Color.LightGray

                val editingMessage = messages.find { it.id == messageId }
                editingMessage?.let { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(replyBackgroundColor, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Editing Message",
                                color = Color(0xFF2F9ECE),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = message.text,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp
                            )
                        }
                        IconButton(onClick = { editingMessageId = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel Edit")
                        }
                    }
                }
            }
            val backgroundColor = if (isSystemInDarkTheme()) Color(0xFF333333) else Color(0xFFF0F0F0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (replyingTo != null) 8.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = {
                        messageText = it
                        val cursorIndex = it.length
                        cursorPosition = cursorIndex
                        val textUpToCursor = it.take(cursorIndex)
                        val mentionMatch = Regex("""@(\w+)$""").find(textUpToCursor)
                        if (mentionMatch != null) {
                            mentionQuery = mentionMatch.groupValues[1]
                            showMentionDropdown = true
                        } else {
                            showMentionDropdown = false
                        }
                        typingRef.child(currentUserId).setValue(it.isNotEmpty())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .background(backgroundColor, shape = RoundedCornerShape(24.dp)),
                    placeholder = { Text("Type a message...", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF2F9ECE),
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                DropdownMenu(
                    expanded = showMentionDropdown && mentionQuery.isNotBlank(),
                    onDismissRequest = { showMentionDropdown = false }
                ) {
                    filteredMembers.forEach { member ->
                        DropdownMenuItem(
                            text = { Text(member) },
                            onClick = {
                                val beforeCursor = messageText.take(cursorPosition)
                                val afterCursor = messageText.drop(cursorPosition)
                                val updatedBefore = beforeCursor.replace(Regex("@\\w+$"), "@$member ")
                                messageText = updatedBefore + afterCursor
                                showMentionDropdown = false
                                mentionQuery = ""
                            }
                        )
                    }
                }
                if (showCamera && hasPermission) {
                        CameraWithGallery(
                            onGalleryClick = { galleryLauncher.launch("image/*") },
                            onCaptureClick = {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    File(context.cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
                                )
                                capturedImageUri.value = uri
                                cameraLauncher.launch(uri)
                            },
                            onImageClick = { uri -> selectedImageUri= uri },
                            selectedImageUri = selectedImageUri,
                            onRemoveSelectedImage = { selectedImageUri = null }
                        )
                } else {
                    IconButton(
                        onClick = { showCamera = true }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.nopfpcam),
                            contentDescription = "camera",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(onClick = {
                    if (messageText.isBlank()) return@IconButton
                    KeystoreHelper.generateKeyIfNecessary()
                    try {
                        val (encryptedText, ivString) = MessageCrypto.encrypt(messageText.trim())
                        val messageKey = messagesRef.push().key ?: return@IconButton
                        val (replyEncryptedText, replyIvString) = replyingTo?.text?.let {
                            MessageCrypto.encrypt(it.trim())
                        } ?: (null to null)

                        val newMessage = Message(
                            id = messageKey,
                            senderId = currentUserId,
                            senderName = currentUsername,
                            receiverId = receiverUserId,
                            text = encryptedText,
                            iv = ivString,
                            timestamp = System.currentTimeMillis(),
                            seen = false,
                            replyTo = replyEncryptedText,
                            replyToIv = replyIvString,
                            edited = false
                        )
                        messagesRef.child(messageKey).setValue(newMessage)
                        messageText = ""
                        replyingTo = null
                    } catch (e: Exception) {
                        Log.e("ChatScreen", "Encryption failed: ${e.message}")
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }

        }
    }
    if (selectedImageUri != null) {
        Column(
            Modifier.fillMaxSize().background(Color.Black)
        ) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.White, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    // send the image and message logic
                    selectedImageUri = null
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Send Image")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    currentUserId: String,
    onReply: (Message) -> Unit,
    onEdit: (Message) -> Unit,
    onDeleteForSelf: (Message) -> Unit,
    onDeleteForEveryone: (Message) -> Unit,
    isGroupChat: Boolean = false
) {
    val senderColorMap = remember { mutableMapOf<String, Color>() }
    val senderColor = remember(message.senderId) {
        senderColorMap.getOrPut(message.senderId) { generateColorFromId(message.senderId) }
    }
    val isSentByUser = message.senderId == currentUserId
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = screenWidth * 0.7f
    val backgroundColor = if (isSentByUser) Color(0xFF2F9ECE) else if (isSystemInDarkTheme()) Color(0xFF333333) else Color(0xFFEEEEEE)
    val textColor = if (isSentByUser) Color.White else if (isSystemInDarkTheme()) Color.White else Color.Black
    var showMenu by remember { mutableStateOf(false) }
    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    var hasReplied by remember { mutableStateOf(false) }
    val currentTime = System.currentTimeMillis()
    val isEditable = (currentTime - message.timestamp) < (10 * 60 * 1000)
    val isDeletableForEveryone = (currentTime - message.timestamp) < (24 * 60 * 60 * 1000)

    val offsetX by animateFloatAsState(
        targetValue = rawOffsetX,
        animationSpec = tween(durationMillis = 200),
        label = ""
    )
    var menuPositionPx by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    var menuPositionDp by remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSentByUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .offset(x = offsetX.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            rawOffsetX = 0f
                            hasReplied = false
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        rawOffsetX = (rawOffsetX + dragAmount * 0.5f).coerceIn(-50f, 50f)
                        if (!hasReplied && (rawOffsetX <= -50f || rawOffsetX >= 50f)) {
                            onReply(message)
                            hasReplied = true
                        }
                    }
                }
                .onGloballyPositioned { coordinates ->
                    menuPositionPx = coordinates.boundsInWindow().bottomLeft
                    menuPositionDp = with(density) {
                        DpOffset(menuPositionPx.x.toDp(), menuPositionPx.y.toDp())
                    }
                }
                .combinedClickable(
                    onLongClick = { showMenu = true },
                    onClick = {}
                )
                .background(backgroundColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column {
                if (isGroupChat && !isSentByUser) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = senderColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                message.replyTo?.let {
                    val replyTextDecrypted = try {
                        MessageCrypto.decrypt(message.replyTo.trim(), message.replyToIv?.trim() ?: "")
                    } catch (e: Exception) {
                        "[error decrypting reply]"
                    }
                    Text(
                        text = "Replying to: $replyTextDecrypted",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                val annotatedText = buildAnnotatedString {
                    val regex = "@\\w+".toRegex()
                    var currentIndex = 0
                    regex.findAll(message.text).forEach { match ->
                        append(message.text.substring(currentIndex, match.range.first))
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)) {
                            append(match.value)
                        }
                        currentIndex = match.range.last + 1
                    }
                    append(message.text.substring(currentIndex))
                }

                Text(text = annotatedText, color = textColor)
            }
        }

        Box(
            modifier = Modifier
                .shadow(8.dp, shape = RoundedCornerShape(16.dp))
                .background(Color.White, shape = RoundedCornerShape(16.dp))
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                offset = menuPositionDp
            ) {
                DropdownMenuItem(
                    text = { Text("Reply") },
                    onClick = {
                        onReply(message)
                        showMenu = false
                    }
                )
                HorizontalDivider()
                if (isSentByUser && isEditable) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEdit(message)
                            showMenu = false
                        }
                    )
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = { Text("Delete for Self") },
                    onClick = {
                        onDeleteForSelf(message)
                        showMenu = false
                    }
                )
                HorizontalDivider()
                if (isSentByUser && isDeletableForEveryone) {
                    DropdownMenuItem(
                        text = { Text("Delete for Everyone") },
                        onClick = {
                            onDeleteForEveryone(message)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

fun generateColorFromId(id: String): Color {
    val colors = listOf(
        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF42A5F5),
        Color(0xFF26A69A), Color(0xFFFF7043), Color(0xFF66BB6A),
        Color(0xFFFFCA28), Color(0xFF7E57C2)
    )
    val index = (id.hashCode().absoluteValue) % colors.size
    return colors[index]
}

suspend fun fetchLastMessageTimestamp(chatId: String): Long {
    return try {
        val database = Firebase.database.reference
        val snapshot = database
            .child("chats")
            .child(chatId)
            .child("messages")
            .orderByChild("timestamp")
            .limitToLast(1)
            .get()
            .await()

        val message = snapshot.children.firstOrNull()
        message?.child("timestamp")?.getValue(Long::class.java) ?: 0L
    } catch (e: Exception) {
        Log.e("fetchLastMessageTimestamp", "Error fetching timestamp", e)
        0L
    }
}
