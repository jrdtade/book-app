package com.folio.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Typographic book cover — a gradient panel with the author/title set in serif type, no artwork required. */
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
