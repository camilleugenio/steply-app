package com.camille.steply.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.camille.steply.viewmodel.ProfileViewModel

private val BgColor = Color(0xFFF4F1EC)
private val AccentColor = Color(0xFFFF8A00)

@Composable
fun Profile(navController: NavHostController) {
    val viewModel: ProfileViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

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
                ProfileHeaderSection(name = uiState.name, username = uiState.username)
                Spacer(modifier = Modifier.height(40.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoCard(
                        label = "Weight",
                        value = uiState.weight.ifEmpty { "--" }.let { if (it != "--") "$it kg" else it },
                        modifier = Modifier.weight(1f)
                    )
                    InfoCard(
                        label = "Daily Goal",
                        value = uiState.goal.ifEmpty { "10,000" },
                        modifier = Modifier.weight(1f)
                    )
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
fun ProfileHeaderSection(name: String, username: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(110.dp).background(AccentColor, CircleShape), contentAlignment = Alignment.Center) {
            Text(
                text = name.take(1).uppercase().ifEmpty { "U" },
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = name.ifEmpty { "User Name" }, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Text(text = "@${username.ifEmpty { "username" }}", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
    }
}

@Composable
fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
        }
    }
}