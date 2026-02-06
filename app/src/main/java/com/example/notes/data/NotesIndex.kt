package com.example.notes.data

import kotlinx.serialization.Serializable

@Serializable
data class NotesIndex(
    val version: Int = 1,
    val notes: List<NoteIndexEntry> = emptyList()
)

@Serializable
data class NoteIndexEntry(
    val id: String,
    val title: String,
    val labels: List<String>,
    val fileName: String,
    val lastModifiedEpochMs: Long
)
