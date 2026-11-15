package net.schedion.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D

internal enum class Glyph {
    Calendar, EmptyCalendar, Search, News, Document, Star, FilledStar,
    Back, Next, Down, Check, Download, Share, External, Remove, Reorder,
}

/** Контурные иконки макета на сетке 24 × 24. */
@Composable
internal fun LineIcon(
    glyph: Glyph,
    modifier: Modifier = Modifier,
    tint: Color = C.Text,
    size: Dp = D.Icon,
) {
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, this.size.height / 24f, Offset.Zero) {
            val stroke = Stroke(1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun line(x: Float, y: Float, x2: Float, y2: Float) =
                drawLine(tint, Offset(x, y), Offset(x2, y2), stroke.width, StrokeCap.Round)
            fun outline(vararg coordinates: Float, closed: Boolean = false, filled: Boolean = false) {
                val path = Path().apply {
                    moveTo(coordinates[0], coordinates[1])
                    for (i in 2 until coordinates.size step 2) lineTo(coordinates[i], coordinates[i + 1])
                    if (closed) close()
                }
                if (filled) drawPath(path, tint) else drawPath(path, tint, style = stroke)
            }
            fun rectangle(x: Float, y: Float, w: Float, h: Float, radius: Float) =
                drawRoundRect(tint, Offset(x, y), Size(w, h), CornerRadius(radius), style = stroke)

            when (glyph) {
                Glyph.Calendar, Glyph.EmptyCalendar -> {
                    rectangle(3f, 4f, 18f, 18f, 2f)
                    line(8f, 2f, 8f, 6f)
                    line(16f, 2f, 16f, 6f)
                    line(3f, 10f, 21f, 10f)
                    if (glyph == Glyph.EmptyCalendar) line(9f, 15f, 15f, 15f)
                }
                Glyph.Search -> {
                    drawCircle(tint, 6.4f, Offset(10.5f, 10.5f), style = stroke)
                    line(15.4f, 15.4f, 21f, 21f)
                }
                Glyph.News -> {
                    rectangle(4f, 4f, 12f, 15f, 1f)
                    outline(16f, 8f, 19f, 8f, 19f, 18f, 17.5f, 19f, 15f, 19f)
                    line(7.5f, 8f, 12.5f, 8f)
                    line(7.5f, 11.5f, 12.5f, 11.5f)
                    line(7.5f, 15f, 10f, 15f)
                }
                Glyph.Document -> {
                    outline(5f, 3f, 14f, 3f, 19.5f, 8.5f, 19.5f, 20.5f, 5f, 20.5f, closed = true)
                    outline(14f, 3f, 14f, 8.5f, 19.5f, 8.5f)
                }
                Glyph.Star, Glyph.FilledStar -> outline(
                    12f, 2f, 15f, 8f, 22f, 9.2f, 17f, 14.3f, 18.2f, 21f,
                    12f, 17.8f, 5.8f, 21f, 7f, 14.3f, 2f, 9.2f, 9f, 8f,
                    closed = true, filled = glyph == Glyph.FilledStar,
                )
                Glyph.Back -> outline(15f, 6f, 9f, 12f, 15f, 18f)
                Glyph.Next -> outline(9f, 6f, 15f, 12f, 9f, 18f)
                Glyph.Down -> outline(6f, 9f, 12f, 15f, 18f, 9f)
                Glyph.Check -> outline(4f, 12f, 9f, 17f, 20f, 6f)
                Glyph.Download -> {
                    line(12f, 3f, 12f, 15f)
                    outline(7f, 11f, 12f, 16f, 17f, 11f)
                    line(5f, 20f, 19f, 20f)
                }
                Glyph.Share -> {
                    outline(5f, 12f, 5f, 21f, 19f, 21f, 19f, 12f)
                    line(12f, 17f, 12f, 3f)
                    outline(7f, 8f, 12f, 3f, 17f, 8f)
                }
                Glyph.External -> {
                    line(5f, 19f, 19f, 5f)
                    outline(8f, 5f, 19f, 5f, 19f, 16f)
                }
                Glyph.Remove -> {
                    drawCircle(tint, 10f, Offset(12f, 12f), style = stroke)
                    line(7f, 12f, 17f, 12f)
                }
                Glyph.Reorder -> {
                    line(4f, 8.5f, 20f, 8.5f)
                    line(4f, 15.5f, 20f, 15.5f)
                }
            }
        }
    }
}
