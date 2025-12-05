package com.camille.steply.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.camille.steply.viewmodel.HomeViewModel

@Preview
@Composable
fun Home(
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val dailyGoal = 6000 // obiettivo giornaliero passi
    val progress = (uiState.steps.toFloat() / dailyGoal.toFloat())
        .coerceIn(0f, 1f)

    var selectedTab by remember { mutableStateOf(0) } // 0 = Day, 1 = Week, 2 = Month

    Scaffold(
        containerColor = Color(0xFFFBFBFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // ---------- TOP BAR ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* TODO: settings */ }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.Black
                    )
                }

                IconButton(onClick = { /* TODO: menu */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.Black
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ---------- TAB (Day / Week / Month) ----------
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.Black,
                indicator = {}
            ) {
                listOf("Day", "Week", "Month").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = label,
                                color = if (selectedTab == index)
                                    Color.Black
                                else
                                    Color(0xFF7E838C),
                                fontWeight = if (selectedTab == index)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------- CERCHIO PASSI ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // cerchio stile ring
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 22.dp,
                        color = Color(0xFF00C6FF),
                        trackColor = Color(0xFF16181F)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Yesterday",
                            color = Color(0xFF00C6FF),
                            fontSize = 18.sp
                        )
                        Text(
                            text = uiState.steps.toString(),
                            color = Color.Black,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "of $dailyGoal steps",
                            color = Color(0xFF7E838C),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ---------- STATISTICHE PICCOLE ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(
                    title = "Streak",
                    value = "4",
                    subtitle = "days"
                )
                StatChip(
                    title = "kcal",
                    value = "348",
                    subtitle = ""
                )
                StatChip(
                    title = "km",
                    value = "7.8",
                    subtitle = ""
                )
                StatChip(
                    title = "Time",
                    value = "2:02",
                    subtitle = "h"
                )
            }

            Spacer(Modifier.height(20.dp))

            // ---------- GRAFICO SETTIMANALE SEMPLIFICATO ----------
            WeeklyStepsSection()

            Spacer(Modifier.height(20.dp))

            // ---------- CLASSIFICA / CARD IN FONDO ----------
            LeaderboardCard(
                name = "Camille Eugenio",
                steps = uiState.steps,
                label = "Yesterday"
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}


@Composable
private fun StatChip(
    title: String,
    value: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                color = Color(0xFF7E838C),
                fontSize = 12.sp
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFF7E838C),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyStepsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF11131B), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        // Semplice "grafico" a colonne, giusto per imitare la schermata
        val days = listOf("TUE", "WED", "THU", "FRI", "SAT", "SUN", "MON")
        val values = listOf(3000, 3200, 5000, 7000, 4500, 8000, 2000)
        val max = values.maxOrNull() ?: 1

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, day ->
                val fraction = values[index].toFloat() / max.toFloat()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height((80 * fraction).dp)
                            .background(
                                if (day == "SUN")
                                    Color(0xFF00C6FF)
                                else
                                    Color(0xFF2A3040),
                                RoundedCornerShape(50)
                            )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = day,
                        color = if (day == "SUN") Color.White else Color(0xFF7E838C),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardCard(
    name: String,
    steps: Int,
    label: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF11131B)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF00C6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CE",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = label,
                        color = Color(0xFF7E838C),
                        fontSize = 12.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = steps.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    // in preview non abbiamo il vero viewModel, quindi qui dovresti
    // usare una UI finta oppure commentare questa preview se dà errore.
}
