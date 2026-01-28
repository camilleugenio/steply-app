package com.camille.steply.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.camille.steply.viewmodel.AuthViewModel

@Composable
fun Registration(navController: NavHostController) {
    val viewModel: AuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableIntStateOf(0) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var chosenUsername by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("10000") }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF7F2EE), Color(0xFFE8F3F1))
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        val progress by remember(currentStep) {
            derivedStateOf { currentStep / 3f }
        }

        AnimatedVisibility(
            visible = currentStep > 0,
            enter = fadeIn(animationSpec = tween(1000)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(1000)) + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 80.dp)
            ) {
                SteplyProgressBar(progress)
            }
        }

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = tween(400)) { it } + fadeIn())
                        .togetherWith(slideOutHorizontally(animationSpec = tween(400)) { -it } + fadeOut())
                } else {
                    (slideInHorizontally(animationSpec = tween(400)) { -it } + fadeIn())
                        .togetherWith(slideOutHorizontally(animationSpec = tween(400)) { it } + fadeOut())
                }
            },
            label = "stepTransition"
        ) { targetStep ->
            when (targetStep) {
                0 -> StepIntro(
                    navController = navController,
                    onNext = { currentStep = 1 }
                )
                1 -> StepAuth(
                    email = email,
                    onEmailChange = {
                        email = it
                        viewModel.updateMessage(null)
                    },
                    password = password,
                    onPasswordChange = { password = it },
                    isChecking = uiState.isSaving,
                    uiStateMessage = uiState.message,
                    onNext = {
                        viewModel.checkEmailAndNext(email) { currentStep = 2 }
                    },
                    onBack = { currentStep = 0 }
                )
                2 -> StepBio(
                    name = name, onNameChange = { name = it },
                    surname = surname, onSurnameChange = { surname = it },
                    username = chosenUsername, onUsernameChange = { chosenUsername = it },
                    onNext = { currentStep = 3 },
                    onBack = { currentStep = 1 }
                )
                3 -> StepHealth(
                    weight = weight, onWeightChange = { weight = it },
                    goal = goal, onGoalChange = { goal = it },
                    uiStateMessage = uiState.message,
                    isSaving = uiState.isSaving,
                    onBack = { currentStep = 2 },
                    onComplete = {
                        viewModel.registerUser(name, surname, chosenUsername, email, password, weight, goal,
                            onSuccess = { navController.navigate("steps") { popUpTo("registration") { inclusive = true } } },
                            onError = { viewModel.updateMessage(it) }
                        )
                    }
                )
            }
        }
    }
}


@Composable
fun CustomLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        color = Color.Black.copy(alpha = 0.8f)
    )
}

@Composable
fun SteplyProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(600, easing = FastOutSlowInEasing), label = "")
    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
        color = Color(0xFFFF8C32),
        trackColor = Color.White.copy(alpha = 0.4f),
        strokeCap = StrokeCap.Round
    )
}

// --- STEP 0: INTRO ---
@Composable
fun StepIntro(navController: NavHostController, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(60.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("Welcome to", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
            Text("Steply", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
        }
        Box(modifier = Modifier.weight(2f).fillMaxWidth(), contentAlignment = Alignment.Center) { InfiniteEmojiRail() }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.8f), verticalArrangement = Arrangement.Bottom) {
            Text("Your 10,000 steps\njourney starts today.", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C32)),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text("Start My Journey", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account? ", color = Color.Gray)
                TextButton(onClick = { navController.navigate("login") },
                    contentPadding = PaddingValues(0.dp)) {
                    Text("Login", fontWeight = FontWeight.Bold, color = Color(0xFFFF8C32))
                }
            }
        }
    }
}

// --- STEP 1: AUTH ---
@Composable
fun StepAuth(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isChecking: Boolean,
    uiStateMessage: String?,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isValid = email.contains("@") && password.length >= 6

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Spacer(modifier = Modifier.height(110.dp))

        Column(modifier = Modifier.weight(1.2f)) {
            Text("Secure Your Start", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Create your credentials to save your progress.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
        }

        Column(modifier = Modifier.weight(2.5f)) {
            CustomLabel("Email Address")
            TextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = { Text("example@email.com", color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                singleLine = true,
                isError = uiStateMessage?.contains("email", ignoreCase = true) == true
            )

            if (uiStateMessage?.contains("email", ignoreCase = true) == true) {
                Text(text = uiStateMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            CustomLabel("Password")
            TextField(
                value = password, onValueChange = onPasswordChange,
                placeholder = { Text("At least 6 characters", color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                    }
                },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                singleLine = true
            )
        }

        Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.Bottom) {
            Button(
                onClick = onNext,
                enabled = isValid && !isChecking,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C32))
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Back", color = Color.Gray)
            }
        }
    }
}

