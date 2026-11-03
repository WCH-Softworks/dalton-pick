import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.play.publisher)
}

// Release secrets come from the environment only (GitHub Actions secrets, see docs/RELEASE.md).
// Nothing secret is ever written to the repo.
fun env(name: String): String? = providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }

val uploadKeystore = env("DALTONPICK_UPLOAD_KEYSTORE")

android {
    namespace = "com.wchsoftworks.daltonpick"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wchsoftworks.daltonpick"
        minSdk = 30
        targetSdk = 36
        // CI passes a new, higher code for every release build; local builds stay at 1.
        versionCode = env("DALTONPICK_VERSION_CODE")?.toInt() ?: 1
        versionName = "0.1.0"
    }

    signingConfigs {
        // The upload key. Play App Signing re-signs with Google's app-signing key.
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = file(uploadKeystore)
                storePassword = env("DALTONPICK_UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = env("DALTONPICK_UPLOAD_KEY_ALIAS")
                keyPassword = env("DALTONPICK_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without the upload key the release AAB is built unsigned (fine for checking R8).
            signingConfig = signingConfigs.findByName("upload")
        }
    }

    dependenciesInfo {
        // Play reads the dependency report from the AAB; keep the opaque blob out of APKs (F-Droid).
        includeInApk = false
        includeInBundle = true
    }

    buildFeatures {
        compose = true
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

// Gradle Play Publisher: ./gradlew publishReleaseBundle, ./gradlew publishListing.
// The service-account JSON comes from ANDROID_PUBLISHER_CREDENTIALS. Store text is in src/main/play.
play {
    defaultToAppBundles.set(true)
    track.set(env("DALTONPICK_PLAY_TRACK") ?: "internal")
    // Play only accepts drafts until the app's first release has been reviewed.
    releaseStatus.set(ReleaseStatus.valueOf((env("DALTONPICK_PLAY_STATUS") ?: "draft").uppercase()))
}

dependencies {
    implementation(project(":colorengine"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.kotlinx.coroutines.android)

    // Main app UI (spec §5.1). AndroidX only: no network, no third-party SDKs.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Camera mode (spec §4.4): CameraX is AndroidX too, and makes no network calls.
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
