package com.el.konnekt.ui.activities.message

import android.app.Application
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.el.konnekt.R
import com.el.konnekt.data.ChatViewModel
import com.el.konnekt.data.ChatViewModelFactory
import com.el.konnekt.data.local.AppDatabase
import com.el.konnekt.data.local.LocalDataSource
import com.el.konnekt.data.models.Message
import com.el.konnekt.data.remote.FirebaseDataSource
import com.el.konnekt.data.repository.ChatRepository
import com.el.konnekt.ui.activities.mainpage.ProfileActivity
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.el.konnekt.utils.MessageObfuscator
import com.el.konnekt.utils.formatTimestamp
import com.el.konnekt.utils.generateColorFromId
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlin.text.firstOrNull
import kotlin.text.removePrefix
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ArrowBack
import com.el.konnekt.ui.activities.mainpage.MessageActivity


class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val friendId = intent.getStringExtra("friendId") ?: ""
        val username = intent.getStringExtra("username") ?: "Unknown"
        val profileImageUri = intent.getStringExtra("profileImageUri") ?: ""
        val chatId = intent.getStringExtra("chatId") ?: ""
        val currentUserId = intent.getStringExtra("currentUserId") ?: ""
        val groupId = intent.getStringExtra("groupId") ?: ""
        val groupName = intent.getStringExtra("groupName") ?: "Group"
        val groupImageUri = intent.getStringExtra("groupImageUri") ?: ""
        val isGroupChat = intent.getBooleanExtra("isGroupChat", false)


        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val app = applicationContext as Application
                    val viewModel: ChatViewModel = viewModel(
                        factory = ChatViewModelFactory(
                            app,
                            ChatRepository(
                                FirebaseDataSource(),
                                LocalDataSource(AppDatabase.getDatabase(app))
                            )
                        )
                    )

                    ChatScreen(
                        viewModel = viewModel,
                        friendId = friendId,
                        username = username ?: "Unknown",
                        profileImageUri = profileImageUri,
                        chatId = chatId ?: "",
                        currentUserId = currentUserId ?: "",
                        isGroupChat = isGroupChat,
                        groupId = groupId,
                        groupName = groupName,
                        groupImageUri = groupImageUri,
                        onBackPressed = {
                            val intent= Intent(this, MessageActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    friendId: String,
    username: String,
    profileImageUri: String,
    chatId: String,
    currentUserId: String,
    isGroupChat: Boolean,
    groupId: String,
    groupName: String,
    groupImageUri: String,
    onBackPressed: () -> Unit
) {

    BackHandler {
        onBackPressed()
    }


    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    var currentUsername by remember { mutableStateOf("") }
    var mentionQuery by remember { mutableStateOf("") }
    var isChatOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }
    var showMentionDropdown by remember { mutableStateOf(false) }
    var cursorPosition by remember { mutableIntStateOf(0) }

    val receiverUserId = if (isGroupChat) "" else friendId

    val db = Firebase.database.reference
    val encryptionChatId = if (isGroupChat) {
        chatId.removePrefix("group_")
    } else {
        chatId
    }
    val firebaseChatId = if (isGroupChat) chatId.removePrefix("group_") else chatId

    val firebasePath = if (isGroupChat) {
        chatId
    } else {
        chatId
    }
    val typingRef = db.child("chats").child(firebasePath).child("typing")

    val encryptionKey = remember(chatId, isGroupChat) {
        if (isGroupChat) {
            chatId.removePrefix("group_")
        } else {
            chatId
        }
    }

    val messages by viewModel.messages.collectAsState()
    val isFriendTyping by viewModel.isFriendTyping.collectAsState()
    val groupMembers by viewModel.groupMembers.collectAsState()

    val filteredMembers = groupMembers.filter {
        it.startsWith(mentionQuery, ignoreCase = true)
    }


    LaunchedEffect(currentUserId) {
        viewModel.fetchCurrentUserName(currentUserId) { name ->
            currentUsername = name ?: "Unknown"
        }
    }

    LaunchedEffect(chatId) {
        viewModel.observeMessages(chatId, currentUserId)

        if (isGroupChat) {
            val groupId = chatId.removePrefix("group_")
            viewModel.fetchGroupMembers(currentUserId, groupId)
        } else {
            viewModel.observeTyping(chatId, receiverUserId)
        }
    }

    LaunchedEffect(chatId, isChatOpen) {
        if (!isChatOpen) return@LaunchedEffect
        kotlinx.coroutines.delay(300)
        viewModel.markMessagesAsSeen(chatId, currentUserId, isGroupChat)
    }

    DisposableEffect(Unit) {
        isChatOpen = true
        onDispose {
            isChatOpen = false
            viewModel.stopObservingMessages(chatId)
            if (!isGroupChat) {
                typingRef.child(currentUserId).setValue(false)
            }
        }
    }

    fun setReplyingTo(message: Message) {
        replyingTo = message
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackPressed,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
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
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            if (profileImageUri.isNotEmpty()) {
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
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            currentUserId = currentUserId,
                            onReply = { setReplyingTo(it) },
                            isGroupChat = isGroupChat,
                            onEdit = { messageToEdit ->
                                messageText = messageToEdit.getDisplayText()
                                editingMessageId = messageToEdit.id
                            },
                            chatId = chatId,
                            encryptionKey = encryptionKey,
                            onDeleteForSelf = { msg ->
                                viewModel.deleteMessageForSelf(chatId, msg.id, currentUserId)
                            },
                            onDeleteForEveryone = { msg ->
                                viewModel.deleteMessageForEveryone(chatId, msg.id)
                            },
                            messages = messages
                        )
                    }
                }
            }

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
                                            text = message.getDisplayText(),
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
                                    if (!isGroupChat) {
                                        typingRef.child(currentUserId).setValue(it.isNotEmpty())
                                    }
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

                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = if (messageText.isNotBlank()) Color(0xFF2F9ECE) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        if (editingMessageId != null) {
                                            viewModel.editMessage(chatId, editingMessageId!!, messageText)
                                            editingMessageId = null
                                            messageText = ""
                                        } else {
                                            viewModel.sendMessage(
                                                chatId = firebasePath,
                                                senderId = currentUserId,
                                                senderName = currentUsername,
                                                receiverId = receiverUserId,
                                                text = messageText,
                                                replyToId = replyingTo?.id
                                            )
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
    chatId: String,
    encryptionKey: String,
    onDeleteForSelf: (Message) -> Unit,
    onDeleteForEveryone: (Message) -> Unit,
    isGroupChat: Boolean = false
) {
    val senderColor = remember(message.senderId) {
        generateColorFromId(message.senderId)
    }



    val displayText = remember(message.text, encryptionKey) {
        MessageObfuscator.deobfuscate(message.text, encryptionKey)
    }



    val isSentByUser = message.senderId == currentUserId
    val backgroundColor = if (isSentByUser) Color(0xFF2F9ECE) else if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFE8E8E8)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = if (isSentByUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
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
                            onReply(message.copy(text = displayText))
                            hasReplied = true
                        }
                    }
                }
                .combinedClickable(
                    onLongClick = { showMenu = true },
                    onClick = {}
                ),
            shape = RoundedCornerShape(
                topStart = if (isSentByUser) 18.dp else 4.dp,
                topEnd = if (isSentByUser) 4.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            color = backgroundColor,
            shadowElevation = 0.5.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (isGroupChat && !isSentByUser) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = senderColor,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                message.replyTo?.let { replyId ->
                    val repliedMessage = messages.find { it.id == replyId }
                    val repliedText = repliedMessage?.let {
                        MessageObfuscator.deobfuscate(it.text, chatId)
                    } ?: "[message unavailable]"

                    Surface(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSentByUser)
                            Color.White.copy(alpha = 0.15f)
                        else
                            Color.Black.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(36.dp)
                                    .background(
                                        if (isSentByUser) Color.White.copy(alpha = 0.7f)
                                        else senderColor,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = repliedMessage?.senderName ?: "Unknown",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSentByUser)
                                        Color.White.copy(alpha = 0.9f)
                                    else
                                        senderColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = repliedText,
                                    fontSize = 12.sp,
                                    color = textColor.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                val annotated = buildAnnotatedString {
                    val regex = "@\\w+".toRegex()
                    val rawText = displayText
                    var currentIndex = 0
                    regex.findAll(rawText).forEach { match ->
                        append(rawText.substring(currentIndex, match.range.first))
                        withStyle(
                            SpanStyle(
                                color = if (isSentByUser) Color.White else senderColor,
                                fontWeight = FontWeight.SemiBold,
                                background = if (isSentByUser)
                                    Color.White.copy(alpha = 0.15f)
                                else
                                    senderColor.copy(alpha = 0.12f)
                            )
                        ) {
                            append(" ${match.value} ")
                        }
                        currentIndex = match.range.last + 1
                    }
                    append(rawText.substring(currentIndex))
                }

                Column {
                    Text(
                        text = annotated,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (message.edited) {
                            Text(
                                text = "edited • ",
                                fontSize = 9.sp,
                                color = textColor.copy(alpha = 0.45f),
                                fontStyle = FontStyle.Italic
                            )
                        }

                        Text(
                            text = formatTimestamp(message.timestamp),
                            fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.45f)
                        )

                        if (isSentByUser) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                modifier = Modifier.size(12.dp),
                                tint = if (message.seen)
                                    Color(0xFF53BDEB)
                                else
                                    textColor.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .widthIn(min = 180.dp, max = 220.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = Color(0xFF2F9ECE).copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF2F9ECE)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Reply",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                onClick = {
                    onReply(message.copy(text = displayText))
                    showMenu = false
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )

            if (isSentByUser && isEditable) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = Color(0xFF2F9ECE).copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF2F9ECE)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Edit",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    onClick = {
                        onEdit(message.copy(text = displayText))
                        showMenu = false
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Delete for Me",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                onClick = {
                    onDeleteForSelf(message)
                    showMenu = false
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )

            if (isSentByUser && isDeletableForEveryone) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Delete for Everyone",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    onClick = {
                        onDeleteForEveryone(message)
                        showMenu = false
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

