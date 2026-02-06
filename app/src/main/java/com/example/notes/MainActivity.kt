package com.example.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.notes.backup.EncryptedBackupManager
import com.example.notes.backup.GoogleDriveBackupStorage
import com.example.notes.backup.DriveUploader
import com.example.notes.data.FileNotesRepository
import com.example.notes.data.NotesDirectoryProvider
import com.example.notes.ui.NotesScreen
import com.example.notes.ui.NotesViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: NotesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notesDirectoryProvider = NotesDirectoryProvider(this)
        viewModel = ViewModelProvider(
            this,
            NotesViewModelFactory(
                repository = FileNotesRepository(notesDirectoryProvider),
                backupManager = EncryptedBackupManager(
                    notesDirectoryProvider = notesDirectoryProvider,
                    backupStorage = GoogleDriveBackupStorage(PlaceholderDriveUploader())
                ),
                notesDirectoryProvider = notesDirectoryProvider
            )
        )[NotesViewModel::class.java]
        viewModel.loadNotes()
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            NotesScreen(
                state = state,
                onStartCreate = viewModel::startCreateNote,
                onOpenNote = viewModel::openNote,
                onSaveNote = viewModel::saveNote,
                onDeleteNote = viewModel::deleteSelectedNote,
                onSelectLabel = viewModel::selectLabel,
                onCloseEditor = viewModel::closeEditor,
                onUpdateNotesDirectory = viewModel::updateNotesDirectory
            )
        }
    }
}

private class NotesViewModelFactory(
    private val repository: FileNotesRepository,
    private val backupManager: EncryptedBackupManager,
    private val notesDirectoryProvider: NotesDirectoryProvider
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository, backupManager, notesDirectoryProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private class PlaceholderDriveUploader : DriveUploader {
    override suspend fun uploadEncryptedBackup(file: java.io.File) {
        error("Skonfiguruj upload do Google Drive przez implementację DriveUploader.")
    }
}
