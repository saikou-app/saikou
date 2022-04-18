import android.annotation.SuppressLint

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-kapt")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "ani.saikou"
        minSdk = 21
        targetSdk = 31
        versionCode = 41
        versionName = "1.1.6"
        signingConfig = signingConfigs["debug"]
    }

    buildTypes {
        release {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":saicore"))

    // Core
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.jsoup:jsoup:1.14.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    // separated notation to avoid IDE warning about updating to 1.6.0
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0-native-mt")

    implementation("com.github.bumptech.glide:glide:4.13.0")
    kapt("com.github.bumptech.glide:compiler:4.13.0")

    implementation(platform("com.google.firebase:firebase-bom:29.0.3"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // Exoplayer
    implementation("com.google.android.exoplayer:exoplayer-core:2.17.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.17.1")
    implementation("com.google.android.exoplayer:exoplayer-hls:2.17.1")
    implementation("com.google.android.exoplayer:extension-okhttp:2.17.1")
    implementation("com.google.android.exoplayer:extension-mediasession:2.17.1")

    // UI
    implementation("com.google.android.material:material:1.5.0")
    implementation("nl.joery.animatedbottombar:library:1.1.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("com.flaviofaria:kenburnsview:1.0.7")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
}
