package com.folio.reader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.folio.reader.data.BookRepository
import com.folio.reader.source.MediaSource
import com.folio.reader.source.SourceChapter
import com.folio.reader.source.SourceMediaDetails
import kotlinx.coroutines.launch

/**
 * Shows a catalog item's details and chapter list, and lets the user download a chapter
 * (which, per [BookRepository.downloadSourceChapter], both saves it to the library and
 * makes it readable — there's no separate "add to library" step, since a chapter isn't
 * useful in the library until it actually has pages to read).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceMediaDetailScreen(
    source: MediaSource,
    sourceMediaId: String,
    repository: BookRepository,
    back: () -> Unit,
    onOpenBook: (bookId: String) -> Unit,
) {
    var details by remember(sourceMediaId) { mutableStateOf<SourceMediaDetails?>(null) }
    var chapters by remember(sourceMediaId) { mutableStateOf<List<SourceChapter>>(emptyList()) }
    var loading by remember(sourceMediaId) { mutableStateOf(true) }
    var errorMessage by remember(sourceMediaId) { mutableStateOf<String?>(null) }
    var downloadingChapterId by remember { mutableStateOf<String?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val downloadedBooks by repository.observeBooksBySource(source.id).collectAsState(initial = emptyList())
    val downloadedChapterIds = remember(downloadedBooks, chapters) {
        val downloadedIds = downloadedBooks.map { it.id }.toSet()
        chapters.filter {
            repository.chapterBookId(source.id, sourceMediaId, it.chapterId) in downloadedIds
        }.map { it.chapterId }.toSet()
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(sourceMediaId) {
        loading = true
        errorMessage = null
        try {
            details = source.fetchMediaDetails(sourceMediaId)
            chapters = source.fetchChapterList(sourceMediaId)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            e.printStackTrace()
            errorMessage = e.message ?: e.javaClass.simpleName
        } finally {
            loading = false
        }
    }

    fun openOrDownload(chapter: SourceChapter) {
        val bookId = repository.chapterBookId(source.id, sourceMediaId, chapter.chapterId)
        if (chapter.chapterId in downloadedChapterIds) {
            onOpenBook(bookId)
            return
        }
        downloadingChapterId = chapter.chapterId
        downloadError = null
        scope.launch {
            try {
                val book = repository.downloadSourceChapter(
                    source = source,
                    sourceMediaId = sourceMediaId,
                    seriesTitle = details?.title ?: chapter.title,
                    seriesAuthor = details?.author ?: "Unknown",
                    seriesCoverUrl = details?.coverUrl,
                    chapter = chapter,
                )
                onOpenBook(book.id)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
                downloadError = e.message ?: e.javaClass.simpleName
            } finally {
                downloadingChapterId = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.title ?: "Loading…", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                errorMessage != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Failed to load: $errorMessage",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    val info = details
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (info != null) {
                            item { MediaHeader(info) }
                        }
                        if (downloadError != null) {
                            item {
                                Text(
                                    "Download failed: $downloadError",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                        }
                        item {
                            Text(
                                "${chapters.size} chapters",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(chapters, key = { it.chapterId }) { chapter ->
                            ChapterRow(
                                chapter = chapter,
                                isDownloaded = chapter.chapterId in downloadedChapterIds,
                                isDownloading = chapter.chapterId == downloadingChapterId,
                                onClick = { openOrDownload(chapter) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaHeader(details: SourceMediaDetails) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Card(modifier = Modifier.width(96.dp).aspectRatio(0.7f)) {
            AsyncImage(
                model = details.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(details.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(details.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            details.genre?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    details.description?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    HorizontalDivider()
}

@Composable
private fun ChapterRow(
    chapter: SourceChapter,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(chapter.title, maxLines = 1) },
        modifier = Modifier.clickable(enabled = !isDownloading) { onClick() },
        trailingContent = {
            when {
                isDownloading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                isDownloaded -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary,
                )
                else -> Icon(Icons.Default.Download, contentDescription = "Download")
            }
        },
    )
}
