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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.brailly.friendly.data.speech.TextToSpeechManager
import com.brailly.friendly.ui.navigation.BraillyApp
import com.brailly.friendly.ui.theme.BraillyTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val speaker: TextToSpeechManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Mesin suara mulai disiapkan sekarang, bukan pada tekanan tombol
        // bacakan yang pertama. Penyiapannya asinkron, dan menundanya membuat
        // tekanan pertama hanya mengantre tanpa suara apa pun keluar.
        speaker.warmUp()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkTheme by remember { mutableStateOf(systemDark) }

            val splashScreen = installSplashScreen()

            BraillyTheme(darkTheme = darkTheme) {
                BraillyApp(
                    darkTheme = darkTheme,
                    onToggleTheme = { darkTheme = it },
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Pembacaan dihentikan saat aplikasi ditinggalkan. Melanjutkannya di
        // latar belakang menuntut foreground service, dan tanpa itu suara yang
        // terus berbunyi tanpa kendali yang terlihat justru membingungkan.
        //
        // Perubahan konfigurasi (rotasi, ganti tema) dikecualikan: layarnya
        // dibangun ulang sesaat, dan memutus bacaan di tengah halaman karena
        // ponsel berpindah orientasi akan terasa seperti kerusakan.
        if (!isChangingConfigurations) speaker.stop()
    }
}
