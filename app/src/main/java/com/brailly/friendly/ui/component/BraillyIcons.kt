package com.brailly.friendly.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icon set custom bergaya line icon (grid 24x24, stroke 2, rounded) sesuai brief
 * (tanpa emoji). Dibangun sebagai ImageVector agar reusable & konsisten.
 */
object BraillyIcons {

    private fun lineIcon(name: String, content: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply(content).build()

    private fun ImageVector.Builder.stroke(pathData: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathData
        )
    }

    val Home: ImageVector = lineIcon("Home") {
        stroke {
            moveTo(3f, 10f); lineTo(12f, 3f); lineTo(21f, 10f)
            lineTo(21f, 20f); lineTo(3f, 20f); close()
        }
    }

    val Translate: ImageVector = lineIcon("Translate") {
        stroke {
            moveTo(4f, 6f); lineTo(11f, 6f)
            moveTo(7.5f, 6f); lineTo(7.5f, 4f)
            moveTo(9.5f, 6f); curveTo(9.5f, 10f, 6f, 13f, 4f, 13f)
            moveTo(5f, 9f); curveTo(6f, 12f, 9f, 13.5f, 11f, 14f)
            moveTo(13f, 20f); lineTo(16.5f, 11f); lineTo(20f, 20f)
            moveTo(14.2f, 17f); lineTo(18.8f, 17f)
        }
    }

    val Scan: ImageVector = lineIcon("Scan") {
        stroke {
            moveTo(4f, 8f); lineTo(4f, 5f); lineTo(7f, 5f)
            moveTo(17f, 5f); lineTo(20f, 5f); lineTo(20f, 8f)
            moveTo(20f, 16f); lineTo(20f, 19f); lineTo(17f, 19f)
            moveTo(7f, 19f); lineTo(4f, 19f); lineTo(4f, 16f)
            moveTo(4f, 12f); lineTo(20f, 12f)
        }
    }

    val Learn: ImageVector = lineIcon("Learn") {
        stroke {
            moveTo(4f, 5f); lineTo(11f, 5f); curveTo(11.5f, 5f, 12f, 5.5f, 12f, 6.5f)
            lineTo(12f, 19f); curveTo(12f, 18f, 11.5f, 18f, 11f, 18f); lineTo(4f, 18f); close()
            moveTo(20f, 5f); lineTo(13f, 5f); curveTo(12.5f, 5f, 12f, 5.5f, 12f, 6.5f)
        }
    }

    val Profile: ImageVector = lineIcon("Profile") {
        stroke {
            moveTo(12f, 11f); curveTo(14f, 11f, 15.5f, 9.5f, 15.5f, 7.5f)
            curveTo(15.5f, 5.5f, 14f, 4f, 12f, 4f)
            curveTo(10f, 4f, 8.5f, 5.5f, 8.5f, 7.5f)
            curveTo(8.5f, 9.5f, 10f, 11f, 12f, 11f); close()
            moveTo(5f, 20f); curveTo(5f, 16f, 8f, 14f, 12f, 14f)
            curveTo(16f, 14f, 19f, 16f, 19f, 20f)
        }
    }

    val Mic: ImageVector = lineIcon("Mic") {
        stroke {
            moveTo(12f, 3f); curveTo(10.5f, 3f, 9.5f, 4f, 9.5f, 5.5f)
            lineTo(9.5f, 11f); curveTo(9.5f, 12.5f, 10.5f, 13.5f, 12f, 13.5f)
            curveTo(13.5f, 13.5f, 14.5f, 12.5f, 14.5f, 11f)
            lineTo(14.5f, 5.5f); curveTo(14.5f, 4f, 13.5f, 3f, 12f, 3f); close()
            moveTo(6f, 11f); curveTo(6f, 15f, 8.5f, 17f, 12f, 17f)
            curveTo(15.5f, 17f, 18f, 15f, 18f, 11f)
            moveTo(12f, 17f); lineTo(12f, 21f)
        }
    }

    val Play: ImageVector = lineIcon("Play") {
        stroke {
            moveTo(7f, 5f); lineTo(19f, 12f); lineTo(7f, 19f); close()
        }
    }

    val Copy: ImageVector = lineIcon("Copy") {
        stroke {
            moveTo(9f, 9f); lineTo(20f, 9f); lineTo(20f, 20f); lineTo(9f, 20f); close()
            moveTo(15f, 9f); lineTo(15f, 4f); lineTo(4f, 4f); lineTo(4f, 15f); lineTo(9f, 15f)
        }
    }

