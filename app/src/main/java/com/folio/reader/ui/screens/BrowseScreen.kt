package com.folio.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.folio.reader.extension.Extension
import com.folio.reader.extension.ExtensionManager
import com.folio.reader.extension.ExtensionRepo
import com.folio.reader.source.MediaSource
import kotlinx.coroutines.launch

@Composable
fun BrowseScreen(
    extensionManager: ExtensionManager,
    sources: List<MediaSource>,
    onSourceClick: (MediaSource) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val tabs = listOf("Sources", "Extensions")

    Column(modifier = modifier.fillMaxSize().padding(top = 16.dp)) {
        PillTabRow(
            selectedTabIndex = selectedTab,
            tabs = tabs,
            onTabSelected = { selectedTab = it }
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search ${tabs[selectedTab].lowercase()}...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            shape = CircleShape,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )

        Spacer(Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                val filteredSources = remember(sources, searchQuery) {
                    sources.filter { it.name.contains(searchQuery, ignoreCase = true) }
                }
                SourcesTab(filteredSources, onSourceClick)
            }
            1 -> ExtensionsTab(extensionManager, searchQuery)
        }
    }
}

@Composable
private fun PillTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedTabIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Enabled sources only — what the rest of the app (library/search) can actually use. */
@Composable
private fun SourcesTab(
    sources: List<MediaSource>,
    onSourceClick: (MediaSource) -> Unit
) {
    if (sources.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sources enabled", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sources, key = { it.id }) { source ->
            ListItem(
                headlineContent = { Text(source.name) },
                supportingContent = { Text(source.mediaType.name) },
                modifier = Modifier.clickable { onSourceClick(source) }
            )
        }
    }
}

/** Every extension known to the app: installed locally, or available from a repo. */
@Composable
private fun ExtensionsTab(extensionManager: ExtensionManager, searchQuery: String) {
    val allExtensions by extensionManager.extensions.collectAsState()
    val scope = rememberCoroutineScope()
    var showRepoDialog by remember { mutableStateOf(false) }

    val extensions = remember(allExtensions, searchQuery) {
        if (searchQuery.isBlank()) allExtensions
        else allExtensions.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showRepoDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Repositories")
            }
        }

        if (extensions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No extensions found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(extensions, key = { it.pkgName }) { extension ->
                    ExtensionRow(
                        extension = extension,
                        onToggle = { enabled ->
                            scope.launch { extensionManager.setEnabled(extension.pkgName, enabled) }
                        },
                        onDelete = { extensionManager.deleteExtension(extension.pkgName) },
                        onInstall = { scope.launch { extensionManager.installExtension(extension) } }
                    )
                }
            }
        }
    }

    if (showRepoDialog) {
        RepositoryDialog(
            extensionManager = extensionManager,
            onDismiss = { showRepoDialog = false }
        )
    }
}

@Composable
private fun ExtensionRow(
    extension: Extension,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onInstall: () -> Unit
) {
    ListItem(
        headlineContent = { Text(extension.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text("v${extension.version} • ${extension.lang}" + if (!extension.isInstalled) " • Available" else "")
                if (extension.loadError != null) {
                    Text(
                        "Failed to load: ${extension.loadError}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        trailingContent = {
            when {
                extension.isInstalling -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                extension.isInstalled -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = extension.isEnabled, onCheckedChange = onToggle)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove extension")
                    }
                }
                else -> Button(onClick = onInstall, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Install")
                }
            }
        }
    )
}

/** Repository management: lists saved repos, lets the user add or remove them. */
@Composable
private fun RepositoryDialog(
    extensionManager: ExtensionManager,
    onDismiss: () -> Unit
) {
    val repos by extensionManager.repos.collectAsState()
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Repositories", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 320.dp)) {
                    items(repos, key = { it.url }) { repo ->
                        RepositoryRow(
                            repo = repo,
                            onDelete = { scope.launch { extensionManager.deleteRepo(repo) } }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Repository")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRepositoryDialog(
            extensionManager = extensionManager,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun RepositoryRow(repo: ExtensionRepo, onDelete: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(repo.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = { if (repo.isDefault) Text("Default") },
        trailingContent = {
            if (repo.isDefault) {
                Icon(Icons.Default.Lock, contentDescription = "Default repository can't be removed")
            } else {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove repository")
                }
            }
        }
    )
}

@Composable
private fun AddRepositoryDialog(
    extensionManager: ExtensionManager,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Repository") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        error = null
                    },
                    label = { Text("Index JSON URL") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    if (extensionManager.addRepo(url)) {
                        onDismiss()
                    } else {
                        error = "Enter a valid http(s) URL"
                    }
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
