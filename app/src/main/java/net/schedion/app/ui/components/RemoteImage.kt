package net.schedion.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import net.schedion.app.R
import net.schedion.app.data.remote.ContentImageLoader
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun RemoteImage(
    url: String,
    description: String,
    loader: ContentImageLoader,
    modifier: Modifier = Modifier,
    scale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
) {
    var attempt by remember(url) { mutableIntStateOf(0) }
    var finished by remember(url, attempt) { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(null, url, attempt) {
        value = loader.load(url)
        finished = true
    }
    Box(modifier.background(C.Cover), contentAlignment = Alignment.Center) {
        val image = bitmap
        when {
            image != null -> Image(image.asImageBitmap(), description, Modifier.fillMaxSize(),
                alignment = alignment, contentScale = scale)
            !finished -> SkeletonBox(modifier = Modifier.fillMaxSize(), height = D.NewsCover)
            else -> Text(
                stringResource(R.string.news_image_retry),
                Modifier.clickable { attempt++ }.padding(D.Sm),
                color = C.Primary, style = T.Small,
            )
        }
    }
}
