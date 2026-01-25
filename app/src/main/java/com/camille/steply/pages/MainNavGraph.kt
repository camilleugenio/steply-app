package com.camille.steply.pages

import android.app.Application
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.camille.steply.viewmodel.HomeViewModel
import com.camille.steply.viewmodel.ProfileViewModel
import com.camille.steply.viewmodel.HomeVmFactory
import com.camille.steply.viewmodel.WorkoutType
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    // Logica di scelta pagina iniziale
    val startRoute = if(auth.currentUser == null) {
        Routes.LOGIN
    } else {
        Routes.STEPS
    }

    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeVmFactory(context.applicationContext as Application)
    )
    val profileViewModel = ProfileViewModel()

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

    LaunchedEffect(Unit) {
        if (!isEmulator) {
            homeViewModel.ensureTrackingRunning()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = { fadeIn(animationSpec = tween(40)) },
        exitTransition = { fadeOut(animationSpec = tween(40)) },
        popEnterTransition = { fadeIn(animationSpec = tween(40)) },
        popExitTransition = { fadeOut(animationSpec = tween(40)) }
    ) {
        composable(Routes.LOGIN) {
            Login(navController = navController)
        }

        composable(Routes.REGISTRATION) {
            Registration(navController = navController)
        }
        composable(Routes.STEPS) {
            Home(navController = navController, homeViewModel = homeViewModel)
        }

        composable(Routes.ACTIVITY) {
            ActivityScreen(navController = navController, homeViewModel = homeViewModel)
        }

        composable(Routes.PROFILE) {
            Profile(
                navController = navController,
                homeViewModel = homeViewModel,
                profileViewModel = profileViewModel)
        }

        composable(Routes.EDITPROFILE) {
            EditProfile(navController =  navController)
        }

        composable(Routes.WEIGHTHISTORY) {
            WeightHistory(navController)
        }

        composable("${Routes.WORKOUT}/{type}") { backStackEntry ->
            val typeStr = backStackEntry.arguments?.getString("type") ?: WorkoutType.WALK.name
            val type = runCatching { WorkoutType.valueOf(typeStr) }.getOrElse { WorkoutType.WALK }

            WorkoutScreen(
                navController = navController,
                homeViewModel = homeViewModel,
                type = type
            )
        }

        composable(Routes.WORKOUT_REPORT) {
            WorkoutReportScreen(navController = navController)
        }
    }
}
