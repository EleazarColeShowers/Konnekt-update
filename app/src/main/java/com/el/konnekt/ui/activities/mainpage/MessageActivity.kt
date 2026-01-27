package com.el.konnekt.ui.activities.mainpage

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.el.konnekt.ui.activities.Settings
import com.el.konnekt.ui.activities.konnekt.Konnekt
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.identity.util.UUID
import com.el.konnekt.ui.activities.calls.CallActivity
import com.el.konnekt.data.local.AppDatabase
import com.el.konnekt.data.repository.ChatRepository
import com.el.konnekt.data.ChatViewModel
import com.el.konnekt.data.ChatViewModelFactory
import com.el.konnekt.data.remote.FirebaseDataSource
import com.el.konnekt.data.local.LocalDataSource
import com.el.konnekt.data.models.Message
import kotlinx.coroutines.launch
import kotlin.collections.filterNot
import kotlin.collections.find
import com.el.konnekt.R
import com.el.konnekt.data.models.Friend
import com.el.konnekt.data.models.GroupChat
import com.el.konnekt.ui.activities.message.ChatActivity
import com.el.konnekt.utils.MessageObfuscator
import com.el.konnekt.utils.NotificationHelper.createNotificationChannel
import com.el.konnekt.utils.formatTimestamp
import com.google.firebase.messaging.FirebaseMessaging

class MessageActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 15.dp)
                    ) {
                        MessagePage()
                    }
                }
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
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
    val context = LocalContext.current
    val app = context.applicationContext as Application
    var isLoading by remember { mutableStateOf(true) }

    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            app,
            ChatRepository(
                FirebaseDataSource(),
                LocalDataSource(AppDatabase.getDatabase(app))
            )
        )
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("Notifications", "Permission granted")
            }
        }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            // Refresh and save FCM token
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    FirebaseDatabase.getInstance().reference
                        .child("users")
                        .child(userId)
                        .child("fcmToken")
                        .setValue(token)
                        .addOnSuccessListener {
                            Log.d("MessageActivity", "FCM token saved for user")
                        }
                }
            }
        }
    }

    val cachedFriends by viewModel.cachedFriends.collectAsState()
    val isLoadingFriends by viewModel.isLoadingFriends.collectAsState()

    LaunchedEffect(userId) {
        viewModel.fetchUserProfile(context, userId) { fetchedUsername, fetchedProfilePicUrl ->
            username = fetchedUsername ?: "Unknown"
            profilePicUrl = fetchedProfilePicUrl
        }
    }

    LaunchedEffect(userId) {
        if (cachedFriends.isNotEmpty()) {
            friendList.clear()
            friendList.addAll(cachedFriends)
            isLoading = false
        } else {
            viewModel.loadFriendsWithDetails(userId, forceRefresh = false) { friends ->
                friendList.clear()
                friendList.addAll(friends)
                isLoading = false
            }
        }
    }

    LaunchedEffect(cachedFriends) {
        if (cachedFriends.isNotEmpty()) {
            friendList.clear()
            friendList.addAll(cachedFriends)
        }
    }

    LaunchedEffect(userId) {
        viewModel.loadGroupChats(userId)
    }

    DisposableEffect(userId) {
        val groupsRef = FirebaseDatabase.getInstance().reference.child("chats")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userGroups = mutableListOf<GroupChat>()

                for (groupSnapshot in snapshot.children) {
                    val groupId = groupSnapshot.key ?: continue
                    if (!groupId.startsWith("group_")) continue

                    val membersSnapshot = groupSnapshot.child("members")
                    val memberIds = membersSnapshot.children.mapNotNull { it.key }

                    if (userId in memberIds) {
                        val groupName = groupSnapshot.child("groupName")
                            .getValue(String::class.java) ?: "Unnamed Group"
                        val groupImage = groupSnapshot.child("groupImage")
                            .getValue(String::class.java) ?: ""

                        userGroups.add(
                            GroupChat(
                                groupId = groupId.removePrefix("group_"),
                                groupName = groupName,
                                members = memberIds,
                                groupImage = groupImage
                            )
                        )
                    }
                }

                viewModel.updateGroupChats(userGroups)
                viewModel.refreshCombinedChatList(
                    userId,
                    friendList,
                    searchQuery,
                    context,
                    userGroups
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MessagePage", "Failed to sync groups: ${error.message}")
            }
        }

        groupsRef.addValueEventListener(listener)
        onDispose {
            groupsRef.removeEventListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        createNotificationChannel(context)
    }

    val navController = rememberNavController()

    Scaffold(
        topBar = {
            val currentBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = currentBackStackEntry?.destination?.route
            if (currentRoute != null && !currentRoute.startsWith("chat")) {
                User(username = username, profilePicUrl = profilePicUrl, userId = userId, searchQuery = searchQuery, onSearchQueryChange = {searchQuery= it}, navController)
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
                startDestination = "friends",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("message") { MessageFrag(username = username) }
                composable("friends") {
                    // CHANGED: Show loading indicator or friends list
                    if (isLoading) {
                        // Loading state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2F9ECE),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading chats...",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (friendList.isEmpty() && viewModel.groupChats.value.isEmpty()) {
                        // Empty state - show welcome message
                        MessageFrag(username = username)
                    } else {
                        // Show friends list
                        FriendsListScreen(
                            friendList = friendList,
                            navController = navController,
                            currentUserId = userId,
                            searchQuery = searchQuery,
                            viewModel
                        )
                    }
                }
                composable("chat") {
                }
                composable("archives") {
                    ArchiveScreen()
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupBottomSheet(onDismiss = { showCreateGroupDialog = false }, friendList = friendList, currentUserId = userId, viewModel = viewModel)
    }
}

@Composable
fun ArchiveScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.coming_soon),
            contentDescription = "Coming Soon",
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "Archive feature is coming soon!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2F9ECE)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    friendList: List<Pair<Friend, Map<String, String>>>,
    onDismiss: () -> Unit,
    currentUserId: String,
    viewModel: ChatViewModel
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var groupName by remember { mutableStateOf("") }
    val selectedFriends = remember { mutableStateListOf<String>() }
    var groupImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        groupImageUri = uri
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create New Group",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (groupImageUri != null) Color.Transparent
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (groupImageUri != null) {
                        AsyncImage(
                            model = groupImageUri,
                            contentDescription = "Group Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Image",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (groupImageUri != null) "Tap to change" else "Add group photo",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                placeholder = { Text("Enter group name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Members",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (selectedFriends.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${selectedFriends.size} selected",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(friendList, key = { (friend, _) ->
                    friend.friendId.ifBlank { UUID.randomUUID().toString() }
                }) { (friend, details) ->
                    val friendId = friend.friendId
                    val username = details["username"] ?: "Unknown"
                    val profileImage = details["profileImageUri"] ?: ""
                    val isSelected = selectedFriends.contains(friendId)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isSelected) selectedFriends.remove(friendId)
                                else selectedFriends.add(friendId)
                            },
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (profileImage.isEmpty())
                                            MaterialTheme.colorScheme.secondaryContainer
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (profileImage.isNotEmpty()) {
                                    AsyncImage(
                                        model = profileImage,
                                        contentDescription = "Profile Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = username.firstOrNull()?.uppercase() ?: "?",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = username,
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF2F9ECE),
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (selectedFriends.size < 2) {
                            Toast.makeText(context, "Select at least two members", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (groupName.isNotBlank()) {
                            viewModel.createGroup(
                                groupName = groupName,
                                selectedFriends = selectedFriends.toList(),
                                groupImageUri = groupImageUri,
                                onSuccess = {
                                    Toast.makeText(context, "Group created", Toast.LENGTH_SHORT).show()
                                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Enter group name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = groupName.isNotBlank() && selectedFriends.size >= 2
                ) {
                    Text("Create Group")
                }
            }
        }
    }
}

@Composable
fun User(
    username: String,
    profilePicUrl: String?,
    userId: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    navController: NavController
) {
    val settingsIcon = painterResource(id = R.drawable.settings)
    val searchIcon = painterResource(id = R.drawable.searchicon)
    val context = LocalContext.current as ComponentActivity
    val requestCount = fetchReceivedRequestsCount(userId).value

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!profilePicUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = profilePicUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.nopfp),
                        contentDescription = "Default Profile",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Username with ellipsis for long names
                Text(
                    text = username,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Settings button with fixed width
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        val intent = Intent(context, Settings::class.java)
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = settingsIcon,
                    contentDescription = "Settings",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF2F9ECE)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Settings",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2F9ECE)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Messages",
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            placeholder = {
                Text(
                    "Search Friends",
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            },
            leadingIcon = {
                Icon(
                    painter = searchIcon,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFF2F9ECE)
            ),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Requests${if (requestCount > 0) " ($requestCount)" else ""}",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { /* Navigate to requests */ }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = if (requestCount > 0) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (requestCount > 0) Color(0xFF2F9ECE) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Text(
                text = "Archives",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { navController.navigate("archives") }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(friendList: List<Pair<Friend, Map<String, String>>>, navController: NavController, currentUserId: String, searchQuery: String, viewModel: ChatViewModel) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    var showDialog by remember { mutableStateOf(false) }
    var friendToRemove by remember { mutableStateOf<Friend?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var groupToLeave by remember { mutableStateOf<GroupChat?>(null) }
    val combinedList by viewModel.combinedChatList.collectAsState()
    val chatTimestamps by viewModel.chatTimestamps.collectAsState()



    LaunchedEffect(searchQuery, friendList, viewModel.groupChats, combinedList.size,chatTimestamps) {
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
                            FriendRow(friend, details, navController, currentUserId, viewModel)
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
                                currentUserId = currentUserId,
                                viewModel = viewModel
                            )
                        }
                    )

                }
            }
        }
    }
    if (showDialog && friendToRemove != null) {
        val usernameToRemove = friendToRemove?.let { friend ->
            friendList.find { it.first.friendId == friend.friendId }?.second?.get("username") ?: "this friend"
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove $usernameToRemove as a friend?") },
            confirmButton = {
                Button(
                    onClick = {
                        friendToRemove?.let { friend ->
                            viewModel.removeFriendFromDatabase(currentUserId, friend.friendId)

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
fun FriendRow(
    friend: Friend,
    details: Map<String, String>,
    navController: NavController,
    currentUserId: String,
    viewModel: ChatViewModel
) {
    val context = LocalContext.current
    val friendUsername = details["username"] ?: "Unknown"
    val friendProfileUri = details["profileImageUri"] ?: ""

    val chatId = if (currentUserId < friend.friendId) {
        "${currentUserId}_${friend.friendId}"
    } else {
        "${friend.friendId}_${currentUserId}"
    }

    var lastMessage by remember { mutableStateOf("Send Hi to your new friend!") }
    var hasUnreadMessages by remember { mutableStateOf(false) }
    var timestamp by remember { mutableStateOf<Long?>(null) }
    var unreadCount by remember { mutableIntStateOf(0) }

    DisposableEffect(chatId) {
        val db = Firebase.database.reference.child("chats").child(chatId).child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }

                if (messages.isNotEmpty()) {
                    val lastMsg = messages.last()
                    lastMessage = MessageObfuscator.deobfuscate(lastMsg.text, chatId)
                    val newTimestamp = lastMsg.timestamp
                    timestamp = newTimestamp

                    viewModel.updateChatTimestamp(chatId, newTimestamp)

                } else {
                    lastMessage = "Send Hi to your new friend!"
                    timestamp = null
                }

                val unreadMessages = messages.filter { it.receiverId == currentUserId && !it.seen }
                hasUnreadMessages = unreadMessages.isNotEmpty()
                unreadCount = unreadMessages.size
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FriendsListScreen", "Error fetching messages: ${error.message}")
            }
        }

        db.addValueEventListener(listener)
        onDispose { db.removeEventListener(listener) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("friendId", friend.friendId)
                    putExtra("username", friendUsername)
                    putExtra("profileImageUri", friendProfileUri)
                    putExtra("chatId", chatId)
                    putExtra("currentUserId", currentUserId)
                    putExtra("isGroupChat", false)
                }
                context.startActivity(intent)
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile image
        if (friendProfileUri.isNotEmpty()) {
            AsyncImage(
                model = friendProfileUri,
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friendUsername.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Message content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = friendUsername,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Timestamp
                timestamp?.let {
                    Text(
                        text = formatTimestamp(it),
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = if (hasUnreadMessages)
                                Color(0xFF2F9ECE)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lastMessage,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = if (hasUnreadMessages) FontWeight.Medium else FontWeight.Normal,
                        color = if (hasUnreadMessages)
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (hasUnreadMessages) {
                    Spacer(modifier = Modifier.width(8.dp))

                    // Show count badge if more than 1 message
                    if (unreadCount > 1) {
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = Color(0xFF2F9ECE)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Single unread - show dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2F9ECE))
                        )
                    }
                }

            }
        }
    }
}


@Composable
fun GroupAsFriendRow(
    group: GroupChat,
    navController: NavController,
    currentUserId: String,
    viewModel: ChatViewModel
) {
    val context = LocalContext.current
    var lastMessage by remember { mutableStateOf("No messages yet") }
    var hasUnreadMessages by remember { mutableStateOf(false) }
    var timestamp by remember { mutableStateOf<Long?>(null) }
    var unreadCount by remember { mutableIntStateOf(0) }


    DisposableEffect(group.groupId) {
        val db = Firebase.database.reference
            .child("chats")
            .child("group_${group.groupId}")
            .child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                if (messages.isNotEmpty()) {
                    val latestMessage = messages.last()
                    val senderName = latestMessage.senderName
                    val text = MessageObfuscator.deobfuscate(latestMessage.text, group.groupId)
//                    val text = MessageObfuscator.deobfuscate(latestMessage.text, "group_${group.groupId}")
                    lastMessage = "$senderName: $text"
                    val newTimestamp = latestMessage.timestamp
                    timestamp = newTimestamp
                    viewModel.updateChatTimestamp("group_${group.groupId}", newTimestamp)

                    val unreadMessages = messages.filter {
                        it.senderId != currentUserId && !it.seen
                    }
                    hasUnreadMessages = unreadMessages.isNotEmpty()
                    unreadCount = unreadMessages.size
                } else {
                    lastMessage = "No messages yet"
                    timestamp = null
                    hasUnreadMessages = false
                    unreadCount = 0
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupRow", "Error fetching group messages: ${error.message}")
            }
        }

        db.addValueEventListener(listener)
        onDispose { db.removeEventListener(listener) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("groupId", group.groupId)
                    putExtra("username", group.groupName)  // Use groupName as username
                    putExtra("profileImageUri", group.groupImage)
                    putExtra("currentUserId", currentUserId)
                    putExtra("chatId", "group_${group.groupId}")
                    putExtra("isGroupChat", true)
                    putExtra("groupName", group.groupName)
                    putExtra("groupImageUri", group.groupImage)
                }
                context.startActivity(intent)
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Group image
        if (group.groupImage.isNotEmpty()) {
            AsyncImage(
                model = group.groupImage,
                contentDescription = "Group Image",
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Group",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Message content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.groupName,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                timestamp?.let {
                    Text(
                        text = formatTimestamp(it),
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = if (hasUnreadMessages)
                                Color(0xFF2F9ECE)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lastMessage,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = if (hasUnreadMessages) FontWeight.Medium else FontWeight.Normal,
                        color = if (hasUnreadMessages)
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (hasUnreadMessages) {
                    Spacer(modifier = Modifier.width(8.dp))

                    // Show count badge if more than 1 message
                    if (unreadCount > 1) {
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = Color(0xFF2F9ECE)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Single unread - show dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2F9ECE))
                        )
                    }
                }
            }
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
            onClick = {
                activeItem = BottomAppBarItem.Calls
                val intent = Intent(context, CallActivity::class.java)
                context.startActivity(intent)
            }

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

enum class BottomAppBarItem {
    Messages,
    Calls,
    AddFriends
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

@Composable
fun fetchReceivedRequestsCount(userId: String): State<Int> {
    val requestCount = remember { mutableIntStateOf(0) }

    DisposableEffect(userId) {
        val requestsRef = FirebaseDatabase.getInstance()
            .reference
            .child("friendRequests")
            .child(userId)
            .child("received")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                requestCount.intValue = snapshot.childrenCount.toInt()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchRequests", "Error fetching requests: ${error.message}")
            }
        }

        requestsRef.addValueEventListener(listener)

        onDispose {
            requestsRef.removeEventListener(listener)
        }
    }

    return requestCount
}
