package com.sam.nox.presentation

/** Quem enviou a mensagem. */
enum class Author { User, Nox, Error }

/** Uma mensagem exibida na conversa. */
data class Message(val id: Long, val author: Author, val text: String)
