package dev.anilbeesetti.nextplayer.feature.player

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import dev.anilbeesetti.nextplayer.feature.player.extensions.toContentScale
import dev.anilbeesetti.nextplayer.feature.player.state.ControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.PictureInPictureState
import dev.anilbeesetti.nextplayer.feature.player.state.SeekGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.TapGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.anilbeesetti.nextplayer.feature.player.state.VolumeAndBrightnessGestureState
import dev.anilbeesetti.nextplayer.feature.player.ui.PlayerGestures
import dev.anilbeesetti.nextplayer.feature.player.ui.ShutterView
import dev.anilbeesetti.nextplayer.feature.player.ui.SubtitleConfiguration
import dev.anilbeesetti.nextplayer.feature.player.ui.SubtitleView

@OptIn(UnstableApi::class)
@Composable
fun PlayerContentFrame(
    modifier: Modifier = Modifier,
    player: Player,
    pictureInPictureState: PictureInPictureState,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    volumeAndBrightnessGestureState: VolumeAndBrightnessGestureState,
    subtitleConfiguration: SubtitleConfiguration,
) {
    val presentationState = rememberPresentationState(player)
    val density = LocalDensity.current
    var videoBounds by remember { mutableStateOf(IntRect.Zero) }
    val videoContentModifier = Modifier
        .resizeWithContentScale(
            contentScale = videoZoomAndContentScaleState.videoContentScale.toContentScale(),
            sourceSizeDp = presentationState.videoSizeDp?.let { size ->
                size.copy(
                    width = with(LocalDensity.current) { size.width.toDp().value },
                    height = with(LocalDensity.current) { size.height.toDp().value },
                )
            },
        )
        .graphicsLayer {
            scaleX = videoZoomAndContentScaleState.zoom
            scaleY = videoZoomAndContentScaleState.zoom
            translationX = videoZoomAndContentScaleState.offset.x
            translationY = videoZoomAndContentScaleState.offset.y
        }

    PlayerSurface(
        player = player,
        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        modifier = modifier.then(videoContentModifier)
            .onGloballyPositioned {
                val parentBounds = it.boundsInParent()
                videoBounds = IntRect(
                    left = parentBounds.left.toInt(),
                    top = parentBounds.top.toInt(),
                    right = parentBounds.right.toInt(),
                    bottom = parentBounds.bottom.toInt(),
                )
                val bounds = it.boundsInWindow()
                val rect = Rect(
                    bounds.left.toInt(),
                    bounds.top.toInt(),
                    bounds.right.toInt(),
                    bounds.bottom.toInt(),
                )
                pictureInPictureState.setVideoViewRect(rect)
            },
    )

    PlayerGestures(
        controlsVisibilityState = controlsVisibilityState,
        tapGestureState = tapGestureState,
        pictureInPictureState = pictureInPictureState,
        seekGestureState = seekGestureState,
        videoZoomAndContentScaleState = videoZoomAndContentScaleState,
        volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
    )

    SubtitleView(
        modifier = if (videoBounds.width > 0 && videoBounds.height > 0) {
            Modifier
                .offset { IntOffset(videoBounds.left, videoBounds.top) }
                .size(
                    width = with(density) { videoBounds.width.toDp() },
                    height = with(density) { videoBounds.height.toDp() },
                )
        } else {
            Modifier.fillMaxSize()
        },
        player = player,
        isInPictureInPictureMode = pictureInPictureState.isInPictureInPictureMode,
        configuration = subtitleConfiguration,
    )

    if (presentationState.coverSurface) {
        ShutterView()
    }
}
