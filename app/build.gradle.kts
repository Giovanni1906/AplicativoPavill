import java.io.FileInputStream
import java.util.Properties

/**
 * Configuración de build para PavillTaxi
 * 
 * Convención de nombres de APK:
 * - Debug: PavillTaxi-v{versionName}-b{versionCode}-debug.apk
 * - Release: PavillTaxi-v{versionName}-b{versionCode}-release.apk
 * - Con flavor: PavillTaxi-v{versionName}-b{versionCode}-{flavorName}-{buildType}.apk
 * 
 * Ejemplo: PavillTaxi-v3.01-b211-debug.apk
 */
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "radiotaxipavill.radiotaxipavillapp"
    compileSdk = 35

    // Cargar las propiedades del archivo key.properties
    val keystorePropertiesFile = rootProject.file("key.properties")
    val keystoreProperties = Properties().apply {
        load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    defaultConfig {
        applicationId = "radiotaxipavill.radiotaxipavillapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 307
        versionName = "3.07"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // Configuración para debug builds
        }
    }
    
    // Configuración personalizada para nombres de APK
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val buildType = variant.buildType.name
                val versionName = variant.versionName
                val versionCode = variant.versionCode
                val flavorName = variant.flavorName
                
                val outputFileName = when {
                    buildType == "debug" -> "PavillTaxi-v${versionName}-b${versionCode}-debug.apk"
                    buildType == "release" -> "PavillTaxi-v${versionName}-b${versionCode}-release.apk"
                    flavorName.isNotEmpty() -> "PavillTaxi-v${versionName}-b${versionCode}-${flavorName}-${buildType}.apk"
                    else -> "PavillTaxi-v${versionName}-b${versionCode}-${buildType}.apk"
                }
                
                output.outputFileName = outputFileName
                println("APK será generado como: $outputFileName")
            }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.playServicesMaps)  // Referencia correcta a Google Maps
    implementation(libs.playServicesLocation)
    implementation(libs.playServicesPlaces)
    implementation(libs.drawerlayout)  // Ubicación
    implementation(libs.android.maps.utils)
    implementation(libs.glide)
    implementation(libs.location.services)
    implementation(libs.cardview)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.playServicesPlaces)
}
