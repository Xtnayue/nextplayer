package dev.anilbeesetti.nextplayer.feature.videopicker.screens.history

import dev.anilbeesetti.nextplayer.core.model.Video
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryViewModelTest {
    @Test
    fun `history excludes unplayed videos and keeps newest entries`() {
        val neverPlayed = video(id = 1, lastPlayedAt = null)
        val oldest = video(id = 2, lastPlayedAt = Date(100))
        val newest = video(id = 3, lastPlayedAt = Date(300))
        val middle = video(id = 4, lastPlayedAt = Date(200))

        val history = listOf(neverPlayed, oldest, newest, middle).asHistory(limit = 2)

        assertEquals(listOf(newest, middle), history)
    }

    private fun video(id: Long, lastPlayedAt: Date?): Video = Video.sample.copy(
        id = id,
        uriString = "content://video/$id",
        lastPlayedAt = lastPlayedAt,
    )
}
