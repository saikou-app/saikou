plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics") version "2.8.1"
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "ani.saikou"
        minSdk = 21
        targetSdk = 31
        versionCode = 43
        versionName = "1.2.0.2"
        signingConfig = signingConfigs.getByName("debug")
    }

    buildTypes {
        getByName("release") {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures.viewBinding = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions.jvmTarget = "11"
}

dependencies {
//    Core
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("com.github.Blatzar:NiceHttp:0.1.8")

    implementation("com.github.bumptech.glide:glide:4.13.1")
    kapt("com.github.bumptech.glide:compiler:4.13.1")

    implementation(platform("com.google.firebase:firebase-bom:29.0.3"))
    implementation("com.google.firebase:firebase-analytics-ktx:20.1.2")
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.2.10")

//    Exoplayer
    implementation("com.google.android.exoplayer:exoplayer-core:2.17.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.17.1")
    implementation("com.google.android.exoplayer:exoplayer-hls:2.17.1")
    implementation("com.google.android.exoplayer:extension-okhttp:2.17.1")
    implementation("com.google.android.exoplayer:extension-mediasession:2.17.1")

//    UI
    implementation("com.google.android.material:material:1.5.0")
    implementation("nl.joery.animatedbottombar:library:1.1.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("com.flaviofaria:kenburnsview:1.0.7")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
}