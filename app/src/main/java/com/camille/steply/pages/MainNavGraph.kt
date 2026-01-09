//package com.camille.steply.pages
//
//import androidx.compose.runtime.Composable
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.camille.steply.pages.Routes
//import androidx.compose.animation.EnterTransition
//import androidx.compose.animation.ExitTransition
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.core.tween
//
//
//
//@Composable
//fun MainNavGraph() {
//    val navController = rememberNavController()
//
//    NavHost(
//        navController = navController,
//        startDestination = Routes.STEPS,
//        enterTransition = { fadeIn(animationSpec = tween(40)) },
//        exitTransition = { fadeOut(animationSpec = tween(40)) },
//        popEnterTransition = { fadeIn(animationSpec = tween(40)) },
//        popExitTransition = { fadeOut(animationSpec = tween(40)) }
//
//    ) {
//        composable(Routes.STEPS) { Home(navController = navController) }
//        composable(Routes.ACTIVITY) { ActivityScreen(navController = navController) }
//    }
//
//}

package com.camille.steply.pages

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.camille.steply.viewmodel.HomeViewModel
import com.camille.steply.viewmodel.HomeVmFactory

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()

    // ✅ UN SOLO HomeViewModel condiviso tra tutte le schermate
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
        composable(Routes.STEPS) {
            Home(navController = navController, homeViewModel = homeViewModel)
        }
        composable(Routes.ACTIVITY) {
            ActivityScreen(navController = navController, homeViewModel = homeViewModel)
        }
    }
}
