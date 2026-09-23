package com.yourapp.sqliautohunter.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class FileStorageHelper(private val context: Context) {

    fun getAppSpecificExternalDir(): File {
        return context.getExternalFilesDir(null) ?: context.filesDir
    }

    fun getCsvDir(): File {
        val dir = File(getAppSpecificExternalDir(), "csv")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getLogsDir(): File {
        val dir = File(getAppSpecificExternalDir(), "logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createCsvFile(fileName: String): File {
        val dir = getCsvDir()
        return File(dir, fileName)
    }

    fun createLogFile(fileName: String): File {
        val dir = getLogsDir()
        return File(dir, fileName)
    }

    fun writeToFile(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun appendToFile(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.appendText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun copyToDownloads(sourceFile: File, targetName: String): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetFile = File(downloadsDir, targetName)
            sourceFile.copyTo(targetFile, overwrite = true)
            targetFile
        } catch (e: Exception) {
            null
        }
    }

    fun copyToDownloadsWithSaf(sourceFile: File, targetName: String): File? {
        return try {
            val contentUri = Uri.fromFile(sourceFile)
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, targetName)
            }
            // This would need to be launched from an Activity
            // For now, fall back to direct copy
            copyToDownloads(sourceFile, targetName)
        } catch (e: Exception) {
            copyToDownloads(sourceFile, targetName)
        }
    }

    fun getFileSize(file: File): Long {
        return if (file.exists()) file.length() else 0L
    }

    fun deleteFile(file: File): Boolean {
        return file.delete()
    }

    fun listFiles(directory: File, extension: String? = null): List<File> {
        val files = directory.listFiles()?.toList() ?: emptyList()
        return if (extension != null) {
            files.filter { it.name.endsWith(extension) }
        } else {
            files
        }
    }

    fun clearDirectory(directory: File): Boolean {
        return try {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    clearDirectory(file)
                } else {
                    file.delete()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readFileContent(file: File): String? {
        return try {
            file.readText()
        } catch (e: Exception) {
            null
        }
    }

    fun readFileAsBytes(file: File): ByteArray? {
        return try {
            file.readBytes()
        } catch (e: Exception) {
            null
        }
    }

    fun createTempFile(prefix: String = "sqli", suffix: String = ".tmp"): File {
        return File.createTempFile(prefix, suffix, context.cacheDir)
    }

    fun getFileUri(file: File): Uri {
        return Uri.fromFile(file)
    }

    fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    fun getFile(path: String): File? {
        val file = File(path)
        return if (file.exists()) file else null
    }
}
