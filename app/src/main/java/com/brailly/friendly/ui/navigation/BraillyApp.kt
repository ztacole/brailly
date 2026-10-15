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
import com.brailly.friendly.ui.screen.profile.ProfileScreen
import com.brailly.friendly.ui.screen.scan.ScanScreen
import com.brailly.friendly.ui.screen.translate.TranslateScreen

@Composable
fun BraillyApp(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }
    val backStack = rememberNavBackStack(HomeRoute)

    Scaffold(
        bottomBar = {
            BraillyBottomBar(
                selected = selectedTab,
                onSelect = { tab ->
                    selectedTab = tab
                    // Reset ke root tab yang dipilih (single back stack sederhana untuk MVP)
                    backStack.clear()
                    backStack.add(tab.route)
                },
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
                        onNavigate = { tab ->
                            selectedTab = tab
                            backStack.clear()
                            backStack.add(tab.route)
                        },
                    )
                }
                entry<TranslateRoute> { TranslateScreen() }
                entry<ScanRoute> { ScanScreen() }
                entry<LearnRoute> { LearnScreen() }
                entry<ProfileRoute> {
                    ProfileScreen(darkTheme = darkTheme, onToggleTheme = onToggleTheme)
                }
            },
        )
    }
}
