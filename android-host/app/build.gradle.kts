plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.xingce.ai"
    compileSdk = 35
    defaultConfig {
        applicationId = (project.findProperty("appId") as String?) ?: "com.xingce.ai"
        minSdk = 28
        targetSdk = 35
        versionCode = 38230
        versionName = (project.findProperty("verName") as String?) ?: "3.8.230-nh"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug") // 便于直接安装测试；正式发布再换正式签名
        }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }
}
