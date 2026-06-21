package com.folio.reader.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.folio.reader.data.Book
import com.folio.reader.data.PageFlip
import com.folio.reader.data.ReaderPrefs
import com.folio.reader.data.overallProgress
import com.folio.reader.ui.ReaderViewModel
import com.folio.reader.ui.components.Cover
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.theme.ReaderThemes
import kotlinx.coroutines.launch
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ReaderScreen(bookId: String, back: () -> Unit) {
    val vm: ReaderViewModel = folioViewModel()
    val scope = rememberCoroutineScope()
    LaunchedEffect(bookId) { vm.load(bookId) }

    val book by vm.book.collectAsState()
    val prefs by vm.prefs.collectAsState()
    var showChrome by remember { mutableStateOf(false) }
    var showThemesSheet by remember { mutableStateOf(false) }
    var showAdvancedSheet by remember { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }
    var pageState by remember { mutableStateOf(0 to 1) } // page, total
    var seeking by remember { mutableStateOf(false) }
    var seekPos by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose { vm.pauseSession() }
    }

    val b = book ?: return

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val totalChapters = b.chapterCount.coerceAtLeast(1)

    fun pushChapter(index: Int, web: WebView) {
        scope.launch {
            val html = vm.loadChapterHtml(index)
            val safe = JSONObject.quote(html)
            web.evaluateJavascript("setHtml($safe)", null)
        }
    }

    // Jump to any point in the book from the progress slider.
    fun seekTo(fraction: Float) {
        val web = webViewRef ?: return
        val n = totalChapters
        val raw = (fraction * n).coerceIn(0f, n.toFloat())
        val chapter = raw.toInt().coerceIn(0, n - 1)
        val frac = (raw - chapter).coerceIn(0f, 1f)
        web.evaluateJavascript("setPendingFraction($frac)", null)
        if (chapter != (vm.book.value?.currentChapter ?: 0)) {
            vm.goToChapter(chapter)
            pushChapter(chapter, web)
        } else {
            web.evaluateJavascript("seekFraction($frac)", null)
        }
    }

    fun pushPrefs(web: WebView, p: ReaderPrefs) {
        val theme = ReaderThemes.firstOrNull { it.id == p.theme } ?: ReaderThemes[1]
        val json = JSONObject().apply {
            put("bg", "#%06X".format(0xFFFFFF and theme.bg.toArgb()))
            put("fg", "#%06X".format(0xFFFFFF and theme.fg.toArgb()))
            put("font", when (p.font) {
                "sans" -> "sans-serif"
                "mono" -> "monospace"
                "condensed" -> "sans-serif-condensed"
                "rounded" -> "sans-serif-rounded"
                else -> "serif"
            })
            put("size", p.size)
            put("lh", p.lineHeight)
            put("weight", if (p.bold) "600" else "400")
            put("align", p.align)
            put("mx", p.margin)
            put("warmth", p.warmth)
            put("brightness", p.brightness)
            put("flip", p.flip.name.lowercase())
            put("scroll", p.scrollMode)
            put("tap", p.tapToTurn)
        }
        web.evaluateJavascript("applyPrefs(${JSONObject.quote(json.toString())})", null)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onState(page: Int, total: Int) {
                                pageState = page to total
                                vm.updatePageProgress(page, total)
                            }

                            @JavascriptInterface
                            fun onScroll(pct: Double) {
                                vm.updatePageProgress((pct * 1000).toInt(), 1000)
                            }

                            @JavascriptInterface
                            fun onTapMiddle() {
                                showChrome = !showChrome
                            }

                            @JavascriptInterface
                            fun onPrevChapter() {
                                val current = vm.book.value ?: return
                                if (current.currentChapter > 0) {
                                    val next = current.currentChapter - 1
                                    vm.goToChapter(next)
                                    webViewRef?.let { pushChapter(next, it) }
                                }
                            }

                            @JavascriptInterface
                            fun onNextChapter() {
                                val current = vm.book.value ?: return
                                if (current.currentChapter < totalChapters - 1) {
                                    val next = current.currentChapter + 1
                                    vm.goToChapter(next)
                                    webViewRef?.let { pushChapter(next, it) }
                                } else {
                                    vm.setFinished(rating = 0)
                                }
                            }
                        },
                        "FolioBridge",
                    )
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            val current = vm.book.value ?: return
                            pushPrefs(this@apply, vm.prefs.value)
                            pushChapter(current.currentChapter, this@apply)
                        }
                    }
                    loadUrl("file:///android_asset/reader/reader.html")
                    webViewRef = this
                }
            },
        )

        LaunchedEffect(prefs, webViewRef) {
            webViewRef?.let { pushPrefs(it, prefs) }
        }

        if (showChrome) {
            val (curPage, totalPages) = pageState
            val pagesLeft = (totalPages - 1 - curPage).coerceAtLeast(0)
            val pct = b.overallProgress()

            // Dim scrim so the chrome reads as an overlay, matching the reference app.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)).clickable { showChrome = false })

            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp, 40.dp, 8.dp, 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = back) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                    Text(
                        if (totalPages > 1) "$pagesLeft pages left in chapter" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    IconButton(onClick = { showChrome = false }) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                }

                Spacer(Modifier.weight(1f))

                Column(
                    Modifier.fillMaxWidth().padding(20.dp, 0.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ChromePill("Contents · ${(pct * 100).toInt()}%", Icons.AutoMirrored.Filled.List) { showToc = true }
                    ChromePill("Search Book", Icons.Filled.Search) { showChrome = false }
                    ChromePill("Themes & Settings", Icons.Filled.TextFields) { showThemesSheet = true }
                }

                Spacer(Modifier.height(28.dp))

                Column(
                    Modifier.fillMaxWidth().padding(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (totalPages > 1) "${curPage + 1} of $totalPages" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {}) { Icon(Icons.Filled.Share, contentDescription = "Share") }
                        IconButton(onClick = { showToc = true }) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Contents") }
                        IconButton(onClick = {}) { Icon(Icons.Filled.Bookmark, contentDescription = "Bookmark") }
                    }
                }
            }
        }
    }

    if (showToc) {
        TocSheet(
            book = b,
            chapterPaths = vm.chapterPaths(),
            onDismiss = { showToc = false },
            onSelect = { index ->
                showToc = false
                showChrome = false
                vm.goToChapter(index)
                webViewRef?.let { pushChapter(index, it) }
            },
        )
    }

    if (showThemesSheet) {
        ThemesAndSettingsSheet(
            prefs = prefs,
            onDismiss = { showThemesSheet = false },
            onChange = { updated -> vm.updatePrefs { updated } },
            onCustomise = { showThemesSheet = false; showAdvancedSheet = true },
        )
    }

    if (showAdvancedSheet) {
        ReaderSettingsSheet(
            prefs = prefs,
            onDismiss = { showAdvancedSheet = false },
            onChange = { updated -> vm.updatePrefs { updated } },
        )
    }
}

