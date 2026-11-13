package net.schedion.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

/**
 * Собственный навигатор Schedion: хранит стек экранов и поддерживает переход вперёд и назад.
 *
 * Используется вместо библиотеки навигации, потому что граф приложения плоский
 * и декларативно описывается в [AppNavGraph].
 */
@Stable
class AppNavController(initialScreen: Screen) {

    private val backStack = mutableStateListOf(initialScreen)

    /** Экран, который отображается сейчас. */
    val currentScreen: Screen
        get() = backStack.last()

    /** true, если из текущего экрана можно вернуться назад. */
    val canNavigateBack: Boolean
        get() = backStack.size > 1

    /** Открывает [screen] поверх текущего; повторный переход на текущий экран игнорируется. */
    fun navigateTo(screen: Screen) {
        if (screen != currentScreen) backStack.add(screen)
    }

    /** Возвращает на предыдущий экран; на корневом экране ничего не делает. */
    fun navigateBack() {
        if (canNavigateBack) backStack.removeAt(backStack.lastIndex)
    }
}

/** Создаёт и запоминает [AppNavController] на время жизни композиции. */
@Composable
fun rememberAppNavController(initialScreen: Screen = Screen.Home): AppNavController =
    remember { AppNavController(initialScreen) }
