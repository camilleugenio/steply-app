package com.camille.steply.pages

import android.app.Application
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.camille.steply.viewmodel.HomeViewModel
import com.camille.steply.viewmodel.HomeVmFactory
import com.camille.steply.viewmodel.WorkoutType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.camille.steply.pages.Profile


@Composable
fun MainNavGraph() {
    val navController = rememberNavController()

    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeVmFactory(context.applicationContext as Application)
    )

    // ✅ Detect emulator once
    val isEmulator = remember {
        val fp = android.os.Build.FINGERPRINT.lowercase()
        val model = android.os.Build.MODEL.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        val device = android.os.Build.DEVICE.lowercase()
        fp.contains("generic") ||
                fp.contains("emulator") ||
                model.contains("emulator") ||
                model.contains("sdk") ||
                brand.contains("generic") ||
                device.contains("generic")
    }

    // ✅ Start simulator ONCE for the whole app (emulator only)
    DisposableEffect(Unit) {
        if (isEmulator) homeViewModel.startAccelerometerSimulation()
        onDispose {
            if (isEmulator) homeViewModel.stopAccelerometerSimulation()
        }
    }

    // ✅ Start/stop the real foreground service based on stored toggle
    val trackingEnabled by homeViewModel.trackingEnabled.collectAsState(initial = false)
    LaunchedEffect(trackingEnabled, isEmulator) {
        if (!isEmulator) {
            if (trackingEnabled) homeViewModel.startStepUpdates()
            else homeViewModel.stopStepUpdates()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.STEPS,
        enterTransition = { fadeIn(animationSpec = tween(40)) },
        exitTransition = { fadeOut(animationSpec = tween(40)) },
        popEnterTransition = { fadeIn(animationSpec = tween(40)) },
        popExitTransition = { fadeOut(animationSpec = tween(40)) }
    ) {
        composable(Routes.STEPS) {
            Home(navController = navController, homeViewModel = homeViewModel)
        }

        composable(Routes.ACTIVITY) {
            ActivityScreen(navController = navController, homeViewModel = homeViewModel)
        }

        composable(Routes.PROFILE) {
            Profile(navController = navController)
        }

        // ✅ NUOVA WORKOUT SCREEN
        composable("${Routes.WORKOUT}/{type}") { backStackEntry ->
            val typeStr = backStackEntry.arguments?.getString("type") ?: WorkoutType.WALK.name
            val type = runCatching { WorkoutType.valueOf(typeStr) }.getOrElse { WorkoutType.WALK }

            WorkoutScreen(
                navController = navController,
                homeViewModel = homeViewModel,
                type = type
            )
        }
    }
}
