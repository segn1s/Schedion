package net.schedion.app.core

/**
 * Обёртка результата любой операции с данными.
 *
 * Заменяет разрозненные `try/catch` и nullable-результаты: слой данных возвращает
 * одно из трёх состояний, а UI отображает его без дополнительных проверок.
 */
sealed interface Resource<out T> {

    /** Операция завершилась успешно, данные доступны в [data]. */
    data class Success<T>(val data: T) : Resource<T>

    /** Операция завершилась ошибкой; [message] пригоден для показа пользователю. */
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>

    /** Данные ещё загружаются. */
    data object Loading : Resource<Nothing>
}
