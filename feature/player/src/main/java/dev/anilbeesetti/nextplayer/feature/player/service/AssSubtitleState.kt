package dev.anilbeesetti.nextplayer.feature.player.service

import io.github.peerless2012.ass.media.AssHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object AssSubtitleState {
    private val mutableHandler = MutableStateFlow<AssHandler?>(null)

    val handler = mutableHandler.asStateFlow()

    fun update(handler: AssHandler?) {
        mutableHandler.value = handler
    }
}
