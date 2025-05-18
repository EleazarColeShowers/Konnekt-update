package com.example.instachatcompose.ui.activities.mainpage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import com.example.instachatcompose.ui.activities.konnekt.Konnekt
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import coil.compose.rememberAsyncImagePainter
import com.android.identity.util.UUID
import com.example.instachatcompose.ui.activities.data.AppDatabase
import com.example.instachatcompose.ui.activities.data.ChatViewModel
import com.example.instachatcompose.ui.activities.data.GroupEntity
import com.example.instachatcompose.ui.activities.data.Message
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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


    val navController = rememberNavController()

    Scaffold(
        topBar = {
            val currentBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = currentBackStackEntry?.destination?.route
            if (currentRoute != null && !currentRoute.startsWith("chat")) {
                User(username = username, profilePicUrl = profilePicUrl, userId = userId, searchQuery = searchQuery, onSearchQueryChange = {searchQuery= it})
            }
        },
        bottomBar = {
            val currentBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = currentBackStackEntry?.destination?.route
            if (currentRoute != null && !currentRoute.startsWith("chat")) {
                BottomAppBar(username = username, profilePic = profilePic)
            }
        },
        floatingActionButton = {
            val currentBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = currentBackStackEntry?.destination?.route
            if (currentRoute == "friends") {
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
            }
        }
    }
    if (showCreateGroupDialog) {
        CreateGroupBottomSheet(onDismiss = { showCreateGroupDialog = false }, friendList = friendList)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(friendList: List<Pair<Friend, Map<String, String>>>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var groupName by remember { mutableStateOf("") }
    val selectedFriends = remember { mutableStateListOf<String>() }
    var groupImageUri by remember { mutableStateOf<Uri?>(null) }
    val db = Room.databaseBuilder(
        context.applicationContext, // use applicationContext to avoid memory leaks
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
                    Text("Select Image")
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
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                    if (selectedFriends.size < 2) {
                        Toast.makeText(context, "Select at least two members", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (groupName.isNotBlank() && currentUserId != null) {
                        val groupId = "group_${UUID.randomUUID()}"
                        TempGroupIdHolder.groupId = groupId

                        val members = selectedFriends.toMutableList().apply {
                            if (!contains(currentUserId)) add(currentUserId)
                        }

                        if (groupImageUri != null) {
                            val storageRef = FirebaseStorage.getInstance().reference
                                .child("group_images/$groupId/profile_image.jpg")

                            storageRef.putFile(groupImageUri!!)
                                .addOnSuccessListener {
                                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                        val groupData = mapOf(
                                            "groupName" to groupName,
                                            "members" to members.associateWith { true },
                                            "groupImage" to downloadUrl.toString(),
                                            "adminId" to currentUserId
                                        )

                                        FirebaseDatabase.getInstance().getReference("chats")
                                            .child(groupId)
                                            .setValue(groupData)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Group created with image", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Group creation failed", Toast.LENGTH_SHORT).show()
                                            }
                                        val groupEntity = GroupEntity(
                                            groupId = groupId,
                                            userId= currentUserId,
                                            groupName = groupName,
                                            groupImageUri = downloadUrl.toString(),
                                            memberIds = members.joinToString(",") // Or use Gson to serialize to JSON
                                        )


                                        CoroutineScope(Dispatchers.IO).launch {
                                            db.groupDao().insertGroup(groupEntity)
                                        }
                                    }


                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            val groupData = mapOf(
                                "groupName" to groupName,
                                "members" to members.associateWith { true },
                                "groupImage" to null,
                                "adminId" to currentUserId
                            )

                            FirebaseDatabase.getInstance().getReference("chats")
                                .child(groupId)
                                .setValue(groupData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Group created", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Group creation failed", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(context, "Enter group name", Toast.LENGTH_SHORT).show()
                    }
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }

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
fun User(username: String, profilePicUrl: String?, userId: String,   searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    val settingsIcon = painterResource(id = R.drawable.settings)
    val searchIcon = painterResource(id = R.drawable.searchicon)
    val context = LocalContext.current as ComponentActivity
    val requestCount = fetchReceivedRequestsCount(userId).value
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
                text = "Requests($requestCount)",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight(400),
                    color = Color(0xFF2F9ECE),
                ),
            )
            Text(
                text = "Archives(1)",
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
    val members: List<String>,
    val groupImage: String = ""
)

suspend fun fetchGroupChats(currentUserId: String): List<GroupChat> = suspendCoroutine { cont ->
    val dbRef = FirebaseDatabase.getInstance().reference.child("chats")
    val usersRef = FirebaseDatabase.getInstance().reference.child("users")

    dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val groupChats = mutableListOf<GroupChat>()
            val allFetchTasks = mutableListOf<Job>()

            for (groupSnapshot in snapshot.children) {
                val key = groupSnapshot.key ?: continue
                if (!key.startsWith("group_")) continue

                val membersSnapshot = groupSnapshot.child("members")
                val memberIds = membersSnapshot.children.mapNotNull { it.key }

                if (currentUserId in memberIds) {
                    val groupName = groupSnapshot.child("groupName").getValue(String::class.java) ?: "Unnamed Group"
                    val groupId = key.removePrefix("group_")
                    val groupImage = groupSnapshot.child("groupImage").getValue(String::class.java) ?: ""

                    val members = mutableListOf<String>()

                    val job = CoroutineScope(Dispatchers.IO).launch {
                        memberIds.forEach { memberId ->
                            val usernameSnapshot = usersRef.child(memberId).child("username").get().await()
                            val username = usernameSnapshot.getValue(String::class.java) ?: memberId
                            members.add(username)
                        }
                    }
                    allFetchTasks.add(job)

                    val groupChat = GroupChat(
                        groupId = groupId,
                        groupName = groupName,
                        members = members, // This will be populated after usernames are fetched
                        groupImage = groupImage
                    )
                    groupChats.add(groupChat)
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                allFetchTasks.joinAll()
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
    val sortedFriendList by remember { mutableStateOf(friendList) }
    var showDialog by remember { mutableStateOf(false) }
    var friendToRemove by remember { mutableStateOf<Friend?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var groupToLeave by remember { mutableStateOf<GroupChat?>(null) }
    val db = AppDatabase.getDatabase(context)
    val friendDao = db.friendDao()
    val combinedList by viewModel.combinedChatList.collectAsState()

    LaunchedEffect(searchQuery, friendList, viewModel.groupChats) {
        viewModel.refreshCombinedChatList(currentUserId, friendList, searchQuery, context, viewModel.groupChats.value)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(combinedList) { item ->
            when (item) {
                is ChatItem.FriendItem -> {
                    val friend = item.friend
                    val details = item.details

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                friendToRemove = friend
                                showDialog = true
                                false
                            } else {
                                true
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                        content = {
                            FriendRow(friend, details, navController, currentUserId)
                        }
                    )
                }

                is ChatItem.GroupItem -> {
                    val groupDismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                groupToLeave = item.group
                                showGroupDialog = true
                                false
                            } else {
                                true
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = groupDismissState,
                        backgroundContent = {},
                        content = {
                            GroupAsFriendRow(
                                group = item.group,
                                navController = navController,
                                currentUserId = currentUserId
                            )
                        }
                    )

                }
            }
        }
    }
    if (showDialog && friendToRemove != null) {
        val usernameToRemove = friendToRemove?.let { friend ->
            sortedFriendList.find { it.first.friendId == friend.friendId }?.second?.get("username") ?: "this friend"
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove $usernameToRemove as a friend?") },
            confirmButton = {
                Button(
                    onClick = {
                        friendToRemove?.let { friend ->
                            viewModel.removeFriendFromDatabase(currentUserId, friend.friendId, friendDao)
//                            sortedFriendList = sortedFriendList.filterNot { it.first.friendId == friend.friendId }

                            Toast.makeText(
                                context,
                                "$usernameToRemove is no longer a friend",
                                Toast.LENGTH_SHORT
                            ).show()

                            viewModel.refreshCombinedChatList(
                                currentUserId,
                                friendList.filterNot { it.first.friendId == friend.friendId }, // updated list
                                searchQuery,
                                context,
                                viewModel.groupChats.value
                            )
                        }
                        showDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }
    if (showGroupDialog && groupToLeave != null) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text("Leave Group") },
            text = { Text("Are you sure you want to leave '${groupToLeave?.groupName}'?") },
            confirmButton = {
                Button(
                    onClick = {
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
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showGroupDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun FriendRow(friend: Friend, details: Map<String, String>, navController: NavController, currentUserId: String) {
    val friendUsername = details["username"] ?: "Unknown"
    val friendProfileUri = details["profileImageUri"] ?: ""

    val chatId = if (currentUserId < friend.friendId) {
        "${currentUserId}_${friend.friendId}"
    } else {
        "${friend.friendId}_${currentUserId}"
    }

    var lastMessage by remember { mutableStateOf("Loading...") }
    var hasUnreadMessages by remember { mutableStateOf(false) }

    DisposableEffect(chatId) {
        val db = Firebase.database.reference.child("chats").child(chatId).child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                if (messages.isNotEmpty()) {
                    lastMessage = messages.last().text
                }
                hasUnreadMessages = messages.any { it.receiverId == currentUserId && !it.seen }
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
                        set("groupImageUri", group.groupImage )
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
        val timestamp: Long
    ) : ChatItem()
}

@Composable
fun ChatScreen(navController: NavController, viewModel: ChatViewModel) {
    // State and context
    var currentUsername by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var showMentionDropdown by remember { mutableStateOf(false) }
    var mentionQuery by remember { mutableStateOf("") }
    var cursorPosition by remember { mutableStateOf(0) }
    var isChatOpen by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var groupMembers by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle

    // Get chat information
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

    // Firebase references
    val db = Firebase.database.reference
    val messagesRef = db.child("chats").child(chatId).child("messages")
    val typingRef = db.child("chats").child(chatId).child("typing")

    // ViewModel state
    val messages by viewModel.messages
        .map { it.sortedByDescending { msg -> msg.timestamp } }
        .collectAsState(initial = emptyList())
    val isFriendTyping by viewModel.isFriendTyping.collectAsState()
    val filteredMembers = groupMembers.filter {
        it.startsWith(mentionQuery, ignoreCase = true)
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
                        if (message.deletedFor?.containsKey(currentUserId) == true) {
                            continue
                        }
                        if (message.receiverId == currentUserId && !message.seen && isChatOpen) {
                            messageSnapshot.ref.child("seen").setValue(true)
                        }
                        messageList.add(message)
                    }
                }
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
        viewModel.observeMessages(chatId, currentUserId, isChatOpen = true)
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
                            putExtra("groupId", chatId.removePrefix("group_")) // Assuming chatId starts with "group_"
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
                                // Replace "@mentionQuery" with "@member"
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

                IconButton(onClick = {
                    if (messageText.isNotBlank()) {
                        if (editingMessageId != null) {
                            val messageUpdates = mapOf(
                                "text" to messageText,
                                "edited" to true
                            )
                            messagesRef.child(editingMessageId!!).updateChildren(messageUpdates)
                                .addOnSuccessListener {
                                    editingMessageId = null
                                    messageText = ""
                                }
                        } else {
                            val messageKey = messagesRef.push().key
                            if (messageKey != null) {
                                val newMessage = Message(
                                    id = messageKey,
                                    senderId = currentUserId,
                                    senderName = currentUsername,
                                    receiverId = receiverUserId,
                                    text = messageText,
                                    timestamp = System.currentTimeMillis() + (0..999).random(),
                                    seen = false,
                                    replyTo = replyingTo?.text,
                                    edited = false // New messages are not edited initially
                                )
                                messagesRef.child(messageKey).setValue(newMessage)
                                messageText = ""
                                replyingTo = null
                            }
                        }
                    }
                })
                {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(message: Message, currentUserId: String, onReply: (Message) -> Unit, onEdit: (Message) -> Unit, onDeleteForSelf: (Message) -> Unit, onDeleteForEveryone: (Message) -> Unit, isGroupChat: Boolean= false) {
    val senderColorMap = remember { mutableMapOf<String, Color>() }
    val senderColor = remember(message.senderId) {
        senderColorMap.getOrPut(message.senderId) {
            generateColorFromId(message.senderId)
        }
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
        animationSpec = tween(durationMillis = 200), label = ""
    )
    var menuPositionPx by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    var menuPositionDp by remember {
        mutableStateOf(DpOffset(0.dp, 0.dp))
    }
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
                        text = message.senderName ,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = senderColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                message.replyTo?.let {
                    Text(
                        text = "Replying to: $it",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                val annotated = buildAnnotatedString {
                    val regex = "@\\w+".toRegex()
                    var currentIndex = 0
                    regex.findAll(message.text).forEach { match ->
                        append(message.text.substring(currentIndex, match.range.first))
                        withStyle(SpanStyle(color = Color(0xFF2F9ECE))) {
                            append(match.value)
                        }
                        currentIndex = match.range.last + 1
                    }
                    append(message.text.substring(currentIndex))
                }

                Text(text = annotated, color = textColor)
            }
        }
        Box(
            modifier = Modifier
                .shadow(8.dp, shape = RoundedCornerShape(16.dp)) // Shadow effect
                .background(Color.White, shape = RoundedCornerShape(16.dp)) // Curved background
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
                if (isSentByUser) {
                    if (isEditable) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEdit(message)
                                showMenu = false
                            }
                        )
                        HorizontalDivider()
                    }
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

enum class BottomAppBarItem {
    Messages,
    Calls,
    AddFriends
}

@Composable
fun BottomAppBar(username: String,profilePic: Uri) {
    var activeItem by remember { mutableStateOf(BottomAppBarItem.Messages) }
    val context= LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomAppBarItem(
            label = "Messages",
            isActive = activeItem == BottomAppBarItem.Messages,
            activeIcon = R.drawable.bottombar_activemessagespage,
            passiveIcon = R.drawable.bottombar_passivemessagespage,
            onClick = {
                activeItem = BottomAppBarItem.Messages
                val intent = Intent(context, MessageActivity::class.java)
                context.startActivity(intent)
            }
        )

        BottomAppBarItem(
            label = "Call Logs",
            isActive = activeItem == BottomAppBarItem.Calls,
            activeIcon = R.drawable.bottombar_activecallspage,
            passiveIcon = R.drawable.bottombar_passivecallspage,
            onClick = { activeItem = BottomAppBarItem.Calls }
        )

        BottomAppBarItem(
            label = "Konnekt",
            isActive = activeItem == BottomAppBarItem.AddFriends,
            activeIcon = R.drawable.bottombar_activeaddfriendspage,
            passiveIcon = R.drawable.bottombar_passiveaddfriendspage,
            onClick = {
                activeItem = BottomAppBarItem.AddFriends
                val intent = Intent(context, Konnekt::class.java)
                intent.putExtra("username", username)
                intent.putExtra("profileUri", profilePic.toString())
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun BottomAppBarItem(label: String, isActive: Boolean, activeIcon: Int, passiveIcon: Int, onClick: () -> Unit) {
    Log.d("BottomAppBarItem", "Rendering item: $label, isActive: $isActive")

    Column(
        modifier = Modifier
            .width(68.dp)
            .height(52.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = if (isActive) activeIcon else passiveIcon),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive) Color(0xFF2F9ECE) else MaterialTheme.colorScheme.onBackground
        )
    }
}
