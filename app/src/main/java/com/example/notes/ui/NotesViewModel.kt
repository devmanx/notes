package com.example.notes.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notes.backup.EncryptedBackupManager
import com.example.notes.data.Note
import com.example.notes.data.NotesDirectoryProvider
import com.example.notes.data.NotesRepository
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesViewModel(
    private val repository: NotesRepository,
    private val backupManager: EncryptedBackupManager,
    private val notesDirectoryProvider: NotesDirectoryProvider
) : ViewModel() {

    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state.asStateFlow()

    fun loadNotes() {
        viewModelScope.launch {
            val notes = repository.listNotes()
            val labels = repository.availableLabels()
            _state.value = _state.value.copy(
                notes = notes,
                labels = labels,
                notesDirectoryPath = notesDirectoryProvider.notesDirectory().absolutePath
            )
        }
    }

    fun startCreateNote() {
        _state.value = _state.value.copy(
            selectedNote = null,
            isEditing = true,
            isViewing = false
        )
    }

    fun openNote(note: Note) {
        _state.value = _state.value.copy(
            selectedNote = note,
            isEditing = false,
            isViewing = true
        )
    }

    fun closeEditor() {
        _state.value = _state.value.copy(isEditing = false, isViewing = false)
    }

    fun closeViewer() {
        _state.value = _state.value.copy(isViewing = false)
    }

    fun startEditNote() {
        if (_state.value.selectedNote == null) return
        _state.value = _state.value.copy(isEditing = true, isViewing = false)
    }

    fun selectLabel(label: String?) {
        _state.value = _state.value.copy(selectedLabel = label)
    }

    fun saveNote(title: String, content: String, labels: List<String>) {
        viewModelScope.launch {
            val existing = _state.value.selectedNote
            val saved = repository.saveNote(
                Note(
                    id = existing?.id.orEmpty(),
                    title = title,
                    content = content,
                    labels = labels,
                    lastModifiedEpochMs = System.currentTimeMillis()
                )
            )
            loadNotes()
            _state.value = _state.value.copy(selectedNote = saved, isEditing = false, isViewing = true)
        }
    }

    fun deleteSelectedNote() {
        val id = _state.value.selectedNote?.id ?: return
        viewModelScope.launch {
            repository.deleteNote(id)
            loadNotes()
            _state.value = _state.value.copy(selectedNote = null, isEditing = false, isViewing = false)
        }
    }

    fun createBackup(password: CharArray, outputDir: File) {
        viewModelScope.launch {
            backupManager.createAndUploadBackup(password, outputDir)
        }
    }

    fun exportNotesToZip() {
        viewModelScope.launch {
            val outputDir = notesDirectoryProvider.notesDirectory()
            backupManager.createZipBackup(outputDir)
        }
    }

    fun exportNotesToGoogleDrive(password: CharArray, outputDir: File) {
        viewModelScope.launch {
            backupManager.createAndUploadBackup(password, outputDir)
        }
    }

    fun importNotesFromZip(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val notesDir = notesDirectoryProvider.notesDirectory()
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipStream ->
                        var entry = zipStream.nextEntry
                        while (entry != null) {
                            val sanitizedName = File(entry.name).name
                            if (!entry.isDirectory && (sanitizedName.endsWith(".txt") || sanitizedName == "notes_index.json")) {
                                val outFile = File(notesDir, sanitizedName)
                                FileOutputStream(outFile).use { output ->
                                    zipStream.copyTo(output)
                                }
                            }
                            zipStream.closeEntry()
                            entry = zipStream.nextEntry
                        }
                    }
                }
            }
            loadNotes()
        }
    }

    fun updateDriveAuthorization(authorized: Boolean) {
        _state.value = _state.value.copy(isDriveAuthorized = authorized)
    }

    fun updateNotesDirectory(path: String) {
        notesDirectoryProvider.setNotesDirectory(path)
        loadNotes()
    }
}

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val labels: List<String> = emptyList(),
    val selectedNote: Note? = null,
    val selectedLabel: String? = null,
    val isEditing: Boolean = false,
    val isViewing: Boolean = false,
    val notesDirectoryPath: String = "",
    val isDriveAuthorized: Boolean = false
)
