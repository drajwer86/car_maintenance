package com.example.car_maintenance.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.car_maintenance.ui.screens.*
import com.example.car_maintenance.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddCar : Screen("add_car")
    object EditCar : Screen("edit_car/{carId}") {
        fun createRoute(carId: Long) = "edit_car/$carId"
    }
    object AddActivity : Screen("add_activity")
    object EditActivity : Screen("edit_activity/{activityId}") {  // Add this
        fun createRoute(activityId: Long) = "edit_activity/$activityId"
    }
    object ActivityDetail : Screen("activity_detail/{activityId}") {
        fun createRoute(activityId: Long) = "activity_detail/$activityId"
    }
    object Cars : Screen("cars")
    object Reports : Screen("reports")
    object Reminders : Screen("reminders")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAddActivity = { navController.navigate(Screen.AddActivity.route) },
                onNavigateToActivityDetail = { activityId ->
                    navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                },
                onNavigateToCars = { navController.navigate(Screen.Cars.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        
        composable(Screen.AddCar.route) {
            AddCarScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.EditCar.route,
            arguments = listOf(navArgument("carId") { type = NavType.LongType })
        ) { backStackEntry ->
            val carId = backStackEntry.arguments?.getLong("carId") ?: 0L
            EditCarScreen(
                viewModel = viewModel,
                carId = carId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.AddActivity.route) {
            AddActivityScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Add this new route
        composable(
            route = Screen.EditActivity.route,
            arguments = listOf(navArgument("activityId") { type = NavType.LongType })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getLong("activityId") ?: 0L
            EditActivityScreen(
                viewModel = viewModel,
                activityId = activityId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.ActivityDetail.route,
            arguments = listOf(navArgument("activityId") { type = NavType.LongType })
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getLong("activityId") ?: 0L
            ActivityDetailScreen(
                viewModel = viewModel,
                activityId = activityId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Screen.EditActivity.createRoute(activityId)) }
            )
        }
        
        composable(Screen.Cars.route) {
            CarsScreen(
                viewModel = viewModel,
                onNavigateToAddCar = { navController.navigate(Screen.AddCar.route) },
                onNavigateToEditCar = { carId ->
                    navController.navigate(Screen.EditCar.createRoute(carId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Reports.route) {
            ReportsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Reminders.route) {
            RemindersScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}