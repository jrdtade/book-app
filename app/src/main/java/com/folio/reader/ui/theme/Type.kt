package com.folio.reader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// No bundled webfonts (keeps the project buildable offline) — serif/sans-serif
// generic families resolve to Noto Serif / Roboto on stock Android, which sit
// close enough to the Newsreader/Hanken Grotesk pairing in the original design.
val FolioSerif = FontFamily.Serif
val FolioSans = FontFamily.SansSerif
val FolioMono = FontFamily.Monospace

val FolioTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FolioSerif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = FolioSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = FolioSerif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = FolioSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FolioSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = FolioSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FolioSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FolioSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FolioMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.4.sp),
)
