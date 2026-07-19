plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

val releaseKeystorePath = providers.gradleProperty("GAMESIDE_KEYSTORE_PATH").orElse(providers.environmentVariable("GAMESIDE_KEYSTORE_PATH")).orNull
val releaseKeystorePassword = providers.gradleProperty("GAMESIDE_KEYSTORE_PASSWORD").orElse(providers.environmentVariable("GAMESIDE_KEYSTORE_PASSWORD")).orNull
val releaseKeyAlias = providers.gradleProperty("GAMESIDE_KEY_ALIAS").orElse(providers.environmentVariable("GAMESIDE_KEY_ALIAS")).orNull
val releaseKeyPassword = providers.gradleProperty("GAMESIDE_KEY_PASSWORD").orElse(providers.environmentVariable("GAMESIDE_KEY_PASSWORD")).orNull
val hasPrivateReleaseSigning = listOf(releaseKeystorePath, releaseKeystorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "com.gameside.ai"
    compileSdk = 36

    defaultConfig {
        applicationId = providers.gradleProperty("GAME_SIDE_APPLICATION_ID").get()
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.1.3-game-detection"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasPrivateReleaseSigning) create("privateRelease") {
            storeFile = file(requireNotNull(releaseKeystorePath))
            storePassword = releaseKeystorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasPrivateReleaseSigning) signingConfig = signingConfigs.getByName("privateRelease")
        }
        create("betaRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":device"))
    implementation(project(":features"))

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    implementation("com.google.dagger:hilt-android:2.57.1")
    kapt("com.google.dagger:hilt-compiler:2.57.1")
}

kapt { correctErrorTypes = true }
