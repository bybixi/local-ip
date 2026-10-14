plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.localip.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.localip.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.4.0"
        vectorDrawables { useSupportLibrary = true }
        // iperf3 只提供 arm64 构建
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Kotlin 2.x: Compose 编译器由 org.jetbrains.kotlin.plugin.compose 提供，
    // 这里【不能】再写 composeOptions { kotlinCompilerExtensionVersion = ... }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // iperf3 是以「原生库」形式塞进 APK 的（jniLibs/arm64-v8a/libiperf3.so）。
        // 必须 useLegacyPackaging=true，安装时才会被解压到 nativeLibraryDir 并带上可执行权限 ——
        // 这是安卓上把一个可执行文件随 App 分发的标准做法（app 私有目录是不可执行的）。
        jniLibs {
            useLegacyPackaging = true
        }
    }
    lint {
        abortOnError = false
    }
}

// kotlinOptions 在 AGP 8.x 已废弃，改用 kotlin.compilerOptions（AGP 9 会移除旧写法）
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.compose.ui.tooling)
}
