plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.vexa.vpn"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.vexa.vpn"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        val apiBaseUrl = providers.gradleProperty("VEXA_API_BASE_URL").orNull ?: ""
        buildConfigField("String", "VEXA_API_BASE_URL", "\"${apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.gradleProperty("VEXA_KEYSTORE_PATH").orNull ?: System.getenv("VEXA_KEYSTORE_PATH")
            val storePassword = providers.gradleProperty("VEXA_KEYSTORE_PASSWORD").orNull ?: System.getenv("VEXA_KEYSTORE_PASSWORD")
            val keyAlias = providers.gradleProperty("VEXA_KEY_ALIAS").orNull ?: System.getenv("VEXA_KEY_ALIAS")
            val keyPassword = providers.gradleProperty("VEXA_KEY_PASSWORD").orNull ?: System.getenv("VEXA_KEY_PASSWORD")
            if (!keystorePath.isNullOrBlank() && file(keystorePath).exists() &&
                !storePassword.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
                storeFile = file(keystorePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            val keystorePath = providers.gradleProperty("VEXA_KEYSTORE_PATH").orNull ?: System.getenv("VEXA_KEYSTORE_PATH")
            val storePassword = providers.gradleProperty("VEXA_KEYSTORE_PASSWORD").orNull ?: System.getenv("VEXA_KEYSTORE_PASSWORD")
            val keyAlias = providers.gradleProperty("VEXA_KEY_ALIAS").orNull ?: System.getenv("VEXA_KEY_ALIAS")
            val keyPassword = providers.gradleProperty("VEXA_KEY_PASSWORD").orNull ?: System.getenv("VEXA_KEY_PASSWORD")
            val hasReleaseSigning = !keystorePath.isNullOrBlank() && file(keystorePath).exists() &&
                !storePassword.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17; isCoreLibraryDesugaringEnabled = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("com.wireguard.android:tunnel:1.0.20260102")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
