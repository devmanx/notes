package com.example.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notes.data.Note

@Composable
fun NotesScreen(
    state: NotesUiState,
    onStartCreate: () -> Unit,
    onOpenNote: (Note) -> Unit,
    onSaveNote: (String, String, List<String>) -> Unit,
    onDeleteNote: () -> Unit,
    onSelectLabel: (String?) -> Unit,
    onCloseEditor: () -> Unit,
    onUpdateNotesDirectory: (String) -> Unit
) {
    var isSettingsOpen by remember { mutableStateOf(false) }
    var directoryInput by remember(state.notesDirectoryPath) {
        mutableStateOf(state.notesDirectoryPath)
    }

    LaunchedEffect(state.notesDirectoryPath) {
        if (!isSettingsOpen) {
            directoryInput = state.notesDirectoryPath
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (state.isEditing) {
            NoteEditorScreen(
                note = state.selectedNote,
                onSaveNote = onSaveNote,
                onDeleteNote = onDeleteNote,
                onCloseEditor = onCloseEditor
            )
        } else {
            NotesListScreen(
                notes = state.notes,
                labels = state.labels,
                selectedLabel = state.selectedLabel,
                notesDirectoryPath = state.notesDirectoryPath,
                onStartCreate = onStartCreate,
                onOpenNote = onOpenNote,
                onSelectLabel = onSelectLabel,
                onOpenSettings = { isSettingsOpen = true }
            )
        }

        if (isSettingsOpen) {
            AlertDialog(
                onDismissRequest = { isSettingsOpen = false },
                title = { Text("Ustawienia katalogu") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Wskaż katalog na notatki:")
                        OutlinedTextField(
                            value = directoryInput,
                            onValueChange = { directoryInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Ścieżka do katalogu") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onUpdateNotesDirectory(directoryInput.trim())
                        isSettingsOpen = false
                    }) {
                        Text("Zapisz")
                    }
                },
                dismissButton = {
                    Button(onClick = { isSettingsOpen = false }) {
                        Text("Anuluj")
                    }
                }
            )
        }
    }
}

@Composable
private fun NotesListScreen(
    notes: List<Note>,
    labels: List<String>,
    selectedLabel: String?,
    notesDirectoryPath: String,
    onStartCreate: () -> Unit,
    onOpenNote: (Note) -> Unit,
    onSelectLabel: (String?) -> Unit,
    onOpenSettings: () -> Unit
) {
    val filteredNotes = selectedLabel?.let { label ->
        notes.filter { it.labels.contains(label) }
    } ?: notes

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Twoje notatki",
                    style = MaterialTheme.typography.titleMedium
                )
                if (notesDirectoryPath.isNotBlank()) {
                    Text(
                        text = notesDirectoryPath,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Ustawienia katalogu"
                )
            }
        }
        Button(onClick = onStartCreate) {
            Text("Nowa notatka")
        }
        LabelsRow(
            labels = labels,
            selectedLabel = selectedLabel,
            onSelectLabel = onSelectLabel
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredNotes) { note ->
                Card(onClick = { onOpenNote(note) }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(note.title.ifBlank { "(bez tytułu)" })
                        if (note.labels.isNotEmpty()) {
                            Text(
                                text = note.labels.joinToString(prefix = "#"),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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
}

@Composable
private fun LabelsRow(
    labels: List<String>,
    selectedLabel: String?,
    onSelectLabel: (String?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Button(onClick = { onSelectLabel(null) }) {
                Text(if (selectedLabel == null) "Wszystkie" else "Wszystkie")
            }
        }
        items(labels) { label ->
            Button(onClick = { onSelectLabel(label) }) {
                Text(label)
            }
        }
    }
}

@Composable
private fun NoteEditorScreen(
    note: Note?,
    onSaveNote: (String, String, List<String>) -> Unit,
    onDeleteNote: () -> Unit,
    onCloseEditor: () -> Unit
) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var labels by remember(note?.id) { mutableStateOf(note?.labels?.joinToString(", ").orEmpty()) }

    LaunchedEffect(note?.id) {
        title = note?.title.orEmpty()
        content = note?.content.orEmpty()
        labels = note?.labels?.joinToString(", ").orEmpty()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = note?.let { "Edytuj notatkę" } ?: "Nowa notatka",
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
            label = { Text("Labele (opcjonalnie)") }
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
            if (note != null) {
                Button(onClick = onDeleteNote) {
                    Text("Usuń")
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onCloseEditor) {
                Text("Wróć")
            }
        }
    }
}
