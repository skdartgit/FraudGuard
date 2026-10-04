plugins {
    id("com.android.application")
id("com.chaquo.python")
}

android {
    namespace = "com.sanat.fraudguard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sanat.fraudguard"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"
    }
}

dependencies {
    implementation("androidx.work:work-runtime:2.12.0")
    implementation("androidx.core:core-ktx:1.17.0")
}
