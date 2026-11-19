package net.schedion.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Собственный навигатор Schedion: хранит стек экранов и поддерживает переход вперёд и назад.
 *
 * Используется вместо библиотеки навигации, потому что граф приложения плоский
 * и декларативно описывается в [AppNavGraph].
 */
@Stable
internal class AppNavController private constructor(screens: List<Screen>) {
    constructor(initialScreen: Screen) : this(listOf(initialScreen))

    private val backStack = mutableStateListOf<Screen>().apply { addAll(screens) }

    companion object {
        val Saver = listSaver<AppNavController, String>(
            save = { controller -> controller.backStack.map { it.route } },
            restore = { routes -> AppNavController(routes.map(::screenFromRoute)) },
        )
    }

    /** Экран, который отображается сейчас. */
    val currentScreen: Screen
        get() = backStack.last()

    val previousScreen: Screen?
        get() = backStack.getOrNull(backStack.lastIndex - 1)

    /** true, если из текущего экрана можно вернуться назад. */
    val canNavigateBack: Boolean
        get() = backStack.size > 1

    /** Открывает [screen] поверх текущего; повторный переход на текущий экран игнорируется. */
    fun navigateTo(screen: Screen) {
        if (screen != currentScreen) backStack.add(screen)
    }

    /** Меняет основной экран без накопления истории вкладок и состояний расписания. */
    fun openRoot(screen: Screen) {
        if (screen == currentScreen && !canNavigateBack) return
        backStack.clear()
        backStack.add(screen)
    }

    /** Возвращает на предыдущий экран; на корневом экране ничего не делает. */
    fun navigateBack() {
        if (canNavigateBack) backStack.removeAt(backStack.lastIndex)
    }
}

/** Создаёт и запоминает [AppNavController] на время жизни композиции. */
@Composable
internal fun rememberAppNavController(initialScreen: Screen = Screen.Onboarding): AppNavController =
    rememberSaveable(saver = AppNavController.Saver) { AppNavController(initialScreen) }
