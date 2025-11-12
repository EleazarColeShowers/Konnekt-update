package com.example.instachatcompose.ui.activities.signup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.activities.data.crypto.CryptoUtil
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

@Suppress("DEPRECATION")
class SignUpActivity: ComponentActivity() {


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
                        SignUpPage(onBackPressed = {
                            onBackPressed()
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun SignUpPage(onBackPressed: () -> Unit){
    var username by rememberSaveable { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0.5f) }
//    var bio: String = ""
//    var selectedImageUri: Uri? = null
    Column (
        modifier= Modifier.padding(horizontal = 15.dp)
            ){
        SignUpProgress(progress= progress,onBackPressed ={
             onBackPressed()
        } )
        Form(username = username){
            username = it
        }

    }
}

@Composable
fun SignUpProgress(progress: Float, onBackPressed: () -> Unit) {
    val returnArrow = painterResource(id = R.drawable.returnarrow)

    Spacer(modifier = Modifier.height(15.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
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

        Spacer(modifier = Modifier.width(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(13.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = Color(0xFF2F9ECE),
            trackColor = Color.LightGray,
        )
    }
}


@Composable
fun Form(username: String, onUsernameChanged: (String) -> Unit){
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var isChecked by remember { mutableStateOf(false) }

    var username by remember {
        mutableStateOf("")
    }
    var email by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    val emailIcon= painterResource(id = R.drawable.emailicon)
    var passwordVisible by remember { mutableStateOf(false) }

    val passwordIcon = if (passwordVisible) {
        painterResource(id = R.drawable.passwordseen)
    } else {
        painterResource(id = R.drawable.passwordinvisible)
    }


    Column{

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Create An Account",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight(500),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Let's help you create an account",
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
                text = "Username",
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
            ) {
                BasicTextField(
                    value = username,
                    onValueChange = { username = it },
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp), // Padding for spacing
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = email,
                        onValueChange = { email = it },
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground),
                        singleLine = true,
                        modifier = Modifier.weight(1f) // Takes up available space
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
                            .clickable { passwordVisible = !passwordVisible },
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                    )
                }
            }
        }
       Spacer(modifier = Modifier.height(16.5.dp))
        Row {
            CustomCheckbox(checked = isChecked) { checked ->
                isChecked = checked
            }
            Spacer(modifier = Modifier.width(6.dp))
            TermsAndConditionsText()
        }
        Spacer(modifier = Modifier.height(44.5.dp))
        ContinueBtn(
            isChecked = isChecked,
            onClick = {
                performSignUp(auth, context as ComponentActivity, email, password, username, onSuccess = {
                    val intent = Intent(context, ProfileSetUp::class.java)
                    intent.putExtra("username", username)
                    context.startActivity(intent)
                })

            },
            onUnchecked = { /* Handle unchecked state */ }
        )

    }
}
@Composable
fun TermsAndConditionsText() {
    val context = LocalContext.current
    val termsAndConditions = "Terms & Conditions"

    val text = buildAnnotatedString {
        val defaultTextStyle = SpanStyle(color = MaterialTheme.colorScheme.onBackground)

        pushStyle(defaultTextStyle)
        append("I agree to the ")
        pushStyle(
            SpanStyle(
                color = Color(0xFF2F9ECE),
                textDecoration = TextDecoration.None
            )
        )
        append(termsAndConditions)
        pop()

        append(" of this App")
        pop()
    }

    ClickableText(
        text = text,
        onClick = {
            val startIndex = text.indexOf(termsAndConditions)
            val endIndex = startIndex + termsAndConditions.length
            if (it in startIndex..endIndex) {
                val intent = Intent(context, TermsAndConditions::class.java)
                context.startActivity(intent)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 80.dp)
    )
}

@Composable
fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked)  }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 1.dp,
                    color = Color(android.graphics.Color.parseColor("#2F9ECE")), // Change color here
                    shape = RoundedCornerShape(4.dp)
                )
                .background(
                    color = if (checked) Color(android.graphics.Color.parseColor("#2F9ECE")) else Color.White, // Change color on check
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Checked",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ContinueBtn(
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
                    Toast.makeText(context, "Tick the checkbox to proceed", Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            Alignment.CenterHorizontally
        ) {
            Text(
                text = "Continue",
                color = Color(0xFFFFFFFF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

fun performSignUp(auth: FirebaseAuth, context: ComponentActivity, email: String, password: String, usernameTxt: String, onSuccess: () -> Unit) {
    if (email.isEmpty() || password.isEmpty() || usernameTxt.isEmpty()) {
        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
        return
    }

    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(context) { task ->
        if (task.isSuccessful) {
            createUser(username = usernameTxt, email = email)

            // 🔐 Generate RSA key pair for this user
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener
            CryptoUtil.generateRsaKeyPairIfNeeded(userId)

            // 📤 Upload public key to Firebase
            val publicKeyBase64 = CryptoUtil.getPublicKeyBase64(userId)
            Firebase.database.reference.child("users").child(userId).child("publicKey").setValue(publicKeyBase64)

            // Continue to profile setup
            val intent = Intent(context, ProfileSetUp::class.java)
            context.startActivity(intent)

            Toast.makeText(context, "Successfully signed up", Toast.LENGTH_SHORT).show()
            onSuccess()
        }
        else {
                Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Error Occurred ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
}

fun createUser(username: String, email: String) {
    val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser
    val database: DatabaseReference = Firebase.database.reference

    val userData = mapOf(
        "username" to username,
        "email" to email
    )

    database.child("users").child(currentUser?.uid.toString()).setValue(userData)
        .addOnSuccessListener {
            Log.d("###", "Data saved")
        }
        .addOnFailureListener {
            Log.d("###", "Data failed ${it.message}")
        }
}
