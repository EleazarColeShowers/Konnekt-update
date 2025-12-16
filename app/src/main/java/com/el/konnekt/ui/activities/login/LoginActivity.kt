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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.el.konnekt.R
import com.el.konnekt.data.crypto.CryptoUtil
import com.el.konnekt.data.local.AppDatabase
import com.el.konnekt.data.storage.SecureStorage
import com.el.konnekt.data.local.UserEntity
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
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier= Modifier.fillMaxSize()) {
                        LoginPage(onBackPressed = {
                            onBackPressed()
                        })
                    }
                }
            }
        }

    }
}

@Composable
fun LoginPage(onBackPressed: () -> Unit){
    Column (
        modifier= Modifier.padding(horizontal = 15.dp)
    ){
        ReturnHome(onBackPressed ={
            onBackPressed()
        } )
        LoginForm()

    }
}

@Composable
fun ReturnHome(onBackPressed: () -> Unit){
    val returnArrow= painterResource(id = R.drawable.returnarrow)
    Spacer(modifier = Modifier.height(15.dp))
    Row(modifier= Modifier
        .fillMaxWidth()
    ) {

        Image(
            painter = returnArrow,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    onBackPressed()
                },
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
        )

    }
}

@Composable
fun LoginForm() {
    val context = LocalContext.current as ComponentActivity
    val auth = FirebaseAuth.getInstance()

    var isChecked by remember { mutableStateOf(false) }
//    val (savedEmail, savedPassword) = SecureStorage.getUserCredentials(context)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val emailIcon = painterResource(id = R.drawable.emailicon)

    var passwordVisible by remember { mutableStateOf(false) }

    val passwordIcon = if (passwordVisible) {
        painterResource(id = R.drawable.passwordseen)
    } else {
        painterResource(id = R.drawable.passwordinvisible)
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))  // Get this from Firebase project settings
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
            val idToken = account.idToken
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        Toast.makeText(context, "You have successfully logged in", Toast.LENGTH_SHORT).show()
                        val intent = Intent(context, MessageActivity::class.java)
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(
                            context,
                            "Firebase Auth Failed: ${authTask.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } catch (e: ApiException) {
            Toast.makeText(context, "Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column {

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Welcome Back",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight(500),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We're glad to have you back with us",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(400),
                color = MaterialTheme.colorScheme.onBackground,
            )
        )

        Spacer(modifier = Modifier.height(48.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Email",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight(400),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            )
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                        shape = RoundedCornerShape(size = 20.dp)
                    )
                    .height(48.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp) // Add some padding around the box
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp), // Add padding to prevent elements from touching edges
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = email,
                        onValueChange = { email = it },
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                    )

                    Image(
                        painter = emailIcon,
                        contentDescription = "Email",
                        modifier = Modifier
                            .size(24.dp)
                            .padding(start = 8.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                    )
                }
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Password",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight(400),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            )

            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                        shape = RoundedCornerShape(size = 20.dp)
                    )
                    .height(48.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = password,
                        onValueChange = { password = it },
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Password
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Image(
                        painter = passwordIcon,
                        contentDescription = "Toggle Password Visibility",
                        modifier = Modifier
                            .size(24.dp)
                            .padding(start = 8.dp)
                            .clickable { passwordVisible = !passwordVisible }, // Toggle visibility
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                    )
                }
            }

        }

        Spacer(modifier = Modifier.height(12.dp))

        Row {
            CustomCheckbox(checked = isChecked) { checked -> isChecked = checked }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Save Password",
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(44.5.dp))

        LoginBtn(
            isChecked = isChecked,
            onClick = {
                if (isChecked) {
                    SecureStorage.saveUserCredentials(context, email, password)
                } else {
                    Log.e(TAG, "did not save credentials")
                }

                performLogin(auth, context, email, password, onSuccess = { username, profileImageUri ->
                    val intent = Intent(context, MessageActivity::class.java)
                    intent.putExtra("username", username)
                    intent.putExtra("profileUri", profileImageUri)
                    context.startActivity(intent)
                })
            },
            onUnchecked = { /* Handle unchecked state */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        GoogleSignInButton {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
        Spacer(modifier = Modifier.height(16.dp))

        SignUpText()
    }
}

@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(25.dp),
        color = Color(0xFF2F9ECE),
        modifier = Modifier
            .height(54.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
            .border(1.dp, Color.Gray, RoundedCornerShape(25.dp)) // Optional border
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), // Padding inside the button
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.googleicon), // Google icon
                contentDescription = "Google Sign-In",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign in with Google",
                color = Color.Black, // Text color
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun performLogin(
    auth: FirebaseAuth,
    context: ComponentActivity,
    email: String,
    password: String,
    onSuccess: (String, Uri?) -> Unit,
) {

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

                    // ✅ Ensure RSA key pair exists locally
                    CryptoUtil.generateRsaKeyPairIfNeeded(userId)

                    // ✅ Upload public key again if needed (e.g. after reinstall)
                    val publicKeyBase64 = CryptoUtil.getPublicKeyBase64(userId)
                    Firebase.database.reference
                        .child("users")
                        .child(userId)
                        .child("publicKey")
                        .setValue(publicKeyBase64)

                    // ✅ Continue to fetch user profile
                    fetchUserProfile(
                        context,
                        userId,
                        onSuccess
                    )
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


fun fetchUserProfile(
    context: Context,
    userId: String,
    onSuccess: (String, Uri?) -> Unit,
//    onFailure: (Exception) -> Unit
) {
    val database = FirebaseDatabase.getInstance().reference
    val userRef = database.child("users").child(userId)


    userRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val snapshot = task.result
            if (snapshot.exists()) {
                val username = snapshot.child("username").getValue(String::class.java) ?: ""
                val email = snapshot.child("email").getValue(String::class.java) ?: ""
                val bio= snapshot.child("bio").getValue(String::class.java) ?: ""  // Fetch email
                val profileImageUriString = snapshot.child("profileImageUri").getValue(String::class.java)
                val profileImageUri = profileImageUriString?.let { Uri.parse(it) }
                val db = AppDatabase.getDatabase(context)
                val userDao = db.userDao()


                CoroutineScope(Dispatchers.IO).launch {
                    userDao.insertUser(UserEntity(
                        userId = userId,
                        username = username,
                        email = email,
                        bio = bio,
                        profileImageUri = profileImageUri?.toString() ?: ""  // Convert Uri to String
                    ))
                }
                onSuccess(username, profileImageUri)  // Callback with data

            } else {
//                onFailure(Exception("User data not found"))  // Handle no data case
            }
        } else {
//            task.exception?.let { onFailure(it) }  // Handle retrieval failure
        }
    }
}

// TODO: 2. write function for user to stay logged in
@Composable
fun LoginBtn(
    isChecked: Boolean,
    onClick: () -> Unit,
    onUnchecked: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(25.dp),
        color = Color(0xFF2F9ECE),
        modifier = Modifier
            .height(54.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                if (isChecked) {
                    onClick()
                } else {
                    onUnchecked()
                    Toast
                        .makeText(context, "Tick the checkbox to proceed", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            Alignment.CenterHorizontally
        ) {
            Text(
                text = "Login",
                color = Color(0xFFFFFFFF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
fun SignUpText() {
    val context = LocalContext.current
    val signUpText = "Sign Up"

    val text = buildAnnotatedString {
        pushStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        append("Don't have an account? ")

        pushStyle(
            SpanStyle(
                color = Color(0xFF2F9ECE),
                textDecoration = TextDecoration.None
            )
        )
        append(signUpText)
        pop()
        pop()
    }

    ClickableText(
        text = text,
        onClick = {
            val startIndex = text.indexOf(signUpText)
            val endIndex = startIndex + signUpText.length
            if (it in startIndex..endIndex) {
                val intent = Intent(context, SignUpActivity::class.java)
                context.startActivity(intent)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 80.dp)
    )
}
