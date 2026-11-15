package net.schedion.app.ui.navigation

/**
 * Экраны приложения в единственной [net.schedion.app.MainActivity].
 *
 * Аргументов у экранов пока нет, поэтому маршрутом служит сам объект экрана:
 * так компилятор гарантирует, что несуществующий маршрут передать нельзя.
 */
internal sealed interface Screen {
    data object Onboarding : Screen
    data object ScheduleDay : Screen
    data object ScheduleWeek : Screen
    data object ScheduleOffline : Screen
    data object Teacher : Screen
    data object Search : Screen
    data object News : Screen
    data object NewsDetails : Screen
    data object Documents : Screen
    data object DocumentDetails : Screen
    data object SchedulePicker : Screen
    data object LessonDetails : Screen
    data object Favorites : Screen
}
