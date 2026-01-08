package com.camille.steply.pages

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.camille.steply.pages.Routes

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.STEPS
    ) {
        composable(Routes.STEPS) {
            Home(navController = navController)
        }
        composable(Routes.ACTIVITY) {
            ActivityScreen(navController = navController)
        }
    }
}
