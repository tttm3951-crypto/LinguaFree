plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "org.linguafree.app"
    compileSdk = 35

    defaultConfig {
        // Kept from LinguaFree 0.9 so the new version installs over it and keeps progress.
        applicationId = "org.linguafree.app"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("LINGUAFREE_VERSION_CODE")?.toInt() ?: 10
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("ru", "en")
    }

    signingConfigs {
        // Release keys are provided only by CI secrets. Nothing secret is stored in the repository.
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
            if (System.getenv("LINGUAFREE_SIGN_DEBUG_FOR_UPGRADE") == "true") {
                signingConfig = signingConfigs.getByName("linguafreeRelease")
            }
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (!System.getenv("LINGUAFREE_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("linguafreeRelease")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*", "DebugProbesKt.bin")
    }
    lint {
        // Lint runs in CI; translation completeness is tracked separately while only Russian exists.
        disable += listOf("MissingTranslation")
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:path"))
    implementation(project(":feature:lesson"))
    implementation(project(":feature:practice"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:leagues"))
    implementation(project(":feature:shop"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
