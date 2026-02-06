package com.example.notes.backup

import java.io.File

interface BackupStorage {
    suspend fun uploadBackup(file: File)
}
