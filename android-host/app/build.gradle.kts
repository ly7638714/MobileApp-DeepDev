plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val appAssetsDir = (project.findProperty("assetsDir") as String?) ?: "src/main/assets"
val appLabel = (project.findProperty("appLabel") as String?) ?: "行测名师AI小助理"
android {
    namespace = "com.xingce.ai"
    compileSdk = 35
    defaultConfig {
        applicationId = (project.findProperty("appId") as String?) ?: "com.xingce.ai"
        minSdk = 28
        targetSdk = 35
        val verNameResolved = (project.findProperty("verName") as String?) ?: "3.8.296-nh"
        versionName = verNameResolved
        // versionCode 自动由版本号推导：major*100000 + minor*1000 + patch（如 3.8.309 → 308309），
        // 保证同版本号始终得到同一 code、且跨版本单调递增（无需再手动改 38296）。
        // 仍可用 -PverCode= 手动覆盖（极少用）。
        versionCode = (project.findProperty("verCode") as String?)?.toIntOrNull()
            ?: run {
                val m = Regex("(\\d+)\\.(\\d+)\\.(\\d+)").find(verNameResolved)
                val (maj, min, pat) = if (m != null) Triple(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) else Triple(3, 8, 296)
                maj * 100000 + min * 1000 + pat
            }
        manifestPlaceholders["appLabel"] = appLabel
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
            assets.srcDirs(appAssetsDir)
        }
    }
}
