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

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.webkit:webkit:1.11.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jsoup:jsoup:1.17.2")
    // Pure-Java unrar implementation, used to read .cbr comic archives.
    implementation("com.github.junrar:junrar:7.5.5")

    // Real Mihon/Keiyoushi extensions are compiled against extensions-lib, whose
    // Source/CatalogueSource/HttpSource contracts return RxJava 1 Observables.
    implementation("io.reactivex:rxjava:1.3.8")
    // Referenced by extensions implementing ConfigurableSource (login/settings screens).
    implementation("androidx.preference:preference-ktx:1.2.1")
    // The real DI container Tachiyomi/Mihon extensions use (`Injekt.get<NetworkHelper>()`,
    // `by injectLazy()`, etc.) — we register our own bindings for it in FolioApp so those
    // calls resolve. This is the exact fork (and commit) the real Mihon app itself depends
    // on (see gradle/libs.versions.toml in github.com/mihonorg/mihon) — published via
    // JitPack, not the original uy.kohesive.injekt artifacts on Maven Central.
    implementation("com.github.mihonapp:injekt:91edab2317")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
