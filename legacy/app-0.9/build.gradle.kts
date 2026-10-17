import java.net.URI

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
        versionName = "0.9"
        versionCode = System.getenv("LINGUAFREE_VERSION_CODE")?.toInt() ?: 9
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val classroomUrl = System.getenv("LINGUAFREE_CLASSROOM_URL").orEmpty().trimEnd('/')
        if (classroomUrl.isNotEmpty()) {
            val endpoint = URI(classroomUrl)
            require(endpoint.scheme == "https" && endpoint.host != null && endpoint.rawUserInfo == null && endpoint.rawQuery == null && endpoint.rawFragment == null) { "Classroom server must be a plain HTTPS endpoint" }
        }
        buildConfigField("String", "CLASSROOM_ENDPOINT", "\""+classroomUrl+"\"")
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
        getByName("debug") {
            // Used only by the private signed upgrade check; never publish this debug build.
            if (System.getenv("LINGUAFREE_SIGN_DEBUG_FOR_UPGRADE") == "true") {
                signingConfig = signingConfigs.getByName("linguafreeRelease")
            }
        }
        getByName("release") {
            if (!System.getenv("LINGUAFREE_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("linguafreeRelease")
            }
            isMinifyEnabled = false
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
}


// Offline corpus must be bundled; never silently ship an empty reference screen.
tasks.named("preBuild") {
    doFirst { require(file("src/main/assets/offline-reference.db").isFile) { "Run python3 scripts/build_offline_library.py before Gradle" } }
}
