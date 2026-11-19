package net.schedion.app.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.schedion.app.core.Resource
import net.schedion.app.data.mapper.toDomain
import net.schedion.app.data.remote.SchedionApiService
import net.schedion.app.data.remote.dto.DocumentItemDto
import net.schedion.app.data.remote.dto.DocumentListDto
import net.schedion.app.domain.model.DocumentCategory
import net.schedion.app.domain.model.PublicDocument

interface DocumentsRepository {
    fun documents(): Flow<Resource<List<DocumentCategory>>>
    fun document(slug: String): Flow<Resource<PublicDocument>>
}

@Singleton
class DocumentsRepositoryImpl @Inject constructor(private val api: SchedionApiService, private val cache: CachedContent) : DocumentsRepository {
    override fun documents(): Flow<Resource<List<DocumentCategory>>> =
        cache.request("documents:list", DocumentListDto.serializer()) { api.getDocuments() }
            .map { it.mapData { dto -> dto.toDomain() } }
    override fun document(slug: String): Flow<Resource<PublicDocument>> =
        cache.request("documents:detail:$slug", DocumentItemDto.serializer()) { api.getDocument(slug) }
            .map { it.mapData { dto -> dto.toDomain() } }
}
