package net.schedion.app.data.repository

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.schedion.app.data.local.SchedionDatabase
import net.schedion.app.data.mapper.toDomain
import net.schedion.app.data.mapper.toEntity
import net.schedion.app.domain.model.FavoriteSource

/** Интерфейс списка избранных источников расписания. */
interface FavoritesRepository {
    /** Избранные группы и преподаватели в сохранённом порядке. */
    val favorites: StateFlow<List<FavoriteSource>>

    /** Читает первый источник при запуске, дожидаясь загрузки избранного из Room. */
    suspend fun getFirstFavorite(): FavoriteSource?

    /** Переключает избранное. Возвращает false, если добавление превысило бы лимит. */
    suspend fun toggle(favorite: FavoriteSource): Boolean

    /** Убирает источник из избранного. */
    suspend fun remove(favorite: FavoriteSource)

    /** Перемещает источник на указанную позицию в сохранённом списке. */
    suspend fun move(favorite: FavoriteSource, toIndex: Int)

    companion object {
        const val MaxFavorites = 10
    }
}

/** Репозиторий избранного, хранящий список в локальной БД Room. */
@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val database: SchedionDatabase,
) : FavoritesRepository {

    private val favoriteDao = database.favoriteDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val favorites: StateFlow<List<FavoriteSource>> = favoriteDao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun getFirstFavorite(): FavoriteSource? =
        favoriteDao.getAll().firstOrNull()?.toDomain()

    override suspend fun toggle(favorite: FavoriteSource): Boolean = database.withTransaction {
        val entry = favorite.toEntity()
        val saved = favoriteDao.getAll()
        when {
            saved.any { it.kind == entry.kind && it.sourceId == entry.sourceId } ->
                favoriteDao.delete(entry.kind, entry.sourceId)
            saved.size >= FavoritesRepository.MaxFavorites -> return@withTransaction false
            else -> {
                val nextPosition = (saved.lastOrNull()?.position ?: -1L) + 1
                favoriteDao.insert(entry.copy(position = nextPosition))
            }
        }
        true
    }

    override suspend fun remove(favorite: FavoriteSource) {
        favoriteDao.delete(favorite.kind.name.lowercase(), favorite.id)
    }

    override suspend fun move(favorite: FavoriteSource, toIndex: Int) = database.withTransaction {
        val entry = favorite.toEntity()
        val saved = favoriteDao.getAll().toMutableList()
        val fromIndex = saved.indexOfFirst { it.kind == entry.kind && it.sourceId == entry.sourceId }
        if (fromIndex < 0) return@withTransaction
        val destination = toIndex.coerceIn(saved.indices)
        if (fromIndex == destination) return@withTransaction

        saved.add(destination, saved.removeAt(fromIndex))
        val updates = saved.mapIndexedNotNull { index, item ->
            val position = index.toLong()
            if (item.position == position) null else item.copy(position = position)
        }
        favoriteDao.updateAll(updates)
    }
}
