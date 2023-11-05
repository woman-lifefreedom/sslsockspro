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
            abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
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
            storeFile = file(gradleLocalProperties(rootDir).getProperty("storeFile"))
            storePassword = gradleLocalProperties(rootDir).getProperty("storePassword")
            keyAlias = gradleLocalProperties(rootDir).getProperty("keyAlias")
            keyPassword = gradleLocalProperties(rootDir).getProperty("keyPassword")
        }
        getByName("debug") {
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
    implementation("com.google.code.gson:gson:2.9.0")
    implementation(group = "commons-io", name = "commons-io", version = "2.6")
}

//license {
//    header = project.file("../COPYING")
//
//    exclude = "**/de/blinkt/openvpn/api/**/*"
//}
