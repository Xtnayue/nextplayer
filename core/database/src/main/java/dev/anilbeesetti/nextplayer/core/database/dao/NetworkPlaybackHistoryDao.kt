package dev.anilbeesetti.nextplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.anilbeesetti.nextplayer.core.database.entities.NetworkPlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkPlaybackHistoryDao {
    @Query("SELECT * FROM network_playback_history ORDER BY played_at DESC")
    fun observeAll(): Flow<List<NetworkPlaybackHistoryEntity>>

    @Upsert
    suspend fun upsert(item: NetworkPlaybackHistoryEntity)
}
