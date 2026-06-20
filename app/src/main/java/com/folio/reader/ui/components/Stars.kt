package com.folio.reader.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.folio.reader.ui.theme.Gold
import com.folio.reader.ui.theme.Paper4

@Composable
fun Stars(n: Int, size: Dp = 14.dp, modifier: Modifier = Modifier) {
    Row(modifier) {
        for (i in 1..5) {
            Icon(
                if (i <= n) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (i <= n) Gold else Paper4,
                modifier = Modifier.size(size),
            )
        }
    }
}
