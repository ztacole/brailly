package com.brailly.friendly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.brailly.friendly.ui.navigation.BraillyApp
import com.brailly.friendly.ui.theme.BraillyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkTheme by remember { mutableStateOf(systemDark) }

            BraillyTheme(darkTheme = darkTheme) {
                BraillyApp(
                    darkTheme = darkTheme,
                    onToggleTheme = { darkTheme = it },
                )
            }
        }
    }
}
