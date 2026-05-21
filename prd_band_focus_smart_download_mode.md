# PRD — BandFocus: Smart Download Focus Mode

## 1. Ringkasan Produk

**Nama Project:** BandFocus  
**Tagline:** Focus your bandwidth. Download smarter.  
**Platform:** Android  
**Tech Stack Utama:** Kotlin, Jetpack Compose, OkHttp, WorkManager, Foreground Service, Room Database, VPNService, Material 3.

BandFocus adalah aplikasi Android yang membantu pengguna memaksimalkan bandwidth yang tersedia saat melakukan download file besar. Aplikasi ini menggabungkan **multi-thread download manager**, **per-app internet blocker**, dan **smart download diagnosis** agar proses download menjadi lebih stabil, terukur, dan efisien.

Project ini **bukan internet booster palsu**. BandFocus tidak mengklaim menaikkan kecepatan internet melebihi batas ISP. BandFocus bekerja dengan cara:

1. Memecah file menjadi beberapa bagian download paralel.
2. Memblokir sementara aplikasi lain yang memakai internet.
3. Memberikan diagnosis penyebab download lambat.
4. Menjalankan mode download otomatis seperti Turbo Mode, Eco Mode, dan Night Mode.

---

## 2. Problem Statement

Pengguna Android sering mengalami download lambat karena beberapa faktor:

- Banyak aplikasi berjalan di background dan memakai internet.
- Server download membatasi kecepatan per koneksi.
- Download browser bawaan tidak optimal untuk file besar.
- Pengguna tidak tahu apakah lambat karena sinyal, server, aplikasi lain, atau batas provider.
- Tidak ada mode sederhana untuk memfokuskan bandwidth ke satu proses download.

---

## 3. Tujuan Produk

BandFocus bertujuan menjadi **download control center** yang jujur, praktis, dan mudah digunakan.

### Tujuan utama

- Mempermudah user melakukan download file besar dengan lebih stabil.
- Mengurangi gangguan bandwidth dari aplikasi lain.
- Memberikan pengalaman download yang transparan dan mudah dipahami.
- Menjadi project portfolio Android/Kotlin yang kuat dan realistis.

### Tujuan teknis

- Membuat multi-thread downloader berbasis HTTP Range Request.
- Membuat firewall sederhana berbasis Android VPNService.
- Membuat UI/UX modern dengan Jetpack Compose dan Material 3.
- Membuat sistem mode download: Eco, Turbo, Night.
- Menyimpan riwayat download dan konfigurasi user.

---

## 4. Non-Goals

BandFocus tidak bertujuan untuk:

- Membypass limit ISP.
- Membobol throttling provider.
- Mempercepat internet melebihi kapasitas paket.
- Mengunduh konten ilegal.
- Mengakses traffic terenkripsi aplikasi lain.
- Menggantikan VPN komersial.
- Menggunakan klaim palsu seperti “internet 10x faster”.

---

## 5. Target User

### Primary User

Pengguna Android yang sering download file besar seperti:

- file ZIP/RAR,
- video,
- dataset,
- installer APK,
- file project,
- materi kuliah,
- file dari direct link.

### Secondary User

- Mahasiswa IT.
- Developer mobile.
- Pengguna jaringan terbatas.
- Pengguna yang ingin mengontrol aplikasi background.
- Power user Android yang memakai Shizuku/root.

---

## 6. Value Proposition

### Positioning utama

> BandFocus helps users maximize available bandwidth by combining multi-thread downloading, app-level traffic control, and smart download diagnostics.

### Versi Indonesia

> BandFocus membantu pengguna memaksimalkan bandwidth yang tersedia dengan menggabungkan download multi-thread, kontrol internet per aplikasi, dan diagnosis download yang cerdas.

### Pembeda dari download manager biasa

Download manager biasa hanya fokus ke download file.  
BandFocus fokus ke **ekosistem download**: file, jaringan, aplikasi background, mode prioritas, dan diagnosis.

---

## 7. MVP Scope

MVP harus cepat dibuat dan tetap terlihat unik.

### MVP 1 — Smart Download Focus

Fitur wajib:

