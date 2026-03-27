plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.enginex0.usbmassstorage"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file("../release.jks")
            storePassword = "usbmassstorage"
            keyAlias = "usbms"
            keyPassword = "usbmassstorage"
        }
    }

    defaultConfig {
        applicationId = "com.enginex0.usbmassstorage"
        minSdk = 30
        targetSdk = 35
        versionCode = 310
        versionName = "3.1.0"

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
