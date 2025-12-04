package com.camille.steply.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.camille.steply.viewmodel.HomeViewModel


@Composable
fun Home(
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState = homeViewModel.uiState.collectAsState()

    Scaffold(
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Passi: ${uiState.value.steps}",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { homeViewModel.addTestStep() }) {
                    Text("Aggiungi passo (test)")
                }

                OutlinedButton(onClick = { homeViewModel.resetSteps() }) {
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (uiState.value.isTracking) homeViewModel.stopTracking()
                    else homeViewModel.startTracking()
                }
            ) {
                Text(
                    if (uiState.value.isTracking) "Ferma tracciamento"
                    else "Avvia tracciamento"
                )
            }
        }
    }
}