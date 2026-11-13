package net.schedion.app.ui.navigation

/**
 * Экраны приложения в единственной [net.schedion.app.MainActivity].
 *
 * Аргументов у экранов пока нет, поэтому маршрутом служит сам объект экрана:
 * так компилятор гарантирует, что несуществующий маршрут передать нельзя.
 */
sealed interface Screen {

    /** Главный экран со сводкой. */
    data object Home : Screen

    /** Расписание занятий. */
    data object Schedule : Screen

    /** Документы и графики учебного процесса. */
    data object Documents : Screen
}
