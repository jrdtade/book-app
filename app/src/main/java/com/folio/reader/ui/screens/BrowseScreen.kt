package com.folio.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
    val tabs = listOf("Sources", "Extensions")

    Column(modifier = modifier.fillMaxSize().padding(top = 16.dp)) {
        PillTabRow(
            selectedTabIndex = selectedTab,
            tabs = tabs,
            onTabSelected = { selectedTab = it }
        )

        Spacer(Modifier.height(16.dp))

        when (selectedTab) {
            0 -> SourcesTab(sources, onSourceClick)
            1 -> ExtensionsTab(extensionManager)
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

/** Every extension found in the local extensions directory, enabled or not. */
@Composable
private fun ExtensionsTab(extensionManager: ExtensionManager) {
    val extensions by extensionManager.extensions.collectAsState()
    val scope = rememberCoroutineScope()

    if (extensions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No extensions found", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(extensions, key = { it.pkgName }) { extension ->
            ExtensionRow(
                extension = extension,
                onToggle = { enabled ->
                    scope.launch { extensionManager.setEnabled(extension.pkgName, enabled) }
                },
                onDelete = { extensionManager.deleteExtension(extension.pkgName) }
            )
        }
    }
}

@Composable
private fun ExtensionRow(
    extension: Extension,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(extension.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("v${extension.version} • ${extension.lang}") },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = extension.isEnabled, onCheckedChange = onToggle)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove extension")
                }
            }
        }
    )
}
