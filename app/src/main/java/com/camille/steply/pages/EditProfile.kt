package com.camille.steply.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.camille.steply.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavHostController) {
    val viewModel: ProfileViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    // Local states for input fields
    var tempName by remember { mutableStateOf(uiState.name) }
    var tempSurname by remember { mutableStateOf(uiState.surname) }
    var tempWeight by remember { mutableStateOf(uiState.weight) }
    var tempGoal by remember { mutableStateOf(uiState.goal) }

    // State for the confirmation dialog
    var showExitDialog by remember { mutableStateOf(false) }

    // Check if any data has been modified compared to the original state
    val hasUnsavedChanges = remember(tempName, tempSurname, tempWeight, tempGoal, uiState) {
        tempName != uiState.name ||
                tempSurname != uiState.surname ||
                tempWeight != uiState.weight ||
                tempGoal != uiState.goal
    }

    // Handles the physical back button or swipe gesture
    BackHandler(enabled = hasUnsavedChanges) {
        showExitDialog = true
    }

    // Sync local states when data is loaded from the database
    LaunchedEffect(uiState) {
        if (tempName.isEmpty()) tempName = uiState.name
        if (tempSurname.isEmpty()) tempSurname = uiState.surname
        if (tempWeight.isEmpty()) tempWeight = uiState.weight
        if (tempGoal == "10000" && uiState.goal != "10000") tempGoal = uiState.goal
    }

    Scaffold(
        containerColor = Color(0xFFF4F1EC),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            showExitDialog = true
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Avatar with dynamic initial
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFFF8A00), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tempName.take(1).uppercase().ifEmpty { "U" },
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- INPUT FIELDS ---

            EditField(
                label = "Name",
                value = tempName,
                onValueChange = { tempName = it },
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditField(
                label = "Surname",
                value = tempSurname,
                onValueChange = { tempSurname = it },
                icon = Icons.Default.Badge
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditField(
                label = "Weight (kg)",
                value = tempWeight,
                onValueChange = { tempWeight = it },
                icon = Icons.Default.Scale,
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(16.dp))

            EditField(
                label = "Daily Step Goal",
                value = tempGoal,
                onValueChange = { tempGoal = it },
                icon = Icons.Default.Flag,
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- SAVE BUTTON ---
            Button(
                onClick = {
                    viewModel.updateFullProfile(tempName, tempSurname, tempWeight, tempGoal) {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A00)),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Confirmation Dialog for unsaved changes
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. \nAre you sure you want to go back? \nYour changes will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.popBackStack()
                }) {
                    Text("Discard", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Keep Editing", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFFFF8A00)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFF8A00),
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            focusedLabelColor = Color(0xFFFF8A00),
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White
        )
    )
}