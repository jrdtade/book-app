package com.folio.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.folio.reader.ui.screens.CoverPickerScreen
import com.folio.reader.ui.screens.DetailScreen
import com.folio.reader.ui.screens.HomeScreen
import com.folio.reader.ui.screens.LibraryScreen
import com.folio.reader.ui.screens.ReaderScreen
import com.folio.reader.ui.screens.SettingsScreen
import com.folio.reader.ui.screens.StatsScreen
import com.folio.reader.ui.theme.FolioTheme

sealed class Tab(val route: String, val label: String) {
    data object Reading : Tab("home", "Reading")
    data object Library : Tab("library", "Library")
    data object Stats : Tab("stats", "Stats")
    data object Settings : Tab("settings", "Settings")
}

private val tabs = listOf(Tab.Reading, Tab.Library, Tab.Stats, Tab.Settings)

class MainActivity : ComponentActivity() {
    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FolioTheme {
                FolioAppRoot()
            }
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun FolioAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabBar = currentRoute == null || tabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showTabBar) {
                NavigationBar {
                    val current = navController.currentBackStackEntry?.destination
                    tabs.forEach { tab ->
                        val selected = current?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    when (tab) {
                                        Tab.Reading -> Icons.Filled.Home
                                        Tab.Library -> Icons.Outlined.LibraryBooks
                                        Tab.Stats -> Icons.Filled.Equalizer
                                        Tab.Settings -> Icons.Filled.Settings
                                    },
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Reading.route,
            modifier = Modifier.padding(if (showTabBar) padding else PaddingValues(0.dp)),
        ) {
            composable(Tab.Reading.route) {
                HomeScreen(
                    openBook = { id -> navController.navigate("detail/$id") },
                    openReader = { id -> navController.navigate("reader/$id") },
                    goToTab = { route -> navController.navigate(route) },
                )
            }
            composable(Tab.Library.route) {
                LibraryScreen(openBook = { id -> navController.navigate("detail/$id") })
            }
            composable(Tab.Stats.route) { StatsScreen() }
            composable(Tab.Settings.route) { SettingsScreen() }
            composable(
                "detail/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("bookId") ?: return@composable
                DetailScreen(
                    bookId = id,
                    back = { navController.popBackStack() },
                    openReader = { navController.navigate("reader/$id") },
                    pickCover = { navController.navigate("cover-picker/$id") },
                )
            }
            composable(
                "cover-picker/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("bookId") ?: return@composable
                CoverPickerScreen(bookId = id, back = { navController.popBackStack() })
            }
            composable(
                "reader/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("bookId") ?: return@composable
                ReaderScreen(bookId = id, back = { navController.popBackStack() })
            }
        }
    }
}
