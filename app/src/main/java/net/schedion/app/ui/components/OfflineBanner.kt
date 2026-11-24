package net.schedion.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import net.schedion.app.R
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

/**
 * Плашка офлайн-режима: предупреждение, время сохранённого снимка и обновление.
 *
 * @param onRefresh показывать ли кнопку «Обновить» и что вызывать при нажатии.
 */
@Composable
internal fun OfflineBanner(
    savedAt: Long?,
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SchedionCard(modifier, outline = C.GoldOutline) {
        Row(
            Modifier.padding(horizontal = D.Page, vertical = D.Ml),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(C.Today)
            Spacer(Modifier.width(D.Ml))
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(D.Tiny),
            ) {
                Text(stringResource(R.string.state_offline), color = C.AmberText, style = T.BodyStrong)
                if (savedAt != null) {
                    val savedTime = SnapshotTime.format(Instant.ofEpochMilli(savedAt).atZone(ZoneId.systemDefault()))
                    Text(
                        stringResource(R.string.state_offline_saved_at, savedTime),
                        color = C.Secondary,
                        style = T.Small,
                    )
                }
            }
            if (onRefresh != null) {
                Spacer(Modifier.width(D.M))
                SchedionButton(
                    stringResource(R.string.common_refresh),
                    compact = true,
                    onClick = onRefresh,
                )
            }
        }
    }
}

private val SnapshotTime = DateTimeFormatter.ofPattern("dd.MM, HH:mm")
