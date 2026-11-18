package net.schedion.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import net.schedion.app.R
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun SchedionBackdrop(
    modifier: Modifier = Modifier,
    warm: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().drawBehind {
            drawRect(C.Background)
            if (!warm) return@drawBehind
            drawRect(
                Brush.radialGradient(
                    0f to C.WarmGlow,
                    .50f to C.WarmGlow.copy(alpha = .55f),
                    1f to Color.Transparent,
                    center = Offset(size.width * .70f, 0f),
                    radius = size.width * .56f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    0f to C.WarmGlow.copy(alpha = .72f),
                    .50f to C.WarmGlow.copy(alpha = .24f),
                    1f to Color.Transparent,
                    center = Offset(0f, size.height),
                    radius = size.width * .49f,
                ),
            )
        },
        content = content,
    )
}

@Composable
internal fun bottomSpacing() = with(LocalDensity.current) {
    val inset = WindowInsets.navigationBars.getBottom(this).toDp()
    if (inset <= D.Xxl) D.Bottom else inset
}

@Composable
internal fun topSpacing() = with(LocalDensity.current) {
    maxOf(D.Top, WindowInsets.statusBars.getTop(this).toDp())
}

@Composable
internal fun ScreenColumn(
    modifier: Modifier = Modifier,
    withTabs: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            start = D.Page,
            end = D.Page,
            top = topSpacing(),
            bottom = bottomSpacing() + if (withTabs) D.NavHeight + D.Ml else D.Ml,
        ),
        content = content,
    )
}

@Composable
internal fun MockCard(
    modifier: Modifier = Modifier,
    color: Color = C.Surface,
    outline: Color = C.Outline,
    large: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(if (large) D.PanelRadius else D.CardRadius)
    Column(
        modifier.fillMaxWidth().clip(shape).background(color).border(D.Hairline, outline, shape)
            .then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)),
        content = content,
    )
}

@Composable
internal fun MockButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    compact: Boolean = false,
    icon: Glyph? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(D.ControlRadius)
    val ink = if (primary) C.White else C.Primary
    Row(
        modifier.heightIn(min = if (compact) D.ButtonSmall else D.Button)
            .clip(shape).background(if (primary) C.Primary else C.Surface)
            .then(if (primary) Modifier else Modifier.border(D.Hairline, C.Outline, shape))
            .then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
            .padding(horizontal = D.Ml, vertical = D.Xs),
        horizontalArrangement = Arrangement.spacedBy(D.Sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) LineIcon(icon, tint = ink, size = D.IconSmall)
        Text(label, color = ink, style = if (compact) T.SmallStrong else T.BodyStrong, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun Segments(
    labels: List<String>,
    active: Int,
    modifier: Modifier = Modifier,
    onSelect: ((Int) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(D.ControlRadius)
    Row(modifier.clip(shape).background(C.Surface).border(D.Hairline, C.Outline, shape)) {
        labels.forEachIndexed { index, label ->
            Box(
                Modifier.heightIn(min = D.ButtonSmall)
                    .background(if (index == active) C.Primary else Color.Transparent)
                    .semantics { selected = index == active }
                    .then(
                        if (onSelect == null) Modifier
                        else Modifier.clickable(role = Role.Tab) { onSelect(index) },
                    )
                    .padding(horizontal = D.Page, vertical = D.Xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (index == active) C.White else C.Primary, style = T.LabelStrong)
            }
        }
    }
}

@Composable
internal fun Chip(
    label: String,
    fill: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    outline: Color = Color.Transparent,
    padding: PaddingValues = PaddingValues(horizontal = D.M, vertical = D.Tiny),
) {
    val shape = RoundedCornerShape(percent = 50)
    Text(
        label,
        modifier.clip(shape).background(fill).border(D.Hairline, outline, shape).padding(padding),
        color = ink,
        style = T.SmallStrong,
    )
}

@Composable
internal fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search_hint),
    onSearch: (() -> Unit)? = null,
) {
    val clearLabel = stringResource(R.string.search_clear)
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(D.ControlRadius)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isFocused) D.SearchField else D.SheetSearchField)
            .then(
                if (!isFocused) Modifier else Modifier.drawBehind {
                    val halo = D.Tiny.toPx()
                    drawRoundRect(
                        C.FocusRing.copy(alpha = .45f),
                        topLeft = Offset(-halo, -halo),
                        size = Size(size.width + halo * 2, size.height + halo * 2),
                        cornerRadius = CornerRadius(D.ControlRadius.toPx() + halo),
                    )
                },
            )
            .background(if (isFocused) C.Sheet else C.Past, shape)
            .border(
                if (isFocused) D.ThickOutline else D.Hairline,
                if (isFocused) C.FocusRing else C.Outline,
                shape,
            )
            .padding(horizontal = D.Page),
        horizontalArrangement = Arrangement.spacedBy(D.Ml),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(Glyph.Search, size = D.Icon, tint = if (isFocused) C.Primary else C.Muted)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = C.Muted, style = T.Search)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = T.Search.copy(color = C.Text),
                cursorBrush = SolidColor(C.Primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier
                    .size(D.Icon)
                    .semantics { contentDescription = clearLabel }
                    .clickable(role = Role.Button) { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                LineIcon(Glyph.Remove, size = D.IconSmall, tint = C.Muted)
            }
        }
    }
}

@Composable
internal fun MockDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = D.Hairline, color = C.Divider)
}

@Composable
internal fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(D.IconSmall).background(color.copy(alpha = .14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(D.Sm).background(color, CircleShape))
    }
}

@Composable
internal fun BackBar(
    label: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = D.Toolbar),
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            Modifier.clickable(role = Role.Button, onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(D.Xs),
        ) {
            LineIcon(Glyph.Back, tint = C.Primary)
            Text(label, color = C.Primary, style = T.Title)
        }
        Spacer(Modifier.weight(1f))
        trailing()
    }
}
