package net.schedion.app.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.schedion.app.core.Resource
import net.schedion.app.data.local.dao.ContentCacheDao
import net.schedion.app.data.local.entity.ContentCacheEntity

/** Общий механизм снимков Room для новостей, документов и расписания. */
@Singleton
class CachedContent @Inject constructor(
    private val dao: ContentCacheDao,
    private val json: Json,
) {
    suspend fun <T> read(key: String, serializer: KSerializer<T>): Pair<T, Long>? {
        val entry = dao.get(key) ?: return null
        return try {
            json.decodeFromString(serializer, entry.payload) to entry.savedAt
        } catch (_: SerializationException) {
            null
        }
    }

    suspend fun <T> save(key: String, value: T, serializer: KSerializer<T>): Long {
        val now = System.currentTimeMillis()
        dao.put(ContentCacheEntity(key, json.encodeToString(serializer, value), now))
        return now
    }

    /** Сначала отдаёт снимок, затем результат обновления; отмена запроса не становится ошибкой UI. */
    fun <T> request(key: String, serializer: KSerializer<T>, fetch: suspend () -> T): Flow<Resource<T>> = flow {
        emit(Resource.Loading)
        try {
            read(key, serializer)?.let { (data, time) -> emit(Resource.Success(data, true, time)) }
            val fresh = fetch()
            val savedAt = save(key, fresh, serializer)
            emit(Resource.Success(fresh, false, savedAt))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            emit(Resource.Error(message = "", cause = error))
        }
    }.flowOn(Dispatchers.IO)
}
