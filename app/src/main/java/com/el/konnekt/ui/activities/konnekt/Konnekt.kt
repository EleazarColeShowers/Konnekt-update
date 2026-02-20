@file:Suppress("NAME_SHADOWING", "UNCHECKED_CAST")

package com.el.konnekt.ui.activities.konnekt

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.google.firebase.database.*
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
//  Design Tokens
// ─────────────────────────────────────────────
private val Blue       = Color(0xFF2F9ECE)
private val BlueLight  = Color(0xFFEBF7FC)
private val BlueMid    = Color(0xFFC6E9F7)
private val BgColor    = Color(0xFFF7FAFC)
private val TextPrimary   = Color(0xFF0D1B2A)
private val TextSecondary = Color(0xFF6B8299)
private val BorderColor   = Color(0xFFE2EDF4)
private val RedColor   = Color(0xFFF04E37)
private val RedLight   = Color(0xFFFEF2F2)
private val RedBorder  = Color(0xFFFECACA)
private val SurfaceColor  = Color.White

// Gradient used for the Add/Accept buttons
private val BlueBrush = Brush.horizontalGradient(listOf(Color(0xFF2F9ECE), Color(0xFF54B8E2)))

// Placeholder avatar gradients
private val avatarGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFF2F9ECE), Color(0xFF88CDE8))),
    Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFC4B5FD))),
    Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFFDE68A))),
    Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF6EE7B7))),
)

class Konnekt : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                FirebaseDatabase.getInstance().reference
                    .child("users").child(currentUserId)
                    .child("username").get()
                    .addOnSuccessListener { snapshot ->
                        val name = snapshot.value as? String ?: "AnonymousUser"
                        lifecycleScope.launch(Dispatchers.Main) { loadUserUI(name) }
                    }
                    .addOnFailureListener {
                        lifecycleScope.launch(Dispatchers.Main) { loadUserUI("AnonymousUser") }
                    }
            } catch (e: Exception) {
                lifecycleScope.launch(Dispatchers.Main) { loadUserUI("AnonymousUser") }
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
            FirebaseAuth.getInstance().currentUser?.uid?.let {
                ForegroundFriendRequestListener.stopListening(it)
            }
        }
    }

    private fun loadUserUI(currentUsername: String) {
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                        .safeDrawingPadding(),
                    color = BgColor
                ) {
                    AddFriendsPage()
                }
            }
        }
    }
}

