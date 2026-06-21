package com.folio.reader.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.folio.reader.network.CoverCandidate
import com.folio.reader.ui.LibraryViewModel
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.theme.Ink3

/** Lets the user search Open Library's cover catalog for an alternate cover and tap one to use it. */
@Composable
fun CoverPickerScreen(bookId: String, back: () -> Unit) {
    val vm: LibraryViewModel = folioViewModel()
    val books by vm.books.collectAsState()
    val book = books.firstOrNull { it.id == bookId } ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Book not found") }
        return
    }
    val results by vm.coverResults.collectAsState()
    val searching by vm.coverSearchInProgress.collectAsState()
    var applying by remember { mutableStateOf(false) }

    LaunchedEffect(book.id) { vm.searchCovers("${book.title} ${book.author}") }

    Box(Modifier.fillMaxSize()) {
        if (results.isEmpty() && !searching) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No alternate covers found", color = Ink3)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp, 88.dp, 16.dp, 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = CoverCandidate::thumbnailUrl) { candidate ->
                    Image(
                        painter = rememberAsyncImagePainter(candidate.thumbnailUrl),
                        contentDescription = candidate.title,
                        contentScale = ContentScale.Cover,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.68f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                applying = true
                                vm.applyCover(book, candidate.thumbnailUrl, onComplete = back)
                            },
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(8.dp, 36.dp, 8.dp, 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = back) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        }

        if (searching || applying) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
