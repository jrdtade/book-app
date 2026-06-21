package com.folio.reader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.TwoStatePreference
import com.folio.reader.extension.TachiyomiSourceAdapter

/** Settings screen for a `ConfigurableSource` extension — renders its real
 *  `androidx.preference` tree (built by the extension's own `setupPreferenceScreen`)
 *  as Compose controls, persisting to the same SharedPreferences file the
 *  extension itself reads from at request time. */
@Composable
fun ExtensionSettingsDialog(adapter: TachiyomiSourceAdapter, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val screen = remember(adapter) { adapter.buildPreferenceScreen(context) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("${adapter.name} settings", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))

                if (screen == null || screen.preferenceCount == 0) {
                    Text(
                        "This source has no settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        renderPreferenceGroup(screen)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

private fun LazyListScope.renderPreferenceGroup(group: PreferenceGroup) {
    val children = (0 until group.preferenceCount).map { group.getPreference(it) }
    items(children) { preference ->
        if (preference is PreferenceGroup) {
            Text(
                text = preference.title?.toString() ?: "",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            Column {
                for (i in 0 until preference.preferenceCount) {
                    PreferenceRow(preference.getPreference(i))
                }
            }
        } else {
            PreferenceRow(preference)
        }
    }
}

@Composable
private fun PreferenceRow(preference: Preference) {
    var version by remember { mutableIntStateOf(0) }
    val title = preference.title?.toString() ?: preference.key ?: ""

    when (preference) {
        is EditTextPreference -> {
            var text by remember(version) { mutableStateOf(preference.text ?: "") }
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    preference.text = it
                },
                label = { Text(title) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }

        is MultiSelectListPreference -> {
            val entries = preference.entries?.map { it.toString() } ?: emptyList()
            val entryValues = preference.entryValues?.map { it.toString() } ?: emptyList()
            var selected by remember(version) { mutableStateOf(preference.values ?: emptySet()) }
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                entries.forEachIndexed { index, entry ->
                    val value = entryValues.getOrNull(index) ?: return@forEachIndexed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = value in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + value else selected - value
                                preference.values = selected
                            },
                        )
                        Text(entry)
                    }
                }
            }
        }

        is ListPreference -> {
            var expanded by remember { mutableStateOf(false) }
            val entries = preference.entries?.map { it.toString() } ?: emptyList()
            val entryValues = preference.entryValues?.map { it.toString() } ?: emptyList()
            val currentIndex = preference.value
                ?.let { entryValues.indexOf(it) }
                ?.takeIf { it >= 0 }
                ?: 0
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { expanded = true }) {
                        Text(entries.getOrElse(currentIndex) { "Select" })
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        entries.forEachIndexed { index, entry ->
                            DropdownMenuItem(
                                text = { Text(entry) },
                                onClick = {
                                    preference.value = entryValues.getOrNull(index)
                                    expanded = false
                                    version++
                                },
                            )
                        }
                    }
                }
            }
        }

        is TwoStatePreference -> {
            var checked by remember(version) {
                mutableStateOf(preference.sharedPreferences?.getBoolean(preference.key, false) ?: false)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        preference.sharedPreferences?.edit()?.putBoolean(preference.key, it)?.apply()
                    },
                )
            }
        }

        else -> {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
