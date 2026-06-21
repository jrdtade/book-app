package com.folio.reader.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import com.folio.reader.data.Book
import com.folio.reader.data.ReadStatus
import com.folio.reader.data.overallProgress
import com.folio.reader.ui.components.Cover
import com.folio.reader.ui.components.ProgressRing
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.theme.Blue
import com.folio.reader.ui.theme.Ink2
import com.folio.reader.ui.theme.Ink3
import com.folio.reader.ui.theme.Paper3
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    openBook: (String) -> Unit,
    openReader: (String) -> Unit,
    goToTab: (String) -> Unit,
) {
    val vm: com.folio.reader.ui.LibraryViewModel = folioViewModel()
    val books by vm.books.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importEpub(it) { goToTab("library") } }
    }

    val reading = books.filter { it.status == ReadStatus.READING }
    val finished = books.filter { it.status == ReadStatus.FINISHED }.take(6)
    val today = androidx.compose.runtime.remember { SimpleDateFormat("EEE d MMM", Locale.US).format(Date()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Column(Modifier.padding(20.dp, 28.dp, 20.dp, 6.dp)) {
                Text(
                    "FOLIO · ${today.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink3,
                )
                Spacer(Modifier.height(6.dp))
                Text("Good day,\nReader.", style = MaterialTheme.typography.headlineLarge)
            }
        }

        if (reading.isEmpty()) {
            item { EmptyLibraryCard(onImport = { launcher.launch(arrayOf("application/epub+zip")) }) }
        } else {
            val hero = reading.first()
            item {
                Card(
                    modifier = Modifier.padding(20.dp, 6.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("CONTINUE READING", style = MaterialTheme.typography.labelMedium, color = Ink3)
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Cover(hero, width = 96.dp, onClick = { openBook(hero.id) })
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(hero.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                                Spacer(Modifier.height(3.dp))
                                Text(hero.author, color = Ink2, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(14.dp))
                                val pct = hero.overallProgress()
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Ink3)
                                    Text("ch. ${hero.currentChapter + 1} / ${hero.chapterCount}", style = MaterialTheme.typography.bodySmall, color = Ink3)
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                    trackColor = Paper3,
                                    color = Blue,
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { openReader(hero.id) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Continue reading")
                        }
                    }
                }
            }

            if (reading.size > 1) {
                item { SectionTitle("Also reading") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(reading.drop(1)) { b ->
                            Column(Modifier.width(104.dp)) {
                                Box {
                                    Cover(b, width = 104.dp, onClick = { openBook(b.id) })
                                    Box(Modifier.align(Alignment.BottomEnd).padding(6.dp)) {
                                        ProgressRing(pct = b.overallProgress(), size = 30.dp, strokeWidth = 3.5.dp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(b.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle("This week", action = "Stats") { goToTab("stats") } }
        item {
            Card(Modifier.padding(20.dp, 0.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(8.dp)) {
                    MiniStat(Icons.Filled.MenuBook, "${books.size}", "Books")
                    MiniStat(Icons.Filled.UploadFile, "${reading.size}", "In progress")
                    MiniStat(Icons.Filled.MenuBook, "${finished.size}", "Finished")
                }
            }
        }

        if (finished.isNotEmpty()) {
            item { SectionTitle("Finished recently", action = "Library") { goToTab("library") } }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(finished) { b -> Cover(b, width = 96.dp, onClick = { openBook(b.id) }) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(20.dp, 20.dp, 20.dp, 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (action != null) {
            Text(action, color = Blue, fontWeight = FontWeight.SemiBold, modifier = Modifier, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RowScope.MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(
        Modifier.weight(1f).padding(14.dp, 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = Ink2, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Ink3)
    }
}

@Composable
private fun EmptyLibraryCard(onImport: () -> Unit) {
    Card(
        Modifier.padding(20.dp, 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Ink3, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("Your library is empty", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Import an EPUB to start reading.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink3,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onImport) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import EPUB")
            }
        }
    }
}
