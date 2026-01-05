package com.camille.steply.pages

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import com.camille.steply.viewmodel.HomeVmFactory
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale




// -------------------- PALETTE --------------------
private val Bg = Color(0xFFF4F1EC)
private val Card = Color.White
private val TextPrimary = Color(0xFF111111)
private val TextSecondary = Color(0xFF8E8E93)
private val Divider = Color(0xFFE6E6EA)
private val Accent = Color(0xFFFF8A00)




@Preview
@Composable
fun Home() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeVmFactory(context.applicationContext as Application)
    )

    val uiState by homeViewModel.uiState.collectAsState()

    val isEmulator = remember {
        val fp = android.os.Build.FINGERPRINT.lowercase()
        val model = android.os.Build.MODEL.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        val device = android.os.Build.DEVICE.lowercase()
        fp.contains("generic") ||
                fp.contains("emulator") ||
                model.contains("emulator") ||
                model.contains("sdk") ||
                brand.contains("generic") ||
                device.contains("generic")
    }



    DisposableEffect(Unit) {
        if (isEmulator) {
            homeViewModel.startAccelerometerSimulation()
        } else {
            homeViewModel.startStepUpdates()
        }

        onDispose {
            homeViewModel.stopAccelerometerSimulation()
            homeViewModel.stopStepUpdates()
        }
    }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) homeViewModel.refreshPlace()
    }

    // ✅ Refresh location EVERY TIME the app/screen is resumed (opened again)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    homeViewModel.refreshPlace()
                } else {
                    // Optional: request permission when opening the app
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dailyGoal = 6000
    val progress = (uiState.steps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        homeViewModel.refreshPlace()
    }



    Scaffold(
        containerColor = Bg,
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                BottomPillNavBar(
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {

            TopBarLight(
                placeText = when {
                    uiState.locationLoading -> "Locating..."
                    uiState.locationError != null -> "Err: ${uiState.locationError}"
                    else -> uiState.currentPlacename
                },
                weatherText = "Sunny  25°C",
                onSettings = { },
                onCalendar = { }
            )

            Spacer(Modifier.height(14.dp))

            // -------------------- CARD GRANDE CENTRALE --------------------
            StepsMainCard(
                dateLabel = uiState.currentDayname,
                dateValue = uiState.currentDate,
                steps = uiState.steps,
                dailyGoal = dailyGoal,
                km = uiState.km,
                kcal = uiState.kcal,
                onRefresh = { }
            )

            Spacer(Modifier.height(14.dp))

            // -------------------- STREAK --------------------
            StreakCard(streakDays = 28)

            Spacer(Modifier.height(30.dp))

            // -------------------- DASHBOARD --------------------
            WeeklyStepsLight(currentDateIso = uiState.currentDateIso)

            Spacer(Modifier.height(24.dp))
        }
    }

}

// -------------------- TOP BAR --------------------
@Composable
private fun TopBarLight(
    placeText: String,
    weatherText: String,
    onCalendar: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                ) {
                    append(placeText)
                }
                withStyle(
                    style = SpanStyle(
                        color = TextSecondary
                    )
                ) {
                    append(weatherText)
                }
            },
            fontSize = 14.sp
        )


        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Card,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // --- CALENDARIO ---
                IconButton(
                    onClick = onCalendar,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar",
                        tint = TextPrimary
                    )
                }

                // --- SETTINGS ---
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary
                    )
                }
            }
        }

    }
}


// -------------------- CARD CENTRALE --------------------
@Composable
private fun StepsMainCard(
    dateLabel: String,
    dateValue: String,
    steps: Int,
    dailyGoal: Int,
    km: String,
    kcal: String,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Card,
        shadowElevation = 18.dp
    ){
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // refresh
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = TextSecondary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(dateLabel, color = TextSecondary, fontSize = 16.sp)
                Text(
                    dateValue,
                    color = TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                        .border(
                            width = 4.dp,
                            color = Color(0xFFE6E6EA),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text(
                            text = steps.toString(),
                            color = TextPrimary,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Steps",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            InfoMini(value = km, label = "km")
                            InfoMini(value = kcal, label = "calories")
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Goal: $dailyGoal",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoMini(value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
    }
}

// -------------------- Streak --------------------
@Composable
fun StreakCard(
    streakDays: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = streakDays.toString(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A00)
                    )

                    Text(
                        text = "day streak!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF8A00)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF6F6F6)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {


                        Text(
                            text = buildAnnotatedString {
                                append("Keep your ")
                                withStyle(
                                    SpanStyle(
                                        color = Color(0xFFFF8A00),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) {
                                    append("Perfect Streak")
                                }
                                append(" \nby walking every day!")
                            },
                            fontSize = 14.sp,
                            color = Color(0xFF444444)
                        )

                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "🔥",
                            fontSize = 28.sp
                        )
                    }
            }

            }
        }
    }
}


// -------------------- DASHBOARD --------------------
@Composable
private fun WeeklyStepsLight(currentDateIso: String) {

    val today = remember(currentDateIso) { LocalDate.parse(currentDateIso) }

    // ultimi 7 giorni: 6 giorni fa ... oggi
    val last7Days = remember(today) {
        (6 downTo 0).map { today.minusDays(it.toLong()) }
    }

    val days = remember(last7Days) {
        last7Days.map { d ->
            d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).lowercase()
        }
    }

    val dates = remember(last7Days) {
        last7Days.map { it.dayOfMonth.toString() }
    }

    // per ora lasciamo i valori finti come prima (poi li colleghiamo allo store)
    val values = remember {
        listOf(2000, 8000, 3200, 6100, 4100, 700, 1643)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val dailyGoal = 6000
        val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, day ->
                val frac = values[index].toFloat() / max.toFloat()
                val isSelected = index == days.lastIndex // oggi

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height((90 * frac).dp)
                            .background(
                                color = when {
                                    values[index] >= dailyGoal -> Color(0xFF4CAF50)
                                    else -> Accent
                                },
                                shape = RoundedCornerShape(50)
                            )
                    )

                    Spacer(Modifier.height(10.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day,
                            color = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = dates[index],
                            color = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.7f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}




@Composable
fun BottomPillNavBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem("Steps", Icons.Default.DirectionsRun),
        NavItem("Activity", Icons.Default.Whatshot),
        NavItem("Profile", Icons.Default.Person)
    )

    val container = Color(0xFFF3F0EC)
    val selectedBg = Color(0xFFE2E2E2)
    val textNormal = Color(0xFF111111)
    val accent = Accent
    val shadow = 12.dp

    Surface(
        modifier = modifier
            .width(360.dp)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(999.dp),
        color = container,
        shadowElevation = shadow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

        items.forEachIndexed { index, item ->
                val selected = index == selectedIndex

                Surface(
                    onClick = { onSelect(index) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) selectedBg else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(min = 96.dp)
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) accent else textNormal,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            color = if (selected) accent else textNormal,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private data class NavItem(
    val label: String,
    val icon: ImageVector
)
