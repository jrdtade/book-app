package com.folio.reader.ui.screens

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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.folio.reader.ui.StatsViewModel
import com.folio.reader.ui.components.ProgressRing
import com.folio.reader.ui.folioViewModel
import com.folio.reader.ui.theme.Blue
import com.folio.reader.ui.theme.Ink3
import com.folio.reader.ui.theme.Paper3
import com.folio.reader.ui.theme.Paper4
import kotlin.math.max

@Composable
fun StatsScreen() {
    val vm: StatsViewModel = folioViewModel()
    val s by vm.state.collectAsState()
    val heatColors = listOf(Paper4, Color(0xFFC0D7F7), Color(0xFF8FB7EE), Color(0xFF5A91E2), Color(0xFF0068E4))

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(20.dp, 28.dp, 20.dp, 12.dp)) {
                Text("YOUR YEAR IN BOOKS", color = Ink3, style = MaterialTheme.typography.labelMedium)
                Text("Reading stats", style = MaterialTheme.typography.headlineLarge)
            }
        }

        item {
            Card(Modifier.padding(20.dp, 4.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        ProgressRing(pct = s.booksRead.toFloat() / max(1, s.goal), size = 110.dp, strokeWidth = 10.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${s.booksRead}", style = MaterialTheme.typography.headlineLarge)
                            Text("of ${s.goal}", color = Ink3, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("Reading goal", color = Ink3, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        Text("${max(0, s.goal - s.booksRead)} books to go", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item {
            Card(Modifier.padding(20.dp, 8.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFD18A33))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("${s.currentStreak} days", style = MaterialTheme.typography.titleLarge)
                                Text("Current streak", color = Ink3, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${s.longestStreak}", style = MaterialTheme.typography.titleMedium)
                            Text("longest", color = Ink3, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth().height(70.dp)) {
                        s.heat.forEach { v ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(1.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(heatColors[v])
                                    .height(70.dp),
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.padding(20.dp, 8.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Time read · this week", color = Ink3, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("${"%.1f".format(s.hoursThisWeek)}h", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    val maxM = max(1, s.weekMinutes.max())
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.weekMinutes.forEachIndexed { i, m ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height((m.toFloat() / maxM * 70).dp.coerceAtLeast(3.dp))
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Blue),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(days[i], color = Ink3, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.padding(20.dp, 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Totem("${s.pagesYear}", "Pages read", Modifier.weight(1f))
                Totem("${"%.0f".format(s.hoursYear)}h", "Hours read", Modifier.weight(1f))
            }
        }

        if (s.topAuthors.isNotEmpty()) {
            item {
                Card(Modifier.padding(20.dp, 8.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column {
                        Text("Most read authors", color = Ink3, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 4.dp))
                        s.topAuthors.forEachIndexed { i, (name, count) ->
                            Row(
                                Modifier.fillMaxWidth().padding(18.dp, 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("${i + 1}.  $name", style = MaterialTheme.typography.titleMedium)
                                Text("$count books", color = Ink3, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        if (s.highlights.isNotEmpty()) {
            item {
                Text(
                    "Highlights & notes · ${s.highlights.size}",
                    color = Ink3,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(22.dp, 16.dp, 20.dp, 8.dp),
                )
            }
            items(s.highlights) { h ->
                Card(Modifier.padding(20.dp, 4.dp).fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("“${h.text}”", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(10.dp))
                        Text("${h.bookTitle} · ${h.author}", color = Ink3, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Totem(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
