import org.gradle.kotlin.dsl.*
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    //id("org.cadixdev.licenser") version "0.6.1"
    id("com.android.application")
    id("kotlin-android")
}

//apply(plugin = "com.android.application")

android {
    compileSdk = 34
    defaultConfig {
        applicationId = "link.infra.sslsockspro"
        minSdk = 23
        targetSdk = 34
        versionCode = 5
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        externalNativeBuild {
            cmake {
                targets += listOf("stunnel")
            }
        }
        ndk {
            // Specifies the ABI configurations of your native
            // libraries Gradle should build and package with your app.
            // abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            abiFilters += listOf("arm64-v8a")
        }
    }

//    productFlavors {
//        create("full") {
//            externalNativeBuild {
//                cmake {
//                    // Specifies which native libraries or executables to build and package
//                    // for this product flavor. The following tells Gradle to build only the
//                    // "native-lib-demo" and "my-executible-demo" outputs from the linked
//                    // CMake project. If you don't configure this property, Gradle builds all
//                    // executables and shared object libraries that you define in your CMake
//                    // (or ndk-build) project. However, by default, Gradle packages only the
//                    // shared libraries in your app.
//                    targets += listOf("stunnel")
//                }
//            }
//        }
//    }

    externalNativeBuild {
        cmake {
            path = File("${projectDir}/src/main/cpp/CMakeLists.txt")
        }
    }

//    sourceSets["main"].java.srcDirs("$buildDir/generated/source/ovpn3swig")

    signingConfigs {
        create("release") {
            storeFile = file(gradleLocalProperties(rootDir, providers).getProperty("storeFile"))
            storePassword = gradleLocalProperties(rootDir, providers).getProperty("storePassword")
            keyAlias = gradleLocalProperties(rootDir, providers).getProperty("keyAlias")
            keyPassword = gradleLocalProperties(rootDir, providers).getProperty("keyPassword")
        }
        getByName("debug") {
            storeFile = file(gradleLocalProperties(rootDir, providers).getProperty("storeFile"))
            storePassword = gradleLocalProperties(rootDir, providers).getProperty("storePassword")
            keyAlias = gradleLocalProperties(rootDir, providers).getProperty("keyAlias")
            keyPassword = gradleLocalProperties(rootDir, providers).getProperty("keyPassword")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            //isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    buildFeatures {
        aidl=true
        buildConfig = true
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    ndkVersion = "25.1.8937393"
    buildToolsVersion = "34.0.0"
    namespace = "link.infra.sslsockspro"

}

//var swigcmd = "swig"
//
//fun registerGenTask(variantName: String, variantDirName: String): File {
//    val baseDir = File(buildDir, "generated/source/ovpn3swig")
//    val genDir = File(baseDir, "net/openvpn/ovpn3")
//
//    tasks.register<Exec>("generateOpenVPN3Swig${variantName}")
//    {
//
//        doFirst {
//            mkdir(genDir)
//        }
//        commandLine(listOf(swigcmd, "-outdir", genDir, "-outcurrentdir", "-c++", "-java", "-package", "net.openvpn.ovpn3",
//            "-Isrc/main/cpp/openvpn3/client", "-Isrc/main/cpp/openvpn3/",
//            "-DOPENVPN_PLATFORM_ANDROID",
//            "-o", "${genDir}/ovpncli_wrap.cxx", "-oh", "${genDir}/ovpncli_wrap.h",
//            "src/main/cpp/openvpn3/client/ovpncli.i"))
//        inputs.files( "src/main/cpp/openvpn3/client/ovpncli.i")
//        outputs.dir( genDir)
//
//    }
//    return baseDir
//}
//
//android.applicationVariants.all(object : Action<com.android.build.gradle.api.ApplicationVariant> {
//    override fun execute(variant: com.android.build.gradle.api.ApplicationVariant) {
//        val sourceDir = registerGenTask(variant.name, variant.baseName.replace("-", "/"))
//        val task = tasks.named("generateOpenVPN3Swig${variant.name}").get()
//
//        variant.registerJavaGeneratingTask(task, sourceDir)
//    }
//})

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    //implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.vectordrawable:vectordrawable:1.2.0")
    implementation("com.squareup.okio:okio:2.9.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("com.android.support:multidex:1.0.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(group = "commons-io", name = "commons-io", version = "2.6")

    // For Google Drive Rest API
    // Guava
    implementation("com.google.guava:guava:32.0.1-jre")
    // Guava fix
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
    //Drive
    implementation("com.google.api-client:google-api-client-android:1.23.0") {
        exclude(group = "org.apache.httpcomponents", module = "guava-jdk5")
    }
    implementation("com.google.apis:google-api-services-drive:v3-rev136-1.25.0") {
        exclude(group = "org.apache.httpcomponents", module = "guava-jdk5")
    }
}

//license {
//    header = project.file("../COPYING")
//
//    exclude = "**/de/blinkt/openvpn/api/**/*"
//}
