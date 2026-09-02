package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.database.dao.NetworkPlaybackHistoryDao
import dev.anilbeesetti.nextplayer.core.database.entities.NetworkPlaybackHistoryEntity
import dev.anilbeesetti.nextplayer.core.model.NetworkPlaybackHistory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LocalNetworkPlaybackHistoryRepository @Inject constructor(
    private val dao: NetworkPlaybackHistoryDao,
) : NetworkPlaybackHistoryRepository {
    override fun observeHistory(): Flow<List<NetworkPlaybackHistory>> =
        dao.observeAll().map { items -> items.map { it.toModel() } }

    override suspend fun record(item: NetworkPlaybackHistory) = dao.upsert(item.toEntity())

    private fun NetworkPlaybackHistoryEntity.toModel() = NetworkPlaybackHistory(
        connectionId = connectionId,
        filePath = filePath,
        fileName = fileName,
        fileSize = fileSize,
        playedAt = playedAt,
    )

    private fun NetworkPlaybackHistory.toEntity() = NetworkPlaybackHistoryEntity(
        connectionId = connectionId,
        filePath = filePath,
        fileName = fileName,
        fileSize = fileSize,
        playedAt = playedAt,
    )
}
