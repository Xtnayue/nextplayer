package dev.anilbeesetti.nextplayer.feature.player.service

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerServiceTest {

    @Test
    fun `frame step uses video frame rate`() {
        val result = frameStepTargetPosition(
            currentPositionMs = 1_000,
            durationMs = 10_000,
            frameDelta = 3,
            frameRate = 30f,
        )

        assertEquals(1_100, result)
    }

    @Test
    fun `backward frame step does not seek before start`() {
        val result = frameStepTargetPosition(
            currentPositionMs = 50,
            durationMs = 10_000,
            frameDelta = -3,
            frameRate = 30f,
        )

        assertEquals(0, result)
    }

    @Test
    fun `forward frame step does not seek past duration`() {
        val result = frameStepTargetPosition(
            currentPositionMs = 9_950,
            durationMs = 10_000,
            frameDelta = 3,
            frameRate = 30f,
        )

        assertEquals(10_000, result)
    }

    @Test
    fun `unknown frame rate and duration use safe defaults`() {
        val result = frameStepTargetPosition(
            currentPositionMs = 1_000,
            durationMs = C.TIME_UNSET,
            frameDelta = 3,
            frameRate = 0f,
        )

        assertEquals(1_100, result)
    }
}
