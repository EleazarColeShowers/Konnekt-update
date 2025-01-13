@file:Suppress("DEPRECATION")

package com.example.instachatcompose.ui.activities.signup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme

class TermsAndConditions : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaChatComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier= Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 15.dp)) {
                        TCText()
                        ContinueTCBtn (
                            onBackPressed ={
                                onBackPressed()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TCText(){
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = "Terms and Conditions",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(15.dp))
        Text(
            text = "Welcome to Konnekt! Before you dive in, please take a moment to review our terms and conditions:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "1. Your Responsibilities\n" +
                    "I agree to use this app responsibly and lawfully. I will ensure that the information I share is accurate and complies with community guidelines.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "2. Privacy Policy\n" +
                    "I understand and agree to the collection, storage, and use of my data as outlined in the Privacy Policy. I acknowledge that my data may be shared with third parties as specified in the Privacy Policy.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "3. Intellectual Property\n" +
                    "I acknowledge that the content I share on the app remains my property or that of the rightful owner. I will not use or share copyrighted material without permission.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "4. Account Termination\n" +
                    "I understand that my account may be suspended or terminated if I violate the terms of use, engage in inappropriate behavior, or exhibit prolonged inactivity.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "5. Limitation of Liability\n" +
                    "I understand that the platform is not liable for user-generated content. I am responsible for my actions and the content I share on the app.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "6. Dispute Resolution\n" +
                    "In the event of disputes, I agree that they will be resolved through arbitration or mediation as outlined in the terms and conditions.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "7. Updates and Changes\n" +
                    "I acknowledge that the terms and conditions may be updated, and I agree to review and accept any changes to continue using the app.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "8. Security Measures\n" +
                    "I understand the security measures in place but acknowledge my responsibility to maintain the security of my account.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun ContinueTCBtn(onBackPressed: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(25.dp), // Adjust the corner radius as needed
        color = Color(0xFF2F9ECE), // Change the background color as needed
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onBackPressed() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Continue",
                color = Color(0xFFFFFFFF), // Change the text color as needed
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}