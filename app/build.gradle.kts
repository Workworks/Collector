plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Signing material stays outside the repository. Never fall back to a machine-local debug key.
val releaseStore = providers.environmentVariable("COLLECTER_KEYSTORE").orNull
val releasePassword = providers.environmentVariable("COLLECTER_STORE_PASSWORD").orNull
val releaseAlias = providers.environmentVariable("COLLECTER_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("COLLECTER_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(releaseStore, releasePassword, releaseAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "com.kfaino.diapertracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kfaino.diapertracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 46
        versionName = "4.3.9"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) create("originalRelease") {
            storeFile = file(releaseStore!!)
            storePassword = releasePassword
            keyAlias = releaseAlias
            keyPassword = releaseKeyPassword
        }
    }
    buildTypes {
        release {
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("originalRelease") else null
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    // 单元测试底座：让 android.util.Log 等桩方法返回默认值而不是抛异常
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

tasks.matching { it.name == "packageRelease" }.configureEach {
    doFirst {
        check(hasReleaseSigning) { "Release requires COLLECTER_KEYSTORE, COLLECTER_STORE_PASSWORD, COLLECTER_KEY_ALIAS and COLLECTER_KEY_PASSWORD; debug signing fallback is forbidden." }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
