import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serializationJson)

    implementation(libs.compose.uiToolingPreview)
}

// Executa o demo CLI de reconhecimento de voz (Whisper), separado da app Compose.
// Uso: ./gradlew :desktopApp:runWhisperDemo --args="/caminho/ggml-base.bin"
tasks.register<JavaExec>("runWhisperDemo") {
    group = "application"
    description = "Grava do microfone e transcreve com Whisper (offline)."
    mainClass.set("com.sam.nox.WhisperDemoKt")
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`
}


// Abre a galeria com os 5 estados do orbe lado a lado (inspeção visual).
// Uso: ./gradlew :desktopApp:runOrbGallery
tasks.register<JavaExec>("runOrbGallery") {
    group = "application"
    description = "Mostra os estados do orbe (Idle/Listening/Thinking/Speaking/Error)."
    mainClass.set("com.sam.nox.GalleryMainKt")
    classpath = sourceSets["main"].runtimeClasspath
}

compose.desktop {
    application {
        mainClass = "com.sam.nox.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.sam.nox"
            packageVersion = "1.0.0"
        }
    }
}

// Passa as API keys (Groq e Tavily) do ambiente para a app como propriedades
// de sistema, para funcionarem mesmo quando a app é lançada pelo IDE.
tasks.withType<JavaExec>().configureEach {
    System.getenv("GROQ_API_KEY")?.let { systemProperty("groq.key", it) }
    System.getenv("TAVILY_API_KEY")?.let { systemProperty("tavily.key", it) }
    System.getenv("GITHUB_TOKEN")?.let { systemProperty("github.token", it) }
}
