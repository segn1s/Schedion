package net.schedion.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.schedion.app.R

private val BodyFamily = FontFamily(
    Font(R.font.manrope_400, FontWeight.Normal),
    Font(R.font.manrope_600, FontWeight.SemiBold),
    Font(R.font.manrope_700, FontWeight.Bold),
    Font(R.font.manrope_800, FontWeight.ExtraBold),
)

private val DisplayFamily = FontFamily(Font(R.font.unbounded_700, FontWeight.Bold))

private fun textStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    family: FontFamily = BodyFamily,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

internal object SchedionType {
    val Brand = textStyle(27, 35, FontWeight.Bold, DisplayFamily).copy(textGeometricTransform = TextGeometricTransform(scaleX = .97f))
    val Heading = textStyle(23, 27, FontWeight.Bold, DisplayFamily).copy(textGeometricTransform = TextGeometricTransform(scaleX = .96f))
    val SheetHeading = textStyle(19, 23, FontWeight.Bold, DisplayFamily).copy(textGeometricTransform = TextGeometricTransform(scaleX = .94f))
    val ArticleHeading = textStyle(17, 21, FontWeight.Bold, DisplayFamily).copy(textGeometricTransform = TextGeometricTransform(scaleX = .94f))
    val DayNumber = textStyle(17, 21, FontWeight.ExtraBold)
    val Lead = textStyle(16, 24, FontWeight.SemiBold)
    val ArticleBody = textStyle(15, 23)
    val Title = textStyle(15, 19, FontWeight.Bold)
    val Body = textStyle(14, 20)
    val BodySemibold = textStyle(14, 20, FontWeight.SemiBold)
    val BodyStrong = textStyle(14, 19, FontWeight.Bold)
    val Search = textStyle(15, 21)
    val Directory = textStyle(15, 21, FontWeight.SemiBold)
    val Label = textStyle(13, 17)
    val LabelStrong = textStyle(13, 17, FontWeight.Bold)
    val Small = textStyle(12, 16)
    val SmallStrong = textStyle(12, 16, FontWeight.Bold)
    val File = textStyle(10, 13, FontWeight.ExtraBold)
    val LargeFile = textStyle(14, 18, FontWeight.ExtraBold)
}

internal val SchedionTypography = Typography(
    headlineMedium = SchedionType.Heading,
    titleMedium = SchedionType.Title,
    bodyLarge = SchedionType.Body,
    bodyMedium = SchedionType.Body,
    labelMedium = SchedionType.LabelStrong,
)
