package com.camille.steply.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.camille.steply.viewmodel.HomeViewModel

@Composable
fun Profile(navController: NavController) {
    Scaffold(
        containerColor = Color(0xFFF4F1EC),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                BottomPillNavBar(
                    selectedIndex = 2, // ✅ Profile selected
                    onSelect = { index ->
                        when (index) {
                            0 -> {
                                val popped = navController.popBackStack(Routes.STEPS, inclusive = false)
                                if (!popped) {
                                    navController.navigate(Routes.STEPS) { launchSingleTop = true }
                                }
                            }
                            1 -> {
                                val popped = navController.popBackStack(Routes.ACTIVITY, inclusive = false)
                                if (!popped) {
                                    navController.navigate(Routes.ACTIVITY) { launchSingleTop = true }
                                }
                            }
                            2 -> Unit // already here
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Profile Page",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}
