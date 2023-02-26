import org.gradle.kotlin.dsl.*
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties



plugins {
    //id("org.cadixdev.licenser") version "0.6.1"
    id("com.android.application")
    id("kotlin-android")
}

//apply(plugin = "com.android.application")

android {
    compileSdk = 33
    defaultConfig {
        applicationId = "link.infra.sslsockspro"
        minSdk = 21
        targetSdk = 33
        versionCode = 4
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
//    sourceSets {
//        main {
//            jniLibs.srcDirs = ["src/main/jni"]
//        }
//    }
//

        signingConfigs {
            create("release") {
                storeFile = file(gradleLocalProperties(rootDir).getProperty("storeFile"))
                storePassword = gradleLocalProperties(rootDir).getProperty("storePassword")
                keyAlias = gradleLocalProperties(rootDir).getProperty("keyAlias")
                keyPassword = gradleLocalProperties(rootDir).getProperty("keyPassword")
            }
        }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }

    buildFeatures {
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    ndkVersion = "25.1.8937393"
    buildToolsVersion = "33.0.1"
}

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    //implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("com.squareup.okio:okio:2.9.0")
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("com.android.support:multidex:1.0.3")
    implementation(group = "commons-io", name = "commons-io", version = "2.6")
}

//license {
//    header = project.file("../COPYING")
//
//    exclude = "**/de/blinkt/openvpn/api/**/*"
//}
