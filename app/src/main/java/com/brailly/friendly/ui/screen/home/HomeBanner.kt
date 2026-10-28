package com.brailly.friendly.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.brailly.friendly.ui.component.BraillyIcons
import com.brailly.friendly.ui.designsystem.Ds
import com.brailly.friendly.ui.designsystem.DsDim

/**
 * Satu item banner beranda. Mengarah ke sebuah video YouTube.
 *
 * @param youtubeUrl tautan tujuan yang dibuka di luar aplikasi saat banner diklik.
 * @param title keterangan singkat untuk pembaca layar.
 * @param thumbnailUrl URL gambar thumbnail. Dikosongkan untuk placeholder,
 *   sehingga banner menampilkan latar netral dengan ikon putar.
 */
data class HomeBannerItem(
    val youtubeUrl: String,
    val title: String,
    val thumbnailUrl: String? = null,
)

/**
 * Daftar placeholder sementara. Ganti [youtubeUrl] dan [thumbnailUrl] dengan
 * tautan video sungguhan saat sudah tersedia. Rasio banner 4:3.
 */
val placeholderBanners: List<HomeBannerItem> = listOf(
    HomeBannerItem(
        youtubeUrl = "https://youtu.be/SIJDlnd9GOg?si=-Vaejzfs1ZwEp1JH",
        title = "Bagaimana Cara Tunanetra Melihat Dunia",
        thumbnailUrl = "https://i.ytimg.com/vi/SIJDlnd9GOg/hqdefault.jpg"
    ),
    HomeBannerItem(
        youtubeUrl = "https://youtu.be/eFTrb3ECVrw?si=F_RFJsMNN_Vey7IZ",
        title = "Edukasi Tentang Kebutaan",
        thumbnailUrl = "https://i.ytimg.com/vi/eFTrb3ECVrw/hqdefault.jpg"
    ),
    HomeBannerItem(
        youtubeUrl = "https://youtu.be/klviWKCC-eQ?si=8tSJHLQ7h73WcHry",
        title = "Keterbatasan Bukan Penghalang Untuk Meraih Cita",
        thumbnailUrl = "https://i.ytimg.com/vi_webp/klviWKCC-eQ/hqdefault.webp"
    ),
)

/**
 * Banner beranda: menampilkan thumbnail YouTube berukuran 4:3 yang bisa
 * digeser. Mengklik sebuah banner memanggil [onOpenLink] dengan tautan
 * videonya, yang diarahkan untuk dibuka keluar aplikasi (browser/YouTube).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeBanner(
    items: List<HomeBannerItem>,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        pageSpacing = 12.dp,
    ) { page ->
        BannerCard(
            item = items[page],
            onClick = { onOpenLink(items[page].youtubeUrl) },
        )
    }
}

@Composable
private fun BannerCard(
    item: HomeBannerItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(DsDim.radiusCard)
    val ledgeColor = Ds.colors.neutralLedge
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Ledge dan border 2dp agar banner memakai bahasa bentuk yang sama
            // dengan kartu lain, meski isinya gambar dari jaringan.
            .padding(bottom = DsDim.ledge)
            .aspectRatio(16f / 9f)
            .drawBehind {
                drawRoundRect(
                    color = ledgeColor,
                    topLeft = Offset(0f, DsDim.ledge.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(DsDim.radiusCard.toPx()),
                )
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${item.title}. Membuka YouTube di luar aplikasi."
                role = Role.Button
                onClick(label = "Buka video di YouTube") { onClick(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (item.thumbnailUrl != null) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Lencana putar di tengah; selalu tampil di atas thumbnail (atau
        // di atas latar netral untuk placeholder).
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                // Hitam dan putih tetap dipakai di sini: lencana ini duduk di
                // atas thumbnail sembarang, dan warna tema tidak menjamin
                // keterbacaan di atas gambar yang tidak bisa diduga.
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                BraillyIcons.Play,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .padding(start = 2.dp),
            )
        }
    }
}
