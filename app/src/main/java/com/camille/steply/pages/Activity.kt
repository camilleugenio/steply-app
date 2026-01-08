package com.camille.steply.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.camille.steply.pages.Routes
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun ActivityScreen(
    navController: NavController
) {
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
                    selectedIndex = 1,
                    onSelect = { index ->
                        when (index) {
                            0 -> {
                                // torna subito a Steps se già esiste nello stack
                                val popped = navController.popBackStack(Routes.STEPS, inclusive = false)
                                if (!popped) {
                                    // fallback: se non esiste (es. deep link), allora naviga
                                    navController.navigate(Routes.STEPS) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                            1 -> Unit
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Activity",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
