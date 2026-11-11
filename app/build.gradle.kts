plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.waha.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.waha.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    signingConfigs {
        create("release") {
            val ks = System.getenv("WAHA_KEYSTORE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("WAHA_KS_PASS")
                keyAlias = System.getenv("WAHA_KEY_ALIAS")
                keyPassword = System.getenv("WAHA_KEY_PASS")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("WAHA_KEYSTORE") != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    androidResources { noCompress += listOf("db", "json") }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.vm)
    implementation(libs.lifecycle.runtime)
    implementation(libs.datastore)
    implementation(libs.adhan)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
}
