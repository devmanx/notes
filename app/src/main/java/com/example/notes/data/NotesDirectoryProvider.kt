package com.example.notes.data

import android.content.Context
import java.io.File

class NotesDirectoryProvider(private val context: Context) {
    fun notesDirectory(): File {
        return requireNotNull(context.getExternalFilesDir("notes"))
    }
}
