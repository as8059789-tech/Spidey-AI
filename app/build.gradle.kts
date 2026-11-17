plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.spidey.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.spidey.ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1-stage1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
