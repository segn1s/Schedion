package net.schedion.app.data.remote

import kotlinx.serialization.json.Json

/**
 * Единая конфигурация JSON для сетевого слоя: новые поля сервера не должны ломать парсинг.
 */
val SchedionJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Базовый URL публичного API Schedion. */
const val SCHEDION_BASE_URL: String = "https://schedion.ru/api/public/v1/"
