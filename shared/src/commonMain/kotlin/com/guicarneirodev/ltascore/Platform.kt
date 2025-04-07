package com.guicarneirodev.ltascore

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform