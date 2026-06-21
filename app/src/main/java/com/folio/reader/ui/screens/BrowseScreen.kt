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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.folio.reader.extension.Extension
import com.folio.reader.extension.ExtensionManager
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
            1 -> {
                ExtensionsTab(extensionManager, searchQuery)
            }
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

@Composable
private fun SourcesTab(
    sources: List<MediaSource>,
    onSourceClick: (MediaSource) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (sources.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sources enabled", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        items(sources, key = { it.id }) { source ->
            ListItem(
                headlineContent = { Text(source.name) },
                supportingContent = { Text(source.mediaType.name) },
                modifier = Modifier.clickable { onSourceClick(source) }
            )
        }
    }
}

@Composable
private fun ExtensionsTab(extensionManager: ExtensionManager, searchQuery: String) {
    val allExtensions by extensionManager.extensions.collectAsState()
    val scope = rememberCoroutineScope()
    var showAddRepoDialog by remember { mutableStateOf(false) }

    val extensions = remember(allExtensions, searchQuery) {
        if (searchQuery.isBlank()) allExtensions
        else allExtensions.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(extensions, key = { it.pkgName }) { extension ->
                ExtensionItem(
                    extension = extension,
                    onToggle = { enabled ->
                        scope.launch { extensionManager.toggleExtension(extension.pkgName, enabled) }
                    },
                    onDelete = {
                        extensionManager.deleteExtension(extension)
                    },
                    onInstall = {
                        scope.launch {
                            extension.remoteInfo?.let { extensionManager.downloadExtension(it) }
                        }
                    }
                )
            }
            item {
                Spacer(Modifier.height(80.dp))
            }
        }

        FloatingActionButton(
            onClick = { showAddRepoDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Repository")
        }
    }

    if (showAddRepoDialog) {
        var repoUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRepoDialog = false },
            title = { Text("Add Extension Repository") },
            text = {
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it },
                    label = { Text("Repository JSON URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (repoUrl.isNotBlank()) {
                        scope.launch {
                            extensionManager.addRepo(repoUrl)
                            showAddRepoDialog = false
                        }
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRepoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExtensionItem(
    extension: Extension,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onInstall: () -> Unit
) {
    ListItem(
        headlineContent = { Text(extension.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { 
            if (extension.isDownloading) {
                LinearProgressIndicator(
                    progress = extension.progress,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            } else {
                Text(
                    if (extension.isInstalled) "v${extension.version} • ${if (extension.isEnabled) "Enabled" else "Disabled"}"
                    else "Available • v${extension.version}"
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (extension.isInstalled) {
                    Switch(
                        checked = extension.isEnabled,
                        onCheckedChange = onToggle
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Uninstall")
                    }
                } else if (!extension.isDownloading) {
                    Button(onClick = onInstall, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Install")
                    }
                }
            }
        }
    )
}
