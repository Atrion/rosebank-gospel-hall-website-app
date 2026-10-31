# Rosebank Gospel Hall – Android app

Native Android app (Kotlin, Jetpack Compose, Material 3) for rosebankgospelhall.ca.

## Build the APK

**Option A – Android Studio (easiest)**
1. Open this folder in Android Studio (Koala or newer).
2. Wait for Gradle sync.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. The APK is in `app/build/outputs/apk/debug/`.

**Option B – GitHub (no install)**
1. Push this folder to a new GitHub repository (branch `main`).
2. Open the Actions tab and run "Build APK".
3. Download `rosebank-gospel-hall-apk` from the finished run.

**Option C – Command line** (JDK 17 + Android SDK + Gradle 8.7): `gradle assembleDebug`

## Where content comes from
Posts load from the site's WordPress API (`/wp-json/wp/v2/posts`), with the RSS feed as a backup.
Pages (What we believe, Resources, Free food hamper) load from `/wp-json/wp/v2/pages?slug=...`.
Contact details and the verse are in `data/Models.kt` (`Site`).
