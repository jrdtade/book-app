package com.folio.reader.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.folio.reader.data.Book
import com.folio.reader.data.MediaType
import com.folio.reader.data.Shelf
import com.folio.reader.data.ReadStatus
import com.folio.reader.data.overallProgress
import com.folio.reader.ui.components.Cover
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.theme.Ink
import com.folio.reader.ui.theme.Ink2
import com.folio.reader.ui.theme.Ink3
import com.folio.reader.ui.theme.Paper
import com.folio.reader.ui.theme.Paper3

private enum class LibFilter(val label: String) { ALL("All"), READING("Reading"), FINISHED("Finished"), WANT("Want to read") }

/** Top-level Library categories, each backed by its own MediaType-filtered Room flow. */
private enum class LibCategory(val mediaType: MediaType, val label: String) {
    EBOOKS(MediaType.EPUB, "eBooks"),
    MANGA(MediaType.MANGA, "Manga"),
    COMICS(MediaType.COMIC, "Comics"),
    LIGHT_NOVELS(MediaType.LIGHT_NOVEL, "Light Novels"),
}

@Composable
fun LibraryScreen(openBook: (String) -> Unit) {
    val vm: com.folio.reader.ui.LibraryViewModel = folioViewModel()
    var category by remember { mutableStateOf(LibCategory.EBOOKS) }
    val books by remember(category) { vm.observeByType(category.mediaType) }.collectAsState(initial = emptyList())
    val collections by vm.collections.collectAsState()
    var filter by remember { mutableStateOf(LibFilter.ALL) }
    var selectedGenre by remember(category) { mutableStateOf<String?>(null) }
    var selectedShelf by remember { mutableStateOf<Shelf?>(null) }
    var showNewShelfDialog by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val name = queryDisplayName(context, it)
            when (name?.substringAfterLast('.', "")?.lowercase()) {
                // Both archive formats default to COMIC; long-press a book to recategorize it as Manga.
                "cbz", "cbr" -> vm.importComic(it, name, MediaType.COMIC)
                else -> vm.importEpub(it)
            }
        }
    }

    val genres = remember(books) { books.mapNotNull { it.genre }.distinct().sorted() }

    val shelfBookIds by (selectedShelf?.let { vm.bookIdsInCollection(it.id) } ?: kotlinx.coroutines.flow.flowOf(null))
        .collectAsState(initial = null)

    if (showNewShelfDialog) {
        NewShelfDialog(onDismiss = { showNewShelfDialog = false }, onCreate = { vm.createCollection(it) })
    }

    bookToDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Remove \"${b.title}\"?") },
            text = {
                Column {
                    Text("This removes the book and its downloaded contents from your library. This can't be undone.")
                    if (b.mediaType == MediaType.COMIC || b.mediaType == MediaType.MANGA) {
                        val other = if (b.mediaType == MediaType.MANGA) MediaType.COMIC else MediaType.MANGA
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { vm.recategorize(b, other); bookToDelete = null }) {
                            Text("Move to ${if (other == MediaType.MANGA) "Manga" else "Comics"} instead")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.deleteBook(b); bookToDelete = null }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { bookToDelete = null }) { Text("Cancel") } },
        )
    }

    val statusFiltered = when (filter) {
        LibFilter.ALL -> books
        LibFilter.READING -> books.filter { it.status == ReadStatus.READING }
        LibFilter.FINISHED -> books.filter { it.status == ReadStatus.FINISHED }
        LibFilter.WANT -> books.filter { it.status == ReadStatus.WANT }
    }
    val genreFiltered = if (selectedGenre != null) statusFiltered.filter { it.genre == selectedGenre } else statusFiltered
    val filtered = shelfBookIds?.let { ids -> genreFiltered.filter { it.id in ids } } ?: genreFiltered

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { launcher.launch(IMPORTABLE_MIME_TYPES) }) {
                Icon(Icons.Filled.Add, contentDescription = "Import book")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(20.dp, 28.dp, 20.dp, 12.dp)) {
                Text("${books.size} ${category.label.uppercase()}", color = Ink3, style = MaterialTheme.typography.labelMedium)
                Text("Library", style = MaterialTheme.typography.headlineLarge)
            }
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(LibCategory.entries) { c ->
                    CategoryPill(label = c.label, selected = category == c, onClick = { category = c })
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LibFilter.entries) { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                }
            }
            if (genres.isNotEmpty()) {
                LazyRow(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(genres) { g ->
                        FilterChip(
                            selected = selectedGenre == g,
                            onClick = { selectedGenre = if (selectedGenre == g) null else g },
                            label = { Text(g) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(collections, key = { it.id }) { shelf ->
                    FilterChip(
                        selected = selectedShelf?.id == shelf.id,
                        onClick = { selectedShelf = if (selectedShelf?.id == shelf.id) null else shelf },
                        label = { Text(shelf.name) },
                    )
                }
                item {
                    IconButton(onClick = { showNewShelfDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "New shelf")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No books here yet", color = Ink3)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp),
                ) {
                    items(filtered, key = { it.id }) { b ->
                        LibraryGridItem(
                            b,
                            onClick = { openBook(b.id) },
                            onLongClick = { bookToDelete = b },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Ink else Paper3, label = "categoryPillBg")
    val fg by animateColorAsState(if (selected) Paper else Ink2, label = "categoryPillFg")
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGridItem(book: Book, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column {
        Box {
            Cover(
                book,
                width = 154.dp,
                modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
            )
            if (book.status == ReadStatus.FINISHED) {
                Box(Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = com.folio.reader.ui.theme.Blue, modifier = Modifier.padding(4.dp))
                }
            }
        }
        Spacer(Modifier.height(11.dp))
        Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        Text(book.author, style = MaterialTheme.typography.bodySmall, color = Ink3)
        if (book.status == ReadStatus.READING) {
            Text(
                "${(book.overallProgress() * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = com.folio.reader.ui.theme.Blue,
            )
        }
    }
}

@Composable
private fun NewShelfDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New shelf") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name); onDismiss() }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** CBZ/CBR have no universally registered MIME type, so cast a wide net and
 *  disambiguate by file extension once a file is actually picked. */
private val IMPORTABLE_MIME_TYPES = arrayOf(
    "application/epub+zip",
    "application/zip",
    "application/vnd.comicbook+zip",
    "application/vnd.comicbook-rar",
    "application/x-rar-compressed",
    "application/octet-stream",
)

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
