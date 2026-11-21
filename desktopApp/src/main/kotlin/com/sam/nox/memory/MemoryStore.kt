package com.sam.nox.memory

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Um fato lembrado sobre o usuário, com um id estável para remoção. */
@Serializable
data class MemoryFact(val id: Int, val text: String)

@Serializable
private data class MemoryData(val nextId: Int = 1, val facts: List<MemoryFact> = emptyList())

/**
 * Memória persistente de fatos sobre o usuário, gravada num arquivo JSON local
 * (por padrão `~/.nox/memory.json`). Sobrevive a "Nova conversa" e a reinícios.
 *
 * As operações são resilientes: arquivo ausente ou corrompido vira memória vazia,
 * e erros de I/O não quebram o chat.
 */
class MemoryStore(private val file: File = defaultFile()) {
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    private var data: MemoryData = load()

    private fun load(): MemoryData = try {
        if (file.exists()) {
            json.decodeFromString(MemoryData.serializer(), file.readText())
        } else {
            MemoryData()
        }
    } catch (_: Throwable) {
        MemoryData()
    }

    private fun persist() {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(MemoryData.serializer(), data))
        } catch (e: Throwable) {
            // gravação falhou; memória em runtime segue válida
        }
    }

    /** Lista todos os fatos conhecidos. */
    fun list(): List<MemoryFact> = data.facts

    /** Adiciona um fato e devolve o fato criado (ignora duplicados exatos). */
    fun add(text: String): MemoryFact {
        val clean = text.trim()
        data.facts.firstOrNull { it.text.equals(clean, ignoreCase = true) }?.let { return it }
        val fact = MemoryFact(id = data.nextId, text = clean)
        data = data.copy(nextId = data.nextId + 1, facts = data.facts + fact)
        persist()
        return fact
    }

    /** Remove o fato cujo texto mais se aproxima do trecho informado. Devolve o removido ou null. */
    fun remove(query: String): MemoryFact? {
        val q = query.trim().lowercase()
        val match =
            data.facts.firstOrNull { it.text.lowercase().contains(q) }
                ?: data.facts.firstOrNull { q.contains(it.text.lowercase()) }
                ?: return null
        data = data.copy(facts = data.facts.filterNot { it.id == match.id })
        persist()
        return match
    }

    /** Localização padrão do arquivo de memória. */
    companion object {
        private fun defaultFile(): File = File(System.getProperty("user.home"), ".nox/memory.json")
    }
}
