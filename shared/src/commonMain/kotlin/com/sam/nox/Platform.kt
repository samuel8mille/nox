package com.sam.nox

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform