plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    id("base")
}

android {
    namespace = "io.github.stupidgame.CalYendar"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.stupidgame.CalYendar"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("CARYENDAR_KEY_PATH")
            val ksPass = System.getenv("KEYSTORE_PASSWORD")
            val keyAl  = "calyendar"
            val keyPass= System.getenv("_KEY_PASSWORD")

            // 環境変数が揃っている時だけ設定（ローカル開発で未設定でもビルドできるように）
            if (!ksPath.isNullOrBlank() && !ksPass.isNullOrBlank() && !keyAl.isNullOrBlank() && !keyPass.isNullOrBlank()) {
                storeFile = file(ksPath)
                storePassword = ksPass
                keyAlias = keyAl
                keyPassword = keyPass
            } else {
                // 未設定なら release 署名は設定されません（CI等で環境変数を入れてください）
                logger.warn("Release signing env vars are missing. APK will be unsigned or use default behavior.")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            // ★追加：release署名に signingConfigs.release を使う
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
    }
    packagingOptions {
        resources.excludes.add("META-INF/versions/9/module-info.class")
        resources.excludes.add("META-INF/INDEX.LIST")
        resources.excludes.add("META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule")
        resources.excludes.add("META-INF/groovy-release-info.properties")
    }
}

base {
    val appId = android.defaultConfig.applicationId
    val vName = android.defaultConfig.versionName ?: "0.0"
    val vCode = android.defaultConfig.versionCode

    // 例: io.github.stupidgame.curyendar-1.0-1-release.apk / .aab みたいな形になる
    archivesName.set("${appId}-${vName}-${vCode}")
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("net.sf.biweekly:biweekly:0.6.7")
    implementation("org.mnode.ical4j:ical4j:3.2.6")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}