plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.parcelize)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.gmail.vondenuelle.denuspend"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gmail.vondenuelle.denuspend"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            // Enables code shrinking, obfuscation, and optimization for only
            // your project's release build type. Make sure to use a build
            // variant with `isDebuggable=false`.
            isMinifyEnabled = true

            // Enables resource shrinking, which is performed by the
            // Android Gradle plugin.
            isShrinkResources = true

            // Cannot be debugged in logcat
            isDebuggable = false
            proguardFiles(
                // Includes the default ProGuard rules files that are packaged with
                // the Android Gradle plugin. To learn more, go to the section about
                // R8 configuration files.
                getDefaultProguardFile("proguard-android-optimize.txt"),
                // Includes a local, custom Proguard rules file
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isMinifyEnabled = false // Keeps debugging info for better traceability in development
            applicationIdSuffix = ".debug" // Optional: helps differentiate debug APK
            versionNameSuffix = "-debug"   // Optional: appends "-debug" to version name
            isDebuggable = true
        }
    }

     compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }  // Rename APK
    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            // Example result: AIPrompter_1.0_release.apk
            output?.outputFileName = "DenuSpend_${variant.versionName}_${variant.buildType.name}.apk"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

//    Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

//    Google Font
    implementation(libs.androidx.ui.text.google.fonts)

//    Navigation
    implementation (libs.androidx.navigation.compose)

//    Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

//    serialization
    implementation(libs.kotlinx.serialization.json)

//    work manager
    implementation(libs.androidx.work)

//    icons extended
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material.icons.core)

//    Room DB
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

//    Encryption
    implementation(libs.bcrypt)
    implementation(libs.androidx.security)
    implementation(libs.datastore)

//    Retrofit
    implementation(libs.retrofit2)
//    implementation(libs.retrofit2.moshi)
    implementation(libs.retrofit2.gson)

//    Ok HTTP
    implementation(libs.logging.interceptor)

//    Coil
    implementation(libs.coil)
    implementation(libs.coil.network)
    implementation(libs.coil.test)

//    Kotlin Reflect
    implementation(libs.kotlin.reflect)
    
//    Shimmer
    implementation(libs.shimmer)

//    Paging 3
    implementation(libs.paging)
    implementation(libs.paging.compose)

//    Palette
    implementation(libs.palette)

//    Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
//    implementation(libs.firebase.pnv)

//    Appcompat
    implementation(libs.appcompat)
}