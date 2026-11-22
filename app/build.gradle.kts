plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.orbit.browser"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.orbit.browser"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        create("orbit") {
            storeFile = file(
                System.getenv("ORBIT_KEYSTORE_PATH")
                    ?: error("ORBIT_KEYSTORE_PATH is missing")
            )
            storePassword = System.getenv("ORBIT_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ORBIT_KEY_ALIAS")
            keyPassword = System.getenv("ORBIT_KEYSTORE_PASSWORD")
            storeType = "pkcs12"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("orbit")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17")
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
    implementation("com.google.firebase:firebase-auth")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}
