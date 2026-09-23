package com.yourapp.sqliautohunter.util

import com.yourapp.sqliautohunter.util.Constants.CSV_DELIMITER
import java.io.File
import java.io.FileWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

class CsvWriter {

    fun writeCsv(
        file: File,
        headers: List<String>,
        rows: List<List<String>>
    ): Boolean {
        return try {
            file.parentFile?.mkdirs()
            FileWriter(file).use { writer ->
                writeHeaders(writer, headers)
                writeRows(writer, rows)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun writeCsv(
        outputStream: OutputStream,
        headers: List<String>,
        rows: List<List<String>>
    ): Boolean {
        return try {
            OutputStreamWriter(outputStream).use { writer ->
                writeHeaders(writer, headers)
                writeRows(writer, rows)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun appendCsv(
        file: File,
        rows: List<List<String>>
    ): Boolean {
        return try {
            file.parentFile?.mkdirs()
            FileWriter(file, true).use { writer ->
                writeRows(writer, rows)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun writeHeaders(writer: java.io.Writer, headers: List<String>) {
        val line = escapeCsvRow(headers)
        writer.write(line)
        writer.write("\n")
    }

    private fun writeRows(writer: java.io.Writer, rows: List<List<String>>) {
        rows.forEach { row ->
            val line = escapeCsvRow(row)
            writer.write(line)
            writer.write("\n")
        }
    }

    private fun escapeCsvRow(row: List<String>): String {
        return row.joinToString(CSV_DELIMITER) { value ->
            escapeCsvValue(value)
        }
    }

    private fun escapeCsvValue(value: String): String {
        val needsQuotes = value.contains(CSV_DELIMITER) || 
                value.contains("\n") || 
                value.contains("\r") ||
                value.contains("\"") ||
                value.startsWith(" ") ||
                value.endsWith(" ")

        return if (needsQuotes) {
            val escaped = value.replace("\"", "\"\"")
            "\"$escaped\""
        } else {
            value
        }
    }

    fun writeBuffered(
        file: File,
        headers: List<String>,
        rowBatches: List<List<String>>,
        bufferSize: Int = Constants.CSV_BUFFER_SIZE
    ): Boolean {
        return try {
            file.parentFile?.mkdirs()
            FileWriter(file).use { writer ->
                writeHeaders(writer, headers)
                
                rowBatches.chunked(bufferSize).forEach { batch ->
                    writeRows(writer, batch)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createCsvString(headers: List<String>, rows: List<List<String>>): String {
        val builder = StringBuilder()
        builder.append(escapeCsvRow(headers))
        builder.append("\n")
        rows.forEach { row ->
            builder.append(escapeCsvRow(row))
            builder.append("\n")
        }
        return builder.toString()
    }
}
