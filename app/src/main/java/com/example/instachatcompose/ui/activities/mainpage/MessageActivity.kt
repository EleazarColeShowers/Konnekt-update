package com.example.instachatcompose.ui.activities.mainpage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.activities.Settings
import com.example.instachatcompose.ui.activities.konnekt.Konnekt
import com.example.instachatcompose.ui.activities.konnekt.loadReceivedRequestsWithDetails
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.firestore.auth.User
import java.net.URLEncoder
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class MessageActivity: ComponentActivity() {
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
    val username: String = activity?.intent?.getStringExtra("username") ?: "DefaultUsername"
    val profilePic: Uri = Uri.parse(activity?.intent?.getStringExtra("profileUri") ?: "")

    val friendList = remember { mutableStateListOf<Pair<Friend, Map<String, String>>>() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""

    // Load friends when the composable starts
    LaunchedEffect(userId) {
        loadFriendsWithDetails(userId) { friends ->
            friendList.clear()
            friendList.addAll(friends)
        }
    }

    val navController = rememberNavController()

    Scaffold(
        topBar = {
            val currentBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = currentBackStackEntry?.destination?.route
            if (currentRoute != null && !currentRoute.startsWith("chat")) {
                User(username = username, profilePic = profilePic, userId)
            }
        },
        bottomBar = {
            val currentBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = currentBackStackEntry?.destination?.route
            if (currentRoute != null && !currentRoute.startsWith("chat")) {
                BottomAppBar(username = username, profilePic = profilePic)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = if (friendList.isEmpty()) "message" else "friends",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("message") { MessageFrag(username = username, navController) }
                composable("friends") {
                    FriendsListScreen(
                        friendList = friendList,
                        navController = navController,
                        currentUserId = userId
                    )
                }

                composable("chat") {

                    ChatScreen(navController)
                }
            }
        }
    }
}


@Composable
fun User(username: String,profilePic: Uri, userId: String){
    val settingsIcon= painterResource(id = R.drawable.settings)
    val searchIcon= painterResource(id = R.drawable.searchicon)
    val context = LocalContext.current as ComponentActivity
    val requestCount = fetchReceivedRequestsCount(userId).value


    var search by remember {
        mutableStateOf("")
    }

    Column{
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row{
            val imagePainter: ImagePainter = rememberImagePainter(data = profilePic)
            Image(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .scale(1.5f)
            )

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
            val intent= Intent(context, Settings::class.java)
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
        text ="Messages",
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
                   value = search,
                   onValueChange = { search = it },
                   textStyle = LocalTextStyle.current.copy(color = Color.Black),
                   singleLine = true,
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(top = 14.dp, start = 27.dp)
               )

               if (search.isEmpty()) {
                   Text(
                       text = "Search",
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
    val requestCount = remember { mutableStateOf(0) }

    LaunchedEffect(userId) {
        loadReceivedRequestsWithDetails(userId) { receivedRequests ->
            requestCount.value = receivedRequests.size
        }
    }

    return requestCount
}

@Composable
fun MessageFrag(username: String, navController: NavController){

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
@Composable
fun FriendsListScreen(
    friendList: List<Pair<Friend, Map<String, String>>>,
    navController: NavController,
    currentUserId: String
) {
    var sortedFriendList by remember { mutableStateOf(friendList) }

    LaunchedEffect(friendList) {
        val updatedList = friendList.map { (friend, details) ->
            val chatId = if (currentUserId < friend.friendId) {
                "${currentUserId}_${friend.friendId}"
            } else {
                "${friend.friendId}_${currentUserId}"
            }

            val lastMessageTimestamp = fetchLastMessageTimestamp(chatId) // Fetch last message timestamp
            Triple(friend, details, lastMessageTimestamp)
        }.sortedByDescending { it.third } // Sort by latest timestamp

        sortedFriendList = updatedList.map { Pair(it.first, it.second) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sortedFriendList) { (friend, details) ->
            val friendUsername = details["username"] ?: "Unknown"
            val friendProfileUri = details["profileImageUri"] ?: ""

            val chatId = if (currentUserId < friend.friendId) {
                "${currentUserId}_${friend.friendId}"
            } else {
                "${friend.friendId}_${currentUserId}"
            }

            var lastMessage by remember { mutableStateOf("Loading...") }
            var hasUnreadMessages by remember { mutableStateOf(false) }

            LaunchedEffect(chatId) {
                fetchLastMessage(chatId) { message ->
                    lastMessage = message
                }
                checkUnreadMessages(chatId, currentUserId) { unread ->
                    hasUnreadMessages = unread
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("friendId", friend.friendId)
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("username", friendUsername)
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("profileImageUri", friendProfileUri)
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("chatId", chatId)
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("currentUserId", currentUserId)

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
    }
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


fun checkUnreadMessages(chatId: String, currentUserId: String, onResult: (Boolean) -> Unit) {
    val db = Firebase.database.reference
    val messagesRef = db.child("chats").child(chatId).child("messages")

    messagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val unreadExists = snapshot.children.any { messageSnapshot ->
                val message = messageSnapshot.getValue(Message::class.java)
                message != null && message.receiverId == currentUserId && !message.seen
            }
            onResult(unreadExists)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("checkUnreadMessages", "Error checking unread messages: ${error.message}")
            onResult(false)
        }
    })
}


fun fetchLastMessage(chatId: String, onLastMessageFetched: (String) -> Unit) {
    val chatsRef = FirebaseDatabase.getInstance().getReference("chats")

    chatsRef.child(chatId).child("messages")
        .orderByChild("timestamp")
        .limitToLast(1)
        .get()
        .addOnSuccessListener { snapshot ->
            Log.d("FirebaseDebug", "Snapshot exists: ${snapshot.exists()} - Children: ${snapshot.childrenCount}")

            if (snapshot.exists() && snapshot.childrenCount > 0) {
                for (child in snapshot.children) {
                    Log.d("FirebaseDebug", "Message data: ${child.value}")
                }
                val lastMessage = snapshot.children.first().child("text").getValue(String::class.java) ?: "Unknown"
                Log.d("FirebaseDebug", "Last Message: $lastMessage")
                onLastMessageFetched(lastMessage)
            } else {
                Log.e("FirebaseDebug", "No messages found in chat: $chatId")
                onLastMessageFetched("No messages yet")
            }
        }
        .addOnFailureListener { exception ->
            Log.e("FirebaseDebug", "Error fetching last message: ${exception.message}", exception)
            onLastMessageFetched("Error fetching message")
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
fun BottomAppBarItem(
    label: String,
    isActive: Boolean,
    activeIcon: Int,
    passiveIcon: Int,
    onClick: () -> Unit
) {
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

fun loadFriendsWithDetails(
    userId: String,
    onFriendsLoaded: (List<Pair<Friend, Map<String, String>>>) -> Unit
) {
    val usersRef = FirebaseDatabase.getInstance().getReference("users")
    val friendsRef = usersRef.child(userId).child("friends")

    friendsRef.get().addOnSuccessListener { snapshot ->
        if (snapshot.exists()) {
            val friendPairs = mutableListOf<Pair<Friend, Map<String, String>>>()

            val detailTasks = snapshot.children.map { friendSnapshot ->
                val friendId = friendSnapshot.child("friendId").getValue(String::class.java) ?: ""
                val timestamp = friendSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                val friend = Friend(friendId, timestamp)

                usersRef.child(friendId).get().continueWith { task ->
                    val userSnapshot = task.result
                    val details = if (userSnapshot.exists()) {
                        mapOf(
                            "username" to (userSnapshot.child("username").getValue(String::class.java) ?: "Unknown"),
                            "profileImageUri" to (userSnapshot.child("profileImageUri").getValue(String::class.java) ?: "")
                        )
                    } else {
                        mapOf("username" to "Unknown", "profileImageUri" to "")
                    }
                    Pair(friend, details)
                }
            }

            Tasks.whenAllSuccess<Pair<Friend, Map<String, String>>>(detailTasks)
                .addOnSuccessListener { friendDetailsList ->
                    onFriendsLoaded(friendDetailsList)
                }
        } else {
            onFriendsLoaded(emptyList())
        }
    }.addOnFailureListener { exception ->
        Log.e("Firebase", "Error loading friends: ${exception.message}")
        onFriendsLoaded(emptyList())
    }
}


data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val seen: Boolean = false
)


@Composable
fun ChatScreen(navController: NavController) {
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle

    val username = savedStateHandle?.get<String>("username") ?: "Unknown"
    val profileImageUri = savedStateHandle?.get<String>("profileImageUri")?.let { Uri.parse(it) } ?: Uri.EMPTY
    val chatId = savedStateHandle?.get<String>("chatId") ?: ""
    val currentUserId = savedStateHandle?.get<String>("currentUserId") ?: ""
    val receiverUserId = savedStateHandle?.get<String>("friendId") ?: ""

    val db = Firebase.database.reference
    val messagesRef = db.child("chats").child(chatId).child("messages")

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(chatId) {
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                    .sortedByDescending { it.timestamp }
                messages = messageList

                // Mark all unread messages as read
                snapshot.children.forEach { messageSnapshot ->
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null && message.receiverId == currentUserId && !message.seen) {
                        messageSnapshot.ref.child("seen").setValue(true) // Update seen status in Firebase
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatScreen", "Failed to load messages: ${error.message}")
            }
        })
    }

    Log.d("ChatScreen", "Profile Image URI: $profileImageUri")


    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            Text(
                text = username,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true,
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message, currentUserId)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") }
            )
            IconButton(onClick = {
                if (messageText.isNotBlank()) {
                    val newMessage = Message(
                        senderId = currentUserId,
                        receiverId = receiverUserId,
                        text = messageText,
                        timestamp = System.currentTimeMillis(),
                        seen = false
                    )
                    messagesRef.push().setValue(newMessage) // ✅ Save message to Firebase
                    messageText = ""  // Clear input field
                }
            }) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}


@Composable
fun MessageBubble(
    message: Message,
    currentUserId: String,
) {
    val isSentByUser = message.senderId == currentUserId
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = screenWidth * 0.7f

    // Get colors based on the theme
    val backgroundColor = if (isSentByUser) {
        Color(0xFF2F9ECE) // Same color for sent messages
    } else {
        if (isSystemInDarkTheme()) Color(0xFF333333) else Color(0xFFEEEEEE) // Dark gray in dark mode
    }

    val textColor = if (isSentByUser) {
        Color.White // Sent messages always have white text
    } else {
        if (isSystemInDarkTheme()) Color.White else Color.Black // White text in dark mode
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalAlignment = if (isSentByUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = maxWidth)
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = TextStyle(fontSize = 16.sp)
            )
        }
    }
}
