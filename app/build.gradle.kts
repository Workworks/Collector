plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kfaino.diapertracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kfaino.diapertracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 42
        versionName = "4.3.5"
    }

    buildTypes {
        release {
            // 个人自用：用调试签名打包，方便直接安装
            signingConfig = signingConfigs.getByName("debug")
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

dependencies {
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
}