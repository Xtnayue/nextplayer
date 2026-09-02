package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "network_playback_history",
    primaryKeys = ["connection_id", "file_path"],
)
data class NetworkPlaybackHistoryEntity(
    @ColumnInfo(name = "connection_id") val connectionId: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "file_size") val fileSize: Long = 0,
    @ColumnInfo(name = "played_at") val playedAt: Long,
)
