package com.camille.steply.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.camille.steply.viewmodel.HomeViewModel
import com.camille.steply.viewmodel.WorkoutType
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun WorkoutScreen(
    navController: NavController,
    homeViewModel: HomeViewModel,
    type: WorkoutType
) {
    val homeState by homeViewModel.uiState.collectAsState()

    var paused by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableStateOf(0) }

    // ✅ timer semplice (poi lo sposteremo in un WorkoutViewModel)
    LaunchedEffect(paused) {
        while (!paused) {
            delay(1000)
            elapsedSec += 1
        }
    }

    val title = when (type) {
        WorkoutType.RUN -> "RUN"
        WorkoutType.WALK -> "WALK"
        WorkoutType.CYCLING -> "CYCLING"
    }

    val durationText = remember(elapsedSec) { formatDuration(elapsedSec) }

    // Per ora placeholders: poi li colleghiamo a GPS/step/calorie reali
    val kmText = "0.00"
    val kcalText = "0"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F1EC))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {

        // -------------------- WORKOUT TOP BAR --------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 3.dp, end = 3.dp, top = 60.dp, bottom = 30.dp)
        ) {

            // ---- TITOLO ----
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = homeState.meteoDesc,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${homeState.meteoTempC}°C",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                }
            }

            // ✅ PILL SINISTRA: END (solo quando in pausa)
            if (paused) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                // per ora: torna indietro
                                navController.popBackStack()
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop, // ⛔ icona stop
                            contentDescription = "End",
                            tint = Color.Black,
                            modifier = Modifier.size(25.dp)
                        )

                        Spacer(Modifier.width(5.dp))

                        Text(
                            text = "End",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    }
                }
            }

            // ✅ PILL DESTRA: PAUSE / RESUME (icona cambia)
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp), // ✅ era start, deve essere end
                shape = RoundedCornerShape(999.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .clickable { paused = !paused }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (paused) Icons.Default.Refresh else Icons.Default.Pause,
                        contentDescription = if (paused) "Resume" else "Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(25.dp)
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        text = if (paused) "Resume" else "Pause",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        // -------- MAP CARD (placeholder) --------
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFFE8E8E8),
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MAP PLACEHOLDER",
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // -------- STATS CARD --------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFFF8F6F2),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatMini(
                    icon = Icons.Default.Timer,
                    iconColor = Color(0xFF7064AF),
                    title = "Duration",
                    value = durationText
                )
                StatMini(
                    icon = Icons.Default.Route,
                    iconColor = Color(0xFF32ADE6),
                    title = "Distance",
                    value = "$kmText km"
                )
                StatMini(
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = Color(0xFFFF9500),
                    title = "Active Energy",
                    value = "$kcalText kcal"
                )
            }
        }
    }
}

@Composable
private fun StatMini(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.Start) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color(0xFF7A7A7A),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = value,
            fontSize = 22.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}


private fun formatDuration(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}
