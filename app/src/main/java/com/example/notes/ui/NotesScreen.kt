package com.example.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notes.data.Note

@Composable
fun NotesScreen(
    state: NotesUiState,
    onSelectNote: (Note?) -> Unit,
    onSaveNote: (String, String, List<String>) -> Unit,
    onDeleteNote: () -> Unit
) {
    var title by remember(state.selectedNote?.id) { mutableStateOf(state.selectedNote?.title.orEmpty()) }
    var content by remember(state.selectedNote?.id) { mutableStateOf(state.selectedNote?.content.orEmpty()) }
    var labels by remember(state.selectedNote?.id) { mutableStateOf(state.selectedNote?.labels?.joinToString(", ").orEmpty()) }

    LaunchedEffect(state.selectedNote) {
        title = state.selectedNote?.title.orEmpty()
        content = state.selectedNote?.content.orEmpty()
        labels = state.selectedNote?.labels?.joinToString(", ").orEmpty()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxHeight().width(220.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Twoje notatki",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(onClick = { onSelectNote(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Nowa notatka")
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.notes) { note ->
                        Card(onClick = { onSelectNote(note) }) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(note.title.ifBlank { "(bez tytułu)" })
                                Text(
                                    text = note.labels.joinToString(prefix = "#"),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = note.content,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.selectedNote?.let { "Edytuj notatkę" } ?: "Nowa notatka",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tytuł") }
                )
                OutlinedTextField(
                    value = labels,
                    onValueChange = { labels = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Labele (np. praca, dom)") }
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    label = { Text("Treść") }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        val parsedLabels = labels.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        onSaveNote(title, content, parsedLabels)
                    }) {
                        Text("Zapisz")
                    }
                    if (state.selectedNote != null) {
                        Button(onClick = onDeleteNote) {
                            Text("Usuń")
                        }
                    }
                }
            }
        }
    }
}
