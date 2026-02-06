package com.example.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notes.backup.EncryptedBackupManager
import com.example.notes.data.Note
import com.example.notes.data.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class NotesViewModel(
    private val repository: NotesRepository,
    private val backupManager: EncryptedBackupManager
) : ViewModel() {

    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state.asStateFlow()

    fun loadNotes() {
        viewModelScope.launch {
            val notes = repository.listNotes()
            val labels = repository.availableLabels()
            _state.value = _state.value.copy(notes = notes, labels = labels)
        }
    }

    fun selectNote(note: Note?) {
        _state.value = _state.value.copy(selectedNote = note)
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
            selectNote(saved)
        }
    }

    fun deleteSelectedNote() {
        val id = _state.value.selectedNote?.id ?: return
        viewModelScope.launch {
            repository.deleteNote(id)
            loadNotes()
            selectNote(null)
        }
    }

    fun createBackup(password: CharArray, outputDir: File) {
        viewModelScope.launch {
            backupManager.createAndUploadBackup(password, outputDir)
        }
    }
}

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val labels: List<String> = emptyList(),
    val selectedNote: Note? = null
)
