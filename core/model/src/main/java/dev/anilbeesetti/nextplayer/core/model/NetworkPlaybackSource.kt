package dev.anilbeesetti.nextplayer.core.model

data class NetworkPlaybackSource(
    val uri: String,
    val connectionId: Long,
    val filePath: String,
)
