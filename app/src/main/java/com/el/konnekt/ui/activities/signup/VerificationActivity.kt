//package com.example.instachatcompose.ui.activities.signup
//
//import android.app.Activity
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.material3.LocalTextStyle
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.instachatcompose.R
//import com.example.instachatcompose.ui.activities.JoinActivity
//import com.example.instachatcompose.ui.theme.InstaChatComposeTheme
//import com.google.firebase.FirebaseException
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.PhoneAuthCredential
//import com.google.firebase.auth.PhoneAuthOptions
//import com.google.firebase.auth.PhoneAuthProvider
//import java.util.concurrent.TimeUnit
//
//class VerificationActivity: ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            InstaChatComposeTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    Column(modifier= Modifier.fillMaxSize()) {
//                        VerificationPage(onBackPressed = {
//                            onBackPressed()
//                        })
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun VerificationPage(onBackPressed: () -> Unit){
//    Column (
//        modifier= Modifier.padding(horizontal = 15.dp)
//    ){
//        VerificationProgress(onBackPressed ={
//            onBackPressed()
//        } )
//        VerificationForm()
//
//    }
//}
//
//@Composable
//fun VerificationProgress(onBackPressed: () -> Unit){
//    val returnArrow= painterResource(id = R.drawable.returnarrow)
//    val goodProgress= painterResource(id = R.drawable.bluerectangle)
//    val noProgress= painterResource(id = R.drawable.whiterectangle)
//    Spacer(modifier = Modifier.height(15.dp))
//    Row(modifier= Modifier
//        .fillMaxWidth()
//    ) {
//
//        Image(painter =returnArrow ,
//            contentDescription = null,
//            modifier = Modifier
//                .size(25.dp)
//                .clickable {
//                    onBackPressed()
//                }
//        )
//        Spacer(modifier = Modifier.width(30.dp))
//        Image(
//            painter = goodProgress,
//            contentDescription = null,
//            modifier = Modifier
//                .height(24.dp)
//                .width(55.dp)
//        )
//        Spacer(modifier = Modifier.width(15.dp))
//        Image(
//            painter = goodProgress,
//            contentDescription = null,
//            modifier = Modifier
//                .height(24.dp)
//                .width(55.dp)
//        )
//        Spacer(modifier = Modifier.width(15.dp))
//        Image(
//            painter = noProgress,
//            contentDescription = null,
//            modifier = Modifier
//                .height(24.dp)
//                .width(55.dp)
//        )
//        Spacer(modifier = Modifier.width(15.dp))
//        Image(
//            painter = noProgress,
//            contentDescription = null,
//            modifier = Modifier
//                .height(24.dp)
//                .width(55.dp)
//        )
//
//    }
//}
//
//@Composable
//fun VerificationForm() {
//    val context = LocalContext.current
//    var phoneNumber by remember {
//        mutableStateOf("")
//    }
//    Column{
//
//        Spacer(modifier = Modifier.height(30.dp))
//
//        Text(
//            text = "Verify your Account",
//            style = TextStyle(
//                fontSize = 24.sp,
//                fontWeight = FontWeight(500),
//                color = Color(0xFF050907),
//                textAlign = TextAlign.Center,
//            )
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Text(
//            text = "Let's keep your account secured and verified",
//            style = TextStyle(
//                fontSize = 14.sp,
//                fontWeight = FontWeight(400),
//                color = Color(0xFF696969),
//            )
//        )
//
//        Spacer(modifier = Modifier.height(48.dp))
//        Column(
//            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
//            horizontalAlignment = Alignment.Start,
//        ) {
//            Text(
//                text = "Phone Number",
//                style = TextStyle(
//                    fontSize = 14.sp,
//                    fontWeight = FontWeight(400),
//                    color = Color(0xFF050907),
//                )
//            )
//            Box(
//                modifier = Modifier
//                    .border(
//                        width = 1.dp,
//                        color = Color(0x33333333),
//                        shape = RoundedCornerShape(size = 20.dp)
//                    )
//                    .height(48.dp)
//                    .fillMaxWidth()
//            ) {
//                BasicTextField(
//                    value = phoneNumber,
//                    onValueChange = { newInput ->
//                        val sanitizedInput = newInput.filter { it.isDigit() || it == '+' }
//                        phoneNumber = sanitizedInput
//                    },
//                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
//                    singleLine = true,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(start = 24.dp, top = 14.dp)
//                )
//
//                if (phoneNumber.isEmpty()) {
//                    Text(
//                        text = "Enter your phone number",
//                        color = Color.Gray,
//                        modifier = Modifier.padding(start = 24.dp,top=14.dp)
//                    )
//                }
//            }
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//        Text(
//            text = "Please enter your phone number for the OTP or you can click here and we’ll send it to your email",
//            style = TextStyle(
//                fontSize = 12.sp,
//                lineHeight = 18.sp,
//                fontWeight = FontWeight(500),
//                color = Color(0x4D333333),
//
//                )
//        )
//        Spacer(modifier = Modifier.height(40.dp))
//        ContinueVerification(
//            phoneNumber = phoneNumber,
//            onClick = {
//                val intent = Intent(context, JoinActivity::class.java)
//                context.startActivity(intent)
//            }
//        )
//    }
//}
//
//@Composable
//fun ContinueVerification(phoneNumber: String,onClick: () -> Unit) {
//    val context = LocalContext.current
//    Surface(
//        shape = RoundedCornerShape(25.dp),
//        color = Color(0xFF2F9ECE),
//        modifier = Modifier
//            .height(54.dp)
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp)
//            .clickable {
//                if (phoneNumber.isNotBlank()) {
//                    sendOtpToPhoneNumber(context, phoneNumber)
//                    onClick()
//                } else {
//                    Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
//                }
//            }
//    ) {
//        Column(
//            modifier = Modifier.fillMaxWidth(),
//            verticalArrangement = Arrangement.Center,
//            Alignment.CenterHorizontally
//        ) {
//            Text(
//                text = "Continue",
//                color = Color(0xFFFFFFFF),
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.padding(16.dp),
//            )
//        }
//    }
//}
//
//fun sendOtpToPhoneNumber(context: Context, phoneNumber: String) {
//    val auth = FirebaseAuth.getInstance()
//    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
//        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
//            val intent = Intent(context, JoinActivity::class.java)
//            context.startActivity(intent)
//        }
//
//        override fun onVerificationFailed(e: FirebaseException) {
//            // This callback is invoked for invalid requests, such as a verification request
//            Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    val formattedPhoneNumber = if (phoneNumber.startsWith("+234")) {
//        phoneNumber // If the number already includes the country code, use it as is
//    } else {
//        "+234$phoneNumber" // If not, prepend the Nigerian country code
//    }
//
//    val options = PhoneAuthOptions.newBuilder(auth)
//        .setPhoneNumber(formattedPhoneNumber) // Phone number to verify
//        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
//        .setActivity(context as Activity) // Activity (for callback binding)
//        .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
//        .build()
//
//    PhoneAuthProvider.verifyPhoneNumber(options)
//
//}