1. Input URL file.
2. Validasi URL.
3. Deteksi ukuran file menggunakan HTTP HEAD request.
4. Deteksi support HTTP Range Request.
5. Download single-thread jika server tidak support range.
6. Download multi-thread jika server support range.
7. Progress download real-time.
8. Speed indicator.
9. ETA indicator.
10. Pause, resume, cancel.
11. History download.
12. Pilih mode download: Eco, Balanced, Turbo.
13. Pilih aplikasi yang diblokir saat Focus Mode aktif.
14. VPNService untuk block koneksi aplikasi yang dipilih.
15. Auto unblock setelah download selesai.
16. Foreground notification saat download berjalan.
17. Responsive UI menggunakan Jetpack Compose.

---

## 8. Future Scope

Fitur lanjutan setelah MVP:

1. Shizuku mode untuk kontrol lebih advanced.
2. Root mode dengan iptables/nftables.
3. Multi-source download dari beberapa mirror.
4. Smart diagnosis berbasis data historis.
5. Auto recommendation thread count.
6. Speed test internal.
7. Bandwidth monitor per aplikasi.
8. Scheduled night download.
9. Cloud backup history.
10. QR/share link download.
11. Browser integration melalui Android Share Intent.
12. Floating download bubble.

---

## 9. Core Features Detail

## 9.1 URL Download Input

### Deskripsi

User memasukkan link file yang ingin didownload.

### User Flow

1. User membuka aplikasi.
2. User paste URL.
3. User menekan tombol Analyze.
4. Aplikasi melakukan HEAD request.
5. Aplikasi menampilkan informasi file.
6. User memilih mode download.
7. User menekan Start Download.

### Acceptance Criteria

- Input URL harus menerima `http://` dan `https://`.
- Jika URL kosong, tampilkan error.
- Jika URL invalid, tampilkan error.
- Jika server tidak merespons HEAD, fallback ke GET request ringan.
- Jika ukuran file tidak diketahui, download tetap bisa berjalan dalam single-thread mode.

### Codex Keywords

`URL validation`, `Kotlin Uri parser`, `OkHttp HEAD request`, `Content-Length`, `Accept-Ranges`, `HTTP Range`, `fallback GET request`, `download metadata`.

---

## 9.2 File Analysis

### Deskripsi

Sebelum download, aplikasi menganalisis link.

### Data yang dianalisis

- Nama file.
- Ukuran file.
- MIME type.
- Server support range atau tidak.
- Rekomendasi jumlah thread.
- Estimasi strategi download.

### Logic

Jika header `Accept-Ranges: bytes` tersedia, gunakan multi-thread.  
Jika tidak tersedia, gunakan single-thread.  
Jika file size kecil, gunakan single-thread atau low thread.  
Jika file size besar, gunakan thread lebih tinggi.

### Acceptance Criteria

- Aplikasi menampilkan status: `Range Supported` atau `Range Not Supported`.
- Aplikasi menampilkan rekomendasi mode.
- Aplikasi tidak crash jika header tidak lengkap.

### Codex Keywords

`Content-Length header`, `Accept-Ranges bytes`, `MIME type detection`, `filename from Content-Disposition`, `thread recommendation`, `network diagnostics`.

---

## 9.3 Multi-thread Download Engine

### Deskripsi

File dipecah menjadi beberapa bagian menggunakan HTTP Range Request.

### Contoh

File 100 MB, 4 thread:

```text
Thread 1: bytes 0 - 24,999,999
Thread 2: bytes 25,000,000 - 49,999,999
Thread 3: bytes 50,000,000 - 74,999,999
Thread 4: bytes 75,000,000 - 99,999,999
```

### Requirements

- Gunakan OkHttp.
- Gunakan coroutine untuk parallel download.
- Simpan setiap part sebagai temporary file.
- Gabungkan part setelah semua selesai.
- Simpan progress setiap part.
- Support pause dan resume.
- Support retry jika koneksi putus.

### Acceptance Criteria

- File hasil akhir tidak corrupt.
- Progress dihitung dari total bytes downloaded semua part.
- Jika salah satu part gagal, hanya part tersebut yang di-retry.
- Jika user pause, semua job coroutine berhenti dengan aman.
- Jika user resume, download melanjutkan dari byte terakhir.

