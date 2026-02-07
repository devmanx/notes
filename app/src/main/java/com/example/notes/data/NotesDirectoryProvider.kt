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
        return if (isWritableDirectory(customDir)) {
            customDir
        } else {
            preferences.edit().remove(KEY_NOTES_DIR).apply()
            defaultDir
        }
    }

    fun setNotesDirectory(path: String?) {
        if (path.isNullOrBlank()) {
            preferences.edit().remove(KEY_NOTES_DIR).apply()
            return
        }
        val target = File(path)
        if (isWritableDirectory(target)) {
            preferences.edit().putString(KEY_NOTES_DIR, target.absolutePath).apply()
        } else {
            preferences.edit().remove(KEY_NOTES_DIR).apply()
        }
    }

    private fun isWritableDirectory(directory: File): Boolean {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        if (!directory.exists() || !directory.isDirectory) {
            return false
        }
        return try {
            val probe = File(directory, ".notes_write_test")
            probe.writeText("probe")
            probe.delete()
            true
        } catch (exception: Exception) {
            false
        }
    }

    private companion object {
        const val PREFS_NAME = "notes_preferences"
        const val KEY_NOTES_DIR = "notes_directory_path"
    }
}
