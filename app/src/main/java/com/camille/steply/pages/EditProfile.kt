package com.camille.steply.pages

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.camille.steply.R
import com.camille.steply.viewmodel.ProfileViewModel
import com.camille.steply.viewmodel.WeightViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfile(navController: NavHostController) {
    val viewModel: ProfileViewModel = viewModel()
    val weightViewModel: WeightViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // --- LOCAL STATES ---
    var tempName by remember { mutableStateOf(uiState.name) }
    var tempSurname by remember { mutableStateOf(uiState.surname) }
    var tempWeight by remember { mutableStateOf(uiState.weight) }
    var tempGoal by remember { mutableStateOf(uiState.goal) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var showExitDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { tempPhotoUri = it }
    }

    val hasUnsavedChanges = remember(tempName, tempSurname, tempWeight, tempGoal, tempPhotoUri, uiState) {
        tempName != uiState.name ||
                tempSurname != uiState.surname ||
                tempWeight != uiState.weight ||
                tempGoal != uiState.goal ||
                tempPhotoUri != null
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showExitDialog = true
    }

    LaunchedEffect(uiState) {
        tempName = uiState.name
        tempSurname = uiState.surname
        tempWeight = uiState.weight
        tempGoal = uiState.goal
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

            // --- AVATAR SECTION ---
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(2.dp, Color.LightGray),
                    shadowElevation = 4.dp
                ) {
                    val hasPhoto = tempPhotoUri != null || uiState.profilePhotoUri != null

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(tempPhotoUri ?: uiState.profilePhotoUri ?: R.drawable.ic_profile_placeholder)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .padding(if (hasPhoto) 0.dp else 25.dp),
                        contentScale = if (hasPhoto) ContentScale.Crop else ContentScale.Fit
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            EditField("Name", tempName, { tempName = it }, Icons.Default.Person)
            Spacer(modifier = Modifier.height(16.dp))
            EditField("Surname", tempSurname, { tempSurname = it }, Icons.Default.Badge)
            Spacer(modifier = Modifier.height(16.dp))
            EditField("Weight (kg)", tempWeight, { tempWeight = it }, Icons.Default.Scale, KeyboardType.Number)
            Spacer(modifier = Modifier.height(16.dp))
            EditField("Daily Step Goal", tempGoal, { tempGoal = it }, Icons.Default.Flag, KeyboardType.Number)

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { showPasswordDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = Color(0xFFFF8A00), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Password", color = Color(0xFFFF8A00), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- SAVE BUTTON LOGIC ---
            Button(
                onClick = {
                    val onComplete = {
                        viewModel.updateFullProfile(tempName, tempSurname, tempWeight, tempGoal) {
                            weightViewModel.addWeightEntry(tempWeight) {
                                Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                    }

                    if (tempPhotoUri != null) {
                        viewModel.uploadProfilePicture(tempPhotoUri!!) { onComplete() }
                    } else {
                        onComplete()
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

    // --- DIALOGS (Password) ---
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                currentPassword = ""
                newPassword = ""
                passwordVisible = false
            },
            title = { Text("Change Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = currentPassword.isNotBlank() && newPassword.length >= 6,
                    onClick = {
                        viewModel.changePassword(
                            currentPassword = currentPassword,
                            newPassword = newPassword,
                            onSuccess = {
                                Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                                showPasswordDialog = false
                                currentPassword = ""
                                newPassword = ""
                                passwordVisible = false
                            },
                            onError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    currentPassword = ""
                    newPassword = ""
                    passwordVisible = false
                }) { Text("Cancel") }
            }
        )
    }

    // --- DIALOGS (Exit) ---
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. Exit anyway?") },
            confirmButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Exit", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Stay") }
            }
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