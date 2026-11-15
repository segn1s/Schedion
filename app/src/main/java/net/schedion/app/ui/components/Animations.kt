package net.schedion.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D

/** Плавный шиммер-градиент для анимированной индикации сетевой загрузки. */
@Composable
internal fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    val shimmerColors = listOf(
        C.Surface,
        C.Divider.copy(alpha = 0.55f),
        C.Surface,
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation, y = translateAnimation),
    )
}

/** Скелетон-заглушка с шиммером для карточек. */
@Composable
internal fun SkeletonBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = D.Page,
    shapeRadius: Dp = D.ControlRadius,
) {
    val brush = rememberShimmerBrush()
    val shape = RoundedCornerShape(shapeRadius)
    val widthModifier = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
    Box(
        modifier = modifier
            .then(widthModifier)
            .height(height)
            .clip(shape)
            .background(brush)
            .border(D.Hairline, C.Outline.copy(alpha = 0.4f), shape),
    )
}

/** Скелетон карточек расписания на время сетевого запроса. */
@Composable
internal fun ScheduleSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(D.M)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(D.S)) {
            repeat(6) {
                SkeletonBox(
                    modifier = Modifier.weight(1f),
                    height = D.DayCell,
                    shapeRadius = D.ControlRadius,
                )
            }
        }
        Spacer(Modifier.height(D.Sm))
        repeat(3) {
            MockCard {
                Row(Modifier.padding(horizontal = D.Page, vertical = D.Ml)) {
                    Column(Modifier.width(D.ScheduleTime), verticalArrangement = Arrangement.spacedBy(D.Xs)) {
                        SkeletonBox(width = 36.dp, height = 12.dp)
                        SkeletonBox(width = 44.dp, height = 16.dp)
                        SkeletonBox(width = 36.dp, height = 12.dp)
                    }
                    Spacer(Modifier.width(D.M))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(D.Sm)) {
                        SkeletonBox(width = 90.dp, height = 12.dp)
                        SkeletonBox(width = 180.dp, height = 16.dp)
                        SkeletonBox(width = 130.dp, height = 12.dp)
                    }
                }
            }
        }
    }
}
