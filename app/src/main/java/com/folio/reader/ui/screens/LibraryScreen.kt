package com.folio.reader.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.folio.reader.data.Book
import com.folio.reader.data.Shelf
import com.folio.reader.data.ReadStatus
import com.folio.reader.data.overallProgress
import com.folio.reader.ui.components.Cover
import com.folio.reader.ui.components.ProgressRing
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.theme.Ink3

private enum class LibFilter(val label: String) { ALL("All"), READING("Reading"), FINISHED("Finished"), WANT("Want to read") }

@Composable
fun LibraryScreen(openBook: (String) -> Unit) {
    val vm: com.folio.reader.ui.LibraryViewModel = folioViewModel()
    val books by vm.books.collectAsState()
    val collections by vm.collections.collectAsState()
    var filter by remember { mutableStateOf(LibFilter.ALL) }
    var selectedShelf by remember { mutableStateOf<Shelf?>(null) }
    var showNewShelfDialog by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importEpub(it) }
    }

    val shelfBookIds by (selectedShelf?.let { vm.bookIdsInCollection(it.id) } ?: kotlinx.coroutines.flow.flowOf(null))
        .collectAsState(initial = null)

    if (showNewShelfDialog) {
        NewShelfDialog(onDismiss = { showNewShelfDialog = false }, onCreate = { vm.createCollection(it) })
    }

    val statusFiltered = when (filter) {
        LibFilter.ALL -> books
        LibFilter.READING -> books.filter { it.status == ReadStatus.READING }
        LibFilter.FINISHED -> books.filter { it.status == ReadStatus.FINISHED }
        LibFilter.WANT -> books.filter { it.status == ReadStatus.WANT }
    }
    val filtered = shelfBookIds?.let { ids -> statusFiltered.filter { it.id in ids } } ?: statusFiltered

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { launcher.launch(arrayOf("application/epub+zip")) }) {
                Icon(Icons.Filled.Add, contentDescription = "Import EPUB")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(20.dp, 28.dp, 20.dp, 12.dp)) {
                Text("${books.size} BOOKS", color = Ink3, style = MaterialTheme.typography.labelMedium)
                Text("Library", style = MaterialTheme.typography.headlineLarge)
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibFilter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                }
            }
            Spacer(Modifier.height(8.dp))
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
                    items(filtered, key = { it.id }) { b -> LibraryGridItem(b, onClick = { openBook(b.id) }) }
                }
            }
        }
    }
}

@Composable
private fun LibraryGridItem(book: Book, onClick: () -> Unit) {
    Column {
        Box {
            Cover(book, width = 154.dp, onClick = onClick)
            when (book.status) {
                ReadStatus.READING -> Box(Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    ProgressRing(pct = book.overallProgress(), size = 32.dp, strokeWidth = 4.dp)
                }
                ReadStatus.FINISHED -> Box(Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = com.folio.reader.ui.theme.Blue, modifier = Modifier.padding(4.dp))
                }
                else -> Unit
            }
        }
        Spacer(Modifier.height(11.dp))
        Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        Text(book.author, style = MaterialTheme.typography.bodySmall, color = Ink3)
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
