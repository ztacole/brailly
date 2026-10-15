# Prompt Figma — Aplikasi Brailly (Full UI Design)

> Tempel prompt di bawah ini ke Figma AI (First Draft) atau gunakan sebagai brief untuk desainer/tim UX-UI.

---

## Ringkasan Proyek

Buatkan **desain UI lengkap (high-fidelity)** — bukan sekadar wireframe/prototype kosong — untuk aplikasi mobile **"Brailly"**: aplikasi pembelajaran dan penerjemah huruf Braille untuk pengguna tunanetra/low-vision, serta orang yang ingin belajar Braille (guru, orang tua, keluarga). Setiap layar harus didesain penuh (warna, tipografi, ikon, ilustrasi, state kosong/isi/error) dan seluruh layar dihubungkan menjadi prototype yang bisa diklik end-to-end.

---

## Arahan Gaya Visual

- **Gaya desain: Minimalis & ramah pengguna (friendly-minimalist)** — banyak whitespace, elemen visual disederhanakan, hindari dekorasi berlebihan, fokus pada konten dan keterbacaan.
- **Rasa interaksi & mood seperti Spotify + Duolingo:**
  - Dari **Spotify**: layout kartu (card-based), penggunaan gradasi warna lembut di background, transisi halus antar layar, bottom sheet untuk aksi cepat, tipografi besar dan tegas untuk judul.
  - Dari **Duolingo**: playful namun tetap rapi — progress path/peta unit, micro-animation saat menjawab benar/salah, karakter/ilustrasi maskot ringan (opsional, bukan wajib), badge & reward yang terasa "hidup", tombol besar dengan feedback tap yang jelas (scale/bounce ringan).
- Kombinasikan keduanya: **minimalis ala Spotify** untuk struktur & tipografi, **playful ala Duolingo** untuk elemen gamifikasi di fitur Daily Activities.
- **Dilarang menggunakan ikon emoji** (🎉, 😀, dll). Semua ikon harus berupa **ikon vektor custom buatan sendiri** (line icon atau filled icon dengan gaya konsisten), dibuat sebagai **komponen SVG di Figma** agar bisa langsung di-export ke format **SVG/XML** untuk keperluan development (Android VectorDrawable/XML, iOS SVG, atau web).
- Ikon menggunakan **grid 24x24px**, stroke width konsisten (misal 1.5–2px untuk line icon), sudut membulat (rounded corner) agar terasa ramah dan senada dengan bentuk lingkaran/titik pada logo Brailly.
- Semua ikon disusun dalam satu **Icon Library / Icon Set** (komponen + variants) supaya konsisten dan reusable, meliputi minimal:
  - Navigasi: Home, Terjemah, Scan, Belajar, Profil/Settings
  - Aksi: Microphone/Voice, Play/Dengarkan, Copy, Share, Save, Search, Edit, Kamera, Flashlight
  - Status: Checklist/Correct, Cross/Incorrect, Lock (unit terkunci), Streak/Fire, Badge/Trophy, Notifikasi
  - Utilitas: Sun/Moon (toggle tema), Globe (bahasa), Back Arrow, Close, Chevron

---

## Prinsip Desain (Aksesibilitas)

- **Accessibility-first**: kontras tinggi, ukuran font besar (minimal 16sp body, 20sp+ heading), touch target minimal 48x48px, label semantik jelas di tiap komponen untuk screen reader.
- **Warna** mengikuti identitas logo, disederhanakan agar tetap minimalis:
  - Primary: biru tua `#2C6C99`
  - Aksen: oranye `#E8873A`
  - Netral/Background Light: putih `#FFFFFF`, abu sangat terang `#F5F6F8`
  - Netral/Background Dark: `#121417`, abu gelap sekunder `#1E2126`
  - Warna semantik: hijau untuk benar/sukses, merah untuk salah/error (gunakan tone lembut, tidak mencolok berlebihan)
- Gunakan motif **titik-titik braille** secara minimal dan konsisten sebagai elemen dekoratif (splash screen, empty state, loading state, background pattern halus) — bukan sebagai ornamen berlebihan.
- Sediakan mode **Light** dan **Dark theme**, kontras tetap AA/AAA di kedua mode (gunakan Figma Variables/Modes).
- Bahasa: default **Bahasa Indonesia**, dengan toggle ke **English** di Settings.

### Tipografi

- **Heading / Branding / Angka besar (skor, streak, judul layar):** **Plus Jakarta Sans** — Bold/SemiBold. Modern, geometris, kuat untuk identitas brand.
- **Body text / UI / Label / Paragraf panjang:** **Inter** — Regular/Medium. Sangat mudah dibaca di ukuran kecil, punya angka tabular (cocok untuk skor/statistik), ramah aksesibilitas.
- Sediakan opsi "besarkan teks" (text scaling) di Settings, minimal 3 tingkat (Normal / Besar / Sangat Besar).

---

## Struktur Navigasi

Bottom navigation bar minimalis (ikon + label singkat) dengan 5 menu utama:
**Home · Terjemah (Braille↔Text) · Scan · Belajar (Daily Activities) · Profil/Settings**

---

## Daftar Layar & Fitur (Desain Penuh, Bukan Wireframe)

Setiap layar berikut harus didesain lengkap dengan komponen final, ikon custom, dan state-nya (default/loading/empty/error), lalu dihubungkan sebagai prototype interaktif.

