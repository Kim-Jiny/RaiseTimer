package com.jiny.raisetimer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object LogoStorage {
    private const val DirectoryName = "logos"

    fun saveFromUri(context: Context, uri: Uri): String? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) ?: return null
        val scaled = bitmap.scaleDown(maxDimension = 1200)
        val fileName = writeBitmap(context, scaled)
        if (scaled != bitmap) scaled.recycle()
        bitmap.recycle()
        return fileName
    }

    fun migrateLegacyBase64(context: Context, base64: String?): String? {
        val bytes = base64?.let { runCatching { Base64.decode(it, Base64.DEFAULT) }.getOrNull() } ?: return null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val scaled = bitmap.scaleDown(maxDimension = 1200)
        val fileName = writeBitmap(context, scaled)
        if (scaled != bitmap) scaled.recycle()
        bitmap.recycle()
        return fileName
    }

    fun delete(context: Context, fileName: String?) {
        fileName ?: return
        logoFile(context, fileName).delete()
    }

    suspend fun loadImageBitmap(context: Context, fileName: String?): ImageBitmap? =
        withContext(Dispatchers.IO) {
            fileName
                ?.let { logoFile(context, it) }
                ?.takeIf(File::exists)
                ?.let { BitmapFactory.decodeFile(it.absolutePath) }
                ?.asImageBitmap()
        }

    private fun writeBitmap(context: Context, bitmap: Bitmap): String? {
        val directory = File(context.filesDir, DirectoryName).apply { mkdirs() }
        val uniqueName = "logo_${java.util.UUID.randomUUID()}.png"
        val file = File(directory, uniqueName)
        return runCatching {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            file.name
        }.getOrNull()
    }

    private fun logoFile(context: Context, fileName: String): File =
        File(File(context.filesDir, DirectoryName), fileName)
}

@Composable
fun rememberLogoImageBitmap(context: Context, fileName: String?): ImageBitmap? {
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, context, fileName) {
        value = LogoStorage.loadImageBitmap(context, fileName)
    }
    return imageBitmap
}

private fun Bitmap.scaleDown(maxDimension: Int): Bitmap {
    val currentMax = maxOf(width, height)
    if (currentMax <= maxDimension) return this
    val scale = maxDimension.toFloat() / currentMax.toFloat()
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt(),
        (height * scale).toInt(),
        true,
    )
}
