import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.paparazzi")
}

val versionName = "1.0"

// Read credentials from local.properties (gitignored).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun localProp(key: String, default: String = ""): String =
    localProps.getProperty(key, default).trim()

android {
    namespace = "com.mimik.wellnessnudge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mimik.wellnessnudge"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        this.versionName = versionName

        // Pixel 9 Pro XL is arm64; the SDK ships arm64-v8a only.
        ndk { abiFilters += "arm64-v8a" }

        // Credentials injected into BuildConfig at compile time.
        buildConfigField("String", "MIMIK_CLIENT_ID", "\"${localProp("mimik.clientId")}\"")
        buildConfigField(
            "String",
            "MIMIK_DEVELOPER_ID_TOKEN",
            "\"${localProp("mimik.developerIdToken")}\""
        )
        // Local mim bearer token — clients send this on /nudge requests.
        buildConfigField("String", "WELLNESS_API_KEY", "\"local-dev-key\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        noCompress += listOf("tar", "gguf", "lic")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // Tell the packager not to compress raw mim tars / models.
        jniLibs.useLegacyPackaging = false
    }

    buildTypes {
        debug {
            // The mimik SDK 3.18.0 ships native libs that aren't aligned for
            // Android 15's 16 KB page size. The OS only shows the warning
            // dialog on debuggable APKs, so build the dev variant as
            // non-debuggable to suppress it. We still see logcat output and
            // sideload via adb fine — only the JDWP attach is disabled.
            isDebuggable = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

// Paparazzi renders full 1008x2244 frames; give the test JVM room.
tasks.withType<Test>().configureEach {
    maxHeapSize = "2g"
}

dependencies {
    // mimik SDK — public S3 Maven repo, developer-tier variant
    implementation("com.mimik.mim-oe-sdk-android:mim-oe-ai-client-developer:3.18.0")
    implementation("com.mimik.mim-oe-sdk-android:mim-oe-milm-client:0.1.1") {
        // milm-client transitively depends on the non-AI client; we use the AI client.
        exclude(group = "com.mimik.mim-oe-sdk-android", module = "mim-oe-client")
    }

    // AndroidX core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // HTTP — talking to the local mim
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
