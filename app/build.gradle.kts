import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val mapsApiKey = providers.gradleProperty("MAPS_API_KEY").orNull
    ?: localProperties.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.spacetecsolutions.meatapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.spacetecsolutions.meatapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.0.0"
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
        manifestPlaceholders["deepLinkScheme"] = "meatbush"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "client"

    productFlavors {
        create("meatstation") {
            dimension = "client"
            applicationId = "com.spacetecsolutions.meatstation"
            manifestPlaceholders["deepLinkScheme"] = "meatstation"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:superadmin"))
    implementation(project(":feature:admin"))
    implementation(project(":feature:catalog"))
    implementation(project(":feature:customerhome"))
    implementation(project(":feature:cart"))
    implementation(project(":feature:address"))
    implementation(project(":feature:checkout"))
    implementation(project(":feature:orders"))
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.hilt.android)
    implementation(libs.razorpay.checkout)
    implementation(libs.play.services.location)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
}
