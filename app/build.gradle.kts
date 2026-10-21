import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Chave de assinatura do APK (fica fora do git: keystore.properties + subaru-dl.jks)
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Onde o app procura a versão nova. Para testar o atualizador sem publicar nada (com adb reverse tcp:8765 tcp:8765), só em build Debug:
// -PupdateApi=http://127.0.0.1:8765/latest.json -PupdateOrigin=http://127.0.0.1:8765/ -PtestVersionName=1.3.98 -PtestVersionCode=100
val testProperties = listOf("updateApi", "updateOrigin", "testVersionName", "testVersionCode").filter { findProperty(it) != null }
val updateApi = findProperty("updateApi") as String? ?: "https://api.github.com/repos/ailtoncja/subaru-dl-mobile/releases/latest"
val updateOrigin = findProperty("updateOrigin") as String? ?: "https://github.com/ailtoncja/subaru-dl-mobile/releases/download/"

android {
    namespace = "com.subarudl.app"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.subarudl.app"
        // Android 10+: salvar em Filmes/Música/Imagens sem pedir permissão (armazenamento por escopo)
        minSdk = 29
        targetSdk = 37
        versionCode = (findProperty("testVersionCode") as String?)?.toInt() ?: 9
        versionName = findProperty("testVersionName") as String? ?: "1.6.0"
        buildConfigField("String", "UPDATE_API", "\"$updateApi\"")
        buildConfigField("String", "UPDATE_ORIGIN", "\"$updateOrigin\"")
        // Celulares (arm64) e o emulador (x86_64): o resto só engordaria o APK
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 quebraria a biblioteca do yt-dlp (usa reflexão); o APK já é grande por causa do Python e do ffmpeg
            isMinifyEnabled = false
            // Sem keystore.properties não há Release (nada de cair na chave de testes): veja checkReleaseBuild
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        // Os binários (QuickJS, Python, ffmpeg…) precisam ser extraídos para a pasta nativa para poderem ser executados
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        disable += "IconLauncherShape"
    }
}

// Um Release nunca sai com o endereço de teste do atualizador, com versão falsa ou sem a chave de verdade
val releaseProblems = buildList {
    testProperties.forEach { add("-P$it é só para testar o atualizador num build Debug") }
    if (keystoreProps.isEmpty()) add("keystore.properties não encontrado: o Release precisa da chave do app (subaru-dl.jks)")
}
val checkReleaseBuild = tasks.register("checkReleaseBuild") {
    val problems = releaseProblems
    doLast { if (problems.isNotEmpty()) throw GradleException("Build Release recusado:\n" + problems.joinToString("\n") { " - $it" }) }
}
tasks.configureEach {
    if (name.contains("Release") && name != checkReleaseBuild.name) dependsOn(checkReleaseBuild)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.documentfile)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    // A lista pública de sufixos (com.br, co.uk, github.io…) para recusar cookies de um sufixo inteiro; já vinha com o Coil
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)
    implementation(libs.youtubedl.aria2c)
}
