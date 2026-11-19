package net.schedion.app.core

import java.io.IOException

/** Сохраняет показанный контент во время обновления и отделяет ошибку сети от ошибки сервера. */
data class ContentState<T>(
    val resource: Resource<T> = Resource.Loading,
    val isRefreshing: Boolean = false,
    val error: Throwable? = null,
    val savedAt: Long? = null,
) {
    val data: T? get() = (resource as? Resource.Success)?.data
    val isOffline: Boolean get() = error is IOException

    fun accept(next: Resource<T>): ContentState<T> = when (next) {
        Resource.Loading -> copy(isRefreshing = true)
        is Resource.Success -> copy(
            resource = next,
            isRefreshing = next.isFromCache,
            error = if (next.isFromCache) error else null,
            savedAt = next.savedAt,
        )
        is Resource.Error -> copy(
            resource = if (data != null) resource else next,
            isRefreshing = false,
            error = next.cause ?: IllegalStateException(),
        )
    }
}
