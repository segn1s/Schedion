package net.schedion.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

/** Строка выбора источника расписания со звёздочкой избранного. */
@Composable
internal fun PickerFavorite(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    subtitleColor: Color = C.Muted,
    onClick: (() -> Unit)? = null,
    favorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = D.Sm)
            .clip(RoundedCornerShape(D.ControlRadius)).background(if (selected) C.Cover else Color.Transparent)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(start = D.M, end = D.Xs, top = D.Sm, bottom = D.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.Sm),
    ) {
        if (selected) LineIcon(Glyph.Check, tint = C.Primary, size = D.IconSmall)
        else Spacer(Modifier.width(D.IconSmall))
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Title)
            Text(subtitle, color = subtitleColor, style = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onToggleFavorite != null) {
            FavoriteStar(favorite = favorite, onToggle = onToggleFavorite)
        }
    }
}
