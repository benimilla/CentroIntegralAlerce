plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.google.gms.google-services")
  id("com.google.firebase.crashlytics")
  kotlin("kapt")
}

android {
  namespace = "cl.alercelab.centrointegral"
  compileSdk = 35

  defaultConfig {
    applicationId = "cl.alercelab.centrointegral"
    minSdk = 24
    targetSdk = 35
    versionCode = 4
    versionName = "1.4"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables.useSupportLibrary = true
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
    debug { isMinifyEnabled = false }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions { jvmTarget = "17" }

  buildFeatures {
    viewBinding = true
    buildConfig = true
  }

  packaging {
    resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
  }
}

dependencies {

  // Android base
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.constraintlayout:constraintlayout:2.2.0")
  implementation("androidx.recyclerview:recyclerview:1.3.2")
  implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

  // Navigation
  implementation("androidx.navigation:navigation-fragment-ktx:2.8.3")
  implementation("androidx.navigation:navigation-ui-ktx:2.8.3")

  // Lifecycle
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")

  // WorkManager (para notificaciones locales)
  implementation("androidx.work:work-runtime-ktx:2.9.1")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

  // Firebase BoM (maneja versiones consistentes automáticamente)
  implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
  implementation("com.google.firebase:firebase-analytics-ktx")
  implementation("com.google.firebase:firebase-auth-ktx")
  implementation("com.google.firebase:firebase-firestore-ktx")
  implementation("com.google.firebase:firebase-storage-ktx")
  implementation("com.google.firebase:firebase-messaging-ktx")
  implementation("com.google.firebase:firebase-crashlytics-ktx")

  // Networking (para FCM manual)
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
