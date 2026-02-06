package com.example.notes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class FileNotesRepository(
    private val notesDirectoryProvider: NotesDirectoryProvider
) : NotesRepository {

    private val json = Json { prettyPrint = true }
    private val indexFileName = "notes_index.json"

    override suspend fun listNotes(): List<Note> = withContext(Dispatchers.IO) {
        val index = readIndex()
        index.notes.sortedByDescending { it.lastModifiedEpochMs }
            .mapNotNull { entry ->
                val content = readContent(entry.fileName)
                content?.let {
                    Note(
                        id = entry.id,
                        title = entry.title,
                        content = it,
                        labels = entry.labels,
                        lastModifiedEpochMs = entry.lastModifiedEpochMs
                    )
                }
            }
    }

    override suspend fun loadNote(id: String): Note? = withContext(Dispatchers.IO) {
        val index = readIndex()
        val entry = index.notes.firstOrNull { it.id == id } ?: return@withContext null
        val content = readContent(entry.fileName) ?: return@withContext null
        Note(
            id = entry.id,
            title = entry.title,
            content = content,
            labels = entry.labels,
            lastModifiedEpochMs = entry.lastModifiedEpochMs
        )
    }

    override suspend fun saveNote(note: Note): Note = withContext(Dispatchers.IO) {
        val notesDir = ensureNotesDir()
        val fileName = note.id.ifBlank { UUID.randomUUID().toString() } + ".txt"
        val file = File(notesDir, fileName)
        file.writeText(note.content)

        val updatedNote = note.copy(
            id = note.id.ifBlank { fileName.removeSuffix(".txt") },
            lastModifiedEpochMs = System.currentTimeMillis()
        )

        val updatedIndex = updateIndex(updatedNote, fileName)
        writeIndex(updatedIndex)
        updatedNote
    }

    override suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        val index = readIndex()
        val entry = index.notes.firstOrNull { it.id == id } ?: return@withContext
        val notesDir = ensureNotesDir()
        File(notesDir, entry.fileName).delete()
        val updatedIndex = index.copy(notes = index.notes.filterNot { it.id == id })
        writeIndex(updatedIndex)
    }

    override suspend fun availableLabels(): List<String> = withContext(Dispatchers.IO) {
        readIndex().notes.flatMap { it.labels }.distinct().sorted()
    }

    private fun readContent(fileName: String): String? {
        val notesDir = ensureNotesDir()
        val file = File(notesDir, fileName)
        return if (file.exists()) file.readText() else null
    }

    private fun updateIndex(note: Note, fileName: String): NotesIndex {
        val index = readIndex()
        val updatedEntry = NoteIndexEntry(
            id = note.id,
            title = note.title,
            labels = note.labels,
            fileName = fileName,
            lastModifiedEpochMs = note.lastModifiedEpochMs
        )
        val updatedNotes = index.notes.filterNot { it.id == note.id } + updatedEntry
        return index.copy(notes = updatedNotes)
    }

    private fun readIndex(): NotesIndex {
        val indexFile = indexFile()
        if (!indexFile.exists()) return NotesIndex()
        return json.decodeFromString(NotesIndex.serializer(), indexFile.readText())
    }

    private fun writeIndex(index: NotesIndex) {
        val indexFile = indexFile()
        indexFile.writeText(json.encodeToString(index))
    }

    private fun indexFile(): File {
        val notesDir = ensureNotesDir()
        return File(notesDir, indexFileName)
    }

    private fun ensureNotesDir(): File {
        val notesDir = notesDirectoryProvider.notesDirectory()
        if (!notesDir.exists()) {
            notesDir.mkdirs()
        }
        return notesDir
    }
}
