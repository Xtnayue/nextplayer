package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.model.NetworkPlaybackHistory
import kotlinx.coroutines.flow.Flow

interface NetworkPlaybackHistoryRepository {
    fun observeHistory(): Flow<List<NetworkPlaybackHistory>>

    suspend fun record(item: NetworkPlaybackHistory)
}
