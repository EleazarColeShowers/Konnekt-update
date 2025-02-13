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
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.net.URLEncoder


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
                User(username = username, profilePic = profilePic)
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
                        currentUserId = userId,
//                        receiverUserId = "defaultReceiverId"
                    )
                }

                composable(
                    "chat/{username}/{profileImageUri}/{chatId}/{currentUserId}/{receiverUserId}",
                    arguments = listOf(
                        navArgument("username") { type = NavType.StringType },
                        navArgument("profileImageUri") { type = NavType.StringType },
                        navArgument("chatId") { type = NavType.StringType },
                        navArgument("currentUserId") { type = NavType.StringType },
                        navArgument("receiverUserId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val username = backStackEntry.arguments?.getString("username") ?: "Unknown"
                    val profileImageUri = Uri.decode(backStackEntry.arguments?.getString("profileImageUri") ?: "")
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    val currentUserId = backStackEntry.arguments?.getString("currentUserId") ?: ""
                    val receiverUserId = backStackEntry.arguments?.getString("receiverUserId") ?: ""

                    ChatScreen(username, profileImageUri, navController, chatId, currentUserId, receiverUserId)
                }
            }
        }
    }

}


@Composable
fun User(username: String,profilePic: Uri){
    val settingsIcon= painterResource(id = R.drawable.settings)
    val searchIcon= painterResource(id = R.drawable.searchicon)
    val context = LocalContext.current as ComponentActivity

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
                    .size(31.dp)
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
                    color = Color(0xFF696969),
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
            color = Color(0xFF000000),
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
                       color = Color.Gray,
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
                    text = "Requests(10)",
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
//        TODO: 3. Username should appear when user logs in (already appears on sign up)
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
                color = Color(0xFF696969),
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
    friendList: List<Pair<Friend, Map<String, String>>>, navController: NavController,
//    chatId: String,
    currentUserId: String,

) {
    val friend by remember { mutableStateOf<List<Friend>>(emptyList()) }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(friendList) { (friend, details) ->
            val friendUsername = details["username"] ?: "Unknown"
            val friendProfileUri = details["profileImageUri"] ?: ""

            val chatId = if (currentUserId < friend.friendId) {
                "${currentUserId}_${friend.friendId}"
            } else {
                "${friend.friendId}_${currentUserId}"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        val encodedProfileUri = URLEncoder.encode(friendProfileUri, "UTF-8")
                        val receiverUserId = friend.friendId
                        navController.navigate("chat/${friendUsername}/${encodedProfileUri}/${chatId}/${currentUserId}/${receiverUserId}")                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (friendProfileUri.isNotEmpty()) {
                    AsyncImage(
                        model = friendProfileUri,
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
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
                    text = friendUsername,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}


@Composable
fun TextFrag(){
    Text(text = "Hello")
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
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically // Align icons and text vertically
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
            .clickable(onClick = onClick),  // Make the item clickable
        horizontalAlignment = Alignment.CenterHorizontally // Align content in the center
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
            color = if (isActive) Color(0xFF2F9ECE) else Color(0xFF696969) // Change text color based on active/passive state
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
fun ChatScreen(
    username: String,
    profileImageUri: String,
    navController: NavController,
    chatId: String,
    currentUserId: String,
    receiverUserId: String,
    ) {
    val db = Firebase.database.reference
    val messagesRef = db.child("chats").child(chatId).child("messages")

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }


    LaunchedEffect(chatId) {
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                    .sortedByDescending { it.timestamp }  // Order by latest messages
                messages = messageList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatScreen", "Failed to load messages: ${error.message}")
            }
        })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with friend's username and profile image
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (profileImageUri.isNotEmpty()) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
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

        // Messages List
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true,
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages) { message ->

                MessageBubble(message, currentUserId)
            }
        }

        // Message Input
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
                        receiverId = receiverUserId, // Correct user ID
                        text = messageText,
                        timestamp = System.currentTimeMillis(),
                        seen = false
                    )
                    messagesRef.push().setValue(newMessage) // ✅ Save message to Firebase
                    messageText = ""  // ✅ Clear input field
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalAlignment = if (isSentByUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isSentByUser) Color(0xFF2F9ECE) else Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(16.dp) // Curved bubble effect
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = maxWidth)
        ) {
            Text(
                text = message.text,
                color = if (isSentByUser) Color.White else Color.Black,
                style = TextStyle(fontSize = 16.sp)
            )
        }
    }
}
