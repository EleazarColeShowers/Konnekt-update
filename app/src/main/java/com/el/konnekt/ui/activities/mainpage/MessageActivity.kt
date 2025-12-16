package com.el.konnekt.ui.activities.mainpage

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.el.konnekt.ui.activities.Settings
import com.el.konnekt.ui.activities.konnekt.Konnekt
import com.el.konnekt.ui.activities.konnekt.loadReceivedRequestsWithDetails
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import com.el.konnekt.ui.activities.calls.CallActivity
import com.el.konnekt.data.local.AppDatabase
import com.el.konnekt.data.repository.ChatRepository
import com.el.konnekt.data.ChatViewModel
import com.el.konnekt.data.ChatViewModelFactory
import com.el.konnekt.data.remote.FirebaseDataSource
import com.el.konnekt.data.local.GroupEntity
import com.el.konnekt.data.local.LocalDataSource
import com.el.konnekt.data.Message
import com.el.konnekt.data.models.TempGroupIdHolder
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.collections.filterNot
import kotlin.collections.find
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue
import com.el.konnekt.R



class MessageActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier= Modifier.fillMaxSize()
                            .padding(horizontal = 15.dp)
                    ) {
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
    val context = LocalContext.current
    val app = context.applicationContext as Application

    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            app,
            ChatRepository(
                FirebaseDataSource(),
                LocalDataSource(AppDatabase.getDatabase(app))
            )
        )
    )
//    val requestPermissionLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestPermission()
//    ) { isGranted: Boolean ->
//        Log.d("PermissionRequest", "Permission granted: $isGranted")
//        if (isGranted) {
//            showNotification(context)
//        } else {
//            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
//        }
//    }



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
                    ArchiveScreen()
                }

            }
        }
    }
    if (showCreateGroupDialog) {
        CreateGroupBottomSheet(onDismiss = { showCreateGroupDialog = false }, friendList = friendList)
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "default_channel",
            "Default Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Used for default notifications"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d("NotificationChannel", "Notification channel created")
    }
}

