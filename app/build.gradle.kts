import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.wchsoftworks.daltonpick"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wchsoftworks.daltonpick"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-spike"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":colorengine"))
}
