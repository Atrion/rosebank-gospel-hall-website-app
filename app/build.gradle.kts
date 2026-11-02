import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release number in hundredths: 1 = version 0.01, 2 = version 0.02, and so on.
// Raise it by 1 for every new release; the version name and code follow automatically.
val releaseNumber = 102

// Launcher icon: built from the website's favicon every build.
// Put your own square image at icon/logo.png to override the download.
val launcherIconDir = layout.buildDirectory.dir("generated/launcher/res").get().asFile

val generateLauncherIcon = tasks.register("generateLauncherIcon") {
    val out = launcherIconDir
    val localLogo = rootProject.file("icon/logo.png")
    outputs.dir(out)
    outputs.upToDateWhen { false }
    doLast {
        fun load(): BufferedImage? {
            if (localLogo.exists()) {
                runCatching { ImageIO.read(localLogo) }.getOrNull()?.let { return it }
            }
            val urls = listOf(
                "https://i0.wp.com/rosebankgospelhall.ca/wp-content/uploads/2026/04/Rosebank_logo_trans-1.png?fit=1024%2C1024&ssl=1",
                "https://rosebankgospelhall.ca/wp-content/uploads/2026/04/Rosebank_logo_trans-1.png",
                "https://i0.wp.com/rosebankgospelhall.ca/wp-content/uploads/2026/04/Rosebank_logo_trans-1.png?fit=270%2C270&ssl=1",
            )
            for (u in urls) {
                try {
                    val c = URL(u).openConnection()
                    c.setRequestProperty("User-Agent", "Mozilla/5.0")
                    c.connectTimeout = 15000
                    c.readTimeout = 30000
                    val img = c.getInputStream().use { ImageIO.read(it) }
                    if (img != null) return img
                } catch (e: Exception) {
                    logger.warn("Launcher icon: could not read $u (${e.message})")
                }
            }
            return null
        }

        val dst = File(out, "drawable-nodpi/ic_launcher_logo.png")
        val colorFile = File(out, "values/launcher_logo_background.xml")
        val src = load()
        if (src == null) {
            if (project.hasProperty("requireIcon")) {
                throw GradleException("Could not get the website icon. Add a square image at icon/logo.png or retry.")
            }
            if (dst.exists()) {
                logger.warn("Launcher icon: keeping the previously generated icon.")
                return@doLast
            }
            logger.warn("Launcher icon: using a plain placeholder because the website icon could not be loaded.")
        }

        val size = 432
        val canvas = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val gfx = canvas.createGraphics()
        gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        var background = "#7A2E3E"
        if (src != null) {
            var minX = src.width; var minY = src.height; var maxX = -1; var maxY = -1
            var lum = 0.0; var n = 0
            for (y in 0 until src.height) {
                for (x in 0 until src.width) {
                    val p = src.getRGB(x, y)
                    if ((p ushr 24) > 16) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                        lum += 0.299 * ((p shr 16) and 0xFF) + 0.587 * ((p shr 8) and 0xFF) + 0.114 * (p and 0xFF)
                        n++
                    }
                }
            }
            if (n == 0) { minX = 0; minY = 0; maxX = src.width - 1; maxY = src.height - 1; lum = 128.0; n = 1 }
            val w = maxX - minX + 1
            val h = maxY - minY + 1
            val scale = 276.0 / maxOf(w, h)
            val dw = maxOf(1, (w * scale).toInt())
            val dh = maxOf(1, (h * scale).toInt())
            val x0 = (size - dw) / 2
            val y0 = (size - dh) / 2
            gfx.drawImage(src, x0, y0, x0 + dw, y0 + dh, minX, minY, maxX + 1, maxY + 1, null)
            // Dark logos get a white background, light logos a deep red one.
            background = if (lum / n < 140.0) "#FFFFFF" else "#7A2E3E"
        } else {
            gfx.color = Color.WHITE
            gfx.fillRect(206, 120, 20, 190)
            gfx.fillRect(165, 160, 102, 20)
        }
        gfx.dispose()

        dst.parentFile.mkdirs()
        colorFile.parentFile.mkdirs()
        ImageIO.write(canvas, "png", dst)
        colorFile.writeText(
            "<resources>\n    <color name=\"ic_launcher_logo_bg\">$background</color>\n</resources>\n",
        )
    }
}

android {
    namespace = "ca.rosebankgospelhall.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ca.rosebankgospelhall.app"
        minSdk = 26
        targetSdk = 34
        versionCode = releaseNumber
        versionName = "${releaseNumber / 100}.${"%02d".format(releaseNumber % 100)}"
    }

    sourceSets.getByName("main").res.srcDir(launcherIconDir)

    // Releases are signed with your own key when KEYSTORE_FILE and the related variables are
    // provided (the GitHub workflow does this from repository secrets); otherwise the debug key is used.
    val releaseKeystore = System.getenv("KEYSTORE_FILE")?.let { File(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    lint { checkReleaseBuilds = false }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

tasks.named("preBuild") { dependsOn(generateLauncherIcon) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
