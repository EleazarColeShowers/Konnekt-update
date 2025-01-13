package com.example.instachatcompose.ui.activities.mainpage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import com.example.instachatcompose.R
import com.example.instachatcompose.ui.activities.konnekt.Konnekt
import com.example.instachatcompose.ui.theme.InstaChatComposeTheme


class MessageActivity: ComponentActivity() {
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
                        MessagePage()
                    }
                }
            }
        }
    }
}

@Composable
fun MessagePage() {
    Box(modifier = Modifier.fillMaxSize()) {
        val activity = LocalContext.current as? ComponentActivity
        val username: String = activity?.intent?.getStringExtra("username") ?: "DefaultUsername"
        val bio: String = activity?.intent?.getStringExtra("bio") ?: "DefaultBio"
        val profilePic: Uri = Uri.parse(activity?.intent?.getStringExtra("profileUri") ?: "")

        Column(
            modifier = Modifier.padding(horizontal = 15.dp)
        ) {

            User(username = username, profilePic = profilePic)
            val navController = rememberNavController()
            NavHost(navController, startDestination = "home" ){
                composable("home"){ MessageFrag(username = username, navController)}
                composable("detail") { TextFrag() }
            }


        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter) // Aligns the Box at the bottom
                .fillMaxWidth() // The BottomAppBar spans the full width
                .height(80.dp)  // Desired height
        ) {
            BottomAppBar(username = username, profilePic = profilePic) // Place your bottom bar here
        }
    }
}


@Composable
fun User(username: String,profilePic: Uri){
    val settingsIcon= painterResource(id = R.drawable.settings)
    val searchIcon= painterResource(id = R.drawable.searchicon)

    var search by remember {
        mutableStateOf("")
    }

    Column{
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row{
            val imagePainter: ImagePainter = rememberImagePainter(data = profilePic)
            Image(
                painter = imagePainter,
                contentDescription = null,
                modifier = Modifier
                    .size(31.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .scale(1.5f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = username,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight(400),
                    color = Color(0xFF696969),
                ),
            )
        }

        Row{
            Image(
                painter = settingsIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Settings",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight(400),
                    color = Color(0xFF2F9ECE),
                ),
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text ="Messages",
        style = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight(500),
            color = Color(0xFF000000),
        )
    )
    Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = Color(0x33333333),
                    shape = RoundedCornerShape(size = 20.dp)
                )
                .height(48.dp)
                .width(444.dp)
        ) {

               Image(
                   painter = searchIcon,
                   contentDescription = "Search",
                   modifier = Modifier
                       .size(35.dp)
                       .padding(top = 15.dp),
               )
            Spacer(modifier = Modifier.width(6.dp))
               BasicTextField(
                   value = search,
                   onValueChange = { search = it },
                   textStyle = LocalTextStyle.current.copy(color = Color.Black),
                   singleLine = true,
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(top = 14.dp, start = 27.dp)
               )

               if (search.isEmpty()) {
                   Text(
                       text = "Search",
                       color = Color.Gray,
                       modifier = Modifier.padding(start = 27.dp, top = 14.dp)
                   )
               }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Text(
                    text = "Requests(10)",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight(400),
                        color = Color(0xFF2F9ECE),
                    ),
                )

                Text(
                    text = "Archives(1)",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight(400),
                        color = Color(0xFF2F9ECE),
                    ),
                )
        }
    }
}

@Composable
fun MessageFrag(username: String, navController: NavController){

    val messageConnected= painterResource(id = R.drawable.messagechats)

    Column(
        modifier= Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Image(
            painter = messageConnected,
            contentDescription = null,
            modifier= Modifier
                .width(160.95.dp)
                .height(160.18.dp)
            )
        Spacer(modifier = Modifier.height(20.dp))
//        TODO: 3. Username should appear when user logs in (already appears on sign up)
        Text(
            text = "Welcome, $username",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight(600),
                color = Color(0xFF2F9ECE),
            ),

        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You don’t have any friend yet, click on the “Konnekt” button below to add friends and start connecting.",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight(500),
                color = Color(0xFF696969),
            ),
            modifier = Modifier.width(300.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            // Navigate to Detail Screen within the same activity
            navController.navigate("detail")
        }) {
            Text("Go to Detail Screen")
        }

    }

}

@Composable
fun TextFrag(){
    Text(text = "Hello")
}

enum class BottomAppBarItem {
    Messages,
    Calls,
    AddFriends
}

@Composable
fun BottomAppBar(username: String,profilePic: Uri) {
    var activeItem by remember { mutableStateOf(BottomAppBarItem.Messages) }
    val context= LocalContext.current


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically // Align icons and text vertically
    ) {
        BottomAppBarItem(
            label = "Messages",
            isActive = activeItem == BottomAppBarItem.Messages,
            activeIcon = R.drawable.bottombar_activemessagespage,
            passiveIcon = R.drawable.bottombar_passivemessagespage,
            onClick = {
                activeItem = BottomAppBarItem.Messages
                val intent = Intent(context, MessageActivity::class.java)
                context.startActivity(intent)
            }
        )

        BottomAppBarItem(
            label = "Call Logs",
            isActive = activeItem == BottomAppBarItem.Calls,
            activeIcon = R.drawable.bottombar_activecallspage,
            passiveIcon = R.drawable.bottombar_passivecallspage,
            onClick = { activeItem = BottomAppBarItem.Calls }
        )

        BottomAppBarItem(
            label = "Konnekt",
            isActive = activeItem == BottomAppBarItem.AddFriends,
            activeIcon = R.drawable.bottombar_activeaddfriendspage,
            passiveIcon = R.drawable.bottombar_passiveaddfriendspage,
            onClick = {
                activeItem = BottomAppBarItem.AddFriends
                val intent = Intent(context, Konnekt::class.java)
                intent.putExtra("username", username)
                intent.putExtra("profileUri", profilePic.toString())
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun BottomAppBarItem(
    label: String,
    isActive: Boolean,
    activeIcon: Int,
    passiveIcon: Int,
    onClick: () -> Unit
) {
    Log.d("BottomAppBarItem", "Rendering item: $label, isActive: $isActive")

    Column(
        modifier = Modifier
            .width(68.dp)
            .height(52.dp)
            .clickable(onClick = onClick),  // Make the item clickable
        horizontalAlignment = Alignment.CenterHorizontally // Align content in the center
    ) {
        Image(
            painter = painterResource(id = if (isActive) activeIcon else passiveIcon),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive) Color(0xFF2F9ECE) else Color(0xFF696969) // Change text color based on active/passive state
        )
    }
}

