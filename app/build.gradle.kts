plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.cryptotracker"
    compileSdk = 36 // ✅ Use stable SDK version

    defaultConfig {
        applicationId = "com.example.cryptotracker"
        minSdk = 26
        targetSdk = 36 // ✅ Match compileSdk for stability
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        buildTypes {
            release {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        kotlinOptions {
            jvmTarget = "11"
        }

        buildFeatures {
            compose = false // XML UI only
        }
    }

    dependencies {
        // Core AndroidX
        implementation(libs.androidx.core.ktx)
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation(libs.androidx.lifecycle.runtime.ktx)

        // Material Design
        implementation("com.google.android.material:material:1.12.0")

        // Retrofit & GSON
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")

        // Moshi & Scalars
        implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
        implementation("com.squareup.moshi:moshi:1.15.0")
        implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
        implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
        implementation("com.journeyapps:zxing-android-embedded:4.3.0")
        implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

        // Testing
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
    }
}