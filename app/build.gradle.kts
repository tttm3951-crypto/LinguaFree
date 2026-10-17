plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "org.linguafree.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.linguafree.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "0.5"
    }
    signingConfigs {
        if (!System.getenv("LINGUAFREE_KEYSTORE_PATH").isNullOrBlank()) {
            create("linguafreeRelease") {
                storeFile = file(System.getenv("LINGUAFREE_KEYSTORE_PATH"))
                storePassword = System.getenv("LINGUAFREE_STORE_PASSWORD")
                keyAlias = System.getenv("LINGUAFREE_KEY_ALIAS")
                keyPassword = System.getenv("LINGUAFREE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            if (!System.getenv("LINGUAFREE_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("linguafreeRelease")
            }
            isMinifyEnabled = false
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
}
