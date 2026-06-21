package com.folio.reader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.folio.reader.extension.TachiyomiSourceAdapter
import com.folio.reader.source.MediaSource
import com.folio.reader.source.SourceFilter
import com.folio.reader.source.SourceMediaInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceBrowseScreen(
    source: MediaSource,
    back: () -> Unit,
    onMediaClick: (SourceMediaInfo) -> Unit
) {
    var items by remember { mutableStateOf<List<SourceMediaInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errored by remember { mutableStateOf(false) }
    var query by remember(source.id) { mutableStateOf("") }
    var filters by remember(source.id) { mutableStateOf(source.getFilters()) }
    var showFilters by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runQuery() {
        scope.launch {
            loading = true
            errored = false
            items = try {
                if (query.isBlank()) source.fetchLatestUpdates() else source.search(query, filters)
            } catch (e: Exception) {
                e.printStackTrace()
                errored = true
                emptyList()
            }
            loading = false
        }
    }

    LaunchedEffect(source.id) { runQuery() }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(source.name, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (filters.isNotEmpty()) {
                            IconButton(onClick = { showFilters = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filters")
                            }
                        }
                        if (source is TachiyomiSourceAdapter && source.isConfigurable) {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Source settings")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search ${source.name}...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = ""; runQuery() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runQuery() }),
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                items.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (errored) {
                                "No content found. The site might be under protection or the layout has changed."
                            } else {
                                "No results."
                            },
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { runQuery() }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
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

    if (showFilters) {
        FilterDialog(
            filters = filters,
            onApply = { runQuery() },
            onDismiss = { showFilters = false }
        )
    }

    if (showSettings && source is TachiyomiSourceAdapter) {
        ExtensionSettingsDialog(adapter = source, onDismiss = { showSettings = false })
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

/** Recursively renders a [SourceFilter] tree, mutating the (mutable, by-reference)
 *  filter objects directly and bumping [onChanged] to force recomposition — these
 *  are plain Kotlin objects, not Compose state, by design (the same instances get
 *  written back into the extension's real Tachiyomi `FilterList`). */
@Composable
private fun FilterDialog(
    filters: List<SourceFilter>,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    var version by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Filters", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    renderFilters(filters, version) { version++ }
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onApply(); onDismiss() }) { Text("Apply") }
                }
            }
        }
    }
}

private fun LazyListScope.renderFilters(filters: List<SourceFilter>, version: Int, onChanged: () -> Unit) {
    items(filters) { filter ->
        // SourceFilter fields are plain `var`s (not Compose State) so the same
        // instances can be written back into the extension's real FilterList;
        // keying on `version` forces this node to fully recompose (re-read the
        // current field values) whenever a sibling filter changes.
        key(version) {
            FilterRow(filter, onChanged)
        }
    }
}

@Composable
private fun FilterRow(filter: SourceFilter, onChanged: () -> Unit) {
    when (filter) {
        is SourceFilter.Header -> Text(
            filter.name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )

        is SourceFilter.Separator -> HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        is SourceFilter.Text -> OutlinedTextField(
            value = filter.value,
            onValueChange = { filter.value = it; onChanged() },
            label = { Text(filter.name) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        is SourceFilter.CheckBox -> Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = filter.checked, onCheckedChange = { filter.checked = it; onChanged() })
            Text(filter.name)
        }

        is SourceFilter.TriState -> {
            val label = when (filter.state) {
                1 -> "Include"
                2 -> "Exclude"
                else -> "Ignore"
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(filter.name)
                TextButton(onClick = { filter.state = (filter.state + 1) % 3; onChanged() }) { Text(label) }
            }
        }

        is SourceFilter.Select -> {
            var expanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(filter.name, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { expanded = true }) {
                        Text(filter.options.getOrElse(filter.selected) { "Select" })
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        filter.options.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { filter.selected = index; expanded = false; onChanged() }
                            )
                        }
                    }
                }
            }
        }

        is SourceFilter.MultiSelect -> Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(filter.name, style = MaterialTheme.typography.bodyMedium)
            filter.options.forEachIndexed { index, option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = index in filter.selected,
                        onCheckedChange = { checked ->
                            filter.selected = if (checked) filter.selected + index else filter.selected - index
                            onChanged()
                        }
                    )
                    Text(option)
                }
            }
        }

        is SourceFilter.Group -> Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(filter.name, style = MaterialTheme.typography.titleSmall)
            filter.filters.forEach { FilterRow(it, onChanged) }
        }

        is SourceFilter.Sort -> Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(filter.name, style = MaterialTheme.typography.bodyMedium)
            filter.options.forEachIndexed { index, option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = filter.selectedIndex == index,
                        onClick = { filter.selectedIndex = index; onChanged() }
                    )
                    Text(option, modifier = Modifier.weight(1f))
                    if (filter.selectedIndex == index) {
                        IconButton(onClick = { filter.ascending = !filter.ascending; onChanged() }) {
                            Icon(
                                if (filter.ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "Sort direction"
                            )
                        }
                    }
                }
            }
        }
    }
}
