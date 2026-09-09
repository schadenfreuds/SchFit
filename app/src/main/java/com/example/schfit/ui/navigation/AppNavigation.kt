package com.example.schfit.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.schfit.ui.screen.ActiveWorkoutScreen
import com.example.schfit.ui.screen.DevOptionsScreen
import com.example.schfit.ui.screen.ExerciseLibraryScreen
import com.example.schfit.ui.screen.HomeScreen
import com.example.schfit.ui.screen.ProfileScreen
import com.example.schfit.ui.screen.SettingsScreen
import com.example.schfit.ui.screen.WeightTrackingScreen
import com.example.schfit.ui.screen.WorkoutScreen
import com.example.schfit.ui.screen.WorkoutSummaryScreen
import com.example.schfit.ui.screen.WorkoutVolumeScreen
import com.example.schfit.ui.viewmodel.DevOptionsViewModel
import com.example.schfit.ui.viewmodel.ExerciseLibraryViewModel
import com.example.schfit.ui.viewmodel.HomeViewModel
import com.example.schfit.ui.viewmodel.ProfileViewModel
import com.example.schfit.ui.viewmodel.ProgressViewModel
import com.example.schfit.ui.viewmodel.SettingsViewModel
import com.example.schfit.ui.viewmodel.ViewModelFactory
import com.example.schfit.ui.viewmodel.WorkoutSessionViewModel
import com.example.schfit.ui.viewmodel.WorkoutViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Workouts : Screen("workouts")
    object ActiveWorkout : Screen("active_workout/{routineId}") {
        fun createRoute(routineId: Long) = "active_workout/$routineId"
    }
    object WorkoutSummary : Screen("workout_summary/{workoutLogId}") {
        fun createRoute(workoutLogId: Long) = "workout_summary/$workoutLogId"
    }
    object WeightTracking : Screen("weight_tracking")
    object WorkoutVolume : Screen("workout_volume")
    object ExerciseLibrary : Screen("exercise_library")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object DevOptions : Screen("dev_options")
}

@Composable
fun AppNavigation(
    viewModelFactory: ViewModelFactory,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { fadeIn(animationSpec = tween(180)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = tween(180)) },
        popExitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToWorkouts = { navController.navigate(Screen.Workouts.route) },
                onNavigateToActiveWorkout = { routineId ->
                    navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
                },
                onNavigateToWeightProgress = { navController.navigate(Screen.WeightTracking.route) },
                onNavigateToExerciseLibrary = { navController.navigate(Screen.ExerciseLibrary.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Workouts.route) {
            val workoutViewModel: WorkoutViewModel = viewModel(factory = viewModelFactory)
            WorkoutScreen(
                viewModel = workoutViewModel,
                onNavigateBack = { navController.popBackStack() },
                onStartWorkout = { routineId ->
                    navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
                },
                onNavigateToWorkoutSummary = { workoutLogId ->
                    navController.navigate(Screen.WorkoutSummary.createRoute(workoutLogId))
                }
            )
        }

        composable(
            route = Screen.ActiveWorkout.route,
            arguments = listOf(
                navArgument("routineId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
            val sessionViewModel: WorkoutSessionViewModel = viewModel(factory = viewModelFactory)
            ActiveWorkoutScreen(
                routineId = routineId,
                viewModel = sessionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onFinishWorkoutNavigation = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.WorkoutSummary.route,
            arguments = listOf(
                navArgument("workoutLogId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val workoutLogId = backStackEntry.arguments?.getLong("workoutLogId") ?: 0L
            val sessionViewModel: WorkoutSessionViewModel = viewModel(factory = viewModelFactory)
            val context = LocalContext.current
            val uiState by sessionViewModel.uiState.collectAsState()

            LaunchedEffect(workoutLogId) {
                if (workoutLogId > 0) {
                    sessionViewModel.loadPastWorkoutSummary(workoutLogId)
                }
            }

            WorkoutSummaryScreen(
                uiState = uiState,
                onShareStory = { userPhoto -> sessionViewModel.shareWorkoutStory(context, userPhoto) },
                onSaveToGallery = { userPhoto -> sessionViewModel.saveWorkoutStoryToGallery(context, userPhoto) },
                onFinishWorkoutNavigation = { navController.popBackStack() }
            )
        }

        composable(Screen.WeightTracking.route) {
            val progressViewModel: ProgressViewModel = viewModel(factory = viewModelFactory)
            WeightTrackingScreen(
                viewModel = progressViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WorkoutVolume.route) {
            val progressViewModel: ProgressViewModel = viewModel(factory = viewModelFactory)
            WorkoutVolumeScreen(
                viewModel = progressViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ExerciseLibrary.route) {
            val exerciseLibraryViewModel: ExerciseLibraryViewModel =
                viewModel(factory = viewModelFactory)
            ExerciseLibraryScreen(
                viewModel = exerciseLibraryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel =
                viewModel(factory = viewModelFactory)
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWeightProgress = { navController.navigate(Screen.WeightTracking.route) }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = viewModelFactory)
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDevOptions = {
                    navController.navigate(Screen.DevOptions.route)
                }
            )
        }

        composable(Screen.DevOptions.route) {
            val devOptionsViewModel: DevOptionsViewModel =
                viewModel(factory = viewModelFactory)
            DevOptionsScreen(
                viewModel = devOptionsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
