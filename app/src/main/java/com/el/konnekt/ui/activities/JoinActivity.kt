package com.el.konnekt.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.el.konnekt.R
import com.el.konnekt.ui.activities.login.LoginActivity
import com.el.konnekt.ui.activities.mainpage.MessageActivity
import com.el.konnekt.ui.activities.signup.SignUpActivity
import com.el.konnekt.ui.theme.InstaChatComposeTheme
import com.google.firebase.auth.FirebaseAuth

class JoinActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MessageActivity::class.java))
            finish()
            return
        }
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier= Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())

                    ) {
                            JoinPage()
                    }
                }
            }
        }
    }
}

@Composable
fun JoinPage() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Lottie with a soft tinted background circle behind it
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(300.dp)
                .clip(CircleShape)
                .background(Color(0xFF2F9ECE).copy(alpha = 0.07f))
        ) {
            AnimatedConnection(
                modifier = Modifier.size(260.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Tag line chip
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = Color(0xFF2F9ECE).copy(alpha = 0.12f),
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                text = "✦ Connect with the world",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2F9ECE),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Get Connected,\nStay Connected.",
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 44.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Chat with friends, family, and people\nacross the globe — all in one place.",
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Primary button
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2F9ECE),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clickable {
                    context.startActivity(Intent(context, SignUpActivity::class.java))
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Create an Account",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
            Text(
                text = "Log in",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2F9ECE),
                modifier = Modifier.clickable {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }
            )
        }
    }
}

@Composable
fun AnimatedConnection(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.connected)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )

    LottieAnimation(
        composition = composition,
        progress = progress,
        modifier = modifier
    )
}