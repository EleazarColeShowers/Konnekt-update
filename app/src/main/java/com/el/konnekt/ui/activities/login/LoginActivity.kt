package com.el.konnekt.ui.activities.login

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.el.konnekt.data.local.AppDatabase
import com.el.konnekt.data.local.UserEntity
import com.el.konnekt.data.storage.SecureStorage
import com.el.konnekt.ui.activities.mainpage.MessageActivity
import com.el.konnekt.ui.activities.signup.CustomCheckbox
import com.el.konnekt.ui.activities.signup.SignUpActivity
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class LoginActivity : ComponentActivity() {
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
                        LoginPage(onBackPressed = { onBackPressed() })
                    }
                }
            }
        }
    }
}

@Composable
fun LoginPage(onBackPressed: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        ReturnHome(onBackPressed = onBackPressed)
        LoginForm()
    }
}

@Composable
fun ReturnHome(onBackPressed: () -> Unit) {
    val returnArrow = painterResource(id = R.drawable.returnarrow)
    Spacer(modifier = Modifier.height(16.dp))
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
}

@Composable
fun LoginForm() {
    val context = LocalContext.current as ComponentActivity
    val auth = FirebaseAuth.getInstance()
    var isChecked by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val emailIcon = painterResource(id = R.drawable.emailicon)
    var passwordVisible by remember { mutableStateOf(false) }
    val passwordIcon = if (passwordVisible) painterResource(id = R.drawable.passwordseen)
    else painterResource(id = R.drawable.passwordinvisible)

    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        Toast.makeText(context, "You have successfully logged in", Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(context, MessageActivity::class.java))
                    } else {
                        Toast.makeText(context, "Firebase Auth Failed: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: ApiException) {
            Toast.makeText(context, "Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome Back",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "We're glad to have you back with us",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Email field
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
                    onValueChange = { email = it },
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

        // Password field
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
                        if (password.isEmpty()) Text("••••••••", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f), fontSize = 15.sp)
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

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            CustomCheckbox(checked = isChecked) { checked -> isChecked = checked }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Save Password",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Login button
        LoginBtn(
            isChecked = isChecked,
            onClick = {
                if (isChecked) SecureStorage.saveUserCredentials(context, email, password)
                else Log.e(TAG, "did not save credentials")
                performLogin(auth, context, email, password, onSuccess = { username, profileImageUri ->
                    val intent = Intent(context, MessageActivity::class.java).apply {
                        putExtra("username", username)
                        putExtra("profileUri", profileImageUri)
                    }
                    context.startActivity(intent)
                })
            },
            onUnchecked = {}
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
            Text(
                text = "  or  ",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        GoogleSignInButton { googleSignInLauncher.launch(googleSignInClient.signInIntent) }

        Spacer(modifier = Modifier.height(20.dp))

        SignUpText()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Shared field components
@Composable
fun AuthFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    )
}

@Composable
fun AuthFieldBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        content()
    }
}

@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .height(52.dp)
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.googleicon),
                contentDescription = "Google Sign-In",
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Continue with Google",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LoginBtn(isChecked: Boolean, onClick: () -> Unit, onUnchecked: () -> Unit) {
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
                text = "Log In",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun SignUpText() {
    val context = LocalContext.current
    val signUpText = "Sign Up"
    val text = buildAnnotatedString {
        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)))
        append("Don't have an account? ")
        pushStyle(SpanStyle(color = Color(0xFF2F9ECE), fontWeight = FontWeight.SemiBold))
        append(signUpText)
        pop(); pop()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        ClickableText(
            text = text,
            onClick = {
                val start = text.indexOf(signUpText)
                if (it in start..(start + signUpText.length))
                    context.startActivity(Intent(context, SignUpActivity::class.java))
            }
        )
    }
}

// Keep all original functions below unchanged
fun performLogin(auth: FirebaseAuth, context: ComponentActivity, email: String, password: String, onSuccess: (String, Uri?) -> Unit) {
    if (email.isEmpty() || password.isEmpty()) {
        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
        return
    }
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener(context) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    val userId = user.uid
                    CryptoUtil.generateRsaKeyPairIfNeeded(userId)
                    val publicKeyBase64 = CryptoUtil.getPublicKeyBase64(userId)
                    Firebase.database.reference.child("users").child(userId).child("publicKey").setValue(publicKeyBase64)
                    fetchUserProfile(context, userId, onSuccess)
                }
                Toast.makeText(context, "Successfully logged in", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Error Occurred ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
}

fun fetchUserProfile(context: Context, userId: String, onSuccess: (String, Uri?) -> Unit) {
    val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
    userRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val snapshot = task.result
            if (snapshot.exists()) {
                val username = snapshot.child("username").getValue(String::class.java) ?: ""
                val email = snapshot.child("email").getValue(String::class.java) ?: ""
                val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                val profileImageUriString = snapshot.child("profileImageUri").getValue(String::class.java)
                val profileImageUri = profileImageUriString?.let { Uri.parse(it) }
                val db = AppDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    db.userDao().insertUser(UserEntity(userId, username, email, bio, profileImageUri?.toString() ?: ""))
                }
                onSuccess(username, profileImageUri)
            }
        }
    }
}