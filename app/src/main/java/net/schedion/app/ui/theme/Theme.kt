package net.schedion.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val ColorScheme = lightColorScheme(
    primary = SchedionColors.Primary,
    onPrimary = SchedionColors.White,
    background = SchedionColors.Background,
    onBackground = SchedionColors.Text,
    surface = SchedionColors.Surface,
    onSurface = SchedionColors.Text,
    onSurfaceVariant = SchedionColors.Muted,
    outline = SchedionColors.Outline,
    error = SchedionColors.Red,
)

@Composable
fun SchedionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = SchedionTypography,
    ) {
        CompositionLocalProvider(LocalContentColor provides SchedionColors.Text, content = content)
    }
}
