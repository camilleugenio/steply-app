package com.camille.steply.pages

import android.app.Application
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.camille.steply.viewmodel.HomeViewModel
import com.camille.steply.viewmodel.HomeVmFactory
import com.camille.steply.viewmodel.ProfileViewModel
import com.camille.steply.viewmodel.WorkoutType
import com.google.firebase.auth.FirebaseAuth

private const val MAIN_GRAPH = "main_graph"
private const val ARG_WORKOUT_TYPE = "type"

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    val context = LocalContext.current
    val app: Application = remember(context) {
        (context.applicationContext as? Application)
            ?: (context as? Application)
            ?: throw IllegalStateException("Application not available from context: ${context::class.java.name}")
    }

    // Start at Welcome if not authenticated, else enter the MAIN_GRAPH
    val startRoute = if (auth.currentUser == null) Routes.REGISTRATION else MAIN_GRAPH

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

        // All authenticated screens live under this graph so they can share the same HomeViewModel
        navigation(
            route = MAIN_GRAPH,
            startDestination = Routes.STEPS
        ) {
            composable(Routes.STEPS) { backStackEntry ->
                val parentEntry: NavBackStackEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(MAIN_GRAPH)
                }

                val homeViewModel: HomeViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = HomeVmFactory(app)
                )

                Home(navController = navController, homeViewModel = homeViewModel)
            }

            composable(Routes.ACTIVITY) { backStackEntry ->
                val parentEntry: NavBackStackEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(MAIN_GRAPH)
                }

                val homeViewModel: HomeViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = HomeVmFactory(app)
                )

                ActivityScreen(navController = navController, homeViewModel = homeViewModel)
            }

            composable(Routes.PROFILE) { backStackEntry ->
                val parentEntry: NavBackStackEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(MAIN_GRAPH)
                }

                val homeViewModel: HomeViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = HomeVmFactory(app)
                )

                val profileViewModel: ProfileViewModel = viewModel()

                Profile(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    profileViewModel = profileViewModel
                )
            }

            composable(Routes.EDITPROFILE) {
                EditProfile(navController = navController)
            }

            composable(Routes.WEIGHTHISTORY) {
                WeightHistory(navController = navController)
            }

            composable(
                route = "${Routes.WORKOUT}/{$ARG_WORKOUT_TYPE}",
                arguments = listOf(
                    navArgument(ARG_WORKOUT_TYPE) {
                        type = NavType.StringType
                        defaultValue = WorkoutType.WALK.name
                    }
                )
            ) { backStackEntry ->
                val parentEntry: NavBackStackEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(MAIN_GRAPH)
                }

                val homeViewModel: HomeViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = HomeVmFactory(app)
                )

                val typeStr = backStackEntry.arguments?.getString(ARG_WORKOUT_TYPE)
                    ?: WorkoutType.WALK.name

                val type = runCatching { WorkoutType.valueOf(typeStr) }
                    .getOrElse { WorkoutType.WALK }

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
}
