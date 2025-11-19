plugins {
//    alias(libs.plugins.android.application)
    id ("com.android.application")
    id ("com.google.gms.google-services")
}

android {
    namespace = "com.trans.translator"
    compileSdk = 36


    defaultConfig {
        applicationId = "com.trans.translator"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
//    implementation(libs.play.services.tasks)

//    implementation(libs["play.services.tasks"]) // ADD THIS LINE INSTEAD


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

//    implementation 'androidx.appcompat:appcompat:1.3.0'
    implementation ("com.google.android.material:material:1.4.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation ("com.google.firebase:firebase-core:17.3.0")
    implementation ("com.google.firebase:firebase-ml-natural-language:22.0.0")
    implementation ("com.google.firebase:firebase-ml-natural-language-language-id-model:20.0.7")
    implementation ("com.google.firebase:firebase-ml-natural-language-translate-model:20.0.8")

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")
}