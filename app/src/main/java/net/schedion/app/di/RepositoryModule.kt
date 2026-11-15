package net.schedion.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
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
}
