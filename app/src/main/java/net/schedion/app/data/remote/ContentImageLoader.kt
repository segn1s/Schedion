package net.schedion.app.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schedion.app.domain.model.contentUrl
import net.schedion.app.domain.model.sha256
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Дисковый кэш обложек и изображений; декодирование и сеть выполняются вне главного потока. */
@Singleton
class ContentImageLoader @Inject constructor(
    @ApplicationContext context: Context,
    client: OkHttpClient,
) {
    private val directory = File(context.cacheDir, "content-images")
    private val client = client.newBuilder().callTimeout(30, TimeUnit.SECONDS).build()
    private val memory = object : LruCache<String, Bitmap>(MemoryBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    suspend fun load(value: String): Bitmap? = withContext(Dispatchers.IO) {
        val url = contentUrl(value) ?: return@withContext null
        memory.get(url)?.let { return@withContext it }
        try {
            directory.mkdirs()
            val target = File(directory, sha256(url))
            if (!target.exists()) {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body ?: return@withContext null
                    if (body.contentLength() > MaxImageBytes) return@withContext null
                    val bytes = body.byteStream().use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (output.size() <= MaxImageBytes) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    if (bytes.size > MaxImageBytes) return@withContext null
                    // Поток каждого запроса получает свой файл: параллельные карточки не повреждают кэш.
                    val temporary = File.createTempFile("image-", ".tmp", directory)
                    try {
                        temporary.writeBytes(bytes)
                        temporary.renameTo(target)
                    } finally { temporary.delete() }
                    trimDisk()
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) { target.delete(); return@withContext null }
            var sample = 1
            while (bounds.outWidth / sample > MaxDimension || bounds.outHeight / sample > MaxDimension) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(target.path, options)?.also { memory.put(url, it) }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { null }
    }

    private fun trimDisk() {
        val files = directory.listFiles()?.filter { !it.name.endsWith(".tmp") }?.sortedBy { it.lastModified() } ?: return
        var bytes = files.sumOf { it.length() }
        for (file in files) {
            if (bytes <= DiskBytes) break
            val size = file.length()
            if (file.delete()) bytes -= size
        }
    }

    private companion object {
        const val MemoryBytes = 12 * 1024 * 1024
        const val DiskBytes = 96L * 1024 * 1024
        const val MaxImageBytes = 12 * 1024 * 1024
        const val MaxDimension = 1600
    }
}
