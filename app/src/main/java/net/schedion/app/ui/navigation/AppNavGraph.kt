package net.schedion.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.SchedionBackdrop
import net.schedion.app.ui.components.bottomSpacing
import net.schedion.app.ui.screens.documents.DocumentDetails
import net.schedion.app.ui.screens.documents.DocumentsScreen
import net.schedion.app.ui.screens.news.NewsDetails
import net.schedion.app.ui.screens.news.NewsScreen
import net.schedion.app.ui.screens.people.FavoritesScreen
import net.schedion.app.ui.screens.people.OnboardingScreen
import net.schedion.app.ui.screens.people.SchedulePicker
import net.schedion.app.ui.screens.people.SearchScreen
import net.schedion.app.ui.screens.schedule.LessonDetails
import net.schedion.app.ui.screens.schedule.LessonPreview
import net.schedion.app.ui.screens.schedule.PickerPreview
import net.schedion.app.ui.screens.schedule.SchedulePage
import net.schedion.app.ui.screens.schedule.ScheduleScreen
import net.schedion.app.ui.screens.schedule.TeacherPrompt
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

/** Связывает экраны навигации, анимирует переходы и обрабатывает системную кнопку «Назад». */
@Composable
internal fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: AppNavController = rememberAppNavController(),
) {
    val screen = navController.currentScreen
    val onBack = navController::navigateBack
    val openDay = { navController.openRoot(Screen.ScheduleDay) }
    val openTeacher = { navController.openRoot(Screen.Teacher) }
    val bottom = bottomSpacing()
    BackHandler(enabled = navController.canNavigateBack, onBack = onBack)

    SchedionBackdrop(modifier, warm = screen != Screen.NewsDetails) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = FastOutSlowInEasing))) togetherWith
                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
            },
            label = "screen_transition",
        ) { currentScreen ->
            when (currentScreen) {
                Screen.Onboarding -> OnboardingScreen(openDay, openTeacher)
                Screen.ScheduleDay, Screen.ScheduleWeek, Screen.ScheduleOffline, Screen.Teacher -> {
                    ScheduleRoute(currentScreen, navController)
                }
                Screen.Search -> SearchScreen(openDay)
                Screen.News -> NewsScreen(onOpenArticle = { navController.navigateTo(Screen.NewsDetails) })
                Screen.NewsDetails -> NewsDetails(onBack)
                Screen.Documents -> DocumentsScreen(onOpenCalendar = { navController.navigateTo(Screen.DocumentDetails) })
                Screen.DocumentDetails -> DocumentDetails(onBack)
                Screen.SchedulePicker -> {
                    PickerPreview(
                        teacher = navController.previousScreen == Screen.Teacher,
                        offline = navController.previousScreen == Screen.ScheduleOffline,
                    )
                    MockSheet(onBack, tall = true) {
                        SchedulePicker(
                            onGroup = openDay,
                            onTeacher = openTeacher,
                            onFavorites = { navController.navigateTo(Screen.Favorites) },
                        )
                    }
                }
                Screen.LessonDetails -> {
                    LessonPreview()
                    MockSheet(onBack) {
                        LessonDetails(onBack, openTeacher)
                    }
                }
                Screen.Favorites -> FavoritesScreen(onBack)
            }
        }

        val tab = tabFor(screen)
        if (tab != null) {
            if (screen == Screen.Teacher) {
                TeacherPrompt(
                    Modifier.align(Alignment.BottomCenter)
                        .padding(horizontal = D.NavMargin)
                        .padding(bottom = bottom + D.NavHeight + D.Xl),
                )
            }
            BottomTabs(
                tab,
                Modifier.align(Alignment.BottomCenter).padding(horizontal = D.NavMargin).padding(bottom = bottom),
                onSelect = { navController.openRoot(it.screen) },
            )
        }
    }
}

@Composable
private fun ScheduleRoute(screen: Screen, controller: AppNavController) {
    val page = when (screen) {
        Screen.ScheduleWeek -> SchedulePage.GroupWeek
        Screen.ScheduleOffline -> SchedulePage.Offline
        Screen.Teacher -> SchedulePage.TeacherDay
        else -> SchedulePage.GroupDay
    }
    ScheduleScreen(
        page = page,
        onDay = { controller.openRoot(Screen.ScheduleDay) },
        onWeek = { controller.openRoot(Screen.ScheduleWeek) },
        onOffline = { controller.openRoot(Screen.ScheduleOffline) },
        onChoose = { controller.navigateTo(Screen.SchedulePicker) },
        onLessonDetails = { controller.navigateTo(Screen.LessonDetails) },
    )
}

private enum class Tab(val label: String, val icon: Glyph, val screen: Screen) {
    Schedule("Расписание", Glyph.Calendar, Screen.ScheduleDay),
    Search("Поиск", Glyph.Search, Screen.Search),
    News("Новости", Glyph.News, Screen.News),
    Documents("Документы", Glyph.Document, Screen.Documents),
}

private fun tabFor(screen: Screen): Tab? = when (screen) {
    Screen.ScheduleDay, Screen.ScheduleWeek, Screen.ScheduleOffline, Screen.Teacher -> Tab.Schedule
    Screen.Search -> Tab.Search
    Screen.News -> Tab.News
    Screen.Documents, Screen.DocumentDetails -> Tab.Documents
    else -> null
}

@Composable
private fun BottomTabs(active: Tab, modifier: Modifier = Modifier, onSelect: (Tab) -> Unit) {
    val shape = RoundedCornerShape(D.PanelRadius)
    Row(
        modifier.fillMaxWidth().shadow(D.FloatingShadow, shape, spotColor = C.Shadow)
            .background(C.Sheet, shape).padding(horizontal = D.S, vertical = D.S),
        horizontalArrangement = Arrangement.spacedBy(D.Xs),
    ) {
        Tab.entries.forEach { tab ->
            val selectedTab = tab == active
            val ink = if (selectedTab) C.Primary else C.Muted
            Column(
                Modifier.weight(1f).height(D.NavHeight - D.S * 2)
                    .clip(RoundedCornerShape(D.NavItemRadius))
                    .background(if (selectedTab) C.PaleBlue else Color.Transparent)
                    .semantics { selected = selectedTab }
                    .clickable(role = Role.Tab) { if (!selectedTab) onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(D.Xs, Alignment.CenterVertically),
            ) {
                LineIcon(tab.icon, tint = ink, size = D.NavIcon)
                Text(tab.label, color = ink, style = if (selectedTab) T.SmallStrong else T.Small, maxLines = 1)
            }
        }
    }
}
