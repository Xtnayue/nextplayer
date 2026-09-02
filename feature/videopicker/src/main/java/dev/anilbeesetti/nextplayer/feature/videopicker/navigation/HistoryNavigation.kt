package dev.anilbeesetti.nextplayer.feature.videopicker.navigation

import android.net.Uri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.history.HistoryScreen
import dev.anilbeesetti.nextplayer.feature.videopicker.screens.history.HistoryPlayback
import kotlinx.serialization.Serializable

@Serializable
object HistoryRoute : NavKey

fun EntryProviderScope<NavKey>.historyEntry(
    onPlayVideo: (HistoryPlayback) -> Unit,
    onSettingsClick: () -> Unit,
) {
    entry<HistoryRoute> {
        HistoryScreen(
            onPlayVideo = onPlayVideo,
            onSettingsClick = onSettingsClick,
        )
    }
}
