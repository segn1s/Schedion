package net.schedion.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.domain.model.FavoriteSource

/** Постоянное хранилище выбранного источника расписания. */
interface SavedSourceStore {

    /** Возвращает сохранённый источник расписания или null, если выбора ещё не было. */
    fun load(): FavoriteSource?

    /** Сохраняет выбранный источник расписания. */
    fun save(source: FavoriteSource)
}

/**
 * Хранит выбранный источник расписания в SharedPreferences.
 *
 * Название и курс дублируются вместе с id, чтобы шапка расписания показывала выбор
 * ещё до ответа сервера — даже если выбранной группы нет в справочнике текущей недели.
 */
@Singleton
class SharedPreferencesSavedSourceStore @Inject constructor(
    @ApplicationContext context: Context,
) : SavedSourceStore {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): FavoriteSource? {
        val kind = preferences.getString(KEY_KIND, null) ?: return null
        val id = preferences.getInt(KEY_ID, NO_ID)
        val title = preferences.getString(KEY_TITLE, null) ?: return null
        if (id == NO_ID) return null

        val course = preferences.getInt(KEY_COURSE, NO_COURSE).takeIf { it != NO_COURSE }
        return FavoriteSource(
            kind = if (kind == KIND_TEACHER) FavoriteKind.Teacher else FavoriteKind.Group,
            id = id,
            title = title,
            subtitle = preferences.getString(KEY_SUBTITLE, null),
            course = course,
        )
    }

    override fun save(source: FavoriteSource) {
        preferences.edit()
            .putString(KEY_KIND, if (source.kind == FavoriteKind.Teacher) KIND_TEACHER else KIND_GROUP)
            .putInt(KEY_ID, source.id)
            .putString(KEY_TITLE, source.title)
            .putString(KEY_SUBTITLE, source.subtitle)
            .putInt(KEY_COURSE, source.course ?: NO_COURSE)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "schedion_settings"
        const val KEY_KIND = "source_kind"
        const val KEY_ID = "source_id"
        const val KEY_TITLE = "source_title"
        const val KEY_SUBTITLE = "source_subtitle"
        const val KEY_COURSE = "source_course"
        const val KIND_GROUP = "group"
        const val KIND_TEACHER = "teacher"
        const val NO_ID = -1
        const val NO_COURSE = -1
    }
}