### Codex Keywords

`Kotlin coroutine`, `OkHttp`, `HTTP Range header`, `206 Partial Content`, `RandomAccessFile`, `temporary part files`, `merge file chunks`, `resume download`, `retry policy`, `checksum optional`, `download engine`.

---

## 9.4 Download Modes

## Eco Mode

Untuk download ringan dan hemat baterai.

- Thread count: 1–2.
- Background app tetap sebagian aktif.
- Retry interval lebih santai.
- Cocok untuk file kecil.

## Balanced Mode

Mode default.

- Thread count: 4–6.
- Blokir aplikasi berat jika dipilih.
- Cocok untuk pemakaian harian.

## Turbo Mode

Untuk file besar dan kecepatan maksimal.

- Thread count: 8–16.
- Aktifkan Focus Mode.
- Blokir aplikasi non-prioritas.
- Gunakan foreground service.
- Cocok untuk download besar.

## Night Mode

Untuk download saat tidur.

- Bisa dijadwalkan.
- Hanya downloader dan aplikasi whitelist yang boleh internet.
- Notification silent atau minimal.
- Auto unblock setelah selesai.

### Acceptance Criteria

- User dapat memilih mode sebelum download.
- Mode menentukan jumlah thread default.
- Mode dapat diedit secara manual oleh user advanced.
- Mode tidak boleh membuat klaim kecepatan palsu.

### Codex Keywords

`download mode enum`, `Eco mode`, `Balanced mode`, `Turbo mode`, `Night mode`, `thread count strategy`, `mode configuration`, `user preferences`.

---

## 9.5 Focus Mode Firewall

### Deskripsi

Focus Mode membatasi akses internet aplikasi lain saat download berjalan.

### Implementasi MVP

Gunakan Android `VPNService` sebagai local firewall no-root.

### Fitur MVP

- User memilih aplikasi yang ingin diblokir.
- Aplikasi menyimpan package name ke database/preferences.
- Saat Focus Mode aktif, VPNService memblokir traffic dari aplikasi terpilih.
- Saat download selesai, VPNService berhenti atau kembali ke mode normal.

### Important Limitation

Android hanya mengizinkan satu VPN aktif dalam satu waktu. Jika BandFocus VPNService aktif, user tidak bisa memakai VPN lain bersamaan.

### Acceptance Criteria

- User melihat daftar aplikasi terinstall.
- User bisa memilih blacklist aplikasi.
- User bisa memilih whitelist aplikasi penting.
- VPN permission muncul saat pertama kali digunakan.
- Focus Mode dapat dinyalakan dan dimatikan.
- Setelah download selesai, rule otomatis dilepas.

### Codex Keywords

`Android VPNService`, `local VPN firewall`, `per-app blocking`, `PackageManager installed applications`, `package name blacklist`, `allowed applications`, `disallowed applications`, `VpnService.Builder`, `addDisallowedApplication`, `addAllowedApplication`, `foreground VPN service`.

---

## 9.6 Smart Diagnosis

### Deskripsi

Aplikasi memberikan analisis jujur kenapa download lambat.

### Diagnosis Rules MVP

1. Jika server tidak support range:
   - tampilkan: “Server tidak mendukung multi-thread. Download berjalan single-thread.”

2. Jika speed rendah dan banyak app diblokir belum aktif:
   - tampilkan: “Aktifkan Focus Mode untuk mengurangi traffic background.”

3. Jika speed sudah mendekati estimasi bandwidth:
   - tampilkan: “Kecepatan sudah mendekati batas koneksi saat ini.”

4. Jika banyak retry:
   - tampilkan: “Koneksi tidak stabil. Gunakan Balanced atau Eco Mode.”

5. Jika file kecil:
   - tampilkan: “File kecil tidak membutuhkan Turbo Mode.”

### Acceptance Criteria

- Diagnosis muncul setelah Analyze URL.
- Diagnosis berubah saat download berjalan.
- Bahasa diagnosis harus jujur dan tidak berlebihan.

### Codex Keywords

`download diagnostics`, `network condition analysis`, `speed threshold`, `retry count`, `range support diagnosis`, `background traffic warning`, `smart recommendation engine`.

