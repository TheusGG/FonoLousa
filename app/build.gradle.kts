plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use(::load)
    }
}

fun localProperty(name: String): String? =
    localProperties.getProperty(name) ?: providers.environmentVariable(name).orNull

android {
    namespace = "com.fonolousa.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fonolousa.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 25
        versionName = "1.0.24"
        buildConfigField(
            "String",
            "UPDATE_MANIFEST_URL",
            "\"${providers.gradleProperty("fonolousa.updateManifestUrl").getOrElse("https://raw.githubusercontent.com/TheusGG/FonoLousa/main/docs/fonolousa-update.json")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val storePath = localProperty("FONOLOUSA_RELEASE_STORE_FILE")
            if (!storePath.isNullOrBlank()) {
                storeFile = rootProject.file(storePath)
                storePassword = localProperty("FONOLOUSA_RELEASE_STORE_PASSWORD")
                keyAlias = localProperty("FONOLOUSA_RELEASE_KEY_ALIAS")
                keyPassword = localProperty("FONOLOUSA_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("androidx.room:room-ktx:2.7.2")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("com.google.code.gson:gson:2.13.1")
    ksp("androidx.room:room-compiler:2.7.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
