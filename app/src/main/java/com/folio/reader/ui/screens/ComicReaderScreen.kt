package com.folio.reader.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.folio.reader.data.ReadingMode
import com.folio.reader.ui.ComicReaderViewModel
import com.folio.reader.ui.components.ZoomableImage
import com.folio.reader.ui.folioViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComicReaderScreen(bookId: String, back: () -> Unit) {
    val vm: ComicReaderViewModel = folioViewModel()
    LaunchedEffect(bookId) { vm.load(bookId) }
    val book by vm.book.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var showModeMenu by remember { mutableStateOf(false) }

    val b = book ?: return
    val pages = remember(b.contentDir, b.spine) { vm.pageFiles() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (pages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val tapModifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { showControls = !showControls })
            }
            when (b.readingMode) {
                ReadingMode.WEBTOON -> {
                    LazyColumn(Modifier.fillMaxSize().then(tapModifier)) {
                        items(pages) { page -> ZoomableImage(page, modifier = Modifier.fillMaxWidth()) }
                    }
                }
                else -> {
                    val pagerState = rememberPagerState(
                        initialPage = b.currentChapter.coerceIn(0, pages.size - 1),
                        pageCount = { pages.size },
                    )
                    LaunchedEffect(pagerState.currentPage) { vm.updatePage(pagerState.currentPage) }
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = b.readingMode == ReadingMode.PAGED_RTL,
                        modifier = Modifier.fillMaxSize().then(tapModifier),
                    ) { index -> ZoomableImage(pages[index], modifier = Modifier.fillMaxSize()) }
                }
            }
        }

        if (showControls) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(8.dp, 36.dp, 12.dp, 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = back) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                    Text(b.title, color = Color.White, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (b.readingMode != ReadingMode.WEBTOON) {
                        Text("${b.currentChapter + 1} / ${pages.size}", color = Color.White)
                    }
                    Box {
                        IconButton(onClick = { showModeMenu = true }) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = "Reading mode", tint = Color.White)
                        }
                        DropdownMenu(expanded = showModeMenu, onDismissRequest = { showModeMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Left to right") },
                                onClick = { vm.setReadingMode(ReadingMode.PAGED_LTR); showModeMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Right to left (manga)") },
                                onClick = { vm.setReadingMode(ReadingMode.PAGED_RTL); showModeMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Vertical scroll (webtoon)") },
                                leadingIcon = { Icon(Icons.Filled.ViewDay, contentDescription = null) },
                                onClick = { vm.setReadingMode(ReadingMode.WEBTOON); showModeMenu = false },
                            )
                        }
                    }
                }
            }
        }
    }
}
