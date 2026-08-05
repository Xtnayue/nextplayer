package dev.anilbeesetti.nextplayer.feature.videopicker.screens.history

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val uiState = combine(
        mediaRepository.observeVideos(),
        preferencesRepository.applicationPreferences,
    ) { videos, preferences ->
        HistoryUiState(
            videos = videos.asHistory(preferences.historyLimit),
            preferences = preferences,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )
}

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
