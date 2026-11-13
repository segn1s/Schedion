package net.schedion.app.data.remote

import net.schedion.app.data.remote.dto.DocumentItemDto
import net.schedion.app.data.remote.dto.DocumentListDto
import net.schedion.app.data.remote.dto.HomeContentDto
import net.schedion.app.data.remote.dto.NewsArticleDto
import net.schedion.app.data.remote.dto.NewsFeedDto
import net.schedion.app.data.remote.dto.PairListDto
import net.schedion.app.data.remote.dto.ScheduleWeekDto
import net.schedion.app.data.remote.dto.WeekListDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Описание публичного API Schedion согласно OpenAPI-спецификации
 * (`https://schedion.ru/openapi/v1.json`). Авторизация не требуется.
 */
interface SchedionApiService {

    /**
     * Возвращает расписание на учебную неделю.
     *
     * @param week дата начала недели (`yyyy-MM-dd`); если не указана, сервер отдаёт неделю по умолчанию.
     * @param group идентификатор группы из `filters.groups`.
     * @param course курс обучения (используется, когда группа не выбрана).
     * @param teacher идентификатор преподавателя из `filters.teachers`.
     */
    @GET("schedule")
    suspend fun getSchedule(
        @Query("week") week: String? = null,
        @Query("group") group: Int? = null,
        @Query("course") course: Int? = null,
        @Query("teacher") teacher: Int? = null,
    ): ScheduleWeekDto

    /** Возвращает семестр и список учебных недель с чётностью. */
    @GET("weeks")
    suspend fun getWeeks(): WeekListDto

    /** Возвращает расписание звонков и часовой пояс. */
    @GET("pairs")
    suspend fun getPairs(): PairListDto

    /** Возвращает сводные данные главного экрана. */
    @GET("home")
    suspend fun getHome(): HomeContentDto

    /**
     * Возвращает страницу новостной ленты.
     *
     * @param offset сколько новостей пропустить.
     * @param count максимальное количество новостей в ответе.
     */
    @GET("news")
    suspend fun getNews(
        @Query("offset") offset: Int? = null,
        @Query("count") count: Int? = null,
    ): NewsFeedDto

    /** Возвращает полную новость по её slug. */
    @GET("news/{slug}")
    suspend fun getNewsArticle(@Path("slug") slug: String): NewsArticleDto

    /** Возвращает документы, сгруппированные по категориям. */
    @GET("documents")
    suspend fun getDocuments(): DocumentListDto

    /** Возвращает один документ по его slug. */
    @GET("documents/{slug}")
    suspend fun getDocument(@Path("slug") slug: String): DocumentItemDto
}
