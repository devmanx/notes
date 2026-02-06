package com.example.notes.backup

import java.io.File

class GoogleDriveBackupStorage(
    private val driveUploader: DriveUploader
) : BackupStorage {
    override suspend fun uploadBackup(file: File) {
        driveUploader.uploadEncryptedBackup(file)
    }
}

interface DriveUploader {
    suspend fun uploadEncryptedBackup(file: File)
}
