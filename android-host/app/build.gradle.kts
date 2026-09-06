plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.xingce.ai"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.xingce.ai"
        minSdk = 28
        targetSdk = 35
        versionCode = 38216
        versionName = "3.8.216-nh"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug") // 便于直接安装测试；正式发布再换正式签名
        }
    }
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
