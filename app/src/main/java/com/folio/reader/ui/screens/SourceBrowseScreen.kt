package com.folio.reader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.folio.reader.source.MediaSource
import com.folio.reader.source.SourceMediaInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceBrowseScreen(
    source: MediaSource,
    back: () -> Unit,
    onMediaClick: (SourceMediaInfo) -> Unit
) {
    var items by remember { mutableStateOf<List<SourceMediaInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(source.id) {
        try {
            items = source.fetchLatestUpdates()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(source.name, style = MaterialTheme.typography.titleMedium)
                        Text("Manga • Latest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (items.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No content found. The site might be under protection or the layout has changed.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { 
                        loading = true
                        // Trigger reload via LaunchedEffect by slightly changing a key if needed, 
                        // or just reset items to trigger the logic.
                        items = emptyList() 
                    }) {
                        Text("Retry / Bypass Check")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items) { media ->
                        MediaCard(media = media, onClick = { onMediaClick(media) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaCard(media: SourceMediaInfo, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Card(modifier = Modifier.aspectRatio(0.7f)) {
            AsyncImage(
                model = media.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = media.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
