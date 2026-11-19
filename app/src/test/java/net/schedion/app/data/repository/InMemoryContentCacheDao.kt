package net.schedion.app.data.repository

import java.util.concurrent.ConcurrentHashMap
import net.schedion.app.data.local.dao.ContentCacheDao
import net.schedion.app.data.local.entity.ContentCacheEntity

internal class InMemoryContentCacheDao : ContentCacheDao {
    val entries = ConcurrentHashMap<String, ContentCacheEntity>()
    override suspend fun get(key: String): ContentCacheEntity? = entries[key]
    override suspend fun put(entry: ContentCacheEntity) { entries[entry.cacheKey] = entry }
}
