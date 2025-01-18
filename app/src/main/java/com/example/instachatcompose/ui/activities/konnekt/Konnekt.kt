@file:Suppress("NAME_SHADOWING", "UNCHECKED_CAST")

package com.example.instachatcompose.ui.activities.konnekt

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.activities.login.LoginActivity
import com.example.instachatcompose.ui.activities.mainpage.MessageActivity
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Konnekt: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier= Modifier.fillMaxSize()) {
                        AddFriendsPage(currentUserId = currentUserId)
                    }
                }
            }
        }
    }
}

@Composable
fun AddFriendsPage(currentUserId: String) {
    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        val activity = LocalContext.current as? ComponentActivity
        val username: String = activity?.intent?.getStringExtra("username") ?: "DefaultUsername"
        val bio: String = activity?.intent?.getStringExtra("bio") ?: "DefaultBio"
        val profilePic: Uri = Uri.parse(activity?.intent?.getStringExtra("profileUri") ?: "")

        // Main Content
        Column(
            modifier = Modifier.padding(horizontal = 15.dp)
        ) {
            NavHost(
                navController = navController,
                startDestination = "user_add_friends"
            ) {
                composable("user_add_friends") {
                    Column {
                        UserAddFriends(
                            username = username,
                            profilePic = profilePic,
                            onSettingsClick = { navController.navigate("settings") }
                        )
                        ReceivedRequestsScreen()
//                        UserReceivesRequest(currentUserId = currentUserId)
                    }
                }
                composable("settings") {
                    SettingsPage(navController) // Replace with your actual settings composable
                }
            }
        }

        // BottomAppBar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
        ) {
            BottomAppBarKonnekt(
                username = username,
                profilePic = profilePic,

            )
        }
    }
}



@Composable
fun UserAddFriends(username: String, profilePic: Uri, onSettingsClick: () -> Unit) {
    val settingsIcon = painterResource(id = R.drawable.settings)
    val searchIcon = painterResource(id = R.drawable.searchicon)

    var searchResults by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    val database = FirebaseDatabase.getInstance().getReference("users")
    var search by remember { mutableStateOf("") }
    var searchPerformed by remember { mutableStateOf(false) }
    var allUsers by remember { mutableStateOf(listOf<Map<String, Any>>()) }


    fun performSearch(query: String) {
        if (query.isNotEmpty()) {
            searchResults = allUsers.filter {
                val username = it["username"] as? String ?: ""
                username.contains(query, ignoreCase = true)
            }
            searchPerformed = true
        } else {
            searchResults = listOf()
            searchPerformed = false
        }
    }
    fun loadAllUsers() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val users = dataSnapshot.children.mapNotNull { snapshot ->
                    val userMap = snapshot.value as? Map<String, Any>
                    userMap?.let {
                        mapOf(
                            "username" to (it["username"] as? String ?: ""),
                            "email" to (it["email"] as? String ?: ""),
                            "profileImageUri" to (it["profileImageUri"] as? String ?: "")
                        )
                    }
                }
                allUsers = users
                performSearch(search) // Filter results initially based on the current search
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseSearch", "Error fetching data", databaseError.toException())
            }
        })
    }


    loadAllUsers()

    fun sendFriendRequest(targetUserId: String) {
        val database = FirebaseDatabase.getInstance().reference
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid

        if (currentUserId != null) {
            // Create the friend request map with the correct "from" and "to" user IDs
            val friendRequest = mapOf(
                "from" to currentUserId, // Current user sending the request
                "to" to targetUserId,   // Target user receiving the request
                "status" to "pending"   // Default status of the request
            )

            // Generate a unique key for the new friend request
            val newRequestKey = database.child("sent_requests").child(currentUserId).push().key

            if (newRequestKey != null) {
                // Create the updates map for both sent and received requests
                val updates = mapOf(
                    "/sent_requests/$currentUserId/$newRequestKey" to friendRequest,
                    "/received_requests/$targetUserId/$newRequestKey" to friendRequest
                )

                // Update the database atomically
                database.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d("FriendRequest", "Friend request sent successfully")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FriendRequest", "Error sending friend request", exception)
                    }
            } else {
                Log.e("FriendRequest", "Failed to generate new request key")
            }
        } else {
            Log.e("FriendRequest", "User not logged in")
        }
    }

    Column {
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                val imagePainter: AsyncImagePainter = rememberAsyncImagePainter(model = profilePic)
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

            Row (
               modifier= Modifier.clickable { onSettingsClick() }
            ){
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
            text = "Add Friends",
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = searchIcon,
                    contentDescription = "Search",
                    modifier = Modifier
                        .size(35.dp)
                        .padding(start = 8.dp)
                )

                BasicTextField(
                    value = search,
                    onValueChange = {
                        search = it
                        performSearch(it)
                    },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp)
                )

                if (search.isEmpty()) {
                    Text(
                        text = "Find Friends",
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))


        LazyColumn {
            if (!searchPerformed) {
                // Show nothing before search is performed
            } else if (searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No results found")
                    }
                }
            } else {
                items(searchResults) { result ->
                    val username = result["username"] as? String ?: ""
                    val email = result["email"] as? String ?: ""
                    val profileImageUri = result["profileImageUri"] as? String ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(Modifier.width(160.dp)) {


                            val painter = rememberAsyncImagePainter(model = profileImageUri)
                            Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(text = username, fontWeight = FontWeight.Bold)
                                Text(text = email, color = Color.Gray)
                            }
                        }
                        val addFriend= painterResource(id = R.drawable.addfriends)
                        Row(
                            modifier = Modifier
                                .width(110.dp)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF2F9ECE),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(8.dp)
                                .clickable {
                                    sendFriendRequest(username)
                                }
                        ){
                            Image(
                                painter = addFriend,
                                contentDescription = null,
                                modifier= Modifier.size(16.dp)
                                )

                            Text(
                                text = "Add Account",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight(400),
                                    color = Color(0xFF2F9ECE),
                                ),
                            )

                        }

                    }
                }
            }
        }
    }
}

