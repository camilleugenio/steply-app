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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.indication
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.foundation.LocalIndication
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp



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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            ActivitiesTopBar(
                onAddClick = {
                    // TODO: naviga alla schermata "Add Activity"
                    // navController.navigate(Routes.ADD_ACTIVITY)
                }
            )

            // lista attività
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Qui andrà la lista delle attività e \nsopra il meteo grande")
            }
        }
    }
}


@Composable
fun ActivitiesTopBar(
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        // Titolo centrato
        Text(
            text = "All Activities",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterEnd)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    clip = false
                ) // ⬅️ OMBRA PRIMA
                .clip(CircleShape)
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current
                ) {
                    onAddClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

    }
}
