package net.schedion.app.ui.screens.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.MockButton
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.MockDivider
import net.schedion.app.ui.components.ScreenColumn
import net.schedion.app.ui.components.SearchField
import net.schedion.app.ui.components.Segments
import net.schedion.app.ui.components.StatusDot
import net.schedion.app.ui.components.bottomSpacing
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

private const val SelectedGroup = "2ИВТ_ТИИ1"
private const val Sidorova = "Сидорова Елена Викторовна"
private const val Petrov = "Петров Алексей Сергеевич"
private const val Program = "Информатика и вычислительная техника"
private val groupNames = listOf("1ИВТ_ТИИ1", "1ИВТ_ТИИ2", SelectedGroup, "2ИВТ_ТИИ2")
private val teachers = listOf("Кузнецова Мария Андреевна", Petrov)

private data class SearchResult(val title: String, val subtitle: String, val favorite: Boolean)

@Composable
internal fun SearchScreen(onGroup: () -> Unit, modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        Text("Поиск", style = T.Heading)
        Spacer(Modifier.height(D.L))
        SearchField("ивт", focused = true, placeholder = false)
        Spacer(Modifier.height(D.L))
        Segments(listOf("Группы", "Преподаватели"), active = 0)
        Spacer(Modifier.height(D.L))
        PeopleHeading("Избранное")
        SearchResults(
            listOf(
                SearchResult(SelectedGroup, "Группа · открыта сейчас", true),
                SearchResult(Sidorova, "Преподаватель", true),
            ),
            onGroup,
        )
        Spacer(Modifier.height(D.L))
        PeopleHeading("Найдено: 4")
        SearchResults(
            listOf(
                SearchResult(groupNames[0], "1 курс · $Program", false),
                SearchResult(SelectedGroup, "2 курс · $Program", true),
                SearchResult(groupNames[3], "2 курс · $Program", false),
                SearchResult("3ИВТ_ТИИ1", "3 курс · $Program", false),
            ),
            onGroup,
        )
    }
}

@Composable
private fun SearchResults(results: List<SearchResult>, onGroup: () -> Unit) {
    MockCard {
        results.forEachIndexed { index, result ->
            if (index > 0) MockDivider()
            Row(
                Modifier.fillMaxWidth().heightIn(min = D.SearchRow)
                    .then(if (result.title == SelectedGroup) Modifier.clickable(onClick = onGroup) else Modifier)
                    .padding(horizontal = D.L, vertical = D.Sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(D.Page),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(result.title, style = T.Title)
                    Text(result.subtitle, color = C.Muted, style = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FavoriteStar(result.favorite)
                Spacer(Modifier.width(D.Xs))
            }
        }
    }
}

@Composable
internal fun OnboardingScreen(onGroup: () -> Unit, onTeacher: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val panelHeight = (maxHeight - D.OnboardingPanelTop).coerceAtLeast(D.Tiny)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().heightIn(min = D.OnboardingPanelTop).padding(start = D.Xxl, end = D.Xxl, top = D.Top + D.Sm)) {
                Text("Schedion", color = C.Primary, style = T.Brand)
                Spacer(Modifier.height(D.Ml))
                Text("Расписание пар, отмены и новости института.\nСохраняется на телефоне и работает без сети.", color = C.Secondary, style = T.ArticleBody)
            }
            Column(
                Modifier.fillMaxWidth().heightIn(min = panelHeight)
                    .shadow(D.FloatingShadow, RoundedCornerShape(topStart = D.PanelRadius, topEnd = D.PanelRadius), spotColor = C.Shadow)
                    .background(C.Sheet, RoundedCornerShape(topStart = D.PanelRadius, topEnd = D.PanelRadius))
                    .padding(start = D.Page, end = D.Page, top = D.Xl, bottom = bottomSpacing() + D.Xxl),
            ) {
                Text("Выберите свою группу или преподавателя", style = T.SheetHeading)
                Spacer(Modifier.height(D.M))
                Text("Позже выбор меняется нажатием на название в шапке расписания", color = C.Muted, style = T.Label)
                Spacer(Modifier.height(D.M))
                SearchField("Например: 2ИВТ или Петров", focused = true)
                Spacer(Modifier.height(D.Page))
                PeopleDirectory(showStars = false, onGroup = onGroup, onTeacher = onTeacher)
            }
        }
    }
}

@Composable
internal fun SchedulePicker(
    onGroup: () -> Unit,
    onTeacher: () -> Unit,
    onFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = D.Page),
            horizontalArrangement = Arrangement.spacedBy(D.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Чьё расписание", Modifier.weight(1f), style = T.SheetHeading)
            Text("Нед. 11 · 09.11 – 15.11", color = C.Muted, style = T.Label)
        }
        Spacer(Modifier.height(D.Sm))
        SearchField("Группа или преподаватель", modifier = Modifier.padding(horizontal = D.Page))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = D.Xl)) {
            PeopleHeading("Избранное", Modifier.padding(horizontal = D.Page))
            PickerFavorite(SelectedGroup, "2 курс · $Program", selected = true, onClick = onGroup)
            PickerFavorite(Sidorova, "Преподаватель")
            PickerFavorite("3ИВТ_ТИИ1", "На этой неделе пар нет", subtitleColor = C.AmberText)
            Spacer(Modifier.height(D.M))
            PeopleDirectory(true, onGroup, onTeacher, Modifier.padding(horizontal = D.Page))
        }
        MockDivider()
        MockButton(
            "Изменить избранное",
            Modifier.fillMaxWidth().padding(horizontal = D.Page, vertical = D.M),
            onClick = onFavorites,
        )
    }
}

