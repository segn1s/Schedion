package net.schedion.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Тело ошибки API в формате RFC 9457 (Problem Details). */
@Serializable
data class ProblemDetailsDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
)
