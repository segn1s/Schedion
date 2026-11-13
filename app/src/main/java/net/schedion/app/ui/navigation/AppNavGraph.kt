package net.schedion.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.schedion.app.ui.screens.documents.DocumentsScreen
import net.schedion.app.ui.screens.home.HomeScreen
import net.schedion.app.ui.screens.schedule.ScheduleScreen

/**
 * Корневой граф приложения: показывает текущий экран и связывает переходы между ними.
 *
 * Системная кнопка «Назад» обрабатывается здесь же, чтобы экраны оставались stateless
 * и не знали о навигации больше, чем им нужно.
 */
@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: AppNavController = rememberAppNavController(),
) {
    BackHandler(enabled = navController.canNavigateBack) { navController.navigateBack() }

    Box(modifier = modifier.fillMaxSize()) {
        when (navController.currentScreen) {
            Screen.Home -> HomeScreen(
                onOpenSchedule = { navController.navigateTo(Screen.Schedule) },
                onOpenDocuments = { navController.navigateTo(Screen.Documents) },
            )

            Screen.Schedule -> ScheduleScreen(onNavigateBack = navController::navigateBack)

            Screen.Documents -> DocumentsScreen(onNavigateBack = navController::navigateBack)
        }
    }
}
