package com.folio.reader.epub

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

data class ParsedBook(
    val id: String,
    val title: String,
    val author: String,
    val contentDir: File,
    val coverPath: String?,
    /** Spine file paths, relative to contentDir, in reading order. */
    val spine: List<String>,
)

/**
 * Minimal EPUB importer: unzips the archive, reads the OPF (via the
 * container.xml rootfile pointer) for metadata + spine order, and copies
 * everything into app-private storage so the source file/SAF permission
 * doesn't need to stay valid afterwards.
 */
object EpubParser {

    fun import(context: Context, uri: Uri): ParsedBook {
        val id = UUID.randomUUID().toString()
        val destDir = File(context.filesDir, "books/$id").apply { mkdirs() }

        val entries = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(uri)!!.use { input ->
            unzipAll(input, entries)
        }

        val containerXml = entries["META-INF/container.xml"]
            ?: error("Not a valid EPUB: missing container.xml")
        val opfPath = readOpfPath(containerXml)
        val opfBytes = entries[opfPath] ?: error("Not a valid EPUB: missing OPF at $opfPath")
        val opfDir = opfPath.substringBeforeLast('/', "")

        val opf = parseOpf(opfBytes)

        // Write every entry to disk under contentDir, preserving the zip's
        // internal paths so relative hrefs/images/CSS resolve unchanged.
        for ((path, bytes) in entries) {
            val file = File(destDir, path)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        }

        val spineFiles = opf.spineHrefs.map { href -> joinPath(opfDir, href) }
        val coverPath = opf.coverHref?.let { joinPath(opfDir, it) }
            ?.takeIf { File(destDir, it).exists() }

        return ParsedBook(
            id = id,
            title = opf.title.ifBlank { "Untitled" },
            author = opf.author.ifBlank { "Unknown" },
            contentDir = destDir,
            coverPath = coverPath,
            spine = spineFiles,
        )
    }

    private fun unzipAll(input: InputStream, out: MutableMap<String, ByteArray>) {
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    out[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun joinPath(dir: String, href: String): String {
        if (dir.isBlank()) return href
        val combined = "$dir/$href"
        // resolve ../ segments
        val parts = ArrayDeque<String>()
        for (segment in combined.split('/')) {
            when (segment) {
                ".." -> { if (parts.isNotEmpty()) parts.removeLast() }
                ".", "" -> Unit
                else -> parts.addLast(segment)
            }
        }
        return parts.joinToString("/")
    }

    private fun readOpfPath(containerXml: ByteArray): String {
        val parser = newParser(containerXml)
        var path = ""
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                path = parser.getAttributeValue(null, "full-path") ?: ""
            }
            event = parser.next()
        }
        return path
    }

    private data class OpfResult(
        val title: String,
        val author: String,
        val spineHrefs: List<String>,
        val coverHref: String?,
    )

    private fun parseOpf(bytes: ByteArray): OpfResult {
        val parser = newParser(bytes)
        var title = ""
        var author = ""
        val manifest = mutableMapOf<String, String>() // id -> href
        val manifestProps = mutableMapOf<String, String>() // id -> properties
        val spineIds = mutableListOf<String>()
        var coverManifestId: String? = null

        var event = parser.eventType
        var inMetadata = false
        var textTarget: ((String) -> Unit)? = null
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "metadata" -> inMetadata = true
                        "title" -> if (inMetadata) textTarget = { title = it }
                        "creator" -> if (inMetadata) textTarget = { author = it }
                        "meta" -> {
                            if (parser.getAttributeValue(null, "name") == "cover") {
                                coverManifestId = parser.getAttributeValue(null, "content")
                            }
                        }
                        "item" -> {
                            val itemId = parser.getAttributeValue(null, "id")
                            val href = parser.getAttributeValue(null, "href")
                            val props = parser.getAttributeValue(null, "properties")
                            if (itemId != null && href != null) {
                                manifest[itemId] = Uri.decode(href)
                                if (props != null) manifestProps[itemId] = props
                            }
                        }
                        "itemref" -> {
                            parser.getAttributeValue(null, "idref")?.let { spineIds.add(it) }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "metadata") inMetadata = false
                    if (parser.name == "title" || parser.name == "creator") textTarget = null
                }
                XmlPullParser.TEXT -> {
                    textTarget?.invoke(parser.text.trim())
                }
            }
            event = parser.next()
        }

        val coverId = coverManifestId
            ?: manifestProps.entries.firstOrNull { it.value.contains("cover-image") }?.key
        val coverHref = coverId?.let { manifest[it] }

        return OpfResult(
            title = title,
            author = author,
            spineHrefs = spineIds.mapNotNull { manifest[it] },
            coverHref = coverHref,
        )
    }

    private fun newParser(bytes: ByteArray): XmlPullParser {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(bytes.inputStream(), "UTF-8")
        return parser
    }
}
