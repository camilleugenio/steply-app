package com.camille.steply.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.camille.steply.viewmodel.AuthViewModel

@Composable
fun Login(navController: NavHostController) {
    val viewModel: AuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF7F2EE), Color(0xFFE8F3F1))
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 40.dp)
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // 1. INTESTAZIONE
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = "Welcome Back!",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Log in to continue your 10,000 steps journey.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }

            // 2. CAMPI DI INPUT
            Column(modifier = Modifier.weight(2.5f)) {
                CustomLabel("Email Address")
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("example@email.com", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFFFF8C32)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                CustomLabel("Password")
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Enter your password", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFFFF8C32)
                    ),
                    singleLine = true
                )

                // Forgot Password
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { /* Azione reset password */ }) {
                        Text("Forgot Password?", color = Color(0xFFFF8C32), fontWeight = FontWeight.Bold)
                    }
                }

                // Messaggio di errore se il login fallisce
                uiState.message?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }
            }

            // 3. BOTTONI E NAVIGAZIONE
            Column(modifier = Modifier.weight(1.3f), verticalArrangement = Arrangement.Bottom) {
                Button(
                    onClick = {
                        viewModel.loginUser(
                            email = email,
                            pass = password,
                            onSuccess = {
                                navController.navigate("steps") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onError = { error -> viewModel.updateMessage(error) }
                        )
                    },
                    enabled = email.isNotEmpty() && password.isNotEmpty() && !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C32))
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Don't have an account? ", color = Color.Gray)
                    TextButton(
                        onClick = { navController.navigate(Routes.REGISTRATION) }, // Usa la costante!
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Sign Up", color = Color(0xFFFF8C32), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}