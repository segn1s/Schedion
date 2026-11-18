package net.schedion.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import net.schedion.app.R
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D

/** Кнопка-звёздочка, добавляющая источник расписания в избранное или убирающая его оттуда. */
@Composable
internal fun FavoriteStar(
    favorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionLabel = stringResource(if (favorite) R.string.favorites_remove else R.string.favorites_add)
    Box(
        modifier.size(D.NavIcon + D.Sm)
            .semantics { contentDescription = actionLabel }
            .clickable(role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        LineIcon(
            if (favorite) Glyph.FilledStar else Glyph.Star,
            tint = if (favorite) C.Primary else C.Muted,
            size = D.NavIcon,
        )
    }
}
