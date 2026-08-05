package dev.anilbeesetti.nextplayer.feature.videopicker.screens.history

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.domain.MediaHolder
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.MediaView
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.NoVideosFound

@Composable
fun HistoryScreen(
    onPlayVideo: (Uri) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(uiState, onPlayVideo, onSettingsClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onPlayVideo: (Uri) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(R.string.history),
                actions = {
                    FilledTonalIconButton(onClick = onSettingsClick) {
                        Icon(NextIcons.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        if (uiState.videos.isEmpty()) {
            NoVideosFound(contentPadding = padding)
        } else {
            MediaView(
                mediaHolder = MediaHolder(videos = uiState.videos, folders = emptyList()),
                recentlyPlayedVideo = null,
                recentlyPlayedFolder = null,
                preferences = uiState.preferences,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                ),
                onFolderClick = {},
                onVideoClick = { onPlayVideo(it) },
            )
        }
    }
}
