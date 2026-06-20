package com.folio.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.folio.reader.ui.LibraryViewModel
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.theme.Blue
import com.folio.reader.ui.theme.BlueSoft
import com.folio.reader.ui.theme.Ink3

@Composable
fun SettingsScreen() {
    val vm: LibraryViewModel = folioViewModel()
    val books by vm.books.collectAsState()
    var reminder by remember { mutableStateOf(true) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(20.dp, 28.dp, 20.dp, 16.dp))
        }
        item {
            Card(Modifier.padding(20.dp, 0.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).background(Blue, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Person, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White) }
                    Spacer(Modifier.width(15.dp))
                    Column {
                        Text("Reader", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Ink3, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Offline library · ${books.size} books", color = Ink3, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item {
            Row(
                Modifier.padding(20.dp, 16.dp).background(BlueSoft, RoundedCornerShape(16.dp)).padding(16.dp),
            ) {
                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Blue)
                Spacer(Modifier.width(13.dp))
                Text(
                    "Everything stays on this device. Folio never sends your books or reading data anywhere — no account required.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            SettingsGroup("Reading") {
                SettingsRow("Match system brightness", toggle = true, on = reminder, onToggle = { reminder = it })
            }
        }
        item {
            SettingsGroup("Goals") {
                SettingsRow("Daily reminder", toggle = true, on = reminder, onToggle = { reminder = it }, last = true)
            }
        }
        item {
            SettingsGroup("About") {
                SettingsRow("Version", detail = "Folio 1.0", chevron = false, last = true)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Folio", style = MaterialTheme.typography.titleMedium, color = Ink3)
                Text("READ · TRACK · KEEP", color = Ink3, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(20.dp, 10.dp)) {
        Text(title, color = Ink3, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
        Card(shape = RoundedCornerShape(16.dp)) { Column { content() } }
    }
}

@Composable
private fun SettingsRow(
    name: String,
    detail: String? = null,
    toggle: Boolean = false,
    on: Boolean = false,
    onToggle: (Boolean) -> Unit = {},
    chevron: Boolean = true,
    last: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .let { if (toggle) it.clickable { onToggle(!on) } else it }
            .padding(15.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
        if (toggle) {
            Switch(checked = on, onCheckedChange = onToggle)
        } else if (detail != null) {
            Text(detail, color = Ink3, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
