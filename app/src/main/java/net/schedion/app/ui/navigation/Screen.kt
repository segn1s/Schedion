package net.schedion.app.ui.navigation

/**
 * Экраны приложения в единственной [net.schedion.app.MainActivity].
 *
 * Детальные экраны получают slug, остальные маршруты остаются объектами.
 */
internal sealed interface Screen {
    data object Onboarding : Screen
    data object ScheduleDay : Screen
    data object ScheduleWeek : Screen
    data object ScheduleOffline : Screen
    data object Teacher : Screen
    data object Search : Screen
    data object News : Screen
    data class NewsDetails(val slug: String) : Screen
    data object Documents : Screen
    data class DocumentDetails(val slug: String) : Screen
    data object SchedulePicker : Screen
    data object LessonDetails : Screen
    data object Favorites : Screen
}

/** Стабильный ключ для сохранения навигации и состояния прокрутки при пересоздании Activity. */
internal val Screen.route: String get() = when (this) {
    Screen.Onboarding -> "onboarding"
    Screen.ScheduleDay -> "schedule-day"
    Screen.ScheduleWeek -> "schedule-week"
    Screen.ScheduleOffline -> "schedule-offline"
    Screen.Teacher -> "teacher"
    Screen.Search -> "search"
    Screen.News -> "news"
    is Screen.NewsDetails -> "news:$slug"
    Screen.Documents -> "documents"
    is Screen.DocumentDetails -> "document:$slug"
    Screen.SchedulePicker -> "picker"
    Screen.LessonDetails -> "lesson"
    Screen.Favorites -> "favorites"
}

internal fun screenFromRoute(route: String): Screen = when {
    route.startsWith("news:") -> Screen.NewsDetails(route.removePrefix("news:"))
    route.startsWith("document:") -> Screen.DocumentDetails(route.removePrefix("document:"))
    else -> when (route) {
        "onboarding" -> Screen.Onboarding
        "schedule-week" -> Screen.ScheduleWeek
        "schedule-offline" -> Screen.ScheduleOffline
        "teacher" -> Screen.Teacher
        "search" -> Screen.Search
        "news" -> Screen.News
        "documents" -> Screen.Documents
        "picker" -> Screen.SchedulePicker
        "lesson" -> Screen.LessonDetails
        "favorites" -> Screen.Favorites
        else -> Screen.ScheduleDay
    }
}
