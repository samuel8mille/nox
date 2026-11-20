import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)

    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.swing) // fornece Dispatchers.Main no desktop
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.vosk)
            implementation(libs.jna) // o Vosk traz a JNA 5.7; a mais nova tem suporte melhor a macOS

            runtimeOnly(libs.slf4j.nop) // silencia o aviso "No SLF4J providers" do Ktor
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.samuel.assistente.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "MinhaAssistente"
            packageVersion = "1.0.0"
            description = "Assistente virtual pessoal"
            vendor = "Samuel"

            // Módulos do JDK que o jlink precisa incluir no runtime embutido.
            // jdk.crypto.ec: cifras TLS de curva elíptica (sem ele o HTTPS falha no .dmg).
            // Confira com: ./gradlew :composeApp:suggestRuntimeModules
            // java.desktop: captura de áudio (javax.sound); jdk.unsupported: JNA, usada pelo Vosk.
            modules("java.desktop", "java.naming", "jdk.crypto.ec", "jdk.unsupported")

            macOS {
                bundleID = "com.samuel.assistente"
                appCategory = "public.app-category.productivity"
                // Sem esta chave o macOS não pede permissão e o microfone grava só silêncio.
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSMicrophoneUsageDescription</key>
                        <string>A MinhaAssistente usa o microfone para entender perguntas por voz.</string>
                    """.trimIndent()
                }
                // iconFile.set(project.file("icons/icon.icns"))
            }
        }
    }
}
