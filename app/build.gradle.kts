import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 版本号统一来源：根目录 version.txt（游戏内显示、构建产物、Release 标签三者保持一致）
val versionValue: String = rootProject.file("version.txt").readText().trim()
val semver: List<String> = versionValue.split(".")

android {
    namespace = "com.stockidle.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.stockidle.game"
        minSdk = 26
        targetSdk = 35
        versionName = versionValue
        versionCode = semver[0].toInt() * 10000 + semver[1].toInt() * 100 + semver[2].toInt()
    }

    signingConfigs {
        create("release") {
            // 签名信息全部来自 CI 环境变量（GitHub Secrets），本地不落盘
            val ksPath = System.getenv("SIGNING_KEY_PATH")
            if (ksPath != null) {
                storeFile = rootProject.file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("SIGNING_KEY_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "META-INF/LICENSE.md"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
