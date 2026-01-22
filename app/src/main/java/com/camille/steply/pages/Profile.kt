package com.camille.steply.pages

import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.camille.steply.viewmodel.ProfileViewModel
import com.camille.steply.viewmodel.HomeViewModel
import androidx.compose.ui.draw.clip


private val BgColor = Color(0xFFF4F1EC)
private val AccentColor = Color(0xFFFF8A00)

@Composable
fun Profile(navController: NavHostController, homeViewModel: HomeViewModel) {
    val viewModel: ProfileViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val goalNotifEnabled by homeViewModel.goalNotificationEnabled.collectAsState(initial = true)

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
                            0 -> navController.navigate("steps") { launchSingleTop = true }
                            1 -> navController.navigate("activity") { launchSingleTop = true }
                            2 -> { /* Stay */ }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // SETTINGS BUTTON
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = sidePad), contentAlignment = Alignment.TopEnd) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF111111))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        offset = DpOffset(x = (-16).dp, y = 0.dp),
                        modifier = Modifier.background(Color.White, RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Profile") },
                            leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(20.dp)) },
                            onClick = {
                                showMenu = false
                                navController.navigate("edit_profile")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Log Out", color = Color.Red) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(20.dp), tint = Color.Red) },
                            onClick = {
                                showMenu = false
                                showLogoutDialog = true
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = sidePad),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                // HEADER SECTION
                ProfileHeaderSection(name = uiState.name, username = uiState.username, photoUri = uiState.profilePhotoUri)

                Spacer(modifier = Modifier.height(40.dp))

                // CONTENT SECTION
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. GOAL PROGRESS CARD (Static / Informative)
                    GoalProgressCard(
                        currentSteps = 6140, // Placeholder: implement real step tracking later
                        goalSteps = uiState.goal.filter { it.isDigit() }.toIntOrNull() ?: 10000
                    )

                    // 2. WEIGHT ROW (Interactive with Chevron)
                    ProfileRowItem(
                        label = "Weight",
                        value = uiState.weight.ifEmpty { "--" }.let { if (it != "--") "$it kg" else it },
                        icon = Icons.Default.Scale,
                        onClick = {
                            navController.navigate("weight_history")
                        }
                    )

                    ProfileToggleRowItem(
                        label = "Daily Goal Notification",
                        icon = Icons.Default.Notifications,
                        checked = goalNotifEnabled,
                        onCheckedChange = { enabled ->
                            homeViewModel.setGoalNotificationEnabled(enabled)
                        }
                    )

                }
            }
        }
    }

    // LOGOUT DIALOG
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout { navController.navigate("login") { popUpTo(0) } }
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
fun ProfileHeaderSection(name: String, username: String, photoUri: android.net.Uri?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // --- LOGICA CONDIZIONALE PER L'AVATAR ---
        if (photoUri != null) {
            // CASO A: C'è la foto -> Mostra AsyncImage con bordo arancione
            Surface(
                modifier = Modifier.size(110.dp),
                shape = CircleShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.LightGray),
                shadowElevation = 4.dp
            ) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(photoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        } else {
            // CASO B: Non c'è la foto -> Cerchio Arancione con Iniziale
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(AccentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase().ifEmpty { "U" },
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // NOME E USERNAME
        Text(
            text = name.ifEmpty { "User Name" },
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "@${username.ifEmpty { "username" }}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

@Composable
fun GoalProgressCard(currentSteps: Int, goalSteps: Int) {
    val progress = (currentSteps.toFloat() / goalSteps.toFloat()).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, null, tint = Color.Black.copy(0.6f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Daily Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "$goalSteps steps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color(0xFFF0F0F0), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(AccentColor, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${(progress * 100).toInt()}% of your goal",
                style = MaterialTheme.typography.labelMedium,
                color = AccentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfileRowItem(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.Black.copy(0.6f), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = AccentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProfileToggleRowItem(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.Black.copy(0.6f), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF34C759),
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE6E6EA),
                    uncheckedThumbColor = Color.White
                )
            )
        }
    }
}