@Composable
fun AddFriendsPage() {
    val navController = rememberNavController()
    val activity      = LocalContext.current as? ComponentActivity
    val username      = activity?.intent?.getStringExtra("username") ?: "DefaultUsername"
    val profilePic    = Uri.parse(activity?.intent?.getStringExtra("profileUri") ?: "")

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            NavHost(navController = navController, startDestination = "user_add_friends") {
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

// ─────────────────────────────────────────────
//  Top Bar
// ─────────────────────────────────────────────
@Composable
fun AddFriendsTopBar(
    username: String,
    profilePicUrl: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    context: ComponentActivity
) {
    val settingsIcon = painterResource(id = R.drawable.settings)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, BlueMid, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BlueBrush)
                            .border(2.dp, SurfaceColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = username,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }

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

        Spacer(modifier = Modifier.height(4.dp))

        // ── Page title ─────────────────────────────────────────
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Add ",
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            )
            Text(
                text = "Friends",
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Blue)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── Search field ───────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceColor,
            shadowElevation = 4.dp,
            border = BorderStroke(
                width = if (searchQuery.isNotEmpty()) 1.5.dp else 1.dp,
                color = if (searchQuery.isNotEmpty()) Blue else BorderColor
            )
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = {
                    Text(
                        "Search by username…",
                        fontSize = 15.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (searchQuery.isNotEmpty()) Blue else TextSecondary.copy(alpha = 0.5f)
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = scaleIn(),
                        exit = scaleOut()
                    ) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp),
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Blue
                ),
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary)
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Section Header with optional count badge
// ─────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.08.sp
            )
        )
        if (count != null && count > 0) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BlueLight
            ) {
                Text(
                    text = "$count found",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Blue)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Gradient placeholder avatar (rounded square or circle)
// ─────────────────────────────────────────────
@Composable
private fun PlaceholderAvatar(
    text: String,
    size: Int,
    roundedSquare: Boolean = true,
    index: Int = 0
) {
    val gradient = avatarGradients[index % avatarGradients.size]
    val shape = if (roundedSquare) RoundedCornerShape(14.dp) else CircleShape
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(shape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = (size * 0.38f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────
//  Search result user card
// ─────────────────────────────────────────────
@Composable
private fun UserResultCard(
    result: Map<String, Any>,
    index: Int,
    onAddClick: (String) -> Unit,
    onCardClick: (String) -> Unit
) {
    val username        = result["username"] as? String ?: ""
    val email           = result["email"] as? String ?: ""
    val profileImageUri = result["profileImageUri"] as? String ?: ""
    val targetUserId    = result["uid"] as? String ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(targetUserId) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (profileImageUri.isNotEmpty()) {
            AsyncImage(
                model = profileImageUri,
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            PlaceholderAvatar(
                text = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                size = 44,
                roundedSquare = false,
                index = index
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = username,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (email.isNotEmpty()) {
                Text(
                    text = email,
                    style = TextStyle(fontSize = 12.sp, color = TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        OutlinedButton(
            onClick = { onAddClick(targetUserId) },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Blue),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = "Add",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Blue)
            )
        }
    }

    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
}

// ─────────────────────────────────────────────
//  Empty state
// ─────────────────────────────────────────────
@Composable
private fun SearchEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BlueLight,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Blue
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Find your people",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Type a username above to search\nand send a friend request",
            style = TextStyle(fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun NoResultsState(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("😶", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No users found",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "No match for \"$query\"",
            style = TextStyle(fontSize = 13.sp, color = TextSecondary)
        )
    }
}

// ─────────────────────────────────────────────
//  Main Search + Results Composable
// ─────────────────────────────────────────────
@Composable
fun UserAddFriends() {
    val context = LocalContext.current as ComponentActivity
    val app     = context.applicationContext as Application

    var searchResults  by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var search         by remember { mutableStateOf("") }
    var searchPerformed by remember { mutableStateOf(false) }
    var isSearching    by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var profilePicUrl  by remember { mutableStateOf<String?>(null) }
    var username       by remember { mutableStateOf("Loading...") }
    var lastRequestTime by remember { mutableStateOf(0L) }
    val REQUEST_COOLDOWN = 2000L

    val database  = FirebaseDatabase.getInstance().getReference("users")
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            app,
            ChatRepository(FirebaseDataSource(), LocalDataSource(AppDatabase.getDatabase(app)))
        )
    )
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        viewModel.fetchUserProfile(context, userId) { fetchedUsername, fetchedPicUrl ->
            username = fetchedUsername ?: "Unknown"
            profilePicUrl = fetchedPicUrl
        }
    }

    fun performSearch(query: String) {
        if (query.isEmpty()) {
            searchResults = emptyList(); searchPerformed = false; isSearching = false; return
        }
        isSearching = true; searchPerformed = true
        val queryLc = query.lowercase()
        database.orderByChild("username").limitToFirst(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    searchResults = snapshot.children
                        .mapNotNull { userSnapshot ->
                            val uid = userSnapshot.key ?: return@mapNotNull null
                            if (uid == userId) return@mapNotNull null
                            val userMap = userSnapshot.value as? Map<String, Any>
                            val uname = userMap?.get("username") as? String ?: ""
                            if (!uname.lowercase().contains(queryLc)) return@mapNotNull null
                            mapOf(
                                "uid" to uid,
                                "username" to uname,
                                "email" to (userMap?.get("email") as? String ?: ""),
                                "profileImageUri" to (userMap?.get("profileImageUri") as? String ?: "")
                            )
                        }
                        .sortedBy { (it["username"] as? String ?: "").lowercase() }
                        .take(20)
                    isSearching = false
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseSearch", "Search cancelled", error.toException())
                    searchResults = emptyList(); isSearching = false
                }
            })
    }

    fun sendFriendRequest(targetUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (targetUserId.isBlank() || targetUserId == currentUserId) return
        val now = System.currentTimeMillis()
        if (now - lastRequestTime < REQUEST_COOLDOWN) {
            android.widget.Toast.makeText(context, "Please wait before sending another request", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        lastRequestTime = now
        database.child(currentUserId).child("sent_requests")
            .orderByChild("to").equalTo(targetUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val exists = snapshot.children.any { it.child("status").value == "pending" }
                    if (exists) { showDuplicateDialog = true; return }
                    val req = mapOf("from" to currentUserId, "to" to targetUserId, "status" to "pending")
                    val rootRef = FirebaseDatabase.getInstance().reference
                    val key = rootRef.child("users/$currentUserId/sent_requests").push().key ?: return
                    rootRef.updateChildren(mapOf(
                        "users/$currentUserId/sent_requests/$key" to req,
                        "users/$targetUserId/received_requests/$key" to req
                    ))
                        .addOnSuccessListener {
                            android.widget.Toast.makeText(context, "Friend request sent!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            android.widget.Toast.makeText(context, "Failed to send request", android.widget.Toast.LENGTH_SHORT).show()
                        }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    Column {
        AddFriendsTopBar(
            username = username,
            profilePicUrl = profilePicUrl,
            searchQuery = search,
            onSearchQueryChange = { search = it; performSearch(it) },
            context = context
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Results area — animated transitions between states
        AnimatedContent(
            targetState = Triple(searchPerformed, isSearching, searchResults.size),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "search_state"
        ) { (performed, loading, resultCount) ->
            when {
                !performed -> SearchEmptyState()
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Blue, modifier = Modifier.size(32.dp))
                    }
                }
                resultCount == 0 -> NoResultsState(search)
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SectionHeader(title = "Results", count = resultCount)
                        Spacer(modifier = Modifier.height(10.dp))
                        searchResults.forEachIndexed { i, result ->
                            UserResultCard(
                                result = result,
                                index = i,
                                onAddClick = { sendFriendRequest(it) },
                                onCardClick = { targetId ->
                                    context.startActivity(
                                        Intent(context, ProfileActivity::class.java)
                                            .putExtra("friendId", targetId)
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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
fun UserReceivesRequest() {
    val friendRequests = remember { mutableStateListOf<Pair<FriendRequest, Map<String, String>>>() }
    val userId         = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val showDialog     = remember { mutableStateOf(false) }
    val message        = remember { mutableStateOf("") }
    val context        = LocalContext.current

    DisposableEffect(userId) {
        if (userId.isEmpty()) return@DisposableEffect onDispose {}

        val ref = FirebaseDatabase.getInstance()
            .getReference("users/$userId/received_requests")
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    friendRequests.clear()
                    return
                }
                val tasks = snapshot.children.map { requestSnapshot ->
                    val key = requestSnapshot.key ?: ""
                    val from   = requestSnapshot.child("from").getValue(String::class.java) ?: ""
                    val to     = requestSnapshot.child("to").getValue(String::class.java) ?: ""
                    val status = requestSnapshot.child("status").getValue(String::class.java) ?: "pending"
                    val req    = FriendRequest(key, from, to, status)

                    usersRef.child(from).get().continueWith { task ->
                        val snap = task.result
                        val details = if (snap.exists()) mapOf(
                            "friendId"        to from,
                            "username"        to (snap.child("username").getValue(String::class.java) ?: "Unknown"),
                            "profileImageUri" to (snap.child("profileImageUri").getValue(String::class.java) ?: "")
                        ) else mapOf("friendId" to from, "username" to "Unknown", "profileImageUri" to "")
                        Pair(req, details)
                    }
                }
                Tasks.whenAllSuccess<Pair<FriendRequest, Map<String, String>>>(tasks)
                    .addOnSuccessListener {
                        friendRequests.clear()
                        friendRequests.addAll(it)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserReceivesRequest", "Listener cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 100.dp) // bottom padding for nav bar
    ) {
        // Section header with live count badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECEIVED REQUESTS",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.08.sp
                )
            )
            if (friendRequests.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = RedLight,
                    border = BorderStroke(1.dp, RedBorder)
                ) {
                    Text(
                        text = "${friendRequests.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedColor
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (friendRequests.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "All caught up!",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No pending friend requests",
                    style = TextStyle(fontSize = 13.sp, color = TextSecondary)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                friendRequests.forEachIndexed { index, (request, userDetails) ->
                    val reqUsername    = userDetails["username"] ?: "Unknown User"
                    val profileImageUri = userDetails["profileImageUri"]
                    val friendId       = userDetails["friendId"] ?: ""

                    ReceivedRequestCard(
                        username = reqUsername,
                        profileImageUri = profileImageUri,
                        friendId = friendId,
                        index = index,
                        onAccept = {
                            handleFriendRequest(request, true, showDialog, message, reqUsername)
                        },
                        onDecline = {
                            handleFriendRequest(request, false, showDialog, message, reqUsername)
                        },
                        onCardClick = {
                            context.startActivity(
                                Intent(context, ProfileActivity::class.java)
                                    .putExtra("friendId", friendId)
                            )
                        }
                    )
                }
            }
        }
    }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = {
                Text("You're connected! 🎉", style = TextStyle(fontWeight = FontWeight.Bold, color = TextPrimary))
            },
            text = { Text(message.value, style = TextStyle(color = TextSecondary)) },
            confirmButton = {
                Button(
                    onClick = { showDialog.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Nice!")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ─────────────────────────────────────────────
//  Individual Request Card
// ─────────────────────────────────────────────
@Composable
private fun ReceivedRequestCard(
    username: String,
    profileImageUri: String?,
    friendId: String,
    index: Int,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCardClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceColor,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCardClick() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (!profileImageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                PlaceholderAvatar(
                    text = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    size = 50,
                    roundedSquare = false,
                    index = index
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Wants to connect with you",
                    style = TextStyle(fontSize = 12.sp, color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlueBrush)
                        .clickable { onAccept() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Accept",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }

                // Decline — red icon-only button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RedLight,
                    border = BorderStroke(1.dp, RedBorder),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onDecline() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Decline",
                            modifier = Modifier.size(16.dp),
                            tint = RedColor
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Duplicate request dialog (unchanged logic)
// ─────────────────────────────────────────────
@Composable
fun ShowDuplicateRequestDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Already Sent", style = TextStyle(fontWeight = FontWeight.Bold)) },
        text = { Text("A pending friend request already exists.", style = TextStyle(color = TextSecondary)) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Got it") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ─────────────────────────────────────────────
//  Data class & helper functions (unchanged logic)
// ─────────────────────────────────────────────
data class FriendRequest(
    val key: String = "",
    val from: String = "",
    val to: String = "",
    val status: String = "pending"
)

fun loadReceivedRequestsWithDetails(
    userId: String,
    onRequestsLoaded: (List<Pair<FriendRequest, Map<String, String>>>) -> Unit
) {
    val database      = FirebaseDatabase.getInstance().getReference("users").child(userId).child("received_requests")
    val usersDatabase = FirebaseDatabase.getInstance().getReference("users")
    database.get().addOnSuccessListener { snapshot ->
        if (!snapshot.exists()) { onRequestsLoaded(emptyList()); return@addOnSuccessListener }
        val tasks = snapshot.children.map { requestSnapshot ->
            val key    = requestSnapshot.key ?: ""
            val from   = requestSnapshot.child("from").getValue(String::class.java) ?: ""
            val to     = requestSnapshot.child("to").getValue(String::class.java) ?: ""
            val status = requestSnapshot.child("status").getValue(String::class.java) ?: "pending"
            val req    = FriendRequest(key, from, to, status)
            usersDatabase.child(from).get().continueWith { task ->
                val userSnapshot = task.result
                val details = if (userSnapshot.exists()) mapOf(
                    "friendId" to from,
                    "username" to (userSnapshot.child("username").getValue(String::class.java) ?: "Unknown User"),
                    "profileImageUri" to (userSnapshot.child("profileImageUri").getValue(String::class.java) ?: "")
                ) else mapOf("username" to "Unknown User", "profileImageUri" to "")
                Pair(req, details)
            }
        }
        Tasks.whenAllSuccess<Pair<FriendRequest, Map<String, String>>>(tasks)
            .addOnSuccessListener { onRequestsLoaded(it) }
    }.addOnFailureListener { onRequestsLoaded(emptyList()) }
}

private fun handleFriendRequest(
    request: FriendRequest,
    isAccepted: Boolean,
    showDialog: MutableState<Boolean>,
    message: MutableState<String>,
    username: String,
//    onComplete: () -> Unit = {}
) {
    val db     = Firebase.database.reference
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val updates = mutableMapOf<String, Any?>()

    // Delete the request from both sides using the known key
    updates["users/$userId/received_requests/${request.key}"] = null
    updates["users/${request.from}/sent_requests/${request.key}"] = null

    if (isAccepted) {
        val rKey = db.child("users/$userId/friends").push().key
        val sKey = db.child("users/${request.from}/friends").push().key
        if (rKey != null && sKey != null) {
            val ts = System.currentTimeMillis()
            updates["users/$userId/friends/$rKey"] = mapOf(
                "friendId" to request.from,
                "timestamp" to ts
            )
            updates["users/${request.from}/friends/$sKey"] = mapOf(
                "friendId" to userId,
                "timestamp" to ts
            )
        }
    }

    db.updateChildren(updates)
        .addOnSuccessListener {
            Log.d("FriendRequest", "Success — accepted: $isAccepted")
            if (isAccepted) {
                showDialog.value = true
                message.value = "You are now friends with $username 🎉"
            }
        }
        .addOnFailureListener { e ->
            Log.e("FriendRequest", "DB update failed: ${e.message}")
        }
}