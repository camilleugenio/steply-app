package com.camille.steply.pages

import android.app.Application
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.camille.steply.viewmodel.HomeViewModel
import com.camille.steply.viewmodel.HomeVmFactory



@Composable
fun MainNavGraph() {
    val navController = rememberNavController()

    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeVmFactory(context.applicationContext as Application)
    )

    NavHost(
        navController = navController,
        startDestination = Routes.STEPS,
        enterTransition = { fadeIn(animationSpec = tween(40)) },
        exitTransition = { fadeOut(animationSpec = tween(40)) },
        popEnterTransition = { fadeIn(animationSpec = tween(40)) },
        popExitTransition = { fadeOut(animationSpec = tween(40)) }

    ) {
        composable(Routes.STEPS) { Home(navController = navController, homeViewModel = homeViewModel) }
        composable(Routes.ACTIVITY) { ActivityScreen(navController = navController, homeViewModel = homeViewModel) }
    }

}