    val Share: ImageVector = lineIcon("Share") {
        stroke {
            moveTo(18f, 5f); moveToRelative(-2.5f, 0f); arcToRelative(2.5f, 2.5f, 0f, true, true, 5f, 0f); arcToRelative(2.5f, 2.5f, 0f, true, true, -5f, 0f)
            moveTo(6f, 12f); moveToRelative(-2.5f, 0f); arcToRelative(2.5f, 2.5f, 0f, true, true, 5f, 0f); arcToRelative(2.5f, 2.5f, 0f, true, true, -5f, 0f)
            moveTo(18f, 19f); moveToRelative(-2.5f, 0f); arcToRelative(2.5f, 2.5f, 0f, true, true, 5f, 0f); arcToRelative(2.5f, 2.5f, 0f, true, true, -5f, 0f)
            moveTo(8.2f, 10.8f); lineTo(15.8f, 6.2f)
            moveTo(8.2f, 13.2f); lineTo(15.8f, 17.8f)
        }
    }

    val Streak: ImageVector = lineIcon("Streak") {
        stroke {
            moveTo(12f, 3f); curveTo(12f, 7f, 16f, 8f, 16f, 13f)
            curveTo(16f, 16.5f, 14f, 20f, 12f, 20f)
            curveTo(10f, 20f, 8f, 16.5f, 8f, 13f)
            curveTo(8f, 10.5f, 10f, 10f, 10f, 7f)
            curveTo(10f, 5.5f, 11f, 4f, 12f, 3f); close()
        }
    }

    val Sun: ImageVector = lineIcon("Sun") {
        stroke {
            moveTo(12f, 8f); curveTo(14f, 8f, 16f, 10f, 16f, 12f); curveTo(16f, 14f, 14f, 16f, 12f, 16f); curveTo(10f, 16f, 8f, 14f, 8f, 12f); curveTo(8f, 10f, 10f, 8f, 12f, 8f); close()
            moveTo(12f, 3f); lineTo(12f, 5f)
            moveTo(12f, 19f); lineTo(12f, 21f)
            moveTo(3f, 12f); lineTo(5f, 12f)
            moveTo(19f, 12f); lineTo(21f, 12f)
            moveTo(5.5f, 5.5f); lineTo(7f, 7f)
            moveTo(17f, 17f); lineTo(18.5f, 18.5f)
            moveTo(18.5f, 5.5f); lineTo(17f, 7f)
            moveTo(7f, 17f); lineTo(5.5f, 18.5f)
        }
    }

    val Moon: ImageVector = lineIcon("Moon") {
        stroke {
            moveTo(20f, 14f); curveTo(18.5f, 15f, 16.5f, 15f, 15f, 13.5f)
            curveTo(12.5f, 11f, 13f, 7f, 15.5f, 5f)
            curveTo(11f, 4.5f, 7f, 8f, 7f, 12.5f)
            curveTo(7f, 16.5f, 10.5f, 20f, 14.5f, 20f)
            curveTo(17f, 20f, 19f, 18.5f, 20f, 14f); close()
        }
    }

    val Search: ImageVector = lineIcon("Search") {
        stroke {
            moveTo(11f, 4f); curveTo(15f, 4f, 18f, 7f, 18f, 11f); curveTo(18f, 15f, 15f, 18f, 11f, 18f); curveTo(7f, 18f, 4f, 15f, 4f, 11f); curveTo(4f, 7f, 7f, 4f, 11f, 4f); close()
            moveTo(16f, 16f); lineTo(21f, 21f)
        }
    }

