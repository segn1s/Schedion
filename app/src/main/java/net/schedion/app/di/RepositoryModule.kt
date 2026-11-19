package net.schedion.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.schedion.app.data.local.SavedSourceStore
import net.schedion.app.data.local.SharedPreferencesSavedSourceStore
import net.schedion.app.data.repository.FavoritesRepository
import net.schedion.app.data.repository.FavoritesRepositoryImpl
import net.schedion.app.data.repository.ScheduleRepository
import net.schedion.app.data.repository.ScheduleRepositoryImpl

/** Hilt-модуль, предоставляющий реализации репозиториев приложения. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(
        impl: ScheduleRepositoryImpl,
    ): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(
        impl: FavoritesRepositoryImpl,
    ): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindSavedSourceStore(
        impl: SharedPreferencesSavedSourceStore,
    ): SavedSourceStore
    @Binds
    abstract fun bindNewsRepository(impl: net.schedion.app.data.repository.NewsRepositoryImpl): net.schedion.app.data.repository.NewsRepository

    @Binds
    abstract fun bindDocumentsRepository(impl: net.schedion.app.data.repository.DocumentsRepositoryImpl): net.schedion.app.data.repository.DocumentsRepository
}
