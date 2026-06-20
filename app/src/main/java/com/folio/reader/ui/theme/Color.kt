package com.folio.reader.ui.theme

import androidx.compose.ui.graphics.Color

// Warm paper neutrals — ported from the Folio design tokens (oklch -> sRGB).
val Paper = Color(0xFFF7F2E9)
val Paper2 = Color(0xFFFDFAF5)
val Paper3 = Color(0xFFEEE8DF)
val Paper4 = Color(0xFFE7E0D5)
val Ink = Color(0xFF2D241E)
val Ink2 = Color(0xFF5D544D)
val Ink3 = Color(0xFF888079)
val LineColor = Color(0xFFDDD7CF)
val Line2 = Color(0xFFD0C9BF)

val Blue = Color(0xFF0068E4)
val Blue2 = Color(0xFF0056D8)
val BlueSoft = Color(0xFFD5EAFF)
val BlueInk = Color(0xFF0040B7)
val Gold = Color(0xFFD1A255)

// Reader page themes
data class ReaderTheme(val id: String, val label: String, val bg: Color, val fg: Color)

val ReaderThemes = listOf(
    ReaderTheme("paper", "Paper", Color(0xFFFDFCFA), Color(0xFF27221D)),
    ReaderTheme("sepia", "Sepia", Color(0xFFF3E5D0), Color(0xFF3F2E25)),
    ReaderTheme("quartz", "Quartz", Color(0xFFC0C3C6), Color(0xFF202328)),
    ReaderTheme("night", "Night", Color(0xFF1C1E22), Color(0xFFC7C2BA)),
    ReaderTheme("black", "Black", Color(0xFF090909), Color(0xFFA7A49F)),
)
