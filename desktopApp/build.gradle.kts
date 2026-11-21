import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serializationJson)

    // Cliente HTTP (Ktor) usado pelo ElevenLabsSpeaker / McpClient no desktop.
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientOkHttp)

    implementation(libs.compose.uiToolingPreview)
}

// Executa o demo CLI de reconhecimento de voz (Whisper), separado da app Compose.
// Uso: ./gradlew :desktopApp:runWhisperDemo --args="/caminho/ggml-base.bin"
tasks.register<JavaExec>("runWhisperDemo") {
    group = "application"
    description = "Grava do microfone e transcreve com Whisper (offline)."
    mainClass.set("com.sam.nox.voice.WhisperDemoKt")
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`
}

// Verifica a conexão MCP (lê ~/.nox/mcp.json, arranca servidores, lista ferramentas).
// Uso: ./gradlew :desktopApp:runMcpSmoke
tasks.register<JavaExec>("runMcpSmoke") {
    group = "verification"
    description = "Testa o McpManager: expande env, arranca servidores MCP e lista ferramentas."
    mainClass.set("com.sam.nox.smoke.McpSmokeTestKt")
    classpath = sourceSets["main"].runtimeClasspath
}

// Teste ponta-a-ponta do chat com uma ação de GitHub via MCP (auto-aprova confirmações).
// Uso: ./gradlew :desktopApp:runGithubChatSmoke
tasks.register<JavaExec>("runGithubChatSmoke") {
    group = "verification"
    description = "Envia um comando de GitHub ao Nox e mostra a resposta (usa MCP)."
    mainClass.set("com.sam.nox.smoke.GithubChatSmokeKt")
    classpath = sourceSets["main"].runtimeClasspath
}

// Abre a galeria com os 5 estados do orbe lado a lado (inspeção visual).
// Uso: ./gradlew :desktopApp:runOrbGallery
tasks.register<JavaExec>("runOrbGallery") {
    group = "application"
    description = "Mostra os estados do orbe (Idle/Listening/Thinking/Speaking/Error)."
    mainClass.set("com.sam.nox.debug.GalleryMainKt")
    classpath = sourceSets["main"].runtimeClasspath
}

compose.desktop {
    application {
        mainClass = "com.sam.nox.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Nox"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("icons/Nox.icns"))
            }
        }
    }
}

// Passa as API keys (Groq e Tavily) do ambiente para a app como propriedades
// de sistema, para funcionarem mesmo quando a app é lançada pelo IDE.
tasks.withType<JavaExec>().configureEach {
    System.getenv("GROQ_API_KEY")?.let { systemProperty("groq.key", it) }
    System.getenv("TAVILY_API_KEY")?.let { systemProperty("tavily.key", it) }
    System.getenv("GITHUB_TOKEN")?.let { systemProperty("github.token", it) }
    System.getenv("ELEVENLABS_API_KEY")?.let { systemProperty("elevenlabs.key", it) }
    System.getenv("ELEVENLABS_VOICE_ID")?.let { systemProperty("elevenlabs.voice", it) }
}
