package com.folio.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.folio.reader.data.Book
import com.folio.reader.network.CoverCandidate
import com.folio.reader.ui.LibraryViewModel
import com.folio.reader.ui.components.NetworkImage
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.theme.Ink3

/** Lets the reader pick an alternate cover for a book from Google Books' cover art,
 *  the same source an image search for "<title> <author> book cover" would surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPickerScreen(book: Book, back: () -> Unit) {
    val vm: LibraryViewModel = folioViewModel()
    var query by remember { mutableStateOf("${book.title} ${book.author}") }
    val results by vm.coverResults.collectAsState()
    val loading by vm.coverSearchInProgress.collectAsState()

    LaunchedEffect(Unit) {
        vm.clearCoverResults()
        vm.searchCovers(query)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose a cover") },
                navigationIcon = {
                    IconButton(onClick = back) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(16.dp, 8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { vm.searchCovers(query) }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                )
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No covers found. Try a different search.", color = Ink3)
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(results, key = { it.volumeId }) { candidate ->
                        CoverCandidateTile(candidate) {
                            vm.applyCover(book, candidate.thumbnailUrl) { back() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverCandidateTile(candidate: CoverCandidate, onClick: () -> Unit) {
    Column {
        NetworkImage(
            url = candidate.thumbnailUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
        )
    }
}
