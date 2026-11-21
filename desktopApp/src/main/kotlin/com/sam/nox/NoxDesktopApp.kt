package com.sam.nox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.sam.nox.Speaker
import com.sam.nox.chat.createHttpClient
import com.sam.nox.data.ChatRepository
import com.sam.nox.mcp.McpManager
import com.sam.nox.memory.MemoryStore
import com.sam.nox.presentation.ChatViewModel
import com.sam.nox.ui.ChatScreen
import com.sam.nox.ui.theme.NoxTheme
import com.sam.nox.voice.ElevenLabsSpeaker
import com.sam.nox.voice.SystemSpeaker
import com.sam.nox.voice.createVoiceInput
import java.nio.file.Files
import java.nio.file.Path

/**
 * App Desktop da Nox: monta data → presentation → ui e exibe a janela de chat.
 * O microfone liga-se ao Whisper quando há modelo GGML disponível.
 *
 * @param model id do modelo no Groq (ex.: "qwen/qwen3.8-27b").
 */
@Composable
fun NoxDesktopApp(model: String = "qwen/qwen3.8-27b") {
    val scope = rememberCoroutineScope()
    val voice = remember { createVoiceInput(resolveWhisperModel()) }
    // TTS neural (ElevenLabs) quando há chave; senão cai no `say` do sistema.
    val speaker: Speaker =
        remember {
            val eleven = ElevenLabsSpeaker(http = createHttpClient())
            if (eleven.available) eleven else SystemSpeaker()
        }
    // O ViewModel cria o repository passando seu próprio confirm (para pausar
    // ações que precisam de aprovação do usuário).
    val viewModel =
        remember {
            ChatViewModel(
                scope = scope,
                repositoryFactory = { confirm ->
                    ChatRepository(
                        modelName = model,
                        confirm = confirm,
                        memory = MemoryStore(),
                        mcp = McpManager(),
                    )
                },
                voice = voice,
                speak = { text -> speaker.speak(text) },
            )
        }

    DisposableEffect(viewModel) {
        onDispose { viewModel.close() }
    }

    val process by viewModel.stateFlow.collectAsState()

    NoxTheme(dark = true) {
        ChatScreen(
            state = process.content,
            onEvent = viewModel::onSendEvent,
        )
    }
}

/**
 * Procura o modelo GGML do Whisper em locais conhecidos. Prioriza a propriedade
 * de sistema `nox.model` e a variável `NOX_MODEL`.
 */
fun resolveWhisperModel(): Path? {
    val userDir = System.getProperty("user.dir") ?: "."
    // Diretórios base a procurar: pasta atual, raiz do projeto (um nível acima, caso
    // esteja rodando de dentro de :desktopApp), e o home do usuário.
    val baseDirs =
        listOf(
            userDir,
            "$userDir/..",
            System.getProperty("user.home")?.let { "$it/.nox" },
        ).filterNotNull()
    // Nomes de modelo aceitos, dentro da pasta base ou de models/.
    val names = listOf("ggml-base.bin", "ggml-small.bin")

    val candidates =
        buildList {
            System.getProperty("nox.model")?.let { add(it) }
            System.getenv("NOX_MODEL")?.let { add(it) }
            baseDirs.forEach { dir ->
                names.forEach { name ->
                    add("$dir/$name")
                    add("$dir/models/$name")
                }
            }
        }
    return candidates
        .map { Path.of(it) }
        .firstOrNull { Files.exists(it) }
        ?.toAbsolutePath()
        ?.normalize()
}
