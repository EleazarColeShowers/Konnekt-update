@file:Suppress("NAME_SHADOWING", "UNCHECKED_CAST")

package com.example.instachatcompose.ui.activities.konnekt

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.activities.Settings
import com.example.instachatcompose.ui.activities.components.SharedBottomAppBar
import com.example.instachatcompose.ui.activities.data.BottomAppBarItem
import com.example.instachatcompose.ui.activities.data.ChatViewModel
import com.example.instachatcompose.ui.activities.data.NotificationHelper
import com.example.instachatcompose.ui.activities.mainpage.ProfileActivity
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class Konnekt : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference
        val userRef = database.child("users").child(currentUserId)

        userRef.child("username").get().addOnSuccessListener { snapshot ->
            val currentUsername = snapshot.value as? String ?: "AnonymousUser"
            loadUserUI(currentUsername)
        }.addOnFailureListener { exception ->
            Log.e("Konnekt", "Failed to fetch username for userId: $currentUserId", exception)

            loadUserUI("AnonymousUser")
        }
    }

    private fun loadUserUI(currentUsername: String) {
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AddFriendsPage()
                    }
                }
            }
        }
    }
}
@Composable
fun AddFriendsPage() {
    val navController = rememberNavController()
    val activity = LocalContext.current as? ComponentActivity
    val context = LocalContext.current
    val username: String = activity?.intent?.getStringExtra("username") ?: "DefaultUsername"
    val profilePic: Uri = Uri.parse(activity?.intent?.getStringExtra("profileUri") ?: "")
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val viewModel: ChatViewModel = viewModel()
    LaunchedEffect(userId) {
        viewModel.listenForFriendRequests(context, userId)
    }

    Box(modifier = Modifier.fillMaxSize()) {

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
                        )
                        UserReceivesRequest()
                    }
                }

            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
        ) {
            SharedBottomAppBar(username = username, profilePic = profilePic, startingTab = BottomAppBarItem.AddFriends)
        }
    }
}

