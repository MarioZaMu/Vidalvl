package com.vidalvl.app.ui.avatar

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import com.vidalvl.app.domain.model.CatalogItem

/** A catalog item's own sprite, scaled with nearest-neighbor like the avatar. */
@Composable
fun ItemSprite(item: CatalogItem, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Image(
        bitmap = ImageBitmap.imageResource(item.spriteRes),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None
    )
}
