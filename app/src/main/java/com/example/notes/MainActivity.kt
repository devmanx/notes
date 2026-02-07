package com.example.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: NotesViewModel
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient
    private var pendingDriveAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notesDirectoryProvider = NotesDirectoryProvider(this)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_FILE_SCOPE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
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
            val signInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                if (task.isSuccessful) {
                    viewModel.updateDriveAuthorization(true)
                    pendingDriveAction?.invoke()
                } else {
                    viewModel.updateDriveAuthorization(false)
                }
                pendingDriveAction = null
            }
            val ensureDriveAuthorized: ((() -> Unit) -> Unit) = { onAuthorized ->
                val account = GoogleSignIn.getLastSignedInAccount(this)
                if (account != null && account.grantedScopes.contains(Scope(DRIVE_FILE_SCOPE))) {
                    viewModel.updateDriveAuthorization(true)
                    onAuthorized()
                } else {
                    pendingDriveAction = onAuthorized
                    signInLauncher.launch(googleSignInClient.signInIntent)
                }
            }
            NotesScreen(
                state = state,
                onStartCreate = viewModel::startCreateNote,
                onOpenNote = viewModel::openNote,
                onStartEdit = viewModel::startEditNote,
                onSaveNote = viewModel::saveNote,
                onDeleteNote = viewModel::deleteSelectedNote,
                onSelectLabel = viewModel::selectLabel,
                onCloseEditor = viewModel::closeEditor,
                onCloseViewer = viewModel::closeViewer,
                onUpdateNotesDirectory = viewModel::updateNotesDirectory,
                onExportZip = viewModel::exportNotesToZip,
                onImportZip = { uri -> viewModel.importNotesFromZip(contentResolver, uri) },
                onExportGoogleDrive = { password ->
                    ensureDriveAuthorized {
                        viewModel.exportNotesToGoogleDrive(password, cacheDir)
                    }
                },
                onImportGoogleDrive = {
                    ensureDriveAuthorized {
                        viewModel.updateDriveAuthorization(true)
                    }
                }
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
    }
}

private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