@Composable
private fun ChromePill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(18.dp, 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun TocSheet(
    book: Book,
    chapterPaths: List<String>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val outline = MaterialTheme.colorScheme.outline
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(20.dp, 4.dp, 20.dp, 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Cover(book, width = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(
                            "Chapter ${book.currentChapter + 1} of ${chapterPaths.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = outline,
                        )
                    }
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }
            Spacer(Modifier.height(10.dp))
            Divider()
            LazyColumn(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp)) {
                items(chapterPaths.size) { index ->
                    val current = index == book.currentChapter
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (current) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onSelect(index) }
                            .padding(12.dp, 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Chapter ${index + 1}",
                            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

private data class FontOption(val id: String, val label: String, val preview: androidx.compose.ui.text.font.FontFamily)

private val FontOptions = listOf(
    FontOption("serif", "Serif", androidx.compose.ui.text.font.FontFamily.Serif),
    FontOption("sans", "Sans", androidx.compose.ui.text.font.FontFamily.SansSerif),
    FontOption("condensed", "Condensed", androidx.compose.ui.text.font.FontFamily.SansSerif),
    FontOption("rounded", "Rounded", androidx.compose.ui.text.font.FontFamily.SansSerif),
    FontOption("mono", "Mono", androidx.compose.ui.text.font.FontFamily.Monospace),
)

private val AlignOptions = listOf(
    "left" to Icons.AutoMirrored.Filled.FormatAlignLeft,
    "justify" to Icons.Filled.FormatAlignJustify,
    "center" to Icons.Filled.FormatAlignCenter,
    "right" to Icons.AutoMirrored.Filled.FormatAlignRight,
)

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun ThemesAndSettingsSheet(
    prefs: ReaderPrefs,
    onDismiss: () -> Unit,
    onChange: (ReaderPrefs) -> Unit,
    onCustomise: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val outline = MaterialTheme.colorScheme.outline
    var tab by remember { mutableStateOf("text") } // "text" or "display"

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(20.dp, 4.dp, 20.dp, 28.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Themes & Settings", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SizeStep("A", small = true, onClick = { onChange(prefs.copy(size = (prefs.size - 1).coerceAtLeast(14))) })
                    SizeStep("A", small = false, onClick = { onChange(prefs.copy(size = (prefs.size + 1).coerceAtMost(30))) })
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TabIcon(Icons.Filled.TextFields, selected = tab == "text") { tab = "text" }
                    TabIcon(Icons.Filled.Brightness6, selected = tab == "display") { tab = "display" }
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = outline, modifier = Modifier.size(16.dp))
                Slider(
                    value = prefs.brightness,
                    onValueChange = { onChange(prefs.copy(brightness = it)) },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = outline, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.height(18.dp))

            val rows = ReaderThemes.chunked(3)
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { theme ->
                        val selected = prefs.theme == theme.id
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(theme.bg)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .clickable { onChange(prefs.copy(theme = theme.id)) }
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Aa", style = MaterialTheme.typography.titleLarge, color = theme.fg)
                            Spacer(Modifier.height(4.dp))
                            Text(theme.label, style = MaterialTheme.typography.labelSmall, color = theme.fg)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.TextButton(
                onClick = onCustomise,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Customise") }
        }
    }
}

@Composable
private fun SizeStep(label: String, small: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp, 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TabIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun ReaderSettingsSheet(
    prefs: ReaderPrefs,
    onDismiss: () -> Unit,
    onChange: (ReaderPrefs) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val outline = MaterialTheme.colorScheme.outline
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(20.dp, 4.dp, 20.dp, 32.dp).verticalScroll(rememberScrollState())) {
            Text("Customise", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            Text("FONT", style = MaterialTheme.typography.labelMedium, color = outline)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FontOptions.forEach { option ->
                    val selected = prefs.font == option.id
                    Column(
                        Modifier
                            .width(62.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(
                                width = if (selected) 1.5.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { onChange(prefs.copy(font = option.id)) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Aa",
                            fontFamily = option.preview,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else outline,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("TEXT SIZE", style = MaterialTheme.typography.labelMedium, color = outline)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("A", style = MaterialTheme.typography.bodySmall, color = outline)
                Slider(
                    value = prefs.size.toFloat(),
                    onValueChange = { onChange(prefs.copy(size = it.toInt())) },
                    valueRange = 14f..30f,
                    steps = 7,
                    modifier = Modifier.weight(1f),
                )
                Text("A", style = MaterialTheme.typography.headlineSmall, color = outline)
            }

            Text("LINE SPACING", style = MaterialTheme.typography.labelMedium, color = outline)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.FormatLineSpacing, contentDescription = null, tint = outline, modifier = Modifier.size(16.dp))
                Slider(
                    value = prefs.lineHeight,
                    onValueChange = { onChange(prefs.copy(lineHeight = it)) },
                    valueRange = 1.2f..2.2f,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.FormatLineSpacing, contentDescription = null, tint = outline, modifier = Modifier.size(26.dp))
            }

            Text("MARGINS", style = MaterialTheme.typography.labelMedium, color = outline)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(10.dp).border(1.dp, outline, RoundedCornerShape(2.dp)))
                Slider(
                    value = prefs.margin.toFloat(),
                    onValueChange = { onChange(prefs.copy(margin = it.toInt())) },
                    valueRange = 12f..56f,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.size(20.dp).border(1.dp, outline, RoundedCornerShape(3.dp)))
            }

            Spacer(Modifier.height(14.dp))
            Text("ALIGNMENT", style = MaterialTheme.typography.labelMedium, color = outline)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AlignOptions.forEach { (id, icon) ->
                    val selected = prefs.align == id
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onChange(prefs.copy(align = id)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            contentDescription = id,
                            tint = if (selected) MaterialTheme.colorScheme.primary else outline,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = outline)
                Spacer(Modifier.width(10.dp))
                Text("WARMTH", style = MaterialTheme.typography.labelMedium, color = outline)
            }
            Slider(
                value = prefs.warmth,
                onValueChange = { onChange(prefs.copy(warmth = it)) },
                valueRange = 0f..1f,
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PageFlip.entries.forEach { flip ->
                    androidx.compose.material3.FilterChip(
                        selected = prefs.flip == flip,
                        onClick = { onChange(prefs.copy(flip = flip)) },
                        label = { Text(flip.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clickable { onChange(prefs.copy(tapToTurn = !prefs.tapToTurn)) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Tap edges to turn", style = MaterialTheme.typography.bodyLarge)
                    Text("Swipe always works", style = MaterialTheme.typography.bodySmall, color = outline)
                }
                androidx.compose.material3.Switch(checked = prefs.tapToTurn, onCheckedChange = { onChange(prefs.copy(tapToTurn = it)) })
            }

            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().clickable { onChange(prefs.copy(scrollMode = !prefs.scrollMode)) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Scroll mode", style = MaterialTheme.typography.bodyLarge)
                androidx.compose.material3.Switch(checked = prefs.scrollMode, onCheckedChange = { onChange(prefs.copy(scrollMode = it)) })
            }
        }
    }
}
