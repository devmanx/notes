package com.example.notes.ui

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.notes.data.Note

@Composable
fun NotesScreen(
    state: NotesUiState,
    onStartCreate: () -> Unit,
    onOpenNote: (Note) -> Unit,
    onStartEdit: () -> Unit,
    onSaveNote: (String, String, List<String>) -> Unit,
    onDeleteNote: () -> Unit,
    onSelectLabel: (String?) -> Unit,
    onCloseEditor: () -> Unit,
    onCloseViewer: () -> Unit,
    onUpdateNotesDirectory: (String) -> Unit,
    onExportZip: () -> Unit,
    onImportZip: (Uri) -> Unit,
    onImportDirectory: (Uri) -> Unit,
    onExportGoogleDrive: (CharArray) -> Unit,
    onImportGoogleDrive: () -> Unit
) {
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isDirectoryDialogOpen by remember { mutableStateOf(false) }
    var isExportDialogOpen by remember { mutableStateOf(false) }
    var isImportDialogOpen by remember { mutableStateOf(false) }
    var isDriveExportDialogOpen by remember { mutableStateOf(false) }
    var isDriveImportDialogOpen by remember { mutableStateOf(false) }
    var drivePassword by remember { mutableStateOf("") }
    var directoryInput by remember(state.notesDirectoryPath) {
        mutableStateOf(state.notesDirectoryPath)
    }
    val context = LocalContext.current
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            val resolvedPath = resolveDirectoryPath(uri)
            if (resolvedPath != null) {
                directoryInput = resolvedPath
            }
        }
    }
    val zipImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportZip(uri)
        }
    }
    val directoryImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            onImportDirectory(uri)
        }
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
        } else if (state.isViewing) {
            NotePreviewScreen(
                note = state.selectedNote,
                onStartEdit = onStartEdit,
                onDeleteNote = onDeleteNote,
                onCloseViewer = onCloseViewer
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
                title = { Text("Ustawienia") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            isSettingsOpen = false
                            isDirectoryDialogOpen = true
                        }) {
                            Text("Katalog notatek")
                        }
                        Button(onClick = {
                            isSettingsOpen = false
                            isExportDialogOpen = true
                        }) {
                            Text("Eksport notatek")
                        }
                        Button(onClick = {
                            isSettingsOpen = false
                            isImportDialogOpen = true
                        }) {
                            Text("Import notatek")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { isSettingsOpen = false }) {
                        Text("Zamknij")
                    }
                },
                dismissButton = {
                    Button(onClick = { isSettingsOpen = false }) {
                        Text("Anuluj")
                    }
                }
            )
        }

        if (isDirectoryDialogOpen) {
            AlertDialog(
                onDismissRequest = { isDirectoryDialogOpen = false },
                title = { Text("Katalog notatek") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Wskaż katalog na notatki:")
                        OutlinedTextField(
                            value = directoryInput,
                            onValueChange = { directoryInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Ścieżka do katalogu") }
                        )
                        Button(onClick = { directoryPickerLauncher.launch(null) }) {
                            Text("Wybierz katalog")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onUpdateNotesDirectory(directoryInput.trim())
                        isDirectoryDialogOpen = false
                    }) {
                        Text("Zapisz")
                    }
                },
                dismissButton = {
                    Button(onClick = { isDirectoryDialogOpen = false }) {
                        Text("Anuluj")
                    }
                }
            )
        }

        if (isExportDialogOpen) {
            AlertDialog(
                onDismissRequest = { isExportDialogOpen = false },
                title = { Text("Eksport notatek") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            isExportDialogOpen = false
                            isDriveExportDialogOpen = true
                        }) {
                            Text("Dysk Google")
                        }
                        Button(onClick = {
                            onExportZip()
                            isExportDialogOpen = false
                        }) {
                            Text("Plik ZIP")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { isExportDialogOpen = false }) {
                        Text("Zamknij")
                    }
                }
            )
        }

        if (isImportDialogOpen) {
            AlertDialog(
                onDismissRequest = { isImportDialogOpen = false },
                title = { Text("Import notatek") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            isImportDialogOpen = false
                            isDriveImportDialogOpen = true
                        }) {
                            Text("Dysk Google")
                        }
                        Button(onClick = {
                            zipImportLauncher.launch(arrayOf("application/zip"))
                            isImportDialogOpen = false
                        }) {
                            Text("Plik ZIP")
                        }
                        Button(onClick = {
                            directoryImportLauncher.launch(null)
                            isImportDialogOpen = false
                        }) {
                            Text("Katalog TXT")
                        }
                        Text(
                            text = "Pliki .txt zostaną wczytane jako notatki, a nazwa pliku stanie się tytułem."
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { isImportDialogOpen = false }) {
                        Text("Zamknij")
                    }
                }
            )
        }

        if (isDriveExportDialogOpen) {
            AlertDialog(
                onDismissRequest = { isDriveExportDialogOpen = false },
                title = { Text("Eksport na Dysk Google") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Podaj hasło do zabezpieczenia kopii:")
                        OutlinedTextField(
                            value = drivePassword,
                            onValueChange = { drivePassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Hasło") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onExportGoogleDrive(drivePassword.toCharArray())
                        drivePassword = ""
                        isDriveExportDialogOpen = false
                    }) {
                        Text("Eksportuj")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        drivePassword = ""
                        isDriveExportDialogOpen = false
                    }) {
                        Text("Anuluj")
                    }
                }
            )
        }

        if (isDriveImportDialogOpen) {
            AlertDialog(
                onDismissRequest = { isDriveImportDialogOpen = false },
                title = { Text("Import z Dysku Google") },
                text = { Text("Autoryzuj dostęp do Dysku Google, aby importować notatki.") },
                confirmButton = {
                    Button(onClick = {
                        onImportGoogleDrive()
                        isDriveImportDialogOpen = false
                    }) {
                        Text("Autoryzuj")
                    }
                },
                dismissButton = {
                    Button(onClick = { isDriveImportDialogOpen = false }) {
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
                    contentDescription = "Ustawienia"
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
                        Text(
                            text = note.content,
                            maxLines = 1,
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
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
    var isDeleteDialogOpen by remember(note?.id) { mutableStateOf(false) }
    var isCancelDialogOpen by remember(note?.id) { mutableStateOf(false) }
    val edgeThresholdPx = with(LocalDensity.current) { 32.dp.toPx() }
    val swipeTriggerDistancePx = with(LocalDensity.current) { 96.dp.toPx() }
    var dragDistance by remember { mutableStateOf(0f) }
    var startNearEdge by remember { mutableStateOf(false) }

    LaunchedEffect(note?.id) {
        title = note?.title.orEmpty()
        content = note?.content.orEmpty()
        labels = note?.labels?.joinToString(", ").orEmpty()
    }

    BackHandler(enabled = !isCancelDialogOpen) {
        isCancelDialogOpen = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent().changes.firstOrNull() ?: continue
                        startNearEdge = down.position.x >= size.width - edgeThresholdPx
                        dragDistance = 0f
                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            if (startNearEdge) {
                                dragDistance += change.position.x - change.previousPosition.x
                                if (dragDistance <= -swipeTriggerDistancePx) {
                                    isCancelDialogOpen = true
                                    startNearEdge = false
                                    break
                                }
                            }
                        }
                    }
                }
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
        Spacer(modifier = Modifier.height(12.dp))
        NoteActionFooter {
            Button(onClick = {
                val parsedLabels = labels.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                onSaveNote(title, content, parsedLabels)
            }) {
                Text("Zapisz")
            }
            if (note != null) {
                Button(onClick = { isDeleteDialogOpen = true }) {
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

    if (isDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDeleteDialogOpen = false },
            title = { Text("Potwierdź usunięcie") },
            text = { Text("Czy na pewno chcesz usunąć tę notatkę?") },
            confirmButton = {
                Button(onClick = {
                    isDeleteDialogOpen = false
                    onDeleteNote()
                }) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                Button(onClick = { isDeleteDialogOpen = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    if (isCancelDialogOpen) {
        AlertDialog(
            onDismissRequest = { isCancelDialogOpen = false },
            title = { Text("Anulować edycję?") },
            text = { Text("Wszystkie niezapisane zmiany zostaną utracone.") },
            confirmButton = {
                Button(onClick = {
                    isCancelDialogOpen = false
                    onCloseEditor()
                }) {
                    Text("Anuluj")
                }
            },
            dismissButton = {
                Button(onClick = { isCancelDialogOpen = false }) {
                    Text("Pozostań")
                }
            }
        )
    }
}

@Composable
private fun NotePreviewScreen(
    note: Note?,
    onStartEdit: () -> Unit,
    onDeleteNote: () -> Unit,
    onCloseViewer: () -> Unit
) {
    var isDeleteDialogOpen by remember(note?.id) { mutableStateOf(false) }
    val edgeThresholdPx = with(LocalDensity.current) { 32.dp.toPx() }
    val swipeTriggerDistancePx = with(LocalDensity.current) { 96.dp.toPx() }
    var dragDistance by remember { mutableStateOf(0f) }
    var startNearEdge by remember { mutableStateOf(false) }

    BackHandler {
        onCloseViewer()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent().changes.firstOrNull() ?: continue
                        startNearEdge = down.position.x >= size.width - edgeThresholdPx
                        dragDistance = 0f
                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            if (startNearEdge) {
                                dragDistance += change.position.x - change.previousPosition.x
                                if (dragDistance <= -swipeTriggerDistancePx) {
                                    onCloseViewer()
                                    startNearEdge = false
                                    break
                                }
                            }
                        }
                    }
                }
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = note?.title?.ifBlank { "(bez tytułu)" } ?: "(bez tytułu)",
                    style = MaterialTheme.typography.titleMedium
                )
                if (!note?.labels.isNullOrEmpty()) {
                    Text(
                        text = note?.labels?.joinToString(prefix = "#").orEmpty(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                SelectionContainer {
                    Text(
                        text = note?.content.orEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        softWrap = true
                    )
                }
            }
            NoteActionFooter {
                Button(onClick = onStartEdit) {
                    Text("Edytuj")
                }
                if (note != null) {
                    Button(onClick = { isDeleteDialogOpen = true }) {
                        Text("Usuń")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onCloseViewer) {
                    Text("Wróć")
                }
            }
        }
    }

    if (isDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDeleteDialogOpen = false },
            title = { Text("Potwierdź usunięcie") },
            text = { Text("Czy na pewno chcesz usunąć tę notatkę?") },
            confirmButton = {
                Button(onClick = {
                    isDeleteDialogOpen = false
                    onDeleteNote()
                }) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                Button(onClick = { isDeleteDialogOpen = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
private fun NoteActionFooter(content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

private fun resolveDirectoryPath(uri: Uri): String? {
    val docId = DocumentsContract.getTreeDocumentId(uri)
    if (docId.startsWith("primary:")) {
        val relativePath = docId.removePrefix("primary:").trimStart('/')
        val basePath = Environment.getExternalStorageDirectory().absolutePath
        return if (relativePath.isBlank()) {
            basePath
        } else {
            "$basePath/$relativePath"
        }
    }
    return null
}
