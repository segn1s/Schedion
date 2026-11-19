package net.schedion.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import net.schedion.app.R
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun OfflineBanner(savedAt: Long?, modifier: Modifier = Modifier) {
    MockCard(modifier, outline = C.GoldOutline) {
        Column(
            Modifier.padding(horizontal = D.Page, vertical = D.Sm),
            verticalArrangement = Arrangement.spacedBy(D.Tiny),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(D.Sm)) {
                Icon(
                    painterResource(R.drawable.ic_wifi_off),
                    contentDescription = null,
                    modifier = Modifier.size(D.Icon),
                    tint = C.AmberText,
                )
                Text(stringResource(R.string.state_offline_title), style = T.Title)
            }
            if (savedAt != null) {
                val savedTime = SnapshotTime.format(Instant.ofEpochMilli(savedAt).atZone(ZoneId.systemDefault()))
                Text(stringResource(R.string.state_offline_saved_at, savedTime), color = C.Muted, style = T.Small)
            }
        }
    }
}

private val SnapshotTime = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")