---

## 9.7 Download History

### Deskripsi

Aplikasi menyimpan riwayat download.

### Data History

- File name.
- URL.
- File size.
- Saved path.
- Status.
- Download date.
- Average speed.
- Mode used.
- Thread count used.

### Acceptance Criteria

- User bisa melihat history.
- User bisa hapus item history.
- User bisa melanjutkan download yang paused/failed.
- User bisa membuka file jika masih tersedia.

### Codex Keywords

`Room database`, `DownloadEntity`, `DownloadDao`, `download history`, `resume failed download`, `delete history`, `open downloaded file`, `FileProvider`.

---

## 10. UI/UX Requirements

## 10.1 Design Direction

### Style

Modern, clean, dynamic, techy, tapi tidak terlalu gelap.

### Visual Keywords

- Gradient background.
- Glassmorphism cards.
- Rounded cards.
- Dynamic progress ring.
- Bandwidth wave animation.
- Soft shadow.
- Neon accent minimal.
- Material 3.
- Responsive layout.

### Color Palette

Recommended:

- Primary: Electric Blue `#3B82F6`
- Secondary: Cyan `#06B6D4`
- Accent: Lime `#A3E635`
- Background Light: `#F8FAFC`
- Background Dark: `#0F172A`
- Card Light: `#FFFFFF`
- Card Dark: `#1E293B`
- Text Dark: `#0F172A`
- Text Light: `#F8FAFC`

### Typography

- Use Material 3 typography.
- Headline: bold, clear.
- Body: readable.
- Numbers/speed: use larger text.

---

## 10.2 App Navigation

Gunakan bottom navigation atau adaptive navigation rail.

### Main Tabs

1. **Home**
2. **Downloads**
3. **Focus Mode**
4. **Insights**
5. **Settings**

### Codex Keywords

`Jetpack Compose Navigation`, `NavigationBar`, `NavigationRail`, `adaptive navigation`, `Material 3 Scaffold`, `WindowSizeClass`.

---

## 10.3 Screen 1 — Home Dashboard

### Tujuan

Tempat user paste link dan memulai download.

### Components

- App logo/title.
- URL input field.
- Paste button.
- Analyze button.
- Download mode selector.
- Quick stats card.
- Active download card.
- Smart diagnosis card.

### Layout Mobile

```text
[Header]
[URL Input Card]
[Mode Selector Chips]
[Analyze Button]
[Smart Diagnosis Card]
[Active Download Card]
[Quick Stats]
```

### Layout Tablet/Desktop

```text
Left Column:
- URL Input
- Mode Selector
- Focus Mode Toggle

Right Column:
- Active Download
- Diagnosis
- Stats
```

### UI Text Example

- “Paste your download link”
- “Analyze URL”
- “Start Focus Download”
- “Turbo Mode recommended”
- “Server supports multi-thread download”

### Codex Keywords

`HomeScreen`, `UrlInputCard`, `ModeSelector`, `AssistChip`, `SmartDiagnosisCard`, `ActiveDownloadCard`, `responsive Compose layout`, `LazyColumn`, `FlowRow`.

---

## 10.4 Screen 2 — Download Detail

### Tujuan

Menampilkan progress download secara real-time.

### Components

- File name.
- File size.
- Progress bar.
- Circular progress.
- Current speed.
- Average speed.
- ETA.
- Thread count.
- Mode badge.
- Pause/resume/cancel buttons.
- Per-part progress list.

### UI Text Example

- “Downloading...”
- “8 active connections”
- “Focus Mode enabled”
- “Estimated time left: 04:21”

### Codex Keywords

`DownloadDetailScreen`, `LinearProgressIndicator`, `CircularProgressIndicator`, `real-time progress state`, `StateFlow`, `ViewModel`, `pause download`, `resume download`, `cancel download`, `download chunks progress`.

---

## 10.5 Screen 3 — Focus Mode

### Tujuan

Mengatur aplikasi yang diblokir saat download.

### Components

- Focus Mode toggle.
- VPN permission status.
- Search app field.
- Installed app list.
- App icon.
- App name.
- Package name.
- Block/Allow switch.
- Preset buttons:
  - Block social media
  - Block streaming apps
  - Allow messaging apps
  - Reset rules