@Composable
fun UserAddFriends() {
    val settingsIcon = painterResource(id = R.drawable.settings)
    val searchIcon = painterResource(id = R.drawable.searchicon)
    val context = LocalContext.current as ComponentActivity

    var searchResults by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    val database = FirebaseDatabase.getInstance().getReference("users")
    var search by remember { mutableStateOf("") }
    var searchPerformed by remember { mutableStateOf(false) }
    var allUsers by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("Loading...") }
    val viewModel: ChatViewModel = viewModel()

    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    LaunchedEffect(userId) {
        viewModel.fetchUserProfile(context, userId) { fetchedUsername, fetchedProfilePicUrl ->
            username = fetchedUsername ?: "Unknown"
            profilePicUrl = fetchedProfilePicUrl
        }
    }

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
                    val uid = snapshot.key ?: return@mapNotNull null
                    val userMap = snapshot.value as? Map<String, Any>
                    userMap?.let {
                        mapOf(
                            "uid" to uid,
                            "username" to (it["username"] as? String ?: ""),
                            "email" to (it["email"] as? String ?: ""),
                            "profileImageUri" to (it["profileImageUri"] as? String ?: "")
                        )
                    }
                }
                allUsers = users
                performSearch(search)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseSearch", "Error fetching data", databaseError.toException())
            }
        })
    }


    loadAllUsers()

    fun sendFriendRequest(targetUserId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid

        if (currentUserId != null) {
            val sentRequestsRef = database.child(currentUserId).child("sent_requests")

            sentRequestsRef.orderByChild("to").equalTo(targetUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val existingRequest = snapshot.children.firstOrNull {
                            it.child("status").value == "pending"
                        }

                        if (existingRequest != null) {
                            showDuplicateDialog = true
                        } else {
                            val friendRequest = mapOf(
                                "from" to currentUserId,
                                "to" to targetUserId,
                                "status" to "pending"
                            )

                            val newRequestKey = sentRequestsRef.push().key

                            if (newRequestKey != null) {
                                val updates = mapOf(
                                    "/$currentUserId/sent_requests/$newRequestKey" to friendRequest,
                                    "/$targetUserId/received_requests/$newRequestKey" to friendRequest
                                )

                                database.updateChildren(updates)
                                    .addOnSuccessListener {
                                        Log.d("FriendRequest", "Friend request sent successfully")
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e("FriendRequest", "Error sending friend request", exception)
                                    }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FriendRequest", "Error checking for duplicate requests", error.toException())
                    }
                })
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
                if (!profilePicUrl.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = profilePicUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(31.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(31.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = Color.White, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = username,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight(400),
                        color = MaterialTheme.colorScheme.onBackground,
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
            text = "Add Friends",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight(500),
                color = MaterialTheme.colorScheme.onBackground,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp)
                )

                if (search.isEmpty()) {
                    Text(
                        text = "Find Friends",
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn {
            if (!searchPerformed) {
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
                    val targetUserId = result["uid"] as? String ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            Modifier.width(160.dp)
                                .clickable {
                                    val intent = Intent(context, ProfileActivity::class.java).apply {
                                        putExtra("friendId", targetUserId) // Pass only the userId
                                    }
                                    context.startActivity(intent)
                                }
                        )
                        {
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
                                Text(text = email, color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                        val addFriend = painterResource(id = R.drawable.addfriends)
                        Button(
                            modifier = Modifier
                                .height(36.dp)
                                .width(100.dp),
                            onClick= {
                                sendFriendRequest(targetUserId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                            border = BorderStroke(1.dp, Color(0xFF2F9ECE)),
                        ) {
                            Image(painter = addFriend, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Add ", color = Color(0xFF2F9ECE))
                        }
                    }
                }
            }
        }
    }

    if (showDuplicateDialog) {
        ShowDuplicateRequestDialog(onDismiss = { showDuplicateDialog = false })
    }
}

@Composable
fun ShowDuplicateRequestDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        title = {
            Text("Duplicate Request")
        },
        text = {
            Text("A pending friend request already exists.")
        }
    )
}

data class FriendRequest(
    val from: String = "",
    val to: String = "",
    val status: String = "pending"
)

fun loadReceivedRequestsWithDetails(
    userId: String,
    onRequestsLoaded: (List<Pair<FriendRequest, Map<String, String>>>) -> Unit
) {
    val database = FirebaseDatabase.getInstance()
        .getReference("users")
        .child(userId)
        .child("received_requests")
    val usersDatabase = FirebaseDatabase.getInstance()
        .getReference("users")

    Log.d("Firebase", "Fetching friend requests for userId: $userId")

    database.get().addOnSuccessListener { snapshot ->
        Log.d("Firebase", "Snapshot exists: ${snapshot.exists()}")

        if (snapshot.exists()) {

            val fetchDetailsTasks = snapshot.children.map { requestSnapshot ->
                val from = requestSnapshot.child("from").getValue(String::class.java) ?: ""
                val to = requestSnapshot.child("to").getValue(String::class.java) ?: ""
                val status = requestSnapshot.child("status").getValue(String::class.java) ?: "pending"

                Log.d("Firebase", "Friend request: from=$from, to=$to, status=$status")

                val request = FriendRequest(from, to, status)
                val detailsTask = usersDatabase.child(from).get().continueWith { userSnapshotTask ->
                    val userSnapshot = userSnapshotTask.result
                    val userDetails = if (userSnapshot.exists()) {
                        mapOf(
                            "friendId" to from, // Store the sender's userId
                            "username" to (userSnapshot.child("username").getValue(String::class.java) ?: "Unknown User"),
                            "profileImageUri" to (userSnapshot.child("profileImageUri").getValue(String::class.java) ?: "")
                        )
                    } else {
                        mapOf("username" to "Unknown User", "profileImageUri" to "")
                    }
                    Pair(request, userDetails)
                }
                detailsTask
            }

            Tasks.whenAllSuccess<Pair<FriendRequest, Map<String, String>>>(fetchDetailsTasks)
                .addOnSuccessListener { detailedRequests ->
                    Log.d("Firebase", "Loaded ${detailedRequests.size} friend requests")
                    onRequestsLoaded(detailedRequests)
                }
        } else {
            Log.d("Firebase", "No received friend requests found for userId: $userId")
            onRequestsLoaded(emptyList())
        }
    }.addOnFailureListener { exception ->
        Log.e("Firebase", "Failed to load received requests for userId: $userId", exception)
        onRequestsLoaded(emptyList())
    }
}

@Composable
fun UserReceivesRequest() {
    val friendRequests = remember { mutableStateListOf<Pair<FriendRequest, Map<String, String>>>() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""
    val showDialog = remember { mutableStateOf(false) }
    val message = remember { mutableStateOf("") }
    val context= LocalContext.current

    LaunchedEffect(userId) {
        Log.d("UserReceivesRequest", "Fetching friend requests for $userId")
        loadReceivedRequestsWithDetails(userId) { requests ->
            Log.d("UserReceivesRequest", "Received ${requests.size} friend requests")
            friendRequests.clear()
            friendRequests.addAll(requests)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Received Friend Requests",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(friendRequests) { (request, userDetails) ->
                val username = userDetails["username"] ?: "Unknown User"
                val profileImageUri = userDetails["profileImageUri"]
                val friendId = userDetails["friendId"] ?: ""

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                            .clickable {
                                val intent = Intent(context, ProfileActivity::class.java).apply {
                                    putExtra("friendId", friendId) // Pass only the userId
                                }
                                context.startActivity(intent)
                            }
                    ) {
                        if (!profileImageUri.isNullOrEmpty()) {
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
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = username,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Button(
                            onClick = {
                                handleFriendRequest(
                                    request = request,
                                    isAccepted = true,
                                    showDialog = showDialog,
                                    message = message,
                                    username = username,
                                    context = context // ✅ Add this
                                ) {
                                    friendRequests.remove(Pair(request, userDetails))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                            border = BorderStroke(1.dp, Color(0xFF2F9ECE)),
                            modifier = Modifier
                                .height(33.dp)
                                .width(80.dp)
                        ) {
                            Text(
                                text = "Accept",
                                fontSize = 10.sp,
                                color = Color(0xFF2F9ECE)
                            )
                        }
                        IconButton(
                            onClick = {
                                handleFriendRequest(request, isAccepted = false, showDialog, message, username, context= context) {
                                    friendRequests.remove(Pair(request, userDetails))
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Decline",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text(text = "Friend Request Accepted") },
            text = { Text(text = message.value) },
            confirmButton = {
                Button(onClick = { showDialog.value = false }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun handleFriendRequest(
    request: FriendRequest,
    isAccepted: Boolean,
    showDialog: MutableState<Boolean>,
    message: MutableState<String>,
    username: String,
    context: Context,
    onComplete: () -> Unit = {}
) {
    val db = Firebase.database.reference
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    if (isAccepted) {
        Log.d("FriendRequest", "Accepted: Deleting from received and sent requests")
        db.child("users").child(userId).child("received_requests")
            .orderByChild("from").equalTo(request.from)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { it.ref.removeValue() }

                db.child("users").child(request.from).child("sent_requests")
                    .orderByChild("to").equalTo(userId)
                    .get()
                    .addOnSuccessListener { sentSnapshot ->
                        sentSnapshot.children.forEach { it.ref.removeValue() }

                        val receiverToSender = mapOf(
                            "friendId" to request.from,
                            "timestamp" to System.currentTimeMillis()
                        )
                        db.child("users").child(userId).child("friends").push()
                            .setValue(receiverToSender)
                            .addOnSuccessListener {
                                val senderToReceiver = mapOf(
                                    "friendId" to userId,
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.child("users").child(request.from).child("friends").push()
                                    .setValue(senderToReceiver)
                                    .addOnSuccessListener {
                                        Log.d("FriendRequest", "Friend request accepted and both users added as friends.")
                                        showDialog.value = true
                                        message.value = "You are now friends with $username"
                                        onComplete()

                                        // ✅ Notify sender
                                        db.child("users").child(request.from).get()
                                            .addOnSuccessListener { senderSnapshot ->
                                                val senderContext = context
                                                NotificationHelper.showNotification(
                                                    context = senderContext,
                                                    title = "Friend Request Accepted",
                                                    message = "You are now friends with $username"
                                                )
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("FriendRequest", "Failed to notify sender: ${e.message}")
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FriendRequest", "Failed to add sender to receiver's friends: ${e.message}")
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("FriendRequest", "Failed to add receiver to sender's friends: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FriendRequest", "Failed to delete from sent requests: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FriendRequest", "Failed to delete from received requests: ${e.message}")
            }
    } else {
        Log.d("FriendRequest", "Declined: Removing request from both received and sent requests")

        db.child("users").child(userId).child("received_requests")
            .orderByChild("from").equalTo(request.from)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { it.ref.removeValue() }

                db.child("users").child(request.from).child("sent_requests")
                    .orderByChild("to").equalTo(userId)
                    .get()
                    .addOnSuccessListener { sentSnapshot ->
                        sentSnapshot.children.forEach { it.ref.removeValue() }
                        Log.d("FriendRequest", "Friend request declined and removed from both users.")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("FriendRequest", "Failed to delete from sender's sent requests: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FriendRequest", "Failed to delete from received requests: ${e.message}")
            }
    }
}
