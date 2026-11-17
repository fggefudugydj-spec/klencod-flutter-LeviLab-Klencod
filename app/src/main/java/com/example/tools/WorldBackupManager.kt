package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.core.LeviLogger
import com.example.model.McPackManifestInfo
import com.example.model.RestorableMcWorldInfo
import com.example.model.WorldFolderSummary
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object WorldBackupManager {

    private data class TreeNode(
        val docId: String,
        val relativePath: String,
        val isDirectory: Boolean,
        val sizeBytes: Long
    )

    fun inspectWorldTree(context: Context, treeUri: Uri, logger: LeviLogger? = null): WorldFolderSummary {
        val resolver = context.contentResolver
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDisplayName = queryDisplayName(context, DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId))
            ?: rootDocId.substringAfterLast('/')

        val nodes = collectTreeNodes(context, treeUri, rootDocId, "")
        val fileNodes = nodes.filter { !it.isDirectory }
        val totalBytes = fileNodes.sumOf { it.sizeBytes }

        val levelDatNode = fileNodes.firstOrNull { it.relativePath.equals("level.dat", ignoreCase = true) }
        val levelNameNode = fileNodes.firstOrNull { it.relativePath.equals("levelname.txt", ignoreCase = true) }

        val levelName = if (levelNameNode != null) {
            runCatching {
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, levelNameNode.docId)
                resolver.openInputStream(docUri)?.bufferedReader()?.use { it.readText().trim() }
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: rootDisplayName
        } else {
            rootDisplayName
        }

        logger?.info(
            "TOOLS_WORLD",
            "Scanned SAF world '$levelName' ($rootDisplayName): ${fileNodes.size} files, $totalBytes bytes, level.dat=${levelDatNode != null}"
        )

        return WorldFolderSummary(
            treeUriString = treeUri.toString(),
            displayName = rootDisplayName,
            levelName = levelName,
            fileCount = fileNodes.size,
            totalBytes = totalBytes,
            hasLevelDat = levelDatNode != null
        )
    }

    fun exportWorldToMcWorld(
        context: Context,
        treeUri: Uri,
        destMcWorldUri: Uri,
        logger: LeviLogger? = null
    ): Result<Int> {
        return runCatching {
            val resolver = context.contentResolver
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val nodes = collectTreeNodes(context, treeUri, rootDocId, "")
            val fileNodes = nodes.filter { !it.isDirectory }

            val outStream = resolver.openOutputStream(destMcWorldUri)
                ?: error("Could not open destination output stream for .mcworld")

            var exportedCount = 0
            ZipOutputStream(BufferedOutputStream(outStream)).use { zos ->
                val buf = ByteArray(32 * 1024)
                for (node in fileNodes) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, node.docId)
                    val entry = ZipEntry(node.relativePath)
                    zos.putNextEntry(entry)
                    resolver.openInputStream(docUri)?.use { input ->
                        BufferedInputStream(input).use { bis ->
                            while (true) {
                                val read = bis.read(buf)
                                if (read <= 0) break
                                zos.write(buf, 0, read)
                            }
                        }
                    }
                    zos.closeEntry()
                    exportedCount++
                }
                zos.finish()
            }

            logger?.success("TOOLS_WORLD", "Exported .mcworld archive with $exportedCount files")
            exportedCount
        }.onFailure { err ->
            logger?.error("TOOLS_WORLD", "World export failed: ${err.message}", err)
        }
    }

    fun inspectMcWorldFile(
        context: Context,
        fileUri: Uri,
        logger: LeviLogger? = null
    ): RestorableMcWorldInfo {
        val fileName = queryOpenableFileName(context, fileUri) ?: "world.mcworld"
        var levelName = fileName.substringBeforeLast('.')
        var entryCount = 0
        var uncompressedBytes = 0L
        var hasLevelDat = false

        runCatching {
            context.contentResolver.openInputStream(fileUri)?.use { raw ->
                ZipInputStream(BufferedInputStream(raw)).use { zis ->
                    while (true) {
                        val entry = zis.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val safeSegments = sanitizeZipRelativePath(entry.name) ?: continue
                        val rel = safeSegments.joinToString("/")
                        entryCount++
                        if (entry.size > 0) uncompressedBytes += entry.size
                        if (rel.equals("level.dat", ignoreCase = true) || rel.endsWith("/level.dat", ignoreCase = true)) {
                            hasLevelDat = true
                        }
                        if (rel.equals("levelname.txt", ignoreCase = true) || rel.endsWith("/levelname.txt", ignoreCase = true)) {
                            val txt = zis.readBytes().toString(Charsets.UTF_8).trim()
                            if (txt.isNotBlank()) levelName = txt
                        }
                    }
                }
            }
        }.onFailure { err ->
            logger?.warn("TOOLS_WORLD", "Failed inspecting .mcworld $fileName: ${err.message}")
        }

        logger?.info(
            "TOOLS_WORLD",
            "Inspected backup '$levelName' ($fileName): $entryCount entries, level.dat=$hasLevelDat"
        )
        return RestorableMcWorldInfo(
            fileUriString = fileUri.toString(),
            fileName = fileName,
            levelName = levelName,
            entryCount = entryCount,
            uncompressedBytes = uncompressedBytes,
            hasLevelDat = hasLevelDat
        )
    }

    fun restoreMcWorldToFolder(
        context: Context,
        mcWorldUri: Uri,
        targetTreeUri: Uri,
        logger: LeviLogger? = null
    ): Result<Int> {
        return runCatching {
            val resolver = context.contentResolver
            val rootDocId = DocumentsContract.getTreeDocumentId(targetTreeUri)
            val dirCache = mutableMapOf<String, String>("" to rootDocId)
            var restoredCount = 0

            val inputStream = resolver.openInputStream(mcWorldUri)
                ?: error("Could not open .mcworld input stream")

            inputStream.use { raw ->
                ZipInputStream(BufferedInputStream(raw)).use { zis ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        val entry = zis.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val segments = sanitizeZipRelativePath(entry.name) ?: continue
                        val fileName = segments.last()
                        val parentSegments = segments.dropLast(1)

                        var currentRel = ""
                        var parentDocId = rootDocId
                        for (dirName in parentSegments) {
                            currentRel = if (currentRel.isEmpty()) dirName else "$currentRel/$dirName"
                            parentDocId = dirCache.getOrPut(currentRel) {
                                findOrCreateChildDir(context, targetTreeUri, parentDocId, dirName)
                            }
                        }

                        val existingFileDocId = findChildDocId(context, targetTreeUri, parentDocId, fileName)
                        val targetFileUri = if (existingFileDocId != null) {
                            DocumentsContract.buildDocumentUriUsingTree(targetTreeUri, existingFileDocId)
                        } else {
                            val parentUri = DocumentsContract.buildDocumentUriUsingTree(targetTreeUri, parentDocId)
                            DocumentsContract.createDocument(
                                resolver,
                                parentUri,
                                "application/octet-stream",
                                fileName
                            ) ?: error("Failed creating document $fileName")
                        }

                        resolver.openOutputStream(targetFileUri, "wt")?.use { out ->
                            BufferedOutputStream(out).use { bos ->
                                while (true) {
                                    val n = zis.read(buf)
                                    if (n <= 0) break
                                    bos.write(buf, 0, n)
                                }
                                bos.flush()
                            }
                        } ?: error("Failed writing restored file $fileName")
                        restoredCount++
                    }
                }
            }

            logger?.success("TOOLS_WORLD", "Restored $restoredCount files from .mcworld into target folder")
            restoredCount
        }.onFailure { err ->
            logger?.error("TOOLS_WORLD", "World restore failed: ${err.message}", err)
        }
    }

    fun sanitizeZipRelativePath(entryName: String): List<String>? {
        val normalized = entryName.replace('\\', '/').trim('/')
        if (normalized.isEmpty()) return null
        val parts = normalized.split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        if (parts.any { it == "." || it == ".." || it.contains(':') }) return null
        return parts
    }

    fun inspectMcPack(context: Context, fileUri: Uri, logger: LeviLogger? = null): McPackManifestInfo {
        val fileName = queryOpenableFileName(context, fileUri) ?: "pack.mcpack"
        var packName = fileName
        var packDesc = ""
        var packUuid = ""
        var packVersion = ""
        var packType = "resource/addon"

        runCatching {
            context.contentResolver.openInputStream(fileUri)?.use { raw ->
                ZipInputStream(BufferedInputStream(raw)).use { zis ->
                    while (true) {
                        val entry = zis.nextEntry ?: break
                        if (!entry.isDirectory && entry.name.endsWith("manifest.json", ignoreCase = true)) {
                            val jsonText = zis.readBytes().toString(Charsets.UTF_8)
                            val root = JSONObject(jsonText)
                            val header = root.optJSONObject("header")
                            if (header != null) {
                                packName = header.optString("name", packName).ifBlank { packName }
                                packDesc = header.optString("description", "")
                                packUuid = header.optString("uuid", "")
                                val verArr = header.optJSONArray("version")
                                if (verArr != null && verArr.length() >= 3) {
                                    packVersion = "${verArr.optInt(0)}.${verArr.optInt(1)}.${verArr.optInt(2)}"
                                }
                            }
                            val modules = root.optJSONArray("modules")
                            if (modules != null && modules.length() > 0) {
                                val firstMod = modules.optJSONObject(0)
                                packType = firstMod?.optString("type", packType) ?: packType
                            }
                            break
                        }
                    }
                }
            }
        }.onFailure { err ->
            logger?.warn("TOOLS_PACK", "Could not parse manifest.json in $fileName: ${err.message}")
        }

        logger?.info("TOOLS_PACK", "Inspected pack '$packName' ($packType $packVersion)")
        return McPackManifestInfo(
            fileUriString = fileUri.toString(),
            fileName = fileName,
            packName = packName,
            packDescription = packDesc,
            packUuid = packUuid,
            packVersion = packVersion,
            packType = packType
        )
    }

    fun openPackInMinecraft(
        context: Context,
        fileUri: Uri,
        targetPackageName: String?,
        logger: LeviLogger? = null
    ): Boolean {
        val baseIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (!targetPackageName.isNullOrBlank()) {
                setPackage(targetPackageName)
            }
        }
        return try {
            context.startActivity(baseIntent)
            logger?.success("TOOLS_PACK", "Sent ACTION_VIEW intent to ${targetPackageName ?: "default handler"}")
            true
        } catch (t: Throwable) {
            logger?.warn("TOOLS_PACK", "Direct package VIEW failed (${t.message}), trying system chooser")
            try {
                val chooserIntent = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, "application/octet-stream")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Open in Minecraft"
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
                true
            } catch (inner: Throwable) {
                logger?.error("TOOLS_PACK", "No app available to open file: ${inner.message}", inner)
                false
            }
        }
    }

    private fun findOrCreateChildDir(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        dirName: String
    ): String {
        val existing = findChildDocId(context, treeUri, parentDocId, dirName)
        if (existing != null) return existing
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
        val createdUri = DocumentsContract.createDocument(
            context.contentResolver,
            parentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            dirName
        ) ?: error("Failed creating directory $dirName")
        return DocumentsContract.getDocumentId(createdUri)
    }

    private fun findChildDocId(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        targetName: String
    ): String? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                if (name == targetName && idIdx >= 0) {
                    return cursor.getString(idIdx)
                }
            }
        }
        return null
    }

    private fun collectTreeNodes(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        prefix: String
    ): List<TreeNode> {
        val resolver = context.contentResolver
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        val out = mutableListOf<TreeNode>()
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else "item"
                val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else ""
                val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                val relPath = if (prefix.isEmpty()) name else "$prefix/$name"
                val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                out.add(TreeNode(docId = docId, relativePath = relPath, isDirectory = isDir, sizeBytes = size))
                if (isDir) {
                    out.addAll(collectTreeNodes(context, treeUri, docId, relPath))
                }
            }
        }
        return out
    }

    private fun queryDisplayName(context: Context, docUri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun queryOpenableFileName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }
}
