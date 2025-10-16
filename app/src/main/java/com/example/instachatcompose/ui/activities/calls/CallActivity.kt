package com.example.instachatcompose.ui.activities.calls

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme


class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier= Modifier.fillMaxSize()) {
                        CallScreen()
                    }
                }
            }
        }

    }
}
@Composable
fun CallScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.coming_soon),
            contentDescription = "Coming Soon",
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "Call feature is coming soon!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2F9ECE)
        )
    }
}

