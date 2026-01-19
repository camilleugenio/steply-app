package com.camille.steply.pages

import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val context = LocalContext.current

    // --- LOCAL STATES ---
    var tempName by remember { mutableStateOf(uiState.name) }
    var tempSurname by remember { mutableStateOf(uiState.surname) }
    var tempWeight by remember { mutableStateOf(uiState.weight) }
    var tempGoal by remember { mutableStateOf(uiState.goal) }

    // Dialog states and password visibility
    var showExitDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Check for unsaved changes
    val hasUnsavedChanges = remember(tempName, tempSurname, tempWeight, tempGoal, uiState) {
        tempName != uiState.name ||
                tempSurname != uiState.surname ||
                tempWeight != uiState.weight ||
                tempGoal != uiState.goal
    }

    // Handle physical back button
    BackHandler(enabled = hasUnsavedChanges) {
        showExitDialog = true
    }

    // Initial data synchronization
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
                        if (hasUnsavedChanges) showExitDialog = true
                        else navController.popBackStack()
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

            // Avatar
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

            // --- EDIT FIELDS ---
            EditField("Name", tempName, { tempName = it }, Icons.Default.Person)
            Spacer(modifier = Modifier.height(16.dp))
            EditField("Surname", tempSurname, { tempSurname = it }, Icons.Default.Badge)
            Spacer(modifier = Modifier.height(16.dp))
            EditField("Weight (kg)", tempWeight, { tempWeight = it }, Icons.Default.Scale, KeyboardType.Number)
            Spacer(modifier = Modifier.height(16.dp))
            EditField("Daily Step Goal", tempGoal, { tempGoal = it }, Icons.Default.Flag, KeyboardType.Number)

            Spacer(modifier = Modifier.height(24.dp))

            // --- PASSWORD BUTTON ---
            TextButton(
                onClick = { showPasswordDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = Color(0xFFFF8A00), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Password", color = Color(0xFFFF8A00), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- SAVE BUTTON ---
            Button(
                onClick = {
                    viewModel.updateFullProfile(tempName, tempSurname, tempWeight, tempGoal) {
                        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
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
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- CHANGE PASSWORD DIALOG ---
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                passwordVisible = false
            },
            title = { Text("Change Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a new password (at least 6 characters).", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = null, tint = Color(0xFFFF8A00))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length >= 6) {
                            viewModel.changePassword(newPassword,
                                onSuccess = {
                                    showPasswordDialog = false
                                    newPassword = ""
                                    passwordVisible = false
                                    Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Minimum 6 characters!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A00))
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    passwordVisible = false
                }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // --- UNSAVED CHANGES DIALOG ---
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. Are you sure you want to exit? Your progress will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.popBackStack()
                }) {
                    Text("Exit", color = Color.Red, fontWeight = FontWeight.Bold)
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