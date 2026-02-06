package com.example.notes.data

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val labels: List<String>,
    val lastModifiedEpochMs: Long
)
