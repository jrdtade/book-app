package com.folio.reader.ui.components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.roundToInt

/** A single comic/manga page with pinch-to-zoom and pan, the Compose equivalent of
 *  SubsamplingScaleImageView used for this app's image reader. */
@Composable
fun ZoomableImage(file: File, modifier: Modifier = Modifier) {
    var scale by remember(file) { mutableStateOf(1f) }
    var offset by remember(file) { mutableStateOf(Offset.Zero) }

    AsyncImage(
        model = file,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(file) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    offset = if (newScale <= 1f) Offset.Zero else offset + pan
                }
            }
            .scale(scale)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) },
    )
}