    val Check: ImageVector = lineIcon("Check") {
        stroke { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 6.5f) }
    }

    val Close: ImageVector = lineIcon("Close") {
        stroke { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) }
    }

    val Back: ImageVector = lineIcon("Back") {
        stroke { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
    }

    val Camera: ImageVector = lineIcon("Camera") {
        stroke {
            moveTo(4f, 8f); lineTo(7f, 8f); lineTo(9f, 5f); lineTo(15f, 5f); lineTo(17f, 8f); lineTo(20f, 8f); lineTo(20f, 19f); lineTo(4f, 19f); close()
            moveTo(12f, 10f); curveTo(14f, 10f, 15.5f, 11.5f, 15.5f, 13.5f); curveTo(15.5f, 15.5f, 14f, 16.5f, 12f, 16.5f); curveTo(10f, 16.5f, 8.5f, 15.5f, 8.5f, 13.5f); curveTo(8.5f, 11.5f, 10f, 10f, 12f, 10f); close()
        }
    }

    val Edit: ImageVector = lineIcon("Edit") {
        stroke {
            moveTo(4f, 20f); lineTo(4f, 16f); lineTo(16f, 4f); lineTo(20f, 8f); lineTo(8f, 20f); close()
            moveTo(13f, 7f); lineTo(17f, 11f)
        }
    }

    val Save: ImageVector = lineIcon("Save") {
        stroke {
            moveTo(5f, 4f); lineTo(17f, 4f); lineTo(20f, 7f); lineTo(20f, 20f); lineTo(4f, 20f); lineTo(4f, 5f); close()
            moveTo(7f, 4f); lineTo(7f, 9f); lineTo(15f, 9f); lineTo(15f, 4f)
            moveTo(7f, 13f); lineTo(17f, 13f)
        }
    }

    val Trophy: ImageVector = lineIcon("Trophy") {
        stroke {
            moveTo(7f, 4f); lineTo(17f, 4f); lineTo(17f, 9f); curveTo(17f, 12f, 15f, 13f, 12f, 13f); curveTo(9f, 13f, 7f, 12f, 7f, 9f); close()
            moveTo(7f, 5f); lineTo(4f, 5f); curveTo(4f, 8f, 5f, 9f, 7f, 9f)
            moveTo(17f, 5f); lineTo(20f, 5f); curveTo(20f, 8f, 19f, 9f, 17f, 9f)
            moveTo(12f, 13f); lineTo(12f, 17f)
            moveTo(8f, 20f); lineTo(16f, 20f); lineTo(15f, 17f); lineTo(9f, 17f); close()
        }
    }

    val Lock: ImageVector = lineIcon("Lock") {
        stroke {
            moveTo(6f, 11f); lineTo(18f, 11f); lineTo(18f, 20f); lineTo(6f, 20f); close()
            moveTo(8.5f, 11f); lineTo(8.5f, 8f); curveTo(8.5f, 5.5f, 10f, 4f, 12f, 4f); curveTo(14f, 4f, 15.5f, 5.5f, 15.5f, 8f); lineTo(15.5f, 11f)
        }
    }

    /** Pengeras suara: menandai tombol "bacakan teks" (text-to-speech). */
    val Speaker: ImageVector = lineIcon("Speaker") {
        stroke {
            moveTo(4f, 9f); lineTo(7f, 9f); lineTo(11f, 5f); lineTo(11f, 19f)
            lineTo(7f, 15f); lineTo(4f, 15f); close()
            moveTo(14.5f, 9.5f); curveTo(16f, 10.8f, 16f, 13.2f, 14.5f, 14.5f)
            moveTo(17.5f, 6.5f); curveTo(20.5f, 9f, 20.5f, 15f, 17.5f, 17.5f)
        }
    }

    /** Kotak henti: dipakai saat pembacaan sedang berjalan. */
    val Stop: ImageVector = lineIcon("Stop") {
        stroke {
            moveTo(6.5f, 6.5f); lineTo(17.5f, 6.5f); lineTo(17.5f, 17.5f)
            lineTo(6.5f, 17.5f); close()
        }
    }

    /** Jam dengan anak panah balik arah: riwayat pemindaian. */
    val History: ImageVector = lineIcon("History") {
        stroke {
            // Busur jam dibiarkan terbuka di kiri atas sebagai tempat anak panah.
            moveTo(4.2f, 8.6f)
            curveTo(5.9f, 5.8f, 8.7f, 4f, 12f, 4f)
            curveTo(16.4f, 4f, 20f, 7.6f, 20f, 12f)
            curveTo(20f, 16.4f, 16.4f, 20f, 12f, 20f)
            curveTo(8.4f, 20f, 5.4f, 17.7f, 4.4f, 14.4f)
            moveTo(3.2f, 4.6f); lineTo(4.2f, 8.6f); lineTo(8.2f, 7.6f)
            moveTo(12f, 8f); lineTo(12f, 12f); lineTo(15f, 13.6f)
        }
    }

    /** Tempat sampah: menghapus satu entri riwayat atau seluruhnya. */
    val Trash: ImageVector = lineIcon("Trash") {
        stroke {
            moveTo(5f, 7f); lineTo(19f, 7f)
            moveTo(9f, 7f); lineTo(9f, 4.5f); lineTo(15f, 4.5f); lineTo(15f, 7f)
            moveTo(6.5f, 7f); lineTo(7.5f, 20f); lineTo(16.5f, 20f); lineTo(17.5f, 7f)
            moveTo(10.5f, 10.5f); lineTo(10.5f, 16.5f)
            moveTo(13.5f, 10.5f); lineTo(13.5f, 16.5f)
        }
    }
}
