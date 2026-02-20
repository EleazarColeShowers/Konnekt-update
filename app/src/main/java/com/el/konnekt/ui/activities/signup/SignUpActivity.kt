package com.el.konnekt.ui.activities.signup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.el.konnekt.R
import com.el.konnekt.data.crypto.CryptoUtil
import com.el.konnekt.ui.activities.login.AuthFieldBox
import com.el.konnekt.ui.activities.login.AuthFieldLabel
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@Suppress("DEPRECATION")
class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    ) {
                        SignUpPage(onBackPressed = { onBackPressed() })
                    }
                }
            }
        }
    }
}

@Composable
fun SignUpPage(onBackPressed: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SignUpProgress(progress = 0.5f, onBackPressed = onBackPressed)
        Form(username = username) { username = it }
    }
}

@Composable
fun SignUpProgress(progress: Float, onBackPressed: () -> Unit) {
    val returnArrow = painterResource(id = R.drawable.returnarrow)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onBackPressed() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = returnArrow,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF2F9ECE), Color(0xFF4DB8E8))
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Step 1 of 2",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun Form(username: String, onUsernameChanged: (String) -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var isChecked by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val emailIcon = painterResource(id = R.drawable.emailicon)
    var passwordVisible by remember { mutableStateOf(false) }
    val passwordIcon = if (passwordVisible) painterResource(id = R.drawable.passwordseen)
    else painterResource(id = R.drawable.passwordinvisible)

    Column {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Create an Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Let's help you get set up",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Username
        AuthFieldLabel("Username")
        Spacer(modifier = Modifier.height(8.dp))
        AuthFieldBox {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = username,
                    onValueChange = { username = it },
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (username.isEmpty()) Text(
                            "Choose a username",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            fontSize = 15.sp
                        )
                        inner()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Email
        AuthFieldLabel("Email")
        Spacer(modifier = Modifier.height(8.dp))
        AuthFieldBox {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (email.isEmpty()) Text("you@example.com", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f), fontSize = 15.sp)
                        inner()
                    }
                )
                Image(
                    painter = emailIcon,
                    contentDescription = "Email",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        AuthFieldLabel("Password")
        Spacer(modifier = Modifier.height(8.dp))
        AuthFieldBox {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = password,
                    onValueChange = { password = it },
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp
                    ),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (password.isEmpty()) Text("Create a password", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f), fontSize = 15.sp)
                        inner()
                    }
                )
                Image(
                    painter = passwordIcon,
                    contentDescription = "Toggle Password",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { passwordVisible = !passwordVisible },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            CustomCheckbox(checked = isChecked) { checked -> isChecked = checked }
            Spacer(modifier = Modifier.width(8.dp))
            TermsAndConditionsText()
        }

        Spacer(modifier = Modifier.height(32.dp))

        ContinueBtn(
            isChecked = isChecked,
            onClick = {
                performSignUp(auth, context as ComponentActivity, email, password, username, onSuccess = {
                    val intent = Intent(context, ProfileSetUp::class.java).apply {
                        putExtra("username", username)
                    }
                    context.startActivity(intent)
                })
            },
            onUnchecked = {}
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TermsAndConditionsText() {
    val context = LocalContext.current
    val termsAndConditions = "Terms & Conditions"
    val text = buildAnnotatedString {
        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)))
        append("I agree to the ")
        pushStyle(SpanStyle(color = Color(0xFF2F9ECE), fontWeight = FontWeight.SemiBold))
        append(termsAndConditions)
        pop()
        append(" of this App")
        pop()
    }
    ClickableText(
        text = text,
        onClick = {
            val start = text.indexOf(termsAndConditions)
            if (it in start..(start + termsAndConditions.length))
                context.startActivity(Intent(context, TermsAndConditions::class.java))
        }
    )
}

@Composable
fun CustomCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (checked) Color(0xFF2F9ECE) else Color.Transparent)
            .border(1.5.dp, Color(0xFF2F9ECE), RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun ContinueBtn(isChecked: Boolean, onClick: () -> Unit, onUnchecked: () -> Unit) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF2F9ECE),
        shadowElevation = 4.dp,
        modifier = Modifier
            .height(52.dp)
            .fillMaxWidth()
            .clickable {
                if (isChecked) onClick()
                else {
                    onUnchecked()
                    Toast.makeText(context, "Tick the checkbox to proceed", Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Continue",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// All original functions unchanged
fun performSignUp(auth: FirebaseAuth, context: ComponentActivity, email: String, password: String, usernameTxt: String, onSuccess: () -> Unit) {
    if (email.isEmpty() || password.isEmpty() || usernameTxt.isEmpty()) {
        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
        return
    }
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(context) { task ->
        if (task.isSuccessful) {
            createUser(username = usernameTxt, email = email)
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener
            CryptoUtil.generateRsaKeyPairIfNeeded(userId)
            val publicKeyBase64 = CryptoUtil.getPublicKeyBase64(userId)
            Firebase.database.reference.child("users").child(userId).child("publicKey").setValue(publicKeyBase64)
            context.startActivity(Intent(context, ProfileSetUp::class.java))
            Toast.makeText(context, "Successfully signed up", Toast.LENGTH_SHORT).show()
            onSuccess()
        } else {
            Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
        }
    }.addOnFailureListener {
        Toast.makeText(context, "Error Occurred ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun createUser(username: String, email: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val database: DatabaseReference = Firebase.database.reference
    database.child("users").child(currentUser?.uid.toString())
        .setValue(mapOf("username" to username, "email" to email))
        .addOnSuccessListener { Log.d("###", "Data saved") }
        .addOnFailureListener { Log.d("###", "Data failed ${it.message}") }
}