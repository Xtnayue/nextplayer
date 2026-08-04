package dev.anilbeesetti.nextplayer.feature.player

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.Window
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
    bounds: Rect,
): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || bounds.width() <= 0 || bounds.height() <= 0) return false

    val bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
    val copySucceeded = suspendCancellableCoroutine { continuation ->
        PixelCopy.request(window, bounds, bitmap, { result ->
            continuation.resume(result == PixelCopy.SUCCESS)
        }, window.decorView.handler)
    }
    if (!copySucceeded) {
        bitmap.recycle()
        return false
    }

    return withContext(Dispatchers.IO) {
        try {
            saveScreenshot(window, bitmap)
        } finally {
            bitmap.recycle()
        }
    }
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
