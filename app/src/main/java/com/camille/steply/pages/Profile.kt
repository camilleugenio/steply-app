package com.camille.steply.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.aspectRatio
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.camille.steply.viewmodel.ProfileViewModel
import com.camille.steply.viewmodel.HomeViewModel

private val BgColor = Color(0xFFF4F1EC)
private val AccentColor = Color(0xFFFF8A00)
private val CardColor = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val IconBgColor = Color(0xFFF4F1EC)

@Composable
fun Profile(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
) {

    val uiState by profileViewModel.uiState.collectAsState()
    val homeState by homeViewModel.uiState.collectAsState()

    val kmStepsOnly = uiState.totalKm

    val goalNotifEnabled by homeViewModel.goalNotificationEnabled.collectAsState(initial = true)
    val goalSteps = uiState.goal.filter { it.isDigit() }.toIntOrNull() ?: 10000

    var showMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val cfg = LocalConfiguration.current
    val isSmall = cfg.screenWidthDp < 420
    val sidePad = if (isSmall) 12.dp else 16.dp

    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = sidePad, end = sidePad, bottom = if (isSmall) 12.dp else 18.dp),
                contentAlignment = Alignment.Center
            ) {
                BottomPillNavBar(
                    selectedIndex = 2,
                    onSelect = { index ->
                        when (index) {
                            0 -> {
                                homeViewModel.selectToday()
                                navController.navigate(Routes.STEPS) { launchSingleTop = true }
                            }
                            1 -> navController.navigate("activity") { launchSingleTop = true }
                            2 -> { /* Stay */ }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = sidePad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR SETTINGS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                ) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF111111)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = Color.White,
                        tonalElevation = 6.dp,
                        shadowElevation = 10.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {

                            PressableMenuRow(
                                label = "Edit Profile",
                                icon = Icons.Default.Edit,
                                iconTint = Color.Black,
                                onClick = {
                                    showMenu = false
                                    navController.navigate("edit_profile")
                                }
                            )

                            PressableMenuRow(
                                label = "Log Out",
                                icon = Icons.AutoMirrored.Filled.Logout,
                                iconTint = Color.Red,
                                textColor = Color.Red,
                                pressedBgColor = Color(0xFFFFEBEE),
                                onClick = {
                                    showMenu = false
                                    showLogoutDialog = true
                                }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(top=14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HEADER SECTION
                ProfileHeaderSection(
                    name = uiState.name,
                    username = uiState.username,
                    photoUri = uiState.profilePhotoUri,
                    streakCount = homeState.streakDays
                )

                Spacer(modifier = Modifier.height(if (isSmall) 10.dp else 12.dp))

                // CONTENT SECTION
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OverviewCard(
                        totalSteps = uiState.totalSteps,
                        bestDay = uiState.bestDaySteps,
                        totalKm = kmStepsOnly,
                        totalWorkouts = homeState.totalWorkouts,
                        homeViewModel = homeViewModel
                    )

                    GoalProgressCard(currentSteps = homeState.steps, goalSteps = goalSteps)

                    ProfileRowItem(
                        label = "Weight Tracker",
                        value = uiState.weight.ifEmpty { "--" }.let { if (it != "--") "$it kg" else it },
                        icon = Icons.Default.Scale,
                        onClick = { navController.navigate("weight_history") }
                    )
                    //Spacer(Modifier.height(30.dp))
                    ProfileToggleRowItem(
                        label = "Daily Goal Notification",
                        icon = Icons.Default.Notifications,
                        checked = goalNotifEnabled,
                        onCheckedChange = { homeViewModel.setGoalNotificationEnabled(it) }
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    profileViewModel.logout { navController.navigate("login") { popUpTo(0) } }
                }) { Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun ProfileHeaderSection(name: String, username: String, photoUri: android.net.Uri?, streakCount: Int) {

    val (userRank, rankColor) = when {
        streakCount >= 30 -> "Legend" to Color(0xFF6200EE)
        streakCount >= 15 -> "Pro" to Color(0xFF007AFF)
        streakCount >= 7 -> "Amateur" to Color(0xFF34C759)
        else -> "Rookie" to Color(0xFF8E8E93)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f).aspectRatio(1.1f),
            shape = RoundedCornerShape(28.dp),
            color = CardColor,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(photoUri).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(60.dp).background(AccentColor, CircleShape), contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase().ifEmpty { "U" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(name.ifEmpty { "User" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text("@$username", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
            }
        }

        Surface(
            modifier = Modifier.weight(1f).aspectRatio(1.1f),
            shape = RoundedCornerShape(28.dp),
            color = CardColor,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(if (streakCount > 0) "🔥" else "🧊", fontSize = 32.sp)
                Text(streakCount.toString(), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = if (streakCount > 0) Color(0xFFFF4500) else TextSecondary)
                Text("Day Streak", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(color = rankColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(userRank, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = rankColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun OverviewCard(
    totalSteps: Long,
    bestDay: Int,
    totalKm: String,
    totalWorkouts: Int,
    homeViewModel: HomeViewModel
) {
    val cfg = LocalConfiguration.current
    val isSmall = cfg.screenWidthDp < 420

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = CardColor,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Your Journey",
                modifier = Modifier.clickable { homeViewModel.simulateStepsDebug() },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF111111)
            )
            Spacer(modifier = Modifier.height(if (isSmall) 16.dp else 20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OverviewStatItem(
                    label = "Lifetime Steps",
                    value = if (totalSteps > 99999) "${totalSteps / 1000}k" else totalSteps.toString(),
                    icon = Icons.Default.DirectionsWalk,
                    modifier = Modifier.weight(1f)
                )
                OverviewStatItem(
                    label = "Daily Record",
                    value = if (bestDay > 0) bestDay.toString() else "--",
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(if (isSmall) 16.dp else 20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OverviewStatItem(
                    label = "Walking Dist.",
                    value = "$totalKm km",
                    icon = Icons.Default.Map,
                    modifier = Modifier.weight(1f)
                )
                OverviewStatItem(
                    label = "Workout sessions",
                    value = totalWorkouts.toString(),
                    icon = Icons.Default.FitnessCenter,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun OverviewStatItem(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(IconBgColor, CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = AccentColor)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = label, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
fun GoalProgressCard(currentSteps: Int, goalSteps: Int) {
    val progress = (currentSteps.toFloat() / goalSteps.toFloat()).coerceIn(0f, 1f)
    val cfg = LocalConfiguration.current
    val isSmall = cfg.screenWidthDp < 420
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = CardColor,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).background(IconBgColor, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Flag, null, tint = AccentColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Goal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("$goalSteps steps", fontSize = 11.sp, color = TextSecondary)
                }
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = AccentColor, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(if (isSmall) 14.dp else 16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFF0F0F0), CircleShape)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(AccentColor, CircleShape))
            }
        }
    }
}

@Composable
fun ProfileRowItem(label: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = CardColor,
        shadowElevation = 6.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(IconBgColor, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = AccentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(value, fontWeight = FontWeight.Bold, color = AccentColor, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ProfileToggleRowItem(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = CardColor,
        shadowElevation = 6.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(IconBgColor, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = AccentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF34C759),
                    checkedThumbColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun PressableMenuRow(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    textColor: Color = Color.Black,
    pressedBgColor: Color = Color(0xFFF2F2F4),
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val bg = if (pressed) pressedBgColor else Color.Transparent

    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() }
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}