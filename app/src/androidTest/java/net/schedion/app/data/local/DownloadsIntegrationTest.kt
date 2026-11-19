package net.schedion.app.data.local

import android.app.DownloadManager
import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.DataInputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.schedion.app.data.repository.DownloadsRepository
import net.schedion.app.data.repository.DownloadStatus
import net.schedion.app.domain.model.ContentFile
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Явно включаемая проверка реального DownloadManager; обычный прогон не зависит от внешнего сервера. */
@RunWith(AndroidJUnit4::class)
class DownloadsIntegrationTest {
    @Test fun downloadReusesVersionRecoversDeletedFileAndReturnsReadableContentUri(): Unit = runBlocking {
        assumeTrue("Enable with -e networkChecks true", InstrumentationRegistry.getArguments().getString("networkChecks") == "true")
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, SchedionDatabase::class.java).build()
        val repository = DownloadsRepository(context, database.downloadDao())
        val manager = context.getSystemService(DownloadManager::class.java)
        val ids = mutableListOf<Long>()
        val file = ContentFile("integration-pdf", "v1",
            "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            "schedion-integration-check.pdf", "application/pdf", 0)
        suspend fun awaitComplete(target: ContentFile) = withTimeout(120_000) {
            repository.downloads.first { it[target.key]?.status == DownloadStatus.Complete }
        }
        try {
            val first = repository.download(file).also { ids.add(it) }
            awaitComplete(file)
            val uri = checkNotNull(repository.downloadedUri(file))
            assertEquals("content", uri.scheme)
            val header = ByteArray(4)
            DataInputStream(checkNotNull(context.contentResolver.openInputStream(uri))).use { it.readFully(header) }
            assertEquals("%PDF", header.toString(Charsets.US_ASCII))
            assertEquals(first, repository.download(file))
            manager.remove(first)
            withTimeout(10_000) { repository.downloads.first { it[file.key]?.status == DownloadStatus.Failed } }
            assertNull(repository.downloadedUri(file))
            val retry = repository.download(file).also { ids.add(it) }
            assertNotEquals(first, retry)
            awaitComplete(file)
            val replacement = file.copy(version = "v2")
            val newer = repository.download(replacement).also { ids.add(it) }
            assertNotEquals(retry, newer)
            awaitComplete(replacement)
            assertNotNull(repository.downloadedUri(replacement))
        } finally {
            ids.forEach { manager.remove(it) }
            // WhileSubscribed завершает опрос после 5 секунд без наблюдателей.
            delay(6000)
            database.close()
        }
    }
}
