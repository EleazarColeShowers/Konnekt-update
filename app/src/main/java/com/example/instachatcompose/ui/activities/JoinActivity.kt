package com.example.instachatcompose.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.activities.login.LoginActivity
import com.example.instachatcompose.ui.activities.login.LoginPage
import com.example.instachatcompose.ui.activities.mainpage.MessageActivity
import com.example.instachatcompose.ui.activities.signup.SignUpActivity
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth

class JoinActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier= Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())

                    ) {
//                        if (checkIfUserIsLoggedIn()) {
//                            // User is already logged in, navigate to MessageActivity
//                            val intent = Intent(this@JoinActivity, MessageActivity::class.java)
//                            startActivity(intent)
//                            finish() // Finish LoginActivity to prevent going back to it on back press
//                        } else {
                            JoinPage()
//                        }
                    }
                }
            }
        }
    }

    private fun checkIfUserIsLoggedIn(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        return currentUser != null
    }
}

@Composable
fun JoinPage(){
    val context = LocalContext.current
    Column {
        AnimatedConnection()
        ConnectionWriteUp()
        RoundedClickableColumn(onClick ={
            val intent = Intent(context, SignUpActivity::class.java)
            context.startActivity(intent)
        })
        Spacer(modifier = Modifier.height(10.dp))
        ExistingAccount()
    }
}

@Composable
fun AnimatedConnection(modifier: Modifier = Modifier) {
    val preloaderLottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            R.raw.connected
        )
    )

    val preloaderProgress by animateLottieCompositionAsState(
        composition = preloaderLottieComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )

    Column(
        modifier= Modifier.padding(horizontal = 10.dp)
    ) {
        LottieAnimation(
            composition = preloaderLottieComposition,
            progress = preloaderProgress,
            modifier = modifier
                .width(400.dp)
                .height(400.dp)
        )
    }
}

@Composable
fun ConnectionWriteUp(){
    Column(modifier= Modifier
        .height(240.dp)
        .fillMaxWidth()
        .padding(horizontal = 10.dp)) {
        Text(
            text = "Get Connected And Stay Connected",
            fontWeight = FontWeight.Bold,
            fontSize = 35.sp,
            lineHeight = 50.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text ="Connect and network with friends, families and strangers all over the globe with voice calls, video calls and chats on Konnekt",
            fontWeight = FontWeight.Light,
            fontSize = 18.sp,
            lineHeight = 25.sp
        )
    }

}

@Composable
fun RoundedClickableColumn(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(25.dp), // Adjust the corner radius as needed
        color = Color(0xFF2F9ECE), // Change the background color as needed
        modifier = Modifier
            .height(54.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            Alignment.CenterHorizontally
        ) {
            Text(
                text = "Join Now",
                color = Color(0xFFFFFFFF), // Change the text color as needed
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
            )

        }
    }
}

@Composable
fun ExistingAccount() {
    val context = LocalContext.current
    val loginText = "Login"
    val text = buildAnnotatedString {
        append("Already have an account? ")
        pushStyle(
            style = SpanStyle(
                color = Color(android.graphics.Color.parseColor("#2F9ECE")), // Change color here
                textDecoration = TextDecoration.Underline
            )
        )
        Spacer(modifier = Modifier.width(2.dp))
        append(loginText)
    }

    ClickableText(text = text, onClick = {
        val startIndex = text.indexOf(loginText)
        val endIndex = startIndex + loginText.length
        if (it in startIndex..endIndex) {
            // Handle click action here if needed
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
            // For example, navigate to terms and conditions screen
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 80.dp)
    )
}
