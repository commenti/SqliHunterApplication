package com.yourapp.sqliautohunter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yourapp.sqliautohunter.ui.screens.dashboard.LiveDashboardScreen
import com.yourapp.sqliautohunter.ui.screens.keywordinput.KeywordInputScreen
import com.yourapp.sqliautohunter.ui.screens.logs.CrashLogScreen
import com.yourapp.sqliautohunter.ui.screens.manualtest.ManualUrlTestScreen
import com.yourapp.sqliautohunter.ui.screens.results.ResultDetailScreen
import com.yourapp.sqliautohunter.ui.screens.results.ResultsScreen
import com.yourapp.sqliautohunter.ui.screens.settings.SettingsScreen

object AppNavGraph {
    const val ROUTE_KEYWORD_INPUT = "keyword_input"
    const val ROUTE_MANUAL_TEST = "manual_test"
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_RESULTS = "results"
    const val ROUTE_RESULT_DETAIL = "result_detail"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_LOGS = "logs"

    const val ARG_RESULT_ID = "result_id"

    fun setup(navController: NavHostController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Handle navigation changes if needed
        }
    }
}

@Composable
fun SetupNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppNavGraph.ROUTE_KEYWORD_INPUT
    ) {
        composable(AppNavGraph.ROUTE_KEYWORD_INPUT) {
            KeywordInputScreen(
                onNavigateToDashboard = {
                    navController.navigate(AppNavGraph.ROUTE_DASHBOARD) {
                        popUpTo(AppNavGraph.ROUTE_KEYWORD_INPUT)
                    }
                },
                onNavigateToManualTest = {
                    navController.navigate(AppNavGraph.ROUTE_MANUAL_TEST)
                },
                onNavigateToResults = {
                    navController.navigate(AppNavGraph.ROUTE_RESULTS)
                },
                onNavigateToSettings = {
                    navController.navigate(AppNavGraph.ROUTE_SETTINGS)
                }
            )
        }

        composable(AppNavGraph.ROUTE_MANUAL_TEST) {
            ManualUrlTestScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToResults = {
                    navController.navigate(AppNavGraph.ROUTE_RESULTS) {
                        popUpTo(AppNavGraph.ROUTE_MANUAL_TEST)
                    }
                }
            )
        }

        composable(AppNavGraph.ROUTE_DASHBOARD) {
            LiveDashboardScreen(
                onNavigateToResults = {
                    navController.navigate(AppNavGraph.ROUTE_RESULTS)
                },
                onNavigateToSettings = {
                    navController.navigate(AppNavGraph.ROUTE_SETTINGS)
                },
                onNavigateToLogs = {
                    navController.navigate(AppNavGraph.ROUTE_LOGS)
                }
            )
        }

        composable(AppNavGraph.ROUTE_RESULTS) {
            ResultsScreen(
                onNavigateToDetail = { resultId ->
                    navController.navigate("${AppNavGraph.ROUTE_RESULT_DETAIL}/$resultId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(AppNavGraph.ROUTE_SETTINGS)
                }
            )
        }

        composable("${AppNavGraph.ROUTE_RESULT_DETAIL}/{${AppNavGraph.ARG_RESULT_ID}}") { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString(AppNavGraph.ARG_RESULT_ID)?.toLongOrNull() ?: 0L
            ResultDetailScreen(
                resultId = resultId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppNavGraph.ROUTE_SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogs = {
                    navController.navigate(AppNavGraph.ROUTE_LOGS)
                }
            )
        }

        composable(AppNavGraph.ROUTE_LOGS) {
            CrashLogScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

fun setupNavigation(navController: NavHostController) {}