data class FriendRequest(
    val from: String = "",
    val to: String = "",
    val status: String = "pending"
)

fun fetchReceivedRequests(callback: (List<FriendRequest>) -> Unit) {
    val database = FirebaseDatabase.getInstance().reference
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid

    if (currentUserId != null) {
        database.child("received_requests").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val receivedRequests = mutableListOf<FriendRequest>()
                    for (childSnapshot in snapshot.children) {
                        val request = childSnapshot.getValue(FriendRequest::class.java)
                        if (request != null) {
                            receivedRequests.add(request)
                        }
                    }
                    callback(receivedRequests)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FetchRequests", "Error fetching received requests", error.toException())
                }
            })
    } else {
        Log.e("FetchRequests", "User not logged in")
    }
}
@Composable
fun ReceivedRequestsScreen() {
    val receivedRequests = remember { mutableStateOf(listOf<FriendRequest>()) }

    LaunchedEffect(Unit) {
        fetchReceivedRequests { requests ->
            receivedRequests.value = requests
        }
    }

    LazyColumn {
        items(receivedRequests.value) { request ->
            Text(text = "From: ${request.from}", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Status: ${request.status}", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
//fun listenForFriendRequests(
//    currentUserId: String,
//    onNewRequest: (Pair<String, FriendRequest>) -> Unit
//) {
//    val database = FirebaseDatabase.getInstance()
//    val requestsRef = database.getReference("received_requests/$currentUserId")
//
//    Log.d("FriendRequestListener", "Listening for requests for user: $currentUserId")
//
//    requestsRef.addChildEventListener(object : ChildEventListener {
//        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//            val key = snapshot.key
//            val request = snapshot.getValue(FriendRequest::class.java)
//
//            if (key != null && request != null) {
//                Log.d("FriendRequestListener", "Parsed FriendRequest: $request")
//                if (request.to == currentUserId) { // Ensure request is for the current user
//                    onNewRequest(key to request)
//                } else {
//                    Log.d("FriendRequestListener", "Request ignored: Not for current user.")
//                }
//            } else {
//                Log.e("FriendRequestListener", "Failed to parse FriendRequest: key=$key, snapshot=${snapshot.value}")
//            }
//        }
//        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//            Log.d("FriendRequestListener", "Friend request updated: ${snapshot.key}")
//        }
//
//        override fun onChildRemoved(snapshot: DataSnapshot) {
//            Log.d("FriendRequestListener", "Friend request removed: ${snapshot.key}")
//        }
//
//        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//            Log.d("FriendRequestListener", "Friend request moved: ${snapshot.key}")
//        }
//
//        override fun onCancelled(error: DatabaseError) {
//            Log.e("FriendRequestListener", "Failed to listen for requests: ${error.message}")
//        }
//    })
//}
//
//fun acceptFriendRequest(requestId: String, senderId: String, receiverId: String) {
//    val database = FirebaseDatabase.getInstance().reference
//
//    val updates = mapOf(
//        "/friend_requests/sent/$senderId/$requestId/status" to "accepted",
//        "/friend_requests/received/$receiverId/$requestId/status" to "accepted",
//        "/friends/$senderId/$receiverId" to true,
//        "/friends/$receiverId/$senderId" to true
//    )
//
//    database.updateChildren(updates).addOnCompleteListener { task ->
//        if (task.isSuccessful) {
//            Log.d("AcceptRequest", "Friend request accepted successfully")
//        } else {
//            Log.e("AcceptRequest", "Error accepting friend request", task.exception)
//        }
//    }
//}
//
//fun removeFriendRequest(requestId: String, senderId: String, receiverId: String) {
//    val database = FirebaseDatabase.getInstance().reference
//
//    val updates = mapOf(
//        "/friend_requests/sent/$senderId/$requestId" to null,
//        "/friend_requests/received/$receiverId/$requestId" to null
//    )
//
//    database.updateChildren(updates).addOnCompleteListener { task ->
//        if (task.isSuccessful) {
//            Log.d("RemoveRequest", "Friend request removed successfully")
//        } else {
//            Log.e("RemoveRequest", "Error removing friend request", task.exception)
//        }
//    }
//}
//
//@Composable
//fun UserReceivesRequest(currentUserId: String) {
//    val friendRequests = remember { mutableStateListOf<Pair<String, FriendRequest>>() }
//
//    LaunchedEffect(currentUserId) {
//        listenForFriendRequests(currentUserId) { newRequest ->
//            val existingIndex = friendRequests.indexOfFirst { it.first == newRequest.first }
//            if (existingIndex >= 0) {
//                friendRequests[existingIndex] = newRequest // Update existing
//            } else {
//                friendRequests.add(newRequest) // Add new
//            }
//        }
//    }
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(top = 16.dp)
//    ) {
//        Text(
//            text = "Friend Requests",
//            style = TextStyle(
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.Black
//            )
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        if (friendRequests.isEmpty()) {
//            Text(
//                text = "No new friend requests",
//                style = TextStyle(
//                    color = Color.Gray,
//                    fontSize = 16.sp
//                ),
//                modifier = Modifier.align(Alignment.CenterHorizontally)
//            )
//        } else {
//            LazyColumn(modifier = Modifier.fillMaxWidth()) {
//                items(friendRequests.filter { it.second.to == currentUserId }) { (requestId, friendRequest) ->
//                    FriendRequestItem(
//                        friendRequest = friendRequest,
//                        onAccept = {
//                            acceptFriendRequest(
//                                requestId = requestId,
//                                senderId = friendRequest.from,
//                                receiverId = currentUserId
//                            )
//                            // Remove accepted request from the list
//                            friendRequests.removeIf { it.first == requestId }
//                        },
//                        onDecline = {
//                            removeFriendRequest(
//                                requestId = requestId,
//                                senderId = friendRequest.from,
//                                receiverId = currentUserId
//                            )
//                            // Remove declined request from the list
//                            friendRequests.removeIf { it.first == requestId }
//                        }
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun FriendRequestItem(
//    friendRequest: FriendRequest,
//    onAccept: () -> Unit,
//    onDecline: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Column {
//            Text(text = "From: ${friendRequest.from}", fontWeight = FontWeight.Bold)
//            Text(text = "Status: ${friendRequest.status}", color = Color.Gray)
//        }
//        Row {
//            Button(onClick = onAccept) {
//                Text("Accept")
//            }
//            Spacer(modifier = Modifier.width(8.dp))
//            Button(onClick = onDecline) {
//                Text("Decline")
//            }
//        }
//    }
//}

enum class BottomAppBarItem {
    Messages,
    Calls,
    AddFriends
}

@Composable
fun BottomAppBarKonnekt(username: String,profilePic: Uri) {
    var activeItem by remember { mutableStateOf(BottomAppBarItem.AddFriends) }
    val context= LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically 
    ) {
        BottomAppBarItemKonnekt(
            label = "Messages",
            isActive = activeItem == BottomAppBarItem.Messages,
            activeIcon = R.drawable.bottombar_activemessagespage,
            passiveIcon = R.drawable.bottombar_passivemessagespage,
            onClick = {
                activeItem = BottomAppBarItem.Messages
                val intent = Intent(context, MessageActivity::class.java)
                intent.putExtra("username", username)
                intent.putExtra("profileUri", profilePic.toString())
                context.startActivity(intent)
            }
        )
        BottomAppBarItemKonnekt(
            label = "Call Logs",
            isActive = activeItem == BottomAppBarItem.Calls,
            activeIcon = R.drawable.bottombar_activecallspage,
            passiveIcon = R.drawable.bottombar_passivecallspage,
            onClick = { activeItem = BottomAppBarItem.Calls }
        )
        BottomAppBarItemKonnekt(
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
fun BottomAppBarItemKonnekt(
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
            color = if (isActive) Color(0xFF2F9ECE) else Color(0xFF696969) // Change text color based on active/passive state
        )
    }
}


@Composable
fun SettingsPage(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // Handle logout
    fun logout() {
        auth.signOut()
        val intent= Intent(context, LoginActivity::class.java)
        context.startActivity(intent)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Settings",
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Example setting options
        Text(text = "Option 1: Change Username")
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Option 2: Change Profile Picture")
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Option 3: Privacy Settings")
        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = { /* Handle save or any action */ }) {
            Text("Save Settings")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Logout Button
        Button(onClick = { logout() }) {
            Text("Logout")
        }
    }
}