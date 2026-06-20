package com.folio.reader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.folio.reader.ui.theme.Blue
import com.folio.reader.ui.theme.Paper4

@Composable
fun ProgressRing(
    pct: Float,
    size: Dp = 44.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = Blue,
    track: Color = Paper4,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(strokeWidth.toPx())
        val inset = stroke.width / 2
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(this.size.width - stroke.width, this.size.height - stroke.width),
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * pct.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(this.size.width - stroke.width, this.size.height - stroke.width),
            style = Stroke(strokeWidth.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}
