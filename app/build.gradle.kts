import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.folio.reader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.folio.reader"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // Read from local.properties (gitignored) so the key never lands in source control.
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.webkit:webkit:1.11.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Pure-Java unrar implementation, used to read .cbr comic archives.
    implementation("com.github.junrar:junrar:7.5.5")

    // ──────────────────────────────────────────────────────────────────────────────
    // Keiyoushi extension runtime: the COMPLETE "common" bundle every extension is
    // compiled against (compileOnly) and expects the host app to provide at runtime.
    // Versions matched EXACTLY to keiyoushi/extensions-source gradle/libs.versions.toml
    // so an extension's compiled serializer/bytecode links against the same APIs it was
    // built with. A mismatch shows up as "Failed resolution of: <class>" or NoSuchMethodError.
    // Keiyoushi pins serialization to 1.7.3 deliberately (1.8+ drops generated classes the
    // compile-only lib needs) — do not bump past it.
    // ──────────────────────────────────────────────────────────────────────────────
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.squareup.okhttp3:okhttp-brotli:5.3.2")
    implementation("org.jsoup:jsoup:1.22.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("org.jspecify:jspecify:1.0.0")
    // JS engine some extensions use via eu.kanade.tachiyomi.network.JavaScriptEngine.
    implementation("app.cash.quickjs:quickjs-android:0.9.2")
    // Referenced by extensions implementing ConfigurableSource (login/settings screens).
    implementation("androidx.preference:preference-ktx:1.2.1")
    // The real DI container Tachiyomi/Mihon extensions use (`Injekt.get<NetworkHelper>()`,
    // `by injectLazy()`, etc.). Extensions' compiled bytecode bakes in (via Kotlin inline
    // functions) the exact internal call shape of whichever injekt fork *they* compiled
    // against — Keiyoushi's own build pins this precise fork+commit (see
    // gradle/libs.versions.toml in github.com/keiyoushi/extensions-source), which is
    // *not* the same fork Mihon itself uses, so we have to match Keiyoushi's, not Mihon's,
    // or extension classes referencing it fail to link at runtime.
    implementation("com.github.null2264.injekt:injekt-core:4135455a2a")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
