@file:Suppress("NAME_SHADOWING", "UNCHECKED_CAST")

package com.el.konnekt.ui.activities.konnekt

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.el.konnekt.KonnektApplication
import com.el.konnekt.R
import com.el.konnekt.data.ChatViewModel
import com.el.konnekt.data.ChatViewModelFactory
import com.el.konnekt.data.ForegroundFriendRequestListener
import com.el.konnekt.data.ForegroundMessageListener
import com.el.konnekt.data.local.AppDatabase
import com.el.konnekt.data.local.LocalDataSource
import com.el.konnekt.data.remote.FirebaseDataSource
import com.el.konnekt.data.repository.ChatRepository
import com.el.konnekt.ui.activities.Settings
import com.el.konnekt.ui.activities.mainpage.ProfileActivity
import com.el.konnekt.ui.components.BottomNavItem
import com.el.konnekt.ui.components.BottomNavigationBar
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.el.konnekt.utils.ForegroundNotificationHandler
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Konnekt : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val database = FirebaseDatabase.getInstance().reference
                val userRef = database.child("users").child(currentUserId)

                userRef.child("username").get().addOnSuccessListener { snapshot ->
                    val currentUsername = snapshot.value as? String ?: "AnonymousUser"
                    lifecycleScope.launch(Dispatchers.Main) {
                        loadUserUI(currentUsername)
                    }
                }.addOnFailureListener { exception ->
                    Log.e("Konnekt", "Failed to fetch username for userId: $currentUserId", exception)
                    lifecycleScope.launch(Dispatchers.Main) {
                        loadUserUI("AnonymousUser")
                    }
                }
            } catch (e: Exception) {
                Log.e("Konnekt", "Error in onCreate", e)
                lifecycleScope.launch(Dispatchers.Main) {
                    loadUserUI("AnonymousUser")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        KonnektApplication.setCurrentChat(null)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    ForegroundMessageListener.startListening(this@Konnekt, userId)
                    ForegroundFriendRequestListener.startListening(this@Konnekt, userId)
                } catch (e: Exception) {
                    Log.e("Konnekt", "Error starting listeners", e)
                }
            }
        }

        ForegroundNotificationHandler.cancelFriendRequestNotification(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            ForegroundMessageListener.stopListening()
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                ForegroundFriendRequestListener.stopListening(userId)
            }
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
    Box(modifier = Modifier.fillMaxSize()) {
        val activity = LocalContext.current as? ComponentActivity
        val username: String = activity?.intent?.getStringExtra("username") ?: "DefaultUsername"
        val profilePic: Uri = Uri.parse(activity?.intent?.getStringExtra("profileUri") ?: "")

        Column(
            modifier = Modifier.padding(horizontal = 15.dp)
        ) {
            NavHost(
                navController = navController,
                startDestination = "user_add_friends"
            ) {
                composable("user_add_friends") {
                    Column {
                        UserAddFriends()
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
            BottomNavigationBar(
                currentScreen = BottomNavItem.Konnekt,
                username = username,
                profilePic = profilePic
            )
        }
    }
}

@Composable
fun UserAddFriends() {
    val context = LocalContext.current as ComponentActivity

    var searchResults by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    val database = FirebaseDatabase.getInstance().getReference("users")
    var search by remember { mutableStateOf("") }
    var searchPerformed by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("Loading...") }

    // ✅ OPTIMIZATION: Add rate limiting for friend requests
    var lastRequestTime by remember { mutableStateOf(0L) }
    val REQUEST_COOLDOWN = 2000L // 2 seconds

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
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        viewModel.fetchUserProfile(context, userId) { fetchedUsername, fetchedProfilePicUrl ->
            username = fetchedUsername ?: "Unknown"
            profilePicUrl = fetchedProfilePicUrl
        }
    }

    // ✅ OPTIMIZED: Search Firebase directly instead of loading all users
    fun performSearch(query: String) {
        if (query.isEmpty()) {
            searchResults = emptyList()
            searchPerformed = false
            isSearching = false
            return
        }

        isSearching = true
        searchPerformed = true

        // Firebase query: search usernames starting with the query
        database
            .orderByChild("username")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limitToFirst(20) // Only fetch 20 results max
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = snapshot.children.mapNotNull { userSnapshot ->
                        val uid = userSnapshot.key ?: return@mapNotNull null
                        if (uid == userId) return@mapNotNull null // Don't show current user

                        val userMap = userSnapshot.value as? Map<String, Any>
                        userMap?.let {
                            mapOf(
                                "uid" to uid,
                                "username" to (it["username"] as? String ?: ""),
                                "email" to (it["email"] as? String ?: ""),
                                "profileImageUri" to (it["profileImageUri"] as? String ?: "")
                            )
                        }
                    }
                    searchResults = users
                    isSearching = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseSearch", "Error searching users", error.toException())
                    searchResults = emptyList()
                    isSearching = false
                }
            })
    }

    // ✅ OPTIMIZED: Add rate limiting and validation
    fun sendFriendRequest(targetUserId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid

        if (currentUserId == null) {
            Log.e("FriendRequest", "User not logged in")
            return
        }

        // ✅ Validate input
        if (targetUserId.isBlank() || targetUserId == currentUserId) {
            Log.w("FriendRequest", "Invalid target user ID")
            return
        }

        // ✅ Rate limiting
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRequestTime < REQUEST_COOLDOWN) {
            android.widget.Toast.makeText(
                context,
                "Please wait before sending another request",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        lastRequestTime = currentTime

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
                                "users/$currentUserId/sent_requests/$newRequestKey" to friendRequest,
                                "users/$targetUserId/received_requests/$newRequestKey" to friendRequest
                            )

                            database.updateChildren(updates)
                                .addOnSuccessListener {
                                    Log.d("FriendRequest", "Friend request sent successfully")
                                    android.widget.Toast.makeText(
                                        context,
                                        "Friend request sent",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("FriendRequest", "Error sending friend request", exception)
                                    android.widget.Toast.makeText(
                                        context,
                                        "Failed to send request",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendRequest", "Error checking for duplicate requests", error.toException())
                }
            })
    }

    Column {
        AddFriendsTopBar(
            username = username,
            profilePicUrl = profilePicUrl,
            searchQuery = search,
            onSearchQueryChange = {
                search = it
                performSearch(it)
            },
            context = context
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn {
            if (!searchPerformed) {
                // ✅ IMPROVED: Show prompt to search
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.searchicon),
                                contentDescription = "Search",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Search for friends by username",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else if (isSearching) {
                // ✅ IMPROVED: Show loading state
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2F9ECE),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else if (searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No users found matching \"$search\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            Modifier
                                .width(160.dp)
                                .clickable {
                                    val intent = Intent(context, ProfileActivity::class.java).apply {
                                        putExtra("friendId", targetUserId)
                                    }
                                    context.startActivity(intent)
                                }
                        ) {
                            val painter = rememberAsyncImagePainter(model = profileImageUri)
                            Image(
                                painter = painter,
                                contentScale = ContentScale.Crop,
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
                            onClick = {
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
fun AddFriendsTopBar(
    username: String,
    profilePicUrl: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    context: ComponentActivity
) {
    val settingsIcon = painterResource(id = R.drawable.settings)
    val searchIcon = painterResource(id = R.drawable.searchicon)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
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

                Text(
                    text = username,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        context.startActivity(
                            Intent(context, Settings::class.java)
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = settingsIcon,
                    contentDescription = "Settings",
                    tint = Color(0xFF2F9ECE),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Settings",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2F9ECE)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Add Friends",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
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
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFF2F9ECE)
            )
        )
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
                            "friendId" to from,
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
    val context = LocalContext.current

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
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val intent = Intent(context, ProfileActivity::class.java).apply {
                                    putExtra("friendId", friendId)
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
                                handleFriendRequest(request, isAccepted = true, showDialog, message, username) {
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
                                handleFriendRequest(request, isAccepted = false, showDialog, message, username) {
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
    onComplete: () -> Unit = {}
) {
    val db = Firebase.database.reference
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    if (isAccepted) {
        Log.d("FriendRequest", "Accepted: Processing friend request")

        val receivedTask = db.child("users/$userId/received_requests")
            .orderByChild("from").equalTo(request.from).get()

        val sentTask = db.child("users/${request.from}/sent_requests")
            .orderByChild("to").equalTo(userId).get()

        Tasks.whenAllSuccess<DataSnapshot>(receivedTask, sentTask)
            .addOnSuccessListener { results ->
                val updates = mutableMapOf<String, Any?>()

                results[0].children.forEach { child ->
                    updates["users/$userId/received_requests/${child.key}"] = null
                }

                results[1].children.forEach { child ->
                    updates["users/${request.from}/sent_requests/${child.key}"] = null
                }

                // Add friends
                val receiverFriendKey = db.child("users/$userId/friends").push().key
                val senderFriendKey = db.child("users/${request.from}/friends").push().key

                if (receiverFriendKey != null && senderFriendKey != null) {
                    updates["users/$userId/friends/$receiverFriendKey"] = mapOf(
                        "friendId" to request.from,
                        "timestamp" to System.currentTimeMillis()
                    )
                    updates["users/${request.from}/friends/$senderFriendKey"] = mapOf(
                        "friendId" to userId,
                        "timestamp" to System.currentTimeMillis()
                    )

                    // Perform all updates at once
                    db.updateChildren(updates)
                        .addOnSuccessListener {
                            Log.d("FriendRequest", "Friend request accepted successfully")
                            showDialog.value = true
                            message.value = "You are now friends with $username"
                            onComplete()
                        }
                        .addOnFailureListener { e ->
                            Log.e("FriendRequest", "Failed to update database: ${e.message}")
                        }
                } else {
                    Log.e("FriendRequest", "Failed to generate keys for friends")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FriendRequest", "Failed to fetch requests: ${e.message}")
            }
    } else {
        Log.d("FriendRequest", "Declined: Removing request from both received and sent requests")

        // ✅ Use Tasks.whenAllSuccess for parallel execution
        val receivedTask = db.child("users/$userId/received_requests")
            .orderByChild("from").equalTo(request.from).get()

        val sentTask = db.child("users/${request.from}/sent_requests")
            .orderByChild("to").equalTo(userId).get()

        Tasks.whenAllSuccess<DataSnapshot>(receivedTask, sentTask)
            .addOnSuccessListener { results ->
                val updates = mutableMapOf<String, Any?>()

                // Delete from received_requests
                results[0].children.forEach { child ->
                    updates["users/$userId/received_requests/${child.key}"] = null
                }

                // Delete from sent_requests
                results[1].children.forEach { child ->
                    updates["users/${request.from}/sent_requests/${child.key}"] = null
                }

                // Perform all deletions at once
                db.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d("FriendRequest", "Friend request declined and removed")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("FriendRequest", "Failed to delete requests: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FriendRequest", "Failed to fetch requests: ${e.message}")
            }
    }
}