package com.el.konnekt.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.el.konnekt.R
import com.el.konnekt.ui.activities.calls.CallActivity
import com.el.konnekt.ui.activities.konnekt.Konnekt
import com.el.konnekt.ui.activities.mainpage.MessageActivity

enum class BottomNavItem {
    Messages,
    Calls,
    Konnekt
}

@Composable
fun BottomNavigationBar(
    currentScreen: BottomNavItem,
    username: String,
    profilePic: Uri,
    onNavigate: ((BottomNavItem) -> Unit)? = null
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavButton(
            label = "Messages",
            isActive = currentScreen == BottomNavItem.Messages,
            activeIcon = R.drawable.bottombar_activemessagespage,
            passiveIcon = R.drawable.bottombar_passivemessagespage,
            onClick = {
                if (currentScreen != BottomNavItem.Messages) {
                    onNavigate?.invoke(BottomNavItem.Messages) ?: run {
                        navigateToMessages(context, username, profilePic)
                    }
                }
            }
        )

//        BottomNavButton(
//            label = "Call Logs",
//            isActive = currentScreen == BottomNavItem.Calls,
//            activeIcon = R.drawable.bottombar_activecallspage,
//            passiveIcon = R.drawable.bottombar_passivecallspage,
//            onClick = {
//                if (currentScreen != BottomNavItem.Calls) {
//                    onNavigate?.invoke(BottomNavItem.Calls) ?: run {
//                        navigateToCalls(context)
//                    }
//                }
//            }
//        )

        BottomNavButton(
            label = "Konnekt",
            isActive = currentScreen == BottomNavItem.Konnekt,
            activeIcon = R.drawable.bottombar_activeaddfriendspage,
            passiveIcon = R.drawable.bottombar_passiveaddfriendspage,
            onClick = {
                if (currentScreen != BottomNavItem.Konnekt) {
                    onNavigate?.invoke(BottomNavItem.Konnekt) ?: run {
                        navigateToKonnekt(context, username, profilePic)
                    }
                }
            }
        )
    }
}

@Composable
private fun BottomNavButton(
    label: String,
    isActive: Boolean,
    activeIcon: Int,
    passiveIcon: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .height(52.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
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
            color = if (isActive) Color(0xFF2F9ECE) else MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun navigateToMessages(context: Context, username: String, profilePic: Uri) {
    val intent = Intent(context, MessageActivity::class.java).apply {
        putExtra("username", username)
        putExtra("profileUri", profilePic.toString())
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    context.startActivity(intent)
}

private fun navigateToCalls(context: Context) {
    val intent = Intent(context, CallActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    context.startActivity(intent)
}

private fun navigateToKonnekt(context: Context, username: String, profilePic: Uri) {
    val intent = Intent(context, Konnekt::class.java).apply {
        putExtra("username", username)
        putExtra("profileUri", profilePic.toString())
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    context.startActivity(intent)
}