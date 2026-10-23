package com.brailly.friendly.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.brailly.friendly.ui.component.BraillyIcons
import kotlinx.serialization.Serializable

/** Rute-rute utama (tab bottom navigation). */
@Serializable
data object HomeRoute : NavKey

@Serializable
data object TranslateRoute : NavKey

@Serializable
data object ScanRoute : NavKey

@Serializable
data object LearnRoute : NavKey

/** Sub-rute Belajar: jalur kuis bergaya pathway (dibuka dari tombol di kamus). */
@Serializable
data object LearnQuizRoute : NavKey

@Serializable
data object ProfileRoute : NavKey

/** Item bottom navigation dengan ikon custom + label. */
enum class BottomTab(
    val route: NavKey,
    val label: String,
) {
    HOME(HomeRoute, "Home") {
        override val icon: ImageVector get() = BraillyIcons.Home
    },
    TRANSLATE(TranslateRoute, "Terjemah") {
        override val icon: ImageVector get() = BraillyIcons.Translate
    },
    SCAN(ScanRoute, "Scan") {
        override val icon: ImageVector get() = BraillyIcons.Scan
    },
    LEARN(LearnRoute, "Belajar") {
        override val icon: ImageVector get() = BraillyIcons.Learn
    },
    PROFILE(ProfileRoute, "Profil") {
        override val icon: ImageVector get() = BraillyIcons.Profile
    };

    // Abstract property or open property with getter
    abstract val icon: ImageVector
}