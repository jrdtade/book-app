package com.folio.reader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
private const val TABS_ROUTE = "tabs"

class MainActivity : ComponentActivity() {
    var pendingImportUri by mutableStateOf<Uri?>(null)

    @androidx.compose.material3.ExperimentalMaterial3Api
    @androidx.compose.foundation.ExperimentalFoundationApi
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
@androidx.compose.foundation.ExperimentalFoundationApi
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
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    fun navigateToTab(route: String) {
        if (tabs.any { it.route == route }) {
            val index = tabs.indexOfFirst { it.route == route }
            navController.navigate(TABS_ROUTE) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            scope.launch { pagerState.animateScrollToPage(index) }
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(pendingImportUri) {
        val uri = pendingImportUri ?: return@LaunchedEffect
        libraryViewModel.importEpub(uri) { navigateToTab(Tab.Library.route) }
        onPendingImportConsumed()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabBar = currentRoute == null || currentRoute == TABS_ROUTE

    Scaffold(
        bottomBar = {
            if (showTabBar) {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        val selected = currentRoute == TABS_ROUTE && pagerState.currentPage == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != TABS_ROUTE) navigateToTab(tab.route)
                                else scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            icon = {
                                Icon(
                                    when (tab) {
                                        Tab.Reading -> Icons.Filled.Home
                                        Tab.Library -> Icons.Outlined.LibraryBooks
                                        Tab.Stats -> Icons.Filled.Equalizer
                                        else -> Icons.Filled.Home
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
            startDestination = TABS_ROUTE,
            modifier = Modifier.padding(if (showTabBar) padding else PaddingValues(0.dp)),
        ) {
            composable(TABS_ROUTE) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        .coerceIn(-1f, 1f)
                    val absOffset = if (offset < 0) -offset else offset
                    val scale = 1f - (absOffset * 0.08f)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                alpha = 1f - (absOffset * 0.5f),
                                scaleX = scale,
                                scaleY = scale,
                            ),
                    ) {
                        when (tabs[page]) {
                            Tab.Reading -> HomeScreen(
                                openBook = { id -> navController.navigate("detail/$id") },
                                openReader = { id -> navController.navigate("reader/$id") },
                                goToTab = { route -> navigateToTab(route) },
                                openProfile = { navigateToTab(Tab.Settings.route) },
                            )
                            Tab.Library -> LibraryScreen(openBook = { id -> navController.navigate("detail/$id") })
                            Tab.Stats -> StatsScreen()
                            else -> Unit
                        }
                    }
                }
            }
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