### 1. Onboarding & Splash
- Splash screen dengan logo animasi titik braille "menyala" berurutan (gaya minimalis, transisi halus ala Spotify intro).
- 3 slide onboarding dengan ilustrasi minimalis (bukan foto/emoji), tombol Skip/Next.
- Pilihan bahasa & aktivasi voice guidance di awal.

### 2. Home / Dashboard
- Kartu ringkasan progres belajar harian (streak) dengan ikon custom "fire/streak".
- Shortcut cepat ke: Text-to-Braille, Braille-to-Text, Scan, Belajar — dalam bentuk card minimalis dengan ikon custom.
- Tombol microphone mengambang (floating action button) di pojok bawah, dengan efek tap/ripple halus.

### 3. Text-to-Braille
- Input field teks minimalis, rounded corner.
- Output otomatis berupa representasi Braille visual (dot pattern digambar sebagai komponen ikon titik) + tombol ikon "dengarkan" (voice playback).
- Ikon copy/share/save dalam bentuk icon button konsisten dengan icon set.

### 4. Braille-to-Text
- "Papan ketik Braille virtual" (6 titik yang bisa ditekan kombinasinya, seperti Perkins Brailler) — desain minimalis dengan feedback visual saat titik ditekan (warna aktif = primary/aksen).
- Live preview teks hasil terjemahan.
- Ikon voice untuk membacakan hasil.

### 5. Scan Brailly-to-Text
- Layar kamera minimalis dengan frame guide (garis tipis, sudut membulat) untuk memindai teks braille fisik.
- State: scanning → processing (loading animation berbasis titik braille, bukan spinner generik) → hasil teks.
- Ikon dengarkan hasil, edit manual (ikon pensil), simpan riwayat scan.

### 6. Panduan Alfabet Braille
- List A–Z (dan angka/simbol dasar) dalam bentuk kartu minimalis: huruf latin + pola titik braille + ikon "play" suara pelafalan.
- Fitur search (ikon kaca pembesar) dan filter huruf.
- Mode "flashcard" swipe untuk latihan cepat, dengan animasi swipe halus.

### 7. Daily Activities (mode belajar ala Duolingo)
- Peta level/unit pembelajaran (path bertahap, node terkunci-terbuka) — gaya playful namun tetap rapi/minimalis, gunakan ikon lock custom untuk unit terkunci.
- Tiap lesson: kuis interaktif (pilih huruf sesuai pola titik, cocokkan huruf-braille, dengarkan lalu tebak), dengan micro-animation feedback benar (ikon checklist, warna hijau lembut) / salah (ikon silang, warna merah lembut).
- Progress bar, sistem XP/streak (ikon custom), reward/badge (ikon trophy custom), halaman hasil skor di akhir sesi dengan tampilan merayakan (tanpa emoji — gunakan ilustrasi/ikon vektor custom).
- Reminder harian (notifikasi) untuk menjaga streak.

### 8. Voice Assistant
- Overlay/modal minimalis saat tombol mic ditekan, dengan animasi gelombang suara (waveform) sebagai indikator "mendengarkan", lalu menjalankan perintah.

### 9. Settings / Profil
- Toggle Light/Dark theme (ikon sun/moon custom).
- Pilihan Bahasa (ikon globe custom).
- Pengaturan ukuran teks, kecepatan voice, feedback getaran (haptic).
- Riwayat aktivitas & statistik belajar dalam bentuk kartu ringkas minimalis.

---

## Interaktivitas Wajib di Prototype

- Semua bottom nav berpindah antar layar saat diklik, dengan transisi halus (Smart Animate).
- Tombol microphone memicu overlay animasi waveform.
- Input teks di Text-to-Braille otomatis meng-update preview braille secara live.
- Papan ketik Braille virtual merespons tap tiap titik (toggle on/off) dengan perubahan warna instan.
- Tombol kamera di Scan berpindah ke state loading lalu ke hasil.
- Kartu alfabet bisa diklik untuk expand detail + play suara.
- Peta pembelajaran: klik unit terbuka → masuk ke lesson → animasi feedback benar/salah → halaman ringkasan skor.
- Toggle Light/Dark benar-benar switch varian komponen di seluruh frame (Figma Variables/Modes).
- Toggle bahasa mengganti seluruh teks UI ke English secara live.
- Semua tombol memiliki state: default, pressed/tap (scale ringan), disabled.

---

## Deliverable yang Diminta

1. **Design System** lengkap:
   - Color palette (light & dark mode) sebagai Figma Variables.
   - Tipografi (Plus Jakarta Sans + Inter) dengan skala type (H1–H3, Body, Caption).
   - **Icon Library** custom (grid 24x24, stroke konsisten) — siap export ke SVG/XML.
   - Komponen UI reusable: button (primary/secondary/icon button), input field, card, bottom nav, dot-braille component, progress bar, badge.
2. **Desain final tiap layar** (bukan wireframe) sesuai daftar layar di atas, untuk frame mobile ukuran **375x812** (iPhone standar), mencakup light & dark mode.
3. **Prototype interaktif** end-to-end yang bisa dites: Onboarding → Home → tiap fitur → Settings, dengan transisi (Smart Animate/Instant) antar frame sesuai daftar interaktivitas di atas.
4. Pastikan seluruh ikon berada dalam **komponen terpisah** (bukan digambar langsung di frame) agar mudah di-export satu per satu ke SVG/XML.
