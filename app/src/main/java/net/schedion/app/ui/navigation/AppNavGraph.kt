package net.schedion.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalResources
import net.schedion.app.ui.screens.documents.DocumentsViewModel
import net.schedion.app.ui.screens.files.FilesViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.flow.collectLatest
import net.schedion.app.R
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.SchedionBackdrop
import net.schedion.app.ui.components.bottomSpacing
import net.schedion.app.ui.screens.documents.DocumentDetails
import net.schedion.app.ui.screens.documents.DocumentsScreen
import net.schedion.app.ui.screens.news.NewsDetails
import net.schedion.app.ui.screens.news.NewsScreen
import net.schedion.app.ui.screens.news.NewsViewModel
import net.schedion.app.ui.screens.people.FavoritesScreen
import net.schedion.app.ui.screens.people.OnboardingScreen
import net.schedion.app.ui.screens.people.SchedulePickerSheet
import net.schedion.app.ui.screens.people.SearchScreen
import net.schedion.app.ui.screens.schedule.LessonDetails
import net.schedion.app.ui.screens.schedule.ScheduleScreen
import net.schedion.app.ui.screens.schedule.ScheduleViewModel
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

/** Связывает экраны навигации, анимирует переходы и обрабатывает системную кнопку «Назад». */
@Composable
internal fun AppNavGraph(
    scheduleViewModel: ScheduleViewModel,
    newsViewModel: NewsViewModel,
    documentsViewModel: DocumentsViewModel,
    filesViewModel: FilesViewModel,
    networkStatusViewModel: NetworkStatusViewModel,
    modifier: Modifier = Modifier,
) {
    val networkStatus by networkStatusViewModel.status.collectAsState()
    val isOffline = networkStatus == NetworkStatus.Offline
    LaunchedEffect(networkStatusViewModel) {
        networkStatusViewModel.status.collect { status ->
            if (status == NetworkStatus.Online) {
                if (scheduleViewModel.contentState.value.isOffline) scheduleViewModel.refreshSchedule()
                newsViewModel.retryNetworkErrors()
                documentsViewModel.retryNetworkErrors()
            }
        }
    }

    val openScheduleOnLaunch = scheduleViewModel.hasInitialSource.collectAsState().value
    if (openScheduleOnLaunch == null) {
        SchedionBackdrop(modifier) {
            net.schedion.app.ui.components.ContentSkeleton(Modifier.padding(D.Page))
        }
        return
    }
    val navController = rememberAppNavController(
        initialScreen = if (openScheduleOnLaunch) Screen.ScheduleDay else Screen.Onboarding,
    )
    val screen = navController.currentScreen
    // Пока открыт шит, за ним остаётся само расписание, а не пустая заглушка
    val visualScreen = when (screen) {
        Screen.SchedulePicker, Screen.LessonDetails -> Screen.ScheduleDay
        else -> screen
    }
    val onBack = navController::navigateBack
    val openDay = { navController.openRoot(Screen.ScheduleDay) }
    val bottom = bottomSpacing()
    BackHandler(enabled = navController.canNavigateBack, onBack = onBack)

    val selectedLesson by scheduleViewModel.selectedLesson.collectAsState()
    val availableTeachers by scheduleViewModel.availableTeachers.collectAsState()
    val savedScreens = rememberSaveableStateHolder()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(filesViewModel, resources) {
        filesViewModel.events.collect { message -> snackbarHostState.showSnackbar(resources.getString(message)) }
    }
    val favoritesLimitMessage = stringResource(R.string.favorites_limit_reached, scheduleViewModel.favoritesLimit)

    LaunchedEffect(scheduleViewModel, favoritesLimitMessage) {
        scheduleViewModel.favoritesLimitReached.collectLatest {
            snackbarHostState.showSnackbar(favoritesLimitMessage)
        }
    }

    SchedionBackdrop(modifier, warm = screen !is Screen.NewsDetails) {
        AnimatedContent(
            targetState = visualScreen,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = FastOutSlowInEasing))) togetherWith
                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
            },
            label = "screen_transition",
        ) { currentScreen ->
            savedScreens.SaveableStateProvider(currentScreen.route) {
                when (currentScreen) {
                    Screen.Onboarding -> OnboardingScreen(
                        viewModel = scheduleViewModel,
                        onGroup = openDay,
                        onTeacher = openDay,
                    )
                    Screen.ScheduleDay, Screen.ScheduleWeek, Screen.ScheduleOffline, Screen.Teacher -> {
                        ScheduleScreen(
                            viewModel = scheduleViewModel,
                            onChoose = { navController.navigateTo(Screen.SchedulePicker) },
                            onLessonDetails = { lesson ->
                                scheduleViewModel.selectLesson(lesson)
                                navController.navigateTo(Screen.LessonDetails)
                            },
                            isOffline = isOffline,
                        )
                    }
                    Screen.Search -> SearchScreen(
                        viewModel = scheduleViewModel,
                        onGroup = openDay,
                        isOffline = isOffline,
                    )
                    Screen.News -> NewsScreen(
                        viewModel = newsViewModel,
                        files = filesViewModel,
                        isOffline = isOffline,
                        onOpenArticle = { slug -> newsViewModel.openArticle(slug); navController.navigateTo(Screen.NewsDetails(slug)) },
                    )
                    is Screen.NewsDetails -> NewsDetails(currentScreen.slug, newsViewModel, filesViewModel, onBack, isOffline)
                    Screen.Documents -> DocumentsScreen(documentsViewModel, filesViewModel,
                        isOffline = isOffline,
                        onOpenDocument = { slug -> documentsViewModel.openDocument(slug); navController.navigateTo(Screen.DocumentDetails(slug)) })
                    is Screen.DocumentDetails -> DocumentDetails(currentScreen.slug, documentsViewModel, filesViewModel, onBack, isOffline)
                    // Под шитом всегда показывается расписание (см. visualScreen)
                    Screen.SchedulePicker, Screen.LessonDetails -> Unit
                    Screen.Favorites -> FavoritesScreen(
                        viewModel = scheduleViewModel,
                        onBack = onBack,
                        onOpenSchedule = openDay,
                    )
                }
            }
        }

        // Шиты рисуются поверх экрана и вне AnimatedContent: так их геометрия считается
        // от границ всего приложения, а не от анимируемого контента.
        val sheetScreen = when (screen) {
            Screen.SchedulePicker, Screen.LessonDetails -> screen
            else -> null
        }
        val sheetVisible = remember { MutableTransitionState(false) }
        // Последний открытый шит: его содержимое остаётся на экране, пока проигрывается
        // анимация закрытия, и только затем убирается из композиции.
        var lastSheet by remember { mutableStateOf<Screen?>(null) }
        LaunchedEffect(sheetScreen) {
            if (sheetScreen != null) lastSheet = sheetScreen
            sheetVisible.targetState = sheetScreen != null
        }
        val sheetContent = sheetScreen ?: lastSheet
        if (sheetContent != null && (sheetVisible.currentState || sheetVisible.targetState)) {
            SchedionBottomSheet(
                visibleState = sheetVisible,
                onClose = onBack,
                tall = sheetContent == Screen.SchedulePicker,
            ) {
                when (sheetContent) {
                    Screen.SchedulePicker -> SchedulePickerSheet(
                        viewModel = scheduleViewModel,
                        onGroup = openDay,
                        onTeacher = openDay,
                        onFavorites = { navController.navigateTo(Screen.Favorites) },
                    )
                    Screen.LessonDetails -> LessonDetails(
                        lesson = selectedLesson,
                        onClose = onBack,
                        onTeacher = {
                            val teacherName = selectedLesson?.teachers
                            val found = availableTeachers.find { it.shortName == teacherName }
                            if (found != null) {
                                scheduleViewModel.selectTeacher(found)
                            }
                            openDay()
                        },
                    )
                    else -> Unit
                }
            }
        }

        val tab = tabFor(screen)
        if (tab != null) {
            BottomTabs(
                tab,
                Modifier.align(Alignment.BottomCenter).padding(horizontal = D.NavMargin).padding(bottom = bottom),
                onSelect = { navController.openRoot(it.screen) },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).imePadding().padding(
                start = D.Page,
                end = D.Page,
                bottom = bottom + D.Sm + if (tab != null) D.NavHeight else D.M,
            ),
        )
    }
}

private enum class Tab(@param:StringRes @field:StringRes val labelRes: Int, val icon: Glyph, val screen: Screen) {
    Schedule(R.string.schedule_title, Glyph.Calendar, Screen.ScheduleDay),
    Search(R.string.search_title, Glyph.Search, Screen.Search),
    News(R.string.news_title, Glyph.News, Screen.News),
    Documents(R.string.docs_title, Glyph.Document, Screen.Documents),
}

private fun tabFor(screen: Screen): Tab? = when (screen) {
    Screen.ScheduleDay, Screen.ScheduleWeek, Screen.ScheduleOffline, Screen.Teacher -> Tab.Schedule
    Screen.Search -> Tab.Search
    Screen.News -> Tab.News
    Screen.Documents, is Screen.DocumentDetails -> Tab.Documents
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
                Text(stringResource(tab.labelRes), color = ink, style = if (selectedTab) T.SmallStrong else T.Small, maxLines = 1)
            }
        }
    }
}