### UI Text Example

- “Block distracting apps while downloading”
- “VPN permission required for no-root firewall”
- “Apps will be restored after download completes”

### Codex Keywords

`FocusModeScreen`, `VPN permission`, `PackageManager`, `installed apps list`, `app icon loader`, `SearchTextField`, `Switch`, `blacklist apps`, `whitelist apps`, `VPNService permission intent`.

---

## 10.6 Screen 4 — Downloads History

### Tujuan

Menampilkan semua file yang sudah, sedang, atau gagal didownload.

### Components

- Download list.
- Filter chips: All, Active, Completed, Failed, Paused.
- Search history.
- Download item card.
- Resume button.
- Delete button.
- Open file button.

### Codex Keywords

`DownloadsScreen`, `DownloadHistoryList`, `Room Flow`, `LazyColumn`, `SwipeToDismiss`, `download status filter`, `open file intent`, `FileProvider`.

---

## 10.7 Screen 5 — Insights

### Tujuan

Menampilkan statistik dan diagnosis performa download.

### Components

- Total downloaded.
- Average speed.
- Fastest download.
- Most used mode.
- Success rate.
- Retry count.
- Recommendation card.

### UI Text Example

- “Turbo Mode works best on large files.”
- “Your average download speed improved when Focus Mode was enabled.”

### Codex Keywords

`InsightsScreen`, `download statistics`, `Room aggregate queries`, `average speed`, `success rate`, `recommendation card`, `charts optional`.

---

## 10.8 Screen 6 — Settings

### Components

- Default download folder.
- Default mode.
- Default thread count.
- Auto-enable Focus Mode.
- Use WiFi only.
- Keep screen awake.
- Notification settings.
- Theme: Light, Dark, System.
- Advanced settings.

### Codex Keywords

`SettingsScreen`, `DataStore Preferences`, `theme setting`, `download directory`, `thread count preference`, `wifi only`, `notification settings`, `advanced settings`.

---

## 11. Responsive UI Rules

### Compact Width

- Use bottom navigation.
- Single-column layout.
- Cards stacked vertically.
- Floating action button optional.

### Medium/Expanded Width

- Use navigation rail.
- Two-column layout for dashboard.
- Detail panel beside list.
- More spacing and larger cards.

### Codex Keywords

`WindowSizeClass`, `compact layout`, `medium layout`, `expanded layout`, `adaptive UI`, `NavigationRail`, `NavigationBar`, `BoxWithConstraints`.

---

## 12. Architecture

Gunakan clean architecture sederhana.

```text
app/
 ├── data/
 │   ├── local/
 │   │   ├── DownloadDatabase.kt
 │   │   ├── DownloadDao.kt
 │   │   └── DownloadEntity.kt
 │   ├── network/
 │   │   ├── DownloadApi.kt
 │   │   └── HeaderAnalyzer.kt
 │   └── repository/
 │       └── DownloadRepositoryImpl.kt
 │
 ├── domain/
 │   ├── model/
 │   │   ├── DownloadTask.kt
 │   │   ├── DownloadMode.kt
 │   │   └── DownloadStatus.kt
 │   ├── repository/
 │   │   └── DownloadRepository.kt
 │   └── usecase/
 │       ├── AnalyzeUrlUseCase.kt
 │       ├── StartDownloadUseCase.kt
 │       ├── PauseDownloadUseCase.kt
 │       ├── ResumeDownloadUseCase.kt
 │       └── CancelDownloadUseCase.kt
 │
 ├── service/
 │   ├── DownloadForegroundService.kt
 │   └── FocusVpnService.kt
 │
 ├── presentation/
 │   ├── navigation/
 │   ├── home/
 │   ├── downloads/
 │   ├── focus/
 │   ├── insights/
 │   └── settings/
 │
 └── core/
     ├── utils/
     ├── permissions/
     ├── notification/
     └── designsystem/
```

### Codex Keywords

`MVVM`, `Clean Architecture`, `Repository Pattern`, `UseCase`, `StateFlow`, `Hilt dependency injection`, `Room`, `DataStore`, `Foreground Service`, `VPNService`, `Jetpack Compose Material 3`.

