package com.brailly.friendly.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.brailly.friendly.ui.screen.home.HomeScreen
import com.brailly.friendly.ui.screen.learn.LearnScreen
import com.brailly.friendly.ui.screen.learn.PathwayScreen
import com.brailly.friendly.ui.screen.profile.ProfileScreen
import com.brailly.friendly.ui.screen.scan.ScanHistoryScreen
import com.brailly.friendly.ui.screen.scan.ScanScreen
import com.brailly.friendly.ui.screen.translate.TranslateScreen

@Composable
fun BraillyApp(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }
    val backStack = rememberNavBackStack(HomeRoute)

    val openTab: (BottomTab) -> Unit = { tab ->
        selectedTab = tab
        // Reset ke root tab yang dipilih (single back stack sederhana untuk MVP)
        backStack.clear()
        backStack.add(tab.route)
    }

    Scaffold(
        bottomBar = {
            BraillyBottomBar(
                selected = selectedTab,
                onSelect = openTab,
            )
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                entry<HomeRoute> {
                    HomeScreen(
                        onNavigate = openTab,
                        // Tab Belajar dibuka lebih dulu, lalu Jalur Kuis
                        // ditumpuk di atasnya. Dengan begitu bilah bawah
                        // menyorot tab yang benar, dan "kembali" dari kuis
                        // mendarat di kamus seperti bila dibuka dari sana.
                        onOpenQuiz = {
                            openTab(BottomTab.LEARN)
                            backStack.add(LearnQuizRoute)
                        },
                    )
                }
                entry<TranslateRoute> { TranslateScreen() }
                entry<ScanRoute> {
                    ScanScreen(
                        onOpenHistory = { backStack.add(ScanHistoryRoute) },
                    )
                }
                entry<ScanHistoryRoute> {
                    ScanHistoryScreen(
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<LearnRoute> {
                    LearnScreen(
                        onOpenQuiz = { backStack.add(LearnQuizRoute) },
                    )
                }
                entry<LearnQuizRoute> {
                    PathwayScreen(
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<ProfileRoute> {
                    ProfileScreen(darkTheme = darkTheme, onToggleTheme = onToggleTheme)
                }
            },
        )
    }
}
