package com.example.notes.data

interface NotesRepository {
    suspend fun listNotes(): List<Note>
    suspend fun loadNote(id: String): Note?
    suspend fun saveNote(note: Note): Note
    suspend fun deleteNote(id: String)
    suspend fun availableLabels(): List<String>
}
