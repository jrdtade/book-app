package com.folio.reader.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import com.folio.reader.data.Book
import java.io.File

/** Book cover — shows the EPUB's own cover artwork when available, otherwise
 *  falls back to a gradient panel with the author/title set in serif type. */
@Composable
fun Cover(
    book: Book,
    width: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val height = width * 1.52f
    val colorA = Color(book.coverColorA)
    val colorB = Color(book.coverColorB)
    var artwork by remember(book.id, book.coverPath) { mutableStateOf<ImageBitmap?>(null) }
    remember(book.id, book.coverPath) {
        val path = book.coverPath
        artwork = if (path != null) {
            val file = File(book.contentDir, path)
            runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
        } else {
            null
        }
        true
    }
    val art = artwork
    if (art != null) {
        Image(
            bitmap = art,
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(width, height)
                .clip(RoundedCornerShape(width.value.coerceAtMost(12f).dp))
                .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        )
        return
    }
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(width.value.coerceAtMost(12f).dp))
            .background(Brush.linearGradient(listOf(colorA, colorB), start = Offset(0f, 0f)))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(width / 7.5f),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                book.author.split(" ").last().uppercase(),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = (width.value / 11).sp,
                letterSpacing = 1.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height((width.value / 14).dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.35f)))
            Spacer(Modifier.height((width.value / 9).dp))
            Text(
                book.title.replace("\\n", "\n"),
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = (width.value / 6.2f).sp,
                lineHeight = (width.value / 5.4f).sp,
            )
        }
    }
}
