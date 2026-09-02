package dev.anilbeesetti.nextplayer.core.model

data class NetworkPlaybackHistory(
    val connectionId: Long,
    val filePath: String,
    val fileName: String,
    val fileSize: Long = 0,
    val playedAt: Long,
)