@Composable
private fun PeopleDirectory(
    showStars: Boolean,
    onGroup: () -> Unit,
    onTeacher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PeopleHeading("Группы")
        groupNames.chunked(2).forEachIndexed { index, groups ->
            if (index > 0) Spacer(Modifier.height(D.Sm))
            Text("${index + 1} курс", Modifier.padding(start = if (showStars) D.Section else D.Tiny), C.Muted, style = T.SmallStrong)
            groups.filterNot { showStars && it == SelectedGroup }.forEachIndexed { row, group ->
                if (row > 0) MockDivider()
                DirectoryRow(group, showStars, onClick = onGroup.takeIf { group == SelectedGroup })
            }
            if (!showStars) MockDivider()
        }
        Spacer(Modifier.height(D.Ml))
        PeopleHeading("Преподаватели")
        teachers.forEachIndexed { index, teacher ->
            if (index > 0) MockDivider()
            DirectoryRow(teacher, showStars, onClick = onTeacher.takeIf { teacher == Petrov })
        }
    }
}

@Composable
private fun DirectoryRow(title: String, showStars: Boolean, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (showStars) D.SheetDirectoryRow else D.PickerRow)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(start = if (showStars) D.Section else D.Hairline, end = D.Xs, top = D.S, bottom = D.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = T.Directory)
        if (showStars) FavoriteStar(false)
    }
}

@Composable
private fun PickerFavorite(
    title: String,
    subtitle: String,
    selected: Boolean = false,
    subtitleColor: Color = C.Muted,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = D.Sm)
            .clip(RoundedCornerShape(D.ControlRadius)).background(if (selected) C.Cover else Color.Transparent)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = D.M, vertical = D.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.Sm),
    ) {
        if (selected) LineIcon(Glyph.Check, tint = C.Primary, size = D.IconSmall)
        else Spacer(Modifier.width(D.IconSmall))
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Title)
            Text(subtitle, color = subtitleColor, style = T.Label)
        }
        FavoriteStar(true)
        Spacer(Modifier.width(D.Tiny))
    }
}

@Composable
private fun FavoriteStar(favorite: Boolean) = LineIcon(
    if (favorite) Glyph.FilledStar else Glyph.Star,
    tint = if (favorite) C.Primary else C.Muted,
    size = D.NavIcon,
)

@Composable
private fun PeopleHeading(label: String, modifier: Modifier = Modifier) {
    Text(label, modifier.padding(bottom = D.S), C.Secondary, style = T.LabelStrong)
}

@Composable
internal fun FavoritesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        ScreenColumn(
            modifier = Modifier.padding(bottom = bottomSpacing() + D.Snackbar),
            withTabs = false,
        ) {
            Row(Modifier.fillMaxWidth()) {
                Text("Отмена", Modifier.clickable(role = Role.Button, onClick = onBack), C.Secondary, style = T.BodyStrong)
                Spacer(Modifier.weight(1f))
                Text("Готово", Modifier.clickable(role = Role.Button, onClick = onBack).padding(end = D.Xs), C.Primary, style = T.BodyStrong)
            }
            Spacer(Modifier.height(D.Xl))
            Text("Избранное", style = T.Heading)
            Spacer(Modifier.height(D.Xs))
            Text("Перетащите, чтобы поменять порядок. Первое открывается при запуске.", color = C.Muted, style = T.Label)
            Spacer(Modifier.height(D.L))
            MockCard {
                listOf(SelectedGroup, Sidorova, "3ИВТ_ТИИ1").forEachIndexed { index, name ->
                    if (index > 0) MockDivider()
                    val moving = index == 1
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = D.FavoritesRow)
                            .then(if (moving) Modifier.shadow(D.Sm, spotColor = C.Shadow).background(C.White) else Modifier)
                            .padding(horizontal = D.Page, vertical = D.S),
                        horizontalArrangement = Arrangement.spacedBy(D.Page),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LineIcon(Glyph.Remove, tint = C.Red)
                        Column(Modifier.weight(1f)) {
                            Text(name, style = T.Title)
                            Text(if (moving) "Преподаватель · перемещается" else "Группа", color = C.Muted, style = T.Label)
                        }
                        LineIcon(Glyph.Reorder, tint = if (moving) C.Primary else C.Muted)
                    }
                }
            }
            Spacer(Modifier.height(D.L))
            Text("В избранном 3 из 10", color = C.Muted, style = T.Label)
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = D.NavMargin)
                .padding(bottom = bottomSpacing() + D.Sm)
                .fillMaxWidth().heightIn(min = D.Snackbar)
                .shadow(D.FloatingShadow, RoundedCornerShape(D.CardRadius), spotColor = C.Shadow)
                .background(C.Sheet, RoundedCornerShape(D.CardRadius)).padding(horizontal = D.Ml, vertical = D.S),
            horizontalArrangement = Arrangement.spacedBy(D.S),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(C.Success)
            Text("1ИВТ_ТИИ2 убрана из избранного", Modifier.weight(1f), style = T.BodySemibold)
            Text("Вернуть", Modifier.padding(horizontal = D.M), C.Primary, style = T.LabelStrong)
        }
    }
}