//fun showNotification(context: Context) {
//    Log.d("Notification", "showNotification() called")
//
//    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//    val builder = NotificationCompat.Builder(context, "default_channel")
//        .setSmallIcon(android.R.drawable.ic_dialog_info)
//        .setContentTitle("Notification Title")
//        .setContentText("This is a Jetpack Compose notification.")
//        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//
//    notificationManager.notify(1, builder.build())
//    Log.d("Notification", "Notification sent")
//}

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
fun CreateGroupBottomSheet(friendList: List<Pair<Friend, Map<String, String>>>, onDismiss: () -> Unit) {
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
            // Header
            Text(
                text = "Create New Group",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Group Image Selection - Centered
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

            // Group Name Input
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

            // Section Header with count
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

            // Friends List
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
                            // Profile Image
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

            // Action Buttons
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
                                                memberIds = members.joinToString(",")
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
        // Header with profile and settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile section with constrained width
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture
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

        // Messages title
        Text(
            text = "Messages",
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Modern search bar
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

        // Requests and Archives row
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

data class GroupChat(
    val groupId: String,
    val groupName: String,
    val members: List<String>,
    val groupImage: String = ""
)

data class Friend(
    val friendId: String = "",
    val timestamp: Long = 0L
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
                            viewModel.removeFriendFromDatabase(currentUserId, friend.friendId)
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
fun FriendRow(
    friend: Friend,
    details: Map<String, String>,
    navController: NavController,
    currentUserId: String,
    viewModel: ChatViewModel
) {
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

    DisposableEffect(chatId) {
        val db = Firebase.database.reference.child("chats").child(chatId).child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }

                if (messages.isNotEmpty()) {
                    val lastMsg = messages.last()
                    lastMessage = lastMsg.text
                    timestamp = lastMsg.timestamp
                } else {
                    lastMessage = "Send Hi to your new friend!"
                    timestamp = null
                }

                hasUnreadMessages = messages.any { it.receiverId == currentUserId && !it.seen }
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

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "now" // Less than 1 minute
        diff < 3600000 -> "${diff / 60000}m" // Less than 1 hour
        diff < 86400000 -> "${diff / 3600000}h" // Less than 1 day
        diff < 604800000 -> "${diff / 86400000}d" // Less than 1 week
        else -> {
            val weeks = diff / 604800000
            "${weeks}w"
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
    var lastMessage by remember { mutableStateOf("No messages yet") }
    var hasUnreadMessages by remember { mutableStateOf(false) }
    var timestamp by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(group.groupId) {
        val db = Firebase.database.reference
            .child("chats")
            .child(group.groupId)
            .child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                if (messages.isNotEmpty()) {
                    val latestMessage = messages.last()
                    val senderName = latestMessage.senderName
                    val text = latestMessage.text
                    lastMessage = "$senderName: $text"
                    timestamp = latestMessage.timestamp

                    hasUnreadMessages = messages.any {
                        it.receiverId == currentUserId && !it.seen
                    }
                } else {
                    lastMessage = "No messages yet"
                    timestamp = null
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
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.apply {
                        set("groupId", group.groupId)
                        set("groupName", group.groupName)
                        set("groupImageUri", group.groupImage)
                        set("currentUserId", currentUserId)
                        set("chatId", "group_${group.groupId}")
                        set("isGroupChat", true)
                    }
                navController.navigate("chat")
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
    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // --- State and context ---
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

    // --- Chat info ---
    val isGroupChat = savedStateHandle?.get<Boolean>("isGroupChat") ?: false
    val username = if (isGroupChat)
        savedStateHandle?.get<String>("groupName") ?: "Group Chat"
    else
        savedStateHandle?.get<String>("username") ?: "Unknown"

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

    // --- ViewModel observers ---
    val messages by viewModel.messages
        .map { it.sortedByDescending { msg -> msg.timestamp } }
        .collectAsState(initial = emptyList())

    val isFriendTyping by viewModel.isFriendTyping.collectAsState()
    val filteredMembers = groupMembers.filter {
        it.startsWith(mentionQuery, ignoreCase = true)
    }

    // --- Load current username ---
    LaunchedEffect(currentUserId) {
        viewModel.fetchCurrentUserName(currentUserId) { name ->
            currentUsername = name ?: "Unknown"
        }
    }

    // --- Load and observe messages ---
    LaunchedEffect(chatId, isChatOpen) {
        if (!isChatOpen) return@LaunchedEffect
        messagesRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = mutableListOf<Message>()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        messageList.add(message)
                    }
                }
                viewModel.updateMessages(chatId, messageList)
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
        val firebaseChatId = if (isGroupChat) chatId.removePrefix("group_") else chatId

        viewModel.observeMessages(
            context = context,
            chatId = firebaseChatId,
            currentUserId = currentUserId,
            isChatOpen = true,
            requestNotificationPermission = {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        )
        if (isGroupChat) {
            val groupChatList = fetchGroupChats(currentUserId)
            val group = groupChatList.find { it.groupId == chatId.removePrefix("group_") }
            groupMembers = group?.members ?: emptyList()
        } else {
            viewModel.observeTyping(chatId, receiverUserId)
        }
    }

    // --- Reply handler ---
    fun setReplyingTo(message: Message) {
        val decrypted = try {
            message.text
        } catch (e: Exception) {
            Log.e("ChatScreen", "Failed to decrypt message", e)
            "[error]"
        }
        replyingTo = message.copy(decryptedText = decrypted)
    }

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Background wallpaper with app icon
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val iconBitmap = remember {
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round)
            }
            iconBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Background",
                    modifier = Modifier
                        .size(300.dp)
                        .alpha(if (isSystemInDarkTheme()) 0.7f else 0.4f),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // HEADER with elevated card design
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    // Profile image with border
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (profileImageUri != Uri.EMPTY) {
                            AsyncImage(
                                model = profileImageUri.toString(),
                                contentDescription = "Profile Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = username.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = username,
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        when {
                            isFriendTyping -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "typing",
                                        style = TextStyle(
                                            fontSize = 13.sp,
                                            color = Color(0xFF2F9ECE)
                                        )
                                    )
                                    // Animated typing indicator
                                    Row(
                                        modifier = Modifier.padding(start = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        repeat(3) { index ->
                                            val alpha by rememberInfiniteTransition(label = "").animateFloat(
                                                initialValue = 0.3f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(500),
                                                    repeatMode = RepeatMode.Reverse,
                                                    initialStartOffset = StartOffset(index * 150)
                                                ), label = ""
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .alpha(alpha)
                                                    .background(Color(0xFF2F9ECE), CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                            isGroupChat -> {
                                Text(
                                    text = "${groupMembers.size} members",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            else -> {
                                Text(
                                    text = "Active now",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // MESSAGES with background
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
                            onReply = { setReplyingTo(it) },
                            isGroupChat = isGroupChat,
                            onEdit = { messageToEdit ->
                                messageText = messageToEdit.text
                                editingMessageId = messageToEdit.id
                            },
                            onDeleteForSelf = { msg ->
                                val specificMessageRef =
                                    db.child("chats").child(chatId).child("messages").child(msg.id)
                                specificMessageRef.child("deletedFor").child(currentUserId)
                                    .setValue(true)
                            },
                            onDeleteForEveryone = { msg ->
                                if (msg.id.isNotBlank()) {
                                    val specificMessageRef =
                                        db.child("chats").child(chatId).child("messages").child(msg.id)
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
                            },
                            messages = messages
                        )
                    }
                }
            }

            // INPUT BAR with enhanced design
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .imePadding()
                ) {
                    // REPLY PREVIEW
                    AnimatedVisibility(
                        visible = replyingTo != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        replyingTo?.let { message ->
                            val senderName = when {
                                message.senderId == currentUserId -> "You"
                                isGroupChat -> message.senderName
                                else -> username
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(40.dp)
                                            .background(Color(0xFF2F9ECE), RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = senderName,
                                            color = Color(0xFF2F9ECE),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = message.text,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { replyingTo = null }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel Reply",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // MESSAGE INPUT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            color = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF5F5F5)
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
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "Message...",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    cursorColor = Color(0xFF2F9ECE),
                                ),
                                maxLines = 4
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // SEND BUTTON
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = if (messageText.isNotBlank()) Color(0xFF2F9ECE) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        val encryptedText = messageText

                                        if (editingMessageId != null) {
                                            val messageUpdates = mapOf(
                                                "text" to encryptedText,
                                                "edited" to true
                                            )
                                            messagesRef.child(editingMessageId!!).updateChildren(messageUpdates)
                                                .addOnSuccessListener {
                                                    editingMessageId = null
                                                    messageText = ""
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Chat", "Failed to edit message: ${e.message}")
                                                }
                                            return@IconButton
                                        }

                                        val messageKey = messagesRef.push().key
                                        if (messageKey != null) {
                                            val newMessage = Message(
                                                id = messageKey,
                                                senderId = currentUserId,
                                                senderName = currentUsername,
                                                receiverId = receiverUserId,
                                                text = encryptedText,
                                                timestamp = System.currentTimeMillis() + (0..999).random(),
                                                seen = false,
                                                replyTo = replyingTo?.id,
                                                edited = false
                                            )
                                            messagesRef.child(messageKey).setValue(newMessage)
                                            messageText = ""
                                            replyingTo = null
                                        }
                                    }
                                },
                                enabled = messageText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (messageText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mentions dropdown
        if (showMentionDropdown && mentionQuery.isNotBlank() && filteredMembers.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 80.dp)
                    .widthIn(max = 250.dp),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    filteredMembers.take(5).forEach { member ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = member,
                                    fontSize = 14.sp
                                )
                            },
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
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    currentUserId: String,
    messages: List<Message>,
    onReply: (Message) -> Unit,
    onEdit: (Message) -> Unit,
    onDeleteForSelf: (Message) -> Unit,
    onDeleteForEveryone: (Message) -> Unit,
    isGroupChat: Boolean = false
) {
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
        Surface(
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
                ),
            shape = RoundedCornerShape(
                topStart = if (isSentByUser) 16.dp else 4.dp,
                topEnd = if (isSentByUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = backgroundColor,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                if (isGroupChat && !isSentByUser) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = senderColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                message.replyTo?.let { replyId ->
                    val repliedMessage = messages.find { it.id == replyId }
                    val repliedText = repliedMessage?.text ?: "[message unavailable]"

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSentByUser)
                            Color.White.copy(alpha = 0.2f)
                        else
                            Color.Black.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(
                                        if (isSentByUser) Color.White.copy(alpha = 0.7f)
                                        else senderColor
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = repliedMessage?.senderName ?: "Unknown",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSentByUser)
                                        Color.White.copy(alpha = 0.9f)
                                    else
                                        senderColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = repliedText,
                                    fontSize = 12.sp,
                                    color = textColor.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                val annotated = buildAnnotatedString {
                    val regex = "@\\w+".toRegex()
                    val rawText = message.text
                    var currentIndex = 0
                    regex.findAll(rawText).forEach { match ->
                        append(rawText.substring(currentIndex, match.range.first))
                        withStyle(
                            SpanStyle(
                                color = if (isSentByUser) Color.White else senderColor,
                                fontWeight = FontWeight.Bold,
                                background = if (isSentByUser)
                                    Color.White.copy(alpha = 0.2f)
                                else
                                    senderColor.copy(alpha = 0.15f)
                            )
                        ) {
                            append(" ${match.value} ")
                        }
                        currentIndex = match.range.last + 1
                    }
                    append(rawText.substring(currentIndex))
                }

                Text(
                    text = annotated,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                // Timestamp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.6f)
                    )

                    if (isSentByUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            modifier = Modifier.size(12.dp),
                            tint = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Enhanced dropdown menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = menuPositionDp,
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Reply", fontSize = 14.sp)
                    }
                },
                onClick = {
                    onReply(message)
                    showMenu = false
                }
            )

            if (isSentByUser && isEditable) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Edit", fontSize = 14.sp)
                        }
                    },
                    onClick = {
                        onEdit(message)
                        showMenu = false
                    }
                )
            }

            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Delete for Me", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                    }
                },
                onClick = {
                    onDeleteForSelf(message)
                    showMenu = false
                }
            )

            if (isSentByUser && isDeletableForEveryone) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Delete for Everyone", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    onClick = {
                        onDeleteForEveryone(message)
                        showMenu = false
                    }
                )
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
