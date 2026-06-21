package com.folio.reader

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.folio.reader.ui.LibraryViewModel
import com.folio.reader.ui.UserViewModel
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.screens.CoverPickerScreen
import com.folio.reader.ui.screens.DetailScreen
import com.folio.reader.ui.screens.HomeScreen
import com.folio.reader.ui.screens.LibraryScreen
import com.folio.reader.ui.screens.OnboardingScreen
import com.folio.reader.ui.screens.ReaderScreen
import com.folio.reader.ui.screens.SettingsScreen
import com.folio.reader.ui.screens.StatsScreen
import com.folio.reader.ui.theme.FolioTheme

sealed class Tab(val route: String, val label: String) {
    data object Reading : Tab("home", "Home")
    data object Library : Tab("library", "Library")
    data object Stats : Tab("stats", "Stats")
    data object Settings : Tab("settings", "Settings")
}

private val tabs = listOf(Tab.Reading, Tab.Library, Tab.Stats)

class MainActivity : ComponentActivity() {
    var pendingImportUri by mutableStateOf<Uri?>(null)

    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingImportUri = importUriFromIntent(intent)
        setContent {
            FolioTheme {
                FolioAppRoot(
                    pendingImportUri = pendingImportUri,
                    onPendingImportConsumed = { pendingImportUri = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importUriFromIntent(intent)?.let { pendingImportUri = it }
    }

    /** A book opened from another app (e.g. a WhatsApp attachment) arrives either as
     *  ACTION_VIEW with the file as the intent data, or ACTION_SEND with it as EXTRA_STREAM. */
    private fun importUriFromIntent(intent: Intent?): Uri? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        else -> null
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun FolioAppRoot(
    pendingImportUri: Uri? = null,
    onPendingImportConsumed: () -> Unit = {},
) {
    val userViewModel: UserViewModel = folioViewModel()
    val userPrefs by userViewModel.prefs.collectAsState()
    var onboardingDone by remember { mutableStateOf(false) }

    val prefs = userPrefs ?: return // still loading from DataStore
    if (!prefs.onboardingComplete && !onboardingDone) {
        OnboardingScreen(onDone = { onboardingDone = true })
        return
    }

    val navController = rememberNavController()
    val libraryViewModel: LibraryViewModel = folioViewModel()

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(pendingImportUri) {
        val uri = pendingImportUri ?: return@LaunchedEffect
        libraryViewModel.importEpub(uri) { navigateToTab(Tab.Library.route) }
        onPendingImportConsumed()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabBar = currentRoute == null || tabs.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (showTabBar) {
                TopAppBar(
                    title = {},
                    actions = {
                        IconButton(onClick = { navigateToTab(Tab.Settings.route) }) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Profile & settings")
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showTabBar) {
                NavigationBar {
                    val current = navController.currentBackStackEntry?.destination
                    tabs.forEach { tab ->
                        val selected = current?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    when (tab) {
                                        Tab.Reading -> Icons.Filled.Home
                                        Tab.Library -> Icons.Outlined.LibraryBooks
                                        Tab.Stats -> Icons.Filled.Equalizer
                                        Tab.Settings -> Icons.Filled.AccountCircle
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
                    goToTab = { route -> navigateToTab(route) },
                )
            }
            composable(Tab.Library.route) {
                LibraryScreen(openBook = { id -> navController.navigate("detail/$id") })
            }
            composable(Tab.Stats.route) { StatsScreen() }
            composable(Tab.Settings.route) { SettingsScreen(back = { navController.popBackStack() }) }
            composable(
                "detail/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("bookId") ?: return@composable
                DetailScreen(
                    bookId = id,
                    back = { navController.popBackStack() },
                    openReader = { navController.navigate("reader/$id") },
                    openCoverPicker = { navController.navigate("cover_picker/$id") },
                )
            }
            composable(
                "cover_picker/{bookId}",
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
