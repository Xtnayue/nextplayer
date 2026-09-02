package dev.anilbeesetti.nextplayer.feature.videopicker.screens.history

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.NetworkConnectionRepository
import dev.anilbeesetti.nextplayer.core.data.repository.NetworkPlaybackHistoryRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.media.network.proxy.NetworkStreamingProxy
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.Video
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    networkHistoryRepository: NetworkPlaybackHistoryRepository,
    private val networkConnectionRepository: NetworkConnectionRepository,
    private val streamingProxy: NetworkStreamingProxy,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val playEventChannel = Channel<HistoryPlayback>()
    val playEvents = playEventChannel.receiveAsFlow()

    val uiState = combine(
        mediaRepository.observeVideos(),
        networkHistoryRepository.observeHistory(),
        preferencesRepository.applicationPreferences,
    ) { videos, networkHistory, preferences ->
        val networkVideos = networkHistory.map { item ->
            val historyUri = Uri.Builder()
                .scheme(NETWORK_HISTORY_SCHEME)
                .authority(item.connectionId.toString())
                .appendQueryParameter("path", item.filePath)
                .appendQueryParameter("name", item.fileName)
                .build()
            Video(
                id = historyUri.toString().hashCode().toLong(),
                path = item.filePath,
                uriString = historyUri.toString(),
                nameWithExtension = item.fileName,
                duration = 0,
                width = 0,
                height = 0,
                size = item.fileSize,
                lastPlayedAt = Date(item.playedAt),
            )
        }
        HistoryUiState(
            videos = (videos + networkVideos).asHistory(preferences.historyLimit),
            preferences = preferences,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun playVideo(uri: Uri) {
        if (uri.scheme != NETWORK_HISTORY_SCHEME) {
            viewModelScope.launch { playEventChannel.send(HistoryPlayback(uri)) }
            return
        }
        val connectionId = uri.authority?.toLongOrNull() ?: return
        val filePath = uri.getQueryParameter("path") ?: return
        val fileName = uri.getQueryParameter("name") ?: return
        viewModelScope.launch {
            val connection = networkConnectionRepository.getConnection(connectionId) ?: return@launch
            val playbackUrl = streamingProxy.registerStream(connection, filePath, fileName)
            playEventChannel.send(HistoryPlayback(playbackUrl.toUri(), connectionId, filePath))
        }
    }

    private companion object {
        const val NETWORK_HISTORY_SCHEME = "nextplayer-network-history"
    }
}

data class HistoryPlayback(
    val uri: Uri,
    val networkConnectionId: Long? = null,
    val networkFilePath: String? = null,
)

@Stable
data class HistoryUiState(
    val videos: List<Video> = emptyList(),
    val preferences: ApplicationPreferences = ApplicationPreferences(),
)

internal fun List<Video>.asHistory(limit: Int): List<Video> = asSequence()
    .filter { it.lastPlayedAt != null }
    .sortedByDescending { it.lastPlayedAt?.time }
    .take(limit.coerceAtLeast(0))
    .toList()
