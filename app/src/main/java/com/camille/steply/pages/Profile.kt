package com.camille.steply.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.camille.steply.viewmodel.ProfileViewModel

@Composable
fun Profile(navController: NavHostController) {
    // Connect to the ViewModel
    val viewModel: ProfileViewModel = viewModel()
    // Observe the StateFlow from the ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Launcher for selecting a profile photo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.onPhotoSelected(uri)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Edit Profile") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Photo Section
            AsyncImage(
                model = uiState.profilePhotoUri ?: "https://via.placeholder.com/150",
                contentDescription = "Profile Photo",
                modifier = Modifier.size(120.dp)
            )

            Button(onClick = { photoPickerLauncher.launch("image/*") }) {
                Text("Change Photo")
            }

            // Input Fields
            OutlinedTextField(
                value = uiState.username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.surname,
                onValueChange = { viewModel.onSurnameChange(it) },
                label = { Text("Surname") },
                modifier = Modifier.fillMaxWidth()
            )

            // Save Button
            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), // Move size here
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp // Optional: reduce stroke for small icons
                    )
                } else {

                    Text("Save Profile")
                }
            }

            // Status Messages (Success/Error)
            uiState.message?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
