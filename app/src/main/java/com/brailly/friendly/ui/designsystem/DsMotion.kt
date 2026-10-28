package com.brailly.friendly.ui.designsystem

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Apakah animasi hiasan boleh dijalankan.
 *
 * Mengikuti setelan "hilangkan animasi" pada sistem. Yang dipadamkan hanyalah
 * gerak yang bersifat dekorasi: denyut node, kedip ikon streak, dan konfeti.
 * Turunnya muka tombol ke ledge tetap dijalankan karena gerakan itu
 * menyampaikan keadaan tertekan, bukan sekadar menghibur.
 */
@Composable
fun dsAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale > 0f
    }
}
