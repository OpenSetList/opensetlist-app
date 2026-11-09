import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val appVersionName = "1.2.0"

android {
    namespace = "com.opensetlist.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.opensetlist.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = appVersionName
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    val releaseKeystoreFile = file(System.getProperty("user.home") + "/.android/debug.keystore")

    signingConfigs {
        if (releaseKeystoreFile.exists()) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
}

base {
    archivesName.set("opensetlist")
}
