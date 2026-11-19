package net.schedion.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.schedion.app.data.local.entity.ContentCacheEntity
import net.schedion.app.data.local.entity.DownloadEntity
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentMigrationTest {
    @Test fun version3To4_preservesFavoritesAndAddsSnapshotAndDownloadTables(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context: Context = instrumentation.targetContext
        val name = "content-migration-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(instrumentation.context.assets.open("schema-3.json").bufferedReader().use { it.readText() })
            .getJSONObject("database")
        context.getDatabasePath(name).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO favorites (id, kind, sourceId, title, subtitle, course, position) VALUES (1, 'group', 42, 'Test group', NULL, 2, 0)")
            db.version = 3
        }
        val database = Room.databaseBuilder(context, SchedionDatabase::class.java, name)
            .addMigrations(SchedionDatabase.MIGRATION_3_4).build()
        try {
            assertEquals("Test group", database.favoriteDao().getAll().single().title)
            database.contentCacheDao().put(ContentCacheEntity("test", "{}", 123))
            assertEquals(123L, database.contentCacheDao().get("test")?.savedAt)
            database.downloadDao().put(DownloadEntity("file", 7, "application/pdf", "file.pdf"))
            assertEquals(7L, database.downloadDao().get("file")?.downloadId)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
