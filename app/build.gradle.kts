import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val moduleProp = rootProject.file("module/module.prop")
    .readLines()
    .filter { it.contains("=") }
    .associate { line ->
        val (k, v) = line.split("=", limit = 2)
        k.trim() to v.trim()
    }

android {
    namespace = "com.enginex0.usbmassstorage"
    compileSdk = 35

    signingConfigs {
        create("release") {
            val props = rootProject.file("local.properties")
                .takeIf { it.exists() }
                ?.inputStream()?.use { stream -> Properties().also { it.load(stream) } }

            storeFile = file("../release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: props?.getProperty("keystore.password") ?: ""
            keyAlias = System.getenv("KEY_ALIAS")
                ?: props?.getProperty("key.alias") ?: "usbms"
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: props?.getProperty("key.password") ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.enginex0.usbmassstorage"
        minSdk = 30
        targetSdk = 35
        versionCode = moduleProp["versionCode"]!!.toInt()
        versionName = moduleProp["version"]!!.removePrefix("v")

        buildConfigField("String", "PROJECT_URL", "\"https://github.com/enginex0/UsbMassStorage\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.compose)
    implementation(libs.core.ktx)
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.coroutines.android)
    implementation(libs.material)
}
