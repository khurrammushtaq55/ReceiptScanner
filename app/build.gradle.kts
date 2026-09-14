plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")


}

android {
    namespace = "com.mmushtaq.smartreceiptscanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mmushtaq.smartreceiptscanner"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

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
    buildFeatures { compose = true }
    // Compose compiler is bundled with Kotlin 2.0.21 via plugin; no extra line needed
    packaging.resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.splashscreen)

    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.compose.icons.extended)

    // AndroidX
    implementation(libs.core.ktx)
//    implementation(libs.lifecycleRuntimeKtx)
    // CameraX
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    implementation(libs.mlkit.text.recognition)
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    implementation(libs.guava)

    //Ads
    // Google Mobile Ads SDK (AdMob)
    implementation(libs.play.services.ads)
    // UMP (User Messaging Platform) for GDPR/EEA consent
//    implementation(libs.user.messaging.platform)

    // Thumbnails
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    // Real org.json implementation for plain JUnit tests — Android's bundled org.json in
    // android.jar is a stub that throws at runtime outside instrumentation tests.
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}