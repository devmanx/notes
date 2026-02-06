package com.example.notes.data

import android.content.Context
import java.io.File

class NotesDirectoryProvider(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun notesDirectory(): File {
        val storedPath = preferences.getString(KEY_NOTES_DIR, null).orEmpty()
        val defaultDir = requireNotNull(context.getExternalFilesDir("notes"))
        if (storedPath.isBlank()) {
            return defaultDir
        }
        val customDir = File(storedPath)
        if (!customDir.exists()) {
            customDir.mkdirs()
        }
        return if (customDir.exists()) customDir else defaultDir
    }

    fun setNotesDirectory(path: String?) {
        if (path.isNullOrBlank()) {
            preferences.edit().remove(KEY_NOTES_DIR).apply()
            return
        }
        val target = File(path)
        if (!target.exists()) {
            target.mkdirs()
        }
        preferences.edit().putString(KEY_NOTES_DIR, target.absolutePath).apply()
    }

    private companion object {
        const val PREFS_NAME = "notes_preferences"
        const val KEY_NOTES_DIR = "notes_directory_path"
    }
}
