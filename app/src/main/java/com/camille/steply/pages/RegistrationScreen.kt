package com.camille.steply.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import com.camille.steply.viewmodel.ProfileViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun RegistrationScreen(navController: NavHostController) {
    val viewModel: ProfileViewModel = viewModel()
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
            val stepProgress = if (targetStep > 0) targetStep / 3f else 0f

            when (targetStep) {
                0 -> StepIntro(
                    navController = navController, //
                    onNext = { currentStep = 1 }
                )
                1 -> StepAuth(
                    progress = stepProgress,
                    email = email,
                    onEmailChange = {
                        email = it
                        viewModel.updateMessage(null)
                                    },
                    password = password, onPasswordChange = { password = it },
                    isChecking = uiState.isSaving, // Passiamo lo stato di caricamento
                    uiStateMessage = uiState.message, // Passiamo l'errore
                    onNext = {
                        // Invece di fare currentStep = 2, controlliamo prima l'email
                        viewModel.checkEmailAndNext(email) {
                            currentStep = 2
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
                2 -> StepBio(
                    progress = stepProgress,
                    name = name, onNameChange = { name = it },
                    surname = surname, onSurnameChange = { surname = it },
                    username = chosenUsername, onUsernameChange = { chosenUsername = it },
                    onNext = { currentStep = 3 },
                    onBack = { currentStep = 1 }
                )
                3 -> StepHealth(
                    progress = stepProgress,
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

// --- COMPONENTI CONDIVISI ---

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Start My Journey", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account? ", color = Color.Gray)
                TextButton(onClick = { navController.navigate(Routes.LOGIN) }, // <--- Aggiungi questo!
                    contentPadding = PaddingValues(0.dp)) {
                    Text("Login", fontWeight = FontWeight.Bold, color = Color(0xFFFF8C32))
                }
            }
        }
    }
}

// --- STEP 1: AUTH  ---
@Composable
fun StepAuth(
    progress: Float,
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
    // Validazione base lato client
    val isValid = email.contains("@") && password.length >= 6

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 56.dp, bottom = 40.dp)) {
        SteplyProgressBar(progress)
        Spacer(modifier = Modifier.height(40.dp))

        Column(modifier = Modifier.weight(1.2f)) {
            Text("Secure Your Start", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Create your credentials to save your progress and access your profile anywhere.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
        }

        Column(modifier = Modifier.weight(2.5f)) {
            CustomLabel("Email Address")
            TextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = { Text("example@email.com", color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                isError = uiStateMessage?.contains("email", ignoreCase = true) == true
            )

            // LOGICA ERRORE IMMEDIATO:
            if (uiStateMessage?.contains("email", ignoreCase = true) == true) {
                Text(
                    text = uiStateMessage ?: "",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
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
                enabled = isValid && !isChecking, // Disabilitato se sta controllando
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C32))
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Back", color = Color.Gray) }
        }
    }
}

// --- STEP 2: BIO (NICE TO MEET YOU) ---
@Composable
fun StepBio(
    progress: Float,
    name: String, onNameChange: (String) -> Unit,
    surname: String, onSurnameChange: (String) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    onNext: () -> Unit, onBack: () -> Unit
) {
    // Validazione: nome e username obbligatori
    val isValid = name.isNotEmpty() && username.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 56.dp, bottom = 40.dp)) {
        SteplyProgressBar(progress)
        Spacer(modifier = Modifier.height(40.dp))

        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = "Nice to meet you!",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Let's start with the basics to personalize your journey.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Sezione Input (2.5f di peso per dare spazio ai tre campi)
        Column(modifier = Modifier.weight(2.8f)) {
            CustomLabel("Name")
            TextField(
                value = name, onValueChange = onNameChange,
                placeholder = { Text("Your name", color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomLabel("Surname")
            TextField(
                value = surname, onValueChange = onSurnameChange,
                placeholder = { Text("Your Surname", color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomLabel("Username")
            TextField(
                value = username, onValueChange = onUsernameChange,
                placeholder = { Text("Your unique Steply ID", color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                singleLine = true
            )
        }

        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.Bottom) {
            Button(
                onClick = onNext,
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C32))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Back", color = Color.Gray)
            }
        }
    }
}

// --- STEP 3: HEALTH (ALMOST THERE!) ---
@Composable
fun StepHealth(
    progress: Float,
    weight: String,
    onWeightChange: (String) -> Unit,
    goal: String,
    onGoalChange: (String) -> Unit,
    uiStateMessage: String?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 56.dp, bottom = 40.dp)) {
        SteplyProgressBar(progress)
        Spacer(modifier = Modifier.height(40.dp))

        // Titoli amichevoli
        Column(modifier = Modifier.weight(1f)) {
            Text("Almost there!", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Set your daily goals and let's turn those steps into achievements.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
        }

        // Sezione centrale interattiva
        Column(modifier = Modifier.weight(2.8f), horizontalAlignment = Alignment.CenterHorizontally) {
            CustomLabel("Weight (kg)")
            // TextField scritto per esteso come piace a te
            TextField(
                value = weight,
                onValueChange = onWeightChange,
                placeholder = { Text("e.g. 75", color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            CustomLabel("Daily Step Goal")
            // Il Picker interattivo per i passi
            StepGoalPicker(goal = goal, onGoalChange = onGoalChange)

            Text(
                "Steps per day",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            uiStateMessage?.let { Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp)) }
        }

        // Bottoni in basso
        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.Bottom) {
            Button(
                onClick = onComplete,
                enabled = !isSaving && weight.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C32))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Finish", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
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
    val progress by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(45000, easing = LinearEasing)))
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
fun StepGoalPicker(
    goal: String,
    onGoalChange: (String) -> Unit
) {
    val stepsOptions = remember { (1000..30000 step 500).map { it.toString() } }

    // Cerchiamo l'indice iniziale corretto (es. 10000)
    val initialIndex = remember { stepsOptions.indexOf("10000").coerceAtLeast(0) }

    // Importante: per far sì che l'elemento sia al centro delle linee,
    // l'initialFirstVisibleItemIndex deve essere bilanciato dal contentPadding
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex
    )

    val haptic = LocalHapticFeedback.current
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // CORREZIONE LOGICA: Calcoliamo l'indice basandoci sul primo elemento visibile.
    // Se il padding e le altezze sono impostati bene, il primo elemento visibile
    // è quello "catturato" dalle linee.
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val selectedIndex = listState.firstVisibleItemIndex
        if (selectedIndex in stepsOptions.indices) {
            val selectedValue = stepsOptions[selectedIndex]
            if (selectedValue != goal) {
                onGoalChange(selectedValue)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp), // Altezza fissa per controllare meglio i pesi
        contentAlignment = Alignment.Center
    ) {
        // Linee di selezione: le posizioniamo esattamente sopra l'area dell'elemento centrale
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Divider(color = Color(0xFFFF8C32).copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.width(100.dp))
            Spacer(modifier = Modifier.height(60.dp)) // Questa altezza deve matchare l'altezza del testo + padding
            Divider(color = Color(0xFFFF8C32).copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.width(100.dp))
        }

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Il contentPadding permette di avere spazio vuoto sopra e sotto
            // così che il primo/ultimo elemento possano finire tra le linee
            contentPadding = PaddingValues(vertical = 70.dp)
        ) {
            itemsIndexed(stepsOptions) { index, step ->
                // Verifichiamo se questo indice è quello correntemente visibile in alto
                val isSelected = goal == step

                val scale by animateFloatAsState(if (isSelected) 1.2f else 0.8f, label = "")
                val opacity by animateFloatAsState(if (isSelected) 1f else 0.3f, label = "")

                Box(
                    modifier = Modifier.height(60.dp), // Altezza fissa per ogni riga
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        ),
                        color = if (isSelected) Color(0xFFFF8C32) else Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = opacity
                        }
                    )
                }
            }
        }
    }
}