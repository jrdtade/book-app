package com.folio.reader.comic

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

data class ParsedComic(
    val id: String,
    val title: String,
    val contentDir: File,
    val coverPath: String?,
    /** Page image paths, relative to contentDir, in reading order. */
    val pages: List<String>,
)

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

/**
 * Extracts .cbz (zip) and .cbr (rar) comic archives into app-private storage, in the
 * same spirit as EpubParser: copy every page image to contentDir and hand back the
 * reading order, so the source SAF permission doesn't need to outlive the import.
 */
object CbzCbrParser {

    fun import(context: Context, uri: Uri, displayName: String?): ParsedComic {
        val id = UUID.randomUUID().toString()
        val destDir = File(context.filesDir, "books/$id").apply { mkdirs() }
        val tempFile = File.createTempFile("import_", ".tmp", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            val pages = if (isRar(tempFile)) extractCbr(tempFile, destDir) else extractCbz(tempFile, destDir)
            val title = displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Untitled"
            return ParsedComic(id = id, title = title, contentDir = destDir, coverPath = pages.firstOrNull(), pages = pages)
        } finally {
            tempFile.delete()
        }
    }

    /** Sniffs the Rar! magic bytes rather than trusting the file extension. */
    private fun isRar(file: File): Boolean {
        val header = ByteArray(4)
        val read = file.inputStream().use { it.read(header) }
        return read == 4 && header[0] == 0x52.toByte() && header[1] == 0x61.toByte() &&
            header[2] == 0x72.toByte() && header[3] == 0x21.toByte()
    }

    private fun extractCbz(file: File, destDir: File): List<String> {
        val pages = mutableListOf<String>()
        ZipInputStream(file.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && isImageName(entry.name)) {
                    val name = sanitizedName(entry.name)
                    val out = File(destDir, name)
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zis.copyTo(it) }
                    pages.add(name)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return pages.sortedWith(NATURAL_ORDER)
    }

    private fun extractCbr(file: File, destDir: File): List<String> {
        val pages = mutableListOf<String>()
        Archive(file).use { archive ->
            var header = archive.nextFileHeader()
            while (header != null) {
                val entryName = header.fileNameString.trim()
                if (!header.isDirectory && isImageName(entryName)) {
                    val name = sanitizedName(entryName)
                    val out = File(destDir, name)
                    out.parentFile?.mkdirs()
                    out.outputStream().use { stream -> archive.extractFile(header, stream) }
                    pages.add(name)
                }
                header = archive.nextFileHeader()
            }
        }
        return pages.sortedWith(NATURAL_ORDER)
    }

    private fun isImageName(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS

    /** Strips ".."/leading-slash segments so an extracted entry can't escape destDir. */
    private fun sanitizedName(name: String): String {
        val parts = ArrayDeque<String>()
        for (segment in name.replace('\\', '/').split('/')) {
            when (segment) {
                "..", ".", "" -> Unit
                else -> parts.addLast(segment)
            }
        }
        return parts.joinToString("/")
    }

    private val NATURAL_ORDER = Comparator<String> { a, b -> naturalCompare(a, b) }

    /** Compares embedded numeric runs numerically, so "page2.jpg" sorts before "page10.jpg". */
    private fun naturalCompare(a: String, b: String): Int {
        val ax = Regex("\\d+|\\D+").findAll(a).map { it.value }.toList()
        val bx = Regex("\\d+|\\D+").findAll(b).map { it.value }.toList()
        for (i in 0 until minOf(ax.size, bx.size)) {
            val x = ax[i]
            val y = bx[i]
            val cmp = if (x.firstOrNull()?.isDigit() == true && y.firstOrNull()?.isDigit() == true) {
                x.toLong().compareTo(y.toLong())
            } else {
                x.compareTo(y)
            }
            if (cmp != 0) return cmp
        }
        return ax.size - bx.size
    }
}