---

## 13. Data Models

## DownloadEntity

```kotlin
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val downloadedBytes: Long,
    val savedPath: String?,
    val status: DownloadStatus,
    val mode: DownloadMode,
    val threadCount: Int,
    val averageSpeed: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val supportsRange: Boolean
)
```

## DownloadMode

```kotlin
enum class DownloadMode {
    ECO,
    BALANCED,
    TURBO,
    NIGHT
}
```

## DownloadStatus

```kotlin
enum class DownloadStatus {
    QUEUED,
    ANALYZING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED
}
```

## AppRuleEntity

```kotlin
@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isBlockedInFocusMode: Boolean,
    val isWhitelisted: Boolean,
    val updatedAt: Long
)
```

---

## 14. Permissions

### Required Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

### VPN Service Declaration

```xml
<service
    android:name=".service.FocusVpnService"
    android:permission="android.permission.BIND_VPN_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>
```

### Note

`QUERY_ALL_PACKAGES` punya batasan Play Store. Untuk portfolio/testing pribadi tidak masalah, tapi untuk rilis publik perlu justifikasi atau alternatif query package terbatas.

---

## 15. Security & Privacy

- Aplikasi tidak membaca isi traffic HTTPS.
- Aplikasi hanya memblokir atau mengizinkan aplikasi berdasarkan package name.
- URL download disimpan lokal di device.
- Tidak ada tracking user di MVP.
- Tidak upload data user ke server.
- Harus ada privacy notice sederhana.

---

## 16. Performance Requirements

- UI tetap responsive saat download berjalan.
- Download berjalan di foreground service.
- Progress update tidak boleh terlalu sering sehingga membebani UI.
- Gunakan throttling update UI, misalnya setiap 500 ms atau 1 detik.
- Part file harus dibersihkan jika download dibatalkan.
- Gunakan retry dengan exponential backoff.

### Codex Keywords

`foreground service performance`, `progress throttling`, `coroutine dispatcher IO`, `exponential backoff`, `partial file cleanup`, `memory efficient download`, `streaming file write`.

---

## 17. Notification Requirements

Saat download berjalan, tampilkan notification:

- File name.
- Progress percentage.
- Current speed.
- Pause action.
- Cancel action.

Saat selesai:

- Download completed.
- Open file action.

### Codex Keywords

`NotificationCompat`, `foreground service notification`, `notification actions`, `pause action`, `cancel action`, `open file action`, `Android 13 notification permission`.

---

## 18. Error Handling

### Common Errors

- Invalid URL.
- Server timeout.
- Server does not support range.
- Storage permission/path error.
- VPN permission denied.
- Not enough storage.
- Connection lost.
- File merge failed.

### UI Error Style

- Gunakan snackbar untuk error ringan.
- Gunakan dialog untuk error penting.
- Berikan solusi singkat.

### Example Messages

- “Server does not support multi-thread download. Switching to single-thread mode.”
- “Storage is almost full. Free up space before downloading.”
- “Focus Mode requires VPN permission.”
- “Connection lost. Retrying...”

---

## 19. MVP Development Roadmap

## Phase 1 — Project Setup

- Create Android project Kotlin + Jetpack Compose.
- Setup Material 3 theme.
- Setup navigation.
- Setup Hilt.
- Setup Room.
- Setup DataStore.

## Phase 2 — UI Skeleton

- HomeScreen.
- DownloadDetailScreen.
- FocusModeScreen.
- DownloadsScreen.
- SettingsScreen.
- Responsive layout.

## Phase 3 — URL Analyzer

- Implement URL validation.
- Implement HEAD request.
- Extract file metadata.
- Detect range support.
- Show diagnosis card.

## Phase 4 — Download Engine

- Single-thread download.
- Multi-thread download.
- Pause/resume/cancel.
- Foreground service.
- Notification progress.
- Room history.

## Phase 5 — Focus Mode

- Installed apps list.
- App blacklist/whitelist.
- VPNService permission.
- Basic app blocking using VPNService Builder.
- Auto enable/disable with download lifecycle.

## Phase 6 — Polish

- Animations.
- Insights screen.
- Better error handling.
- Testing.
- README.
- Demo video.

