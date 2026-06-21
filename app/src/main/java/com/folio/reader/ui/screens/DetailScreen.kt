package com.folio.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.folio.reader.data.ReadStatus
import com.folio.reader.data.overallProgress
import com.folio.reader.ui.LibraryViewModel
import com.folio.reader.ui.components.ProgressRing
import com.folio.reader.ui.components.Stars
import com.folio.reader.ui.components.Cover
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.theme.Blue
import com.folio.reader.ui.theme.Ink2
import com.folio.reader.ui.theme.Ink3
import com.folio.reader.ui.theme.Paper3

@Composable
fun DetailScreen(bookId: String, back: () -> Unit, openReader: () -> Unit, openCoverPicker: () -> Unit = {}) {
    val vm: LibraryViewModel = folioViewModel()
    val books by vm.books.collectAsState()
    val book = books.firstOrNull { it.id == bookId } ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Book not found") }
        return
    }

    LaunchedEffect(book.id) { vm.fetchSynopsis(book) }

    var showShelfDialog by remember { mutableStateOf(false) }
    if (showShelfDialog) {
        ShelfPickerDialog(bookId = book.id, onDismiss = { showShelfDialog = false })
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().padding(12.dp, 40.dp, 12.dp, 0.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = back) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
            Column(
                Modifier.fillMaxWidth().padding(24.dp, 12.dp, 24.dp, 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    Cover(book, width = 172.dp)
                    IconButton(
                        onClick = openCoverPicker,
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50)),
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = "Change cover", tint = Blue)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text(book.title, style = MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(5.dp))
                Text(book.author, color = Ink2)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Meta("${book.chapterCount} chapters")
                }
            }
        }

        item {
            ShelvesRow(bookId = book.id, onManage = { showShelfDialog = true })
        }

        item {
            Column(Modifier.padding(24.dp, 4.dp, 24.dp, 0.dp)) {
                Text("SYNOPSIS", color = Ink3, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                when {
                    book.synopsis != null -> Text(book.synopsis!!, style = MaterialTheme.typography.bodyMedium)
                    book.synopsisFetchFailed -> Text("No synopsis found online.", color = Ink3, style = MaterialTheme.typography.bodyMedium)
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Fetching synopsis…", color = Ink3, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Card(Modifier.padding(20.dp, 4.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    when (book.status) {
                        ReadStatus.READING -> {
                            val pct = book.overallProgress()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium)
                                Text("ch. ${book.currentChapter + 1} of ${book.chapterCount}", color = Ink3, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(12.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { pct }, modifier = Modifier.fillMaxWidth(), trackColor = Paper3, color = Blue,
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = openReader, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp)); Text("Continue reading")
                            }
                        }
                        ReadStatus.FINISHED -> {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Finished", style = MaterialTheme.typography.titleMedium)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Stars(book.rating)
                                }
                                ProgressRing(pct = 1f, size = 48.dp, strokeWidth = 4.5.dp)
                            }
                            Spacer(Modifier.height(14.dp))
                            OutlinedButton(onClick = openReader, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp)); Text("Read again")
                            }
                        }
                        ReadStatus.WANT -> {
                            Button(onClick = openReader, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp)); Text("Start reading")
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = { vm.setStatus(book, ReadStatus.READING) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Mark as reading")
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(24.dp, 18.dp, 24.dp, 6.dp)) {
                Text("DETAILS", color = Ink3, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column {
                        DRow("Author", book.author)
                        DRow("Chapters", "${book.chapterCount}")
                        DRow("Status", book.status.name.lowercase().replaceFirstChar { it.uppercase() }, last = true)
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Meta(text: String) {
    Box(
        Modifier
            .background(Paper3, RoundedCornerShape(9.dp))
            .padding(11.dp, 5.dp),
    ) { Text(text, color = Ink2, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun DRow(k: String, v: String, last: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp, 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(k, color = Ink3)
        Text(v, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}

@Composable
private fun ShelvesRow(bookId: String, onManage: () -> Unit) {
    val vm: LibraryViewModel = folioViewModel()
    val collections by vm.collections.collectAsState()
    val myCollectionIds by vm.collectionIdsForBook(bookId).collectAsState(initial = emptyList())
    val myShelves = collections.filter { it.id in myCollectionIds }

    Column(Modifier.padding(24.dp, 4.dp, 24.dp, 0.dp)) {
        Text("SHELVES", color = Ink3, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            myShelves.forEach { shelf ->
                Box(Modifier.background(Paper3, RoundedCornerShape(9.dp)).padding(11.dp, 5.dp)) {
                    Text(shelf.name, color = Ink2, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onManage, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Add to shelf", tint = Blue)
            }
        }
    }
}

@Composable
private fun ShelfPickerDialog(bookId: String, onDismiss: () -> Unit) {
    val vm: LibraryViewModel = folioViewModel()
    val collections by vm.collections.collectAsState()
    val myCollectionIds by vm.collectionIdsForBook(bookId).collectAsState(initial = emptyList())
    var newShelfName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shelves") },
        text = {
            Column {
                collections.forEach { shelf ->
                    val inShelf = shelf.id in myCollectionIds
                    FilterChip(
                        selected = inShelf,
                        onClick = { vm.toggleBookInCollection(bookId, shelf.id, inShelf) },
                        label = { Text(shelf.name) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newShelfName,
                        onValueChange = { newShelfName = it },
                        label = { Text("New shelf") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        vm.createCollection(newShelfName)
                        newShelfName = ""
                    }) { Icon(Icons.Filled.Add, contentDescription = "Create shelf") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
