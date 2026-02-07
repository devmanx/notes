package com.example.notes.backup

import com.example.notes.data.NotesDirectoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class EncryptedBackupManager(
    private val notesDirectoryProvider: NotesDirectoryProvider,
    private val backupStorage: BackupStorage
) {
    suspend fun createAndUploadBackup(password: CharArray, outputDir: File) {
        withContext(Dispatchers.IO) {
            val zipFile = File(outputDir, "notes_backup.zip")
            createZip(zipFile)
            val encryptedFile = File(outputDir, "notes_backup.enc")
            encrypt(zipFile, encryptedFile, password)
            backupStorage.uploadBackup(encryptedFile)
            zipFile.delete()
            encryptedFile.delete()
        }
    }

    suspend fun createZipBackup(outputDir: File): File {
        return withContext(Dispatchers.IO) {
            val zipFile = File(outputDir, "notes_export_${System.currentTimeMillis()}.zip")
            createZip(zipFile)
            zipFile
        }
    }

    private fun createZip(target: File) {
        val notesDirectory = notesDirectoryProvider.notesDirectory()
        ZipOutputStream(FileOutputStream(target)).use { zipOut ->
            notesDirectory.listFiles()?.forEach { file ->
                if (file.isFile && file.extension == "txt" || file.name == "notes_index.json") {
                    val entry = ZipEntry(file.name)
                    zipOut.putNextEntry(entry)
                    FileInputStream(file).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
        }
    }

    private fun encrypt(input: File, output: File, password: CharArray) {
        val salt = Random.nextBytes(16)
        val iv = Random.nextBytes(12)
        val keySpec = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, iv))

        FileOutputStream(output).use { fileOut ->
            fileOut.write(salt)
            fileOut.write(iv)
            CipherOutputStream(fileOut, cipher).use { cipherOut ->
                FileInputStream(input).use { it.copyTo(cipherOut) }
            }
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, 120_000, 256)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }
}
