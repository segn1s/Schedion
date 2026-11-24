package net.schedion.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun PeopleHeading(label: String, modifier: Modifier = Modifier) {
    Text(label, modifier.padding(bottom = D.S), C.Secondary, style = T.LabelStrong)
}
