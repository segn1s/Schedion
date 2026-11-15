package net.schedion.app.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.schedion.app.data.local.dao.FavoriteDao
import net.schedion.app.data.mapper.toDomain
import net.schedion.app.data.mapper.toEntity
import net.schedion.app.domain.model.FavoriteSource

/** Интерфейс списка избранных источников расписания. */
interface FavoritesRepository {
    /** Избранные группы и преподаватели в порядке добавления. */
    val favorites: StateFlow<List<FavoriteSource>>

    /** Добавляет источник в избранное или убирает его оттуда. */
    suspend fun toggle(favorite: FavoriteSource)

    /** Убирает источник из избранного. */
    suspend fun remove(favorite: FavoriteSource)
}

/** Репозиторий избранного, хранящий список в локальной БД Room. */
@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
) : FavoritesRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val favorites: StateFlow<List<FavoriteSource>> = favoriteDao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun toggle(favorite: FavoriteSource) {
        val stored = favoriteDao.count(favorite.kind.name.lowercase(), favorite.id) > 0
        if (stored) remove(favorite) else favoriteDao.insert(favorite.toEntity())
    }

    override suspend fun remove(favorite: FavoriteSource) {
        favoriteDao.delete(favorite.kind.name.lowercase(), favorite.id)
    }
}
