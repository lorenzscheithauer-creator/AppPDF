plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android { namespace = "de.pdfwerkzeugkasten"; compileSdk = 35
    defaultConfig { applicationId = "de.pdfwerkzeugkasten"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    signingConfigs { create("release") { storeFile = file(System.getenv("PDFWZ_KEYSTORE") ?: "release-keystore.jks"); storePassword = System.getenv("PDFWZ_KEYSTORE_PASSWORD") ?: "changeit"; keyAlias = System.getenv("PDFWZ_KEY_ALIAS") ?: "pdfwerkzeugkasten"; keyPassword = System.getenv("PDFWZ_KEY_PASSWORD") ?: "changeit" } }
    buildTypes { debug { applicationIdSuffix = ".debug" }; release { isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"); signingConfig = signingConfigs.getByName("release") } }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*") }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.3")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.01.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