---

## 20. Suggested AI Coding Agent Prompt

Gunakan prompt ini di Codex/AI coding agent:

```text
Build an Android app named BandFocus using Kotlin and Jetpack Compose. The app is a Smart Download Focus Manager, not a fake internet booster. It combines a multi-thread HTTP downloader, a Focus Mode firewall using Android VPNService, and smart download diagnostics.

Use this stack:
- Kotlin
- Jetpack Compose
- Material 3
- MVVM + simple Clean Architecture
- Hilt for dependency injection
- Room for download history
- DataStore for user preferences
- OkHttp for HTTP requests
- Kotlin Coroutines and StateFlow
- Foreground Service for active downloads
- Android VPNService for no-root app-level blocking

Create these screens:
1. HomeScreen with URL input, Analyze button, mode selector, SmartDiagnosisCard, ActiveDownloadCard.
2. DownloadDetailScreen with progress, speed, ETA, thread count, pause/resume/cancel actions.
3. FocusModeScreen with installed apps list, search, switches for block/allow, VPN permission status.
4. DownloadsScreen with history, filters, resume/open/delete actions.
5. InsightsScreen with basic statistics.
6. SettingsScreen with default mode, thread count, theme, WiFi-only, auto Focus Mode.

Implement MVP features:
- Validate URL.
- Analyze URL using OkHttp HEAD request.
- Read Content-Length, Content-Disposition, Content-Type, Accept-Ranges.
- Detect whether server supports HTTP Range requests.
- Implement single-thread download if range is not supported.
- Implement multi-thread download if range is supported using HTTP Range header and 206 Partial Content.
- Save chunk files temporarily and merge them after completion.
- Support pause, resume, cancel, retry.
- Store download history in Room.
- Show progress with StateFlow.
- Run active download in Foreground Service with notification actions.
- Implement Focus Mode using VPNService.Builder with allowed/disallowed applications based on package names.
- Auto stop Focus Mode after download completes.
- Build responsive UI using WindowSizeClass, bottom navigation for compact screens and navigation rail for expanded screens.

Design style:
- Modern, clean, dynamic, Material 3.
- Light and dark theme.
- Rounded cards, gradient background, soft shadows, animated progress.
- Use blue/cyan/lime accents.

Important:
- Do not claim the app increases ISP speed.
- Use honest messages such as “Maximize available bandwidth by reducing background traffic.”
- Make code modular, readable, and production-oriented.
```

---

## 21. Suggested Folder Structure for Codex

```text
BandFocus/
 ├── app/src/main/java/com/bandfocus/app/
 │   ├── MainActivity.kt
 │   ├── BandFocusApp.kt
 │   ├── core/
 │   │   ├── design/
 │   │   ├── notification/
 │   │   ├── permissions/
 │   │   └── util/
 │   ├── data/
 │   │   ├── local/
 │   │   ├── network/
 │   │   └── repository/
 │   ├── domain/
 │   │   ├── model/
 │   │   ├── repository/
 │   │   └── usecase/
 │   ├── service/
 │   │   ├── DownloadForegroundService.kt
 │   │   └── FocusVpnService.kt
 │   └── presentation/
 │       ├── navigation/
 │       ├── home/
 │       ├── download_detail/
 │       ├── downloads/
 │       ├── focus/
 │       ├── insights/
 │       └── settings/
```

---

## 22. UI Component List

### Design System Components

- `BandFocusButton`
- `BandFocusCard`
- `GradientBackground`
- `ModeChip`
- `SpeedBadge`
- `ProgressRing`
- `DiagnosisCard`
- `DownloadProgressCard`
- `AppRuleItem`
- `SectionHeader`
- `EmptyState`
- `ErrorState`

### Codex Keywords

`reusable Compose components`, `design system`, `Material 3 Card`, `AssistChip`, `FilterChip`, `AnimatedVisibility`, `animateFloatAsState`, `Crossfade`, `rememberSaveable`.

---

## 23. Example Home UI Wireframe