// --- STEP 2: BIO ---
@Composable
fun StepBio(
    name: String, onNameChange: (String) -> Unit,
    surname: String, onSurnameChange: (String) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    onNext: () -> Unit, onBack: () -> Unit
) {
    val isValid = name.isNotEmpty() && username.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Spacer(modifier = Modifier.height(110.dp))

        Column(modifier = Modifier.weight(1.2f)) {
            Text("Nice to meet you!", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Let's start with the basics.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
        }

        Column(modifier = Modifier.weight(2.8f)) {
            CustomLabel("Name")
            TextField(value = name, onValueChange = onNameChange, placeholder = { Text("Your Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            CustomLabel("Surname")
            TextField(value = surname, onValueChange = onSurnameChange, placeholder = { Text("Your Surname") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            CustomLabel("Username")
            TextField(value = username, onValueChange = onUsernameChange, placeholder = { Text("Unique Steply ID") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), singleLine = true)
        }

        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.Bottom) {
            Button(onClick = onNext, enabled = isValid, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C32))) {
                Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Back", color = Color.Gray)
            }
        }
    }
}

// --- STEP 3: HEALTH ---
@Composable
fun StepHealth(
    weight: String, onWeightChange: (String) -> Unit,
    goal: String, onGoalChange: (String) -> Unit,
    uiStateMessage: String?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Spacer(modifier = Modifier.height(110.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Almost there!", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Set your daily goals.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
        }

        Column(modifier = Modifier.weight(2.8f), horizontalAlignment = Alignment.CenterHorizontally) {
            CustomLabel("Weight (kg)")
            TextField(value = weight, onValueChange = onWeightChange, placeholder = { Text("e.g. 75") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), singleLine = true)
            Spacer(modifier = Modifier.height(32.dp))
            CustomLabel("Daily Step Goal")
            StepGoalPicker(goal = goal, onGoalChange = onGoalChange)
            uiStateMessage?.let { Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp)) }
        }

        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.Bottom) {
            Button(onClick = onComplete, enabled = !isSaving && weight.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C32))) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Finish", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Back", color = Color.Gray)
            }
        }
    }
}

// --- ANIMATION COMPONENTS ---
@Composable
fun InfiniteEmojiRail() {
    val baseIcons = remember { listOf("🚴", "🏃", "🚶", "🏄", "🧘", "🏌️", "⛷️", "🏀", "🚵", "🏊", "🤽", "🧗", "🏋️", "🤸", "🎾", "🏐", "🏸", "⛸️", "🛶", "🏇", "🛹", "⚽", "🏏", "🏓") }
    val row1 = remember { baseIcons.shuffled() }
    val row2 = remember { baseIcons.shuffled() }
    val row3 = remember { baseIcons.shuffled() }
    val progress by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(45000, easing = LinearEasing)), label = "")
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EmojiRow(row1, progress, 0.dp)
        EmojiRow(row2, progress, 45.dp)
        EmojiRow(row3, progress, 15.dp)
    }
}

@Composable
fun EmojiRow(icons: List<String>, progress: Float, paddingStart: androidx.compose.ui.unit.Dp) {
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { (icons.size * 81).dp.toPx() }
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = paddingStart).graphicsLayer { translationX = -(progress * px) }.wrapContentWidth(unbounded = true, align = Alignment.Start)) {
            repeat(3) { icons.forEach { SportCircle(it) } }
        }
    }
}

@Composable
fun SportCircle(e: String) {
    Box(Modifier.padding(8.dp).size(65.dp).clip(CircleShape).background(Color.White.copy(0.6f)), Alignment.Center) { Text(e, fontSize = 28.sp) }
}

@Composable
fun StepGoalPicker(goal: String, onGoalChange: (String) -> Unit) {
    val stepsOptions = remember { (1000..30000 step 500).map { it.toString() } }
    val initialIndex = remember { stepsOptions.indexOf("10000").coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val haptic = LocalHapticFeedback.current
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { selectedIndex ->
                if (selectedIndex in stepsOptions.indices) {
                    val selectedValue = stepsOptions[selectedIndex]
                    if (selectedValue != goal) {
                        onGoalChange(selectedValue)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
    }

    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(color = Color(0xFFFF8C32).copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.width(100.dp))
            Spacer(modifier = Modifier.height(60.dp))
            HorizontalDivider(color = Color(0xFFFF8C32).copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.width(100.dp))
        }
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 70.dp)
        ) {
            itemsIndexed(stepsOptions) { _, step ->
                val isSelected = goal == step
                val scale by animateFloatAsState(if (isSelected) 1.2f else 0.8f, label = "")
                val opacity by animateFloatAsState(if (isSelected) 1f else 0.3f, label = "")
                Box(modifier = Modifier.height(60.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = step,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold),
                        color = if (isSelected) Color(0xFFFF8C32) else Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = opacity }
                    )
                }
            }
        }
    }
}