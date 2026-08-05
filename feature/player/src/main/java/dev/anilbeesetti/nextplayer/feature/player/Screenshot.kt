package dev.anilbeesetti.nextplayer.feature.player

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.media3.ui.SubtitleView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal suspend fun captureAndSaveScreenshot(
    window: Window,
    includeSubtitles: Boolean,
): Boolean {
    val surfaceView = window.decorView.findDescendant(SurfaceView::class.java) ?: return false
    if (!surfaceView.isAttachedToWindow || surfaceView.width <= 0 || surfaceView.height <= 0) return false

    val bitmap = Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)
    val copySucceeded = suspendCancellableCoroutine { continuation ->
        PixelCopy.request(
            surfaceView,
            bitmap,
            { result -> continuation.resume(result == PixelCopy.SUCCESS) },
            surfaceView.handler,
        )
    }
    if (!copySucceeded) {
        bitmap.recycle()
        return false
    }

    if (includeSubtitles) {
        window.decorView.findDescendant(SubtitleView::class.java)?.let { subtitleView ->
            drawSubtitlesOnVideo(
                bitmap = bitmap,
                surfaceView = surfaceView,
                subtitleView = subtitleView,
            )
        }
    }

    return withContext(Dispatchers.IO) {
        try {
            saveScreenshot(window, bitmap)
        } finally {
            bitmap.recycle()
        }
    }
}

private fun drawSubtitlesOnVideo(
    bitmap: Bitmap,
    surfaceView: SurfaceView,
    subtitleView: SubtitleView,
) {
    val surfaceLocation = IntArray(2).also(surfaceView::getLocationInWindow)
    val subtitleLocation = IntArray(2).also(subtitleView::getLocationInWindow)
    Canvas(bitmap).run {
        save()
        translate(
            (subtitleLocation[0] - surfaceLocation[0]).toFloat(),
            (subtitleLocation[1] - surfaceLocation[1]).toFloat(),
        )
        subtitleView.draw(this)
        restore()
    }
}

private fun <T : View> View.findDescendant(viewClass: Class<T>): T? {
    if (viewClass.isInstance(this)) return viewClass.cast(this)
    if (this !is ViewGroup) return null
    for (index in 0 until childCount) {
        getChildAt(index).findDescendant(viewClass)?.let { return it }
    }
    return null
}

private fun saveScreenshot(window: Window, bitmap: Bitmap): Boolean {
    val context = window.context
    val filename = "NextPlayer_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())}.png"
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Next Player")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        } else {
            @Suppress("DEPRECATION")
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Next Player",
            ).apply { mkdirs() }
            @Suppress("DEPRECATION")
            put(MediaStore.Images.Media.DATA, File(directory, filename).absolutePath)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        } ?: error("Unable to open screenshot destination")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null,
            )
        }
        true
    }.getOrElse {
        resolver.delete(uri, null, null)
        false
    }
}
