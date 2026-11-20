package com.samuel.assistente.input.voice

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import kotlin.io.path.isDirectory
import kotlin.math.abs

/**
 * Reconhecimento de voz offline com Vosk. Na primeira vez baixa o modelo em português
 * (~31 MB) para [modelsDir]; depois tudo roda localmente, sem enviar áudio para a rede.
 */
class VoskSpeechRecognizer(
    private val modelsDir: Path,
    private val modelName: String = "vosk-model-small-pt-0.3",
    private val maxDurationMillis: Long = 30_000,
) : SpeechRecognizer {

    @Volatile private var stopRequested = false
    private val modelMutex = Mutex()
    private var model: Model? = null

    override fun stopListening() {
        stopRequested = true
    }

    override fun listen(): Flow<SpeechEvent> = flow {
        stopRequested = false
        val model = loadModel { emit(SpeechEvent.Preparing(it)) }

        val format = AudioFormat(SAMPLE_RATE, 16, 1, true, false) // PCM 16 bits mono, little-endian
        val line = try {
            AudioSystem.getTargetDataLine(format).apply { open(format); start() }
        } catch (e: LineUnavailableException) {
            throw SpeechError("Não foi possível abrir o microfone.", e)
        } catch (e: IllegalArgumentException) {
            throw SpeechError("Nenhum microfone compatível encontrado.", e)
        } catch (e: SecurityException) {
            throw SpeechError("Acesso ao microfone negado.", e)
        }

        emit(SpeechEvent.Listening)

        var heardSomething = false
        var text = ""
        try {
            Recognizer(model, SAMPLE_RATE).use { recognizer ->
                val buffer = ByteArray(4096) // ~128 ms de áudio
                val deadline = System.currentTimeMillis() + maxDurationMillis
                var lastPartial = ""

                while (!stopRequested && currentCoroutineContext().isActive &&
                    System.currentTimeMillis() < deadline
                ) {
                    val read = line.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    if (!heardSomething) heardSomething = hasSignal(buffer, read)

                    if (recognizer.acceptWaveForm(buffer, read)) {
                        // O Vosk detectou o fim de uma frase (pausa na fala).
                        val sentence = field(recognizer.result, "text")
                        if (sentence.isNotBlank()) {
                            text = sentence
                            break
                        }
                    } else {
                        val partial = field(recognizer.partialResult, "partial")
                        if (partial != lastPartial) {
                            lastPartial = partial
                            emit(SpeechEvent.Partial(partial))
                        }
                    }
                }
                if (text.isBlank()) text = field(recognizer.finalResult, "text")
            }
        } finally {
            line.stop()
            line.close()
        }

        if (!heardSomething) {
            // Sem permissão, o macOS entrega só zeros em vez de lançar erro.
            throw SpeechError(
                "O microfone só captou silêncio. Confira a permissão em Ajustes do Sistema → " +
                    "Privacidade e Segurança → Microfone."
            )
        }
        emit(SpeechEvent.Final(text.trim()))
    }.flowOn(Dispatchers.IO)

    private suspend fun loadModel(onStatus: suspend (String) -> Unit): Model = modelMutex.withLock {
        model?.let { return it }

        val modelDir = modelsDir.resolve(modelName)
        if (!modelDir.isDirectory()) {
            onStatus("Baixando modelo de voz (~31 MB, só na primeira vez)…")
            downloadModel(modelDir)
        }
        onStatus("Carregando modelo de voz…")
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS)
            Model(modelDir.toString()).also { model = it }
        } catch (e: IOException) {
            throw SpeechError("Falha ao carregar o modelo de voz em $modelDir.", e)
        } catch (e: LinkageError) {
            // UnsatisfiedLinkError etc.: a biblioteca nativa do Vosk não carregou nesta máquina.
            throw SpeechError("Não foi possível carregar o motor de voz (Vosk): ${e.message}", e)
        }
    }

    private suspend fun downloadModel(modelDir: Path) {
        Files.createDirectories(modelsDir)
        val zip = Files.createTempFile(modelsDir, "download-", ".zip")
        val staging = Files.createTempDirectory(modelsDir, "unzip-")
        try {
            HttpClient(CIO) {
                install(HttpTimeout) { requestTimeoutMillis = 10 * 60_000 }
            }.use { client ->
                client.prepareGet("$MODELS_URL/$modelName.zip").execute { response ->
                    if (!response.status.isSuccess()) {
                        throw SpeechError("Falha ao baixar o modelo de voz (HTTP ${response.status.value}).")
                    }
                    response.bodyAsChannel().toInputStream().use {
                        Files.copy(it, zip, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
            unzip(zip, staging)
            // Move de uma vez só para não deixar um modelo pela metade se algo falhar.
            Files.move(staging.resolve(modelName), modelDir, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SpeechError) {
            throw e
        } catch (e: IOException) {
            throw SpeechError("Não foi possível baixar o modelo de voz. Verifique a internet.", e)
        } finally {
            Files.deleteIfExists(zip)
            staging.toFile().deleteRecursively()
        }
    }

    private fun unzip(zip: Path, target: Path) {
        val root = target.normalize()
        ZipInputStream(Files.newInputStream(zip)).use { input ->
            generateSequence { input.nextEntry }.forEach { entry ->
                val out = root.resolve(entry.name).normalize()
                if (!out.startsWith(root)) throw IOException("Entrada inválida no zip: ${entry.name}")
                if (entry.isDirectory) {
                    Files.createDirectories(out)
                } else {
                    Files.createDirectories(out.parent)
                    Files.copy(input, out, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun field(json: String, key: String): String =
        Json.parseToJsonElement(json).jsonObject[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun hasSignal(buffer: ByteArray, length: Int): Boolean {
        var i = 0
        while (i + 1 < length) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            if (abs(sample.toShort().toInt()) > SILENCE_THRESHOLD) return true
            i += 2
        }
        return false
    }

    override fun close() {
        stopRequested = true
        model?.close()
        model = null
    }

    private companion object {
        const val SAMPLE_RATE = 16_000f
        const val SILENCE_THRESHOLD = 30
        const val MODELS_URL = "https://alphacephei.com/vosk/models"
    }
}