```text
┌────────────────────────────────────┐
│ BandFocus                          │
│ Focus your bandwidth               │
├────────────────────────────────────┤
│ Paste download link                │
│ [ https://example.com/file.zip   ] │
│ [Paste]        [Analyze URL]       │
├────────────────────────────────────┤
│ Mode                               │
│ [Eco] [Balanced] [Turbo] [Night]   │
├────────────────────────────────────┤
│ Smart Diagnosis                    │
│ Server supports multi-thread.      │
│ Turbo Mode recommended.            │
├────────────────────────────────────┤
│ Active Download                    │
│ file.zip                           │
│ ███████████░░░░░ 68%               │
│ 4.8 MB/s • ETA 02:14 • 8 threads   │
│ [Pause] [Cancel]                   │
└────────────────────────────────────┘
```

---

## 24. Example Focus Mode UI Wireframe

```text
┌────────────────────────────────────┐
│ Focus Mode                         │
│ Block distracting apps while       │
│ downloading.                       │
│ [Enable Focus Mode]                │
├────────────────────────────────────┤
│ VPN Permission: Required           │
│ [Grant Permission]                 │
├────────────────────────────────────┤
│ Search apps                        │
│ [ Instagram                     ]  │
├────────────────────────────────────┤
│ Instagram                 [Block]  │
│ TikTok                    [Block]  │
│ YouTube                   [Block]  │
│ WhatsApp                  [Allow]  │
│ Telegram                  [Allow]  │
└────────────────────────────────────┘
```

---

## 25. Success Metrics

### Product Metrics

- User berhasil download file tanpa error.
- User bisa pause/resume download.
- User bisa mengaktifkan Focus Mode.
- User memahami diagnosis aplikasi.

### Technical Metrics

- Download tidak corrupt.
- Progress akurat.
- UI tidak freeze.
- VPNService aktif dan mati sesuai lifecycle.
- History tersimpan dengan benar.

---

## 26. Testing Checklist

### Download Engine

- Test URL valid.
- Test URL invalid.
- Test server support range.
- Test server not support range.
- Test pause/resume.
- Test cancel.
- Test retry.
- Test file merge.
- Test low storage.

### Focus Mode

- Test VPN permission accepted.
- Test VPN permission denied.
- Test app block rule.
- Test whitelist rule.
- Test auto disable after download.

### UI

- Test compact screen.
- Test tablet layout.
- Test dark theme.
- Test orientation change.
- Test long file name.

---

## 27. README Positioning

Gunakan kalimat ini di README:

```text
BandFocus is an Android Smart Download Focus Manager. It does not magically increase your internet speed. Instead, it helps maximize available bandwidth by combining multi-thread downloads, app-level focus blocking, and clear download diagnostics.
```

Versi Indonesia:

```text
BandFocus adalah aplikasi Android untuk membantu proses download lebih fokus dan stabil. Aplikasi ini tidak menaikkan kecepatan internet melebihi batas provider, tetapi memaksimalkan bandwidth yang tersedia melalui multi-thread download, pemblokiran aplikasi pengganggu, dan diagnosis download yang transparan.
```

---

## 28. MVP Definition of Done

MVP dianggap selesai jika:

- User bisa paste URL.
- User bisa analyze URL.
- User bisa download file.
- Multi-thread berjalan untuk server yang support range.
- Single-thread fallback berjalan.
- Progress, speed, dan ETA tampil.
- Pause/resume/cancel berjalan.
- History tersimpan.
- User bisa memilih aplikasi yang diblokir.
- VPNService Focus Mode aktif.
- Focus Mode otomatis mati setelah download selesai.
- UI responsif dan rapi.
- Tidak ada klaim palsu tentang mempercepat internet.

---

## 29. Execution Priority

Urutan pengerjaan paling efisien:

1. UI skeleton dulu.
2. URL analyzer.
3. Single-thread downloader.
4. Multi-thread downloader.
5. Foreground service + notification.
6. Room history.
7. Focus Mode VPNService.
8. Polish UI/UX.
9. Insights.
10. README + demo.

---

## 30. Final Product Identity

BandFocus harus terasa seperti:

- jujur,
- teknis tapi mudah dipahami,
- modern,
- tidak scammy,
- powerful untuk pengguna Android,
- bagus untuk portfolio developer.

Core message:

> Don’t boost fake speed. Focus real bandwidth.

