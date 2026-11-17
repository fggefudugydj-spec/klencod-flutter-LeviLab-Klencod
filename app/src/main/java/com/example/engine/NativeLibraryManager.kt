package com.example.engine

import android.content.Context
import android.os.Build
import com.example.core.LeviLogger
import com.example.model.ElfInspectionResult
import com.example.model.InstalledMinecraftPackage
import com.example.model.LibraryCacheSummary
import com.example.model.LibraryLoadSource
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object NativeLibraryManager {

    private const val BUFFER_SIZE = 64 * 1024
    private const val CACHE_META_FILE = "cache_meta.json"

    fun cacheRootDir(context: Context): File = File(context.filesDir, "mc_libs")

    fun targetLibDir(
        context: Context,
        pkg: InstalledMinecraftPackage,
        abi: String = selectPreferredAbi(pkg)
    ): File {
        val safePkg = pkg.packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val updateTag = if (pkg.lastUpdateTime > 0L) "${pkg.versionCode}_${pkg.lastUpdateTime}" else "${pkg.versionCode}"
        return File(cacheRootDir(context), "$safePkg/$updateTag/$abi")
    }

    fun selectPreferredAbi(
        pkg: InstalledMinecraftPackage,
        apkAvailableAbis: List<String> = scanApkAvailableAbis(pkg)
    ): String {
        val deviceAbis = (Build.SUPPORTED_ABIS?.toList().orEmpty() + pkg.supportedAbis)
            .filter { it.isNotBlank() }
            .distinct()

        if (apkAvailableAbis.isNotEmpty()) {
            // Prioritize arm64-v8a if both device and APK support it
            if ("arm64-v8a" in deviceAbis && "arm64-v8a" in apkAvailableAbis) {
                return "arm64-v8a"
            }
            for (abi in deviceAbis) {
                if (abi in apkAvailableAbis) return abi
            }
            return apkAvailableAbis.first()
        }

        return when {
            "arm64-v8a" in deviceAbis -> "arm64-v8a"
            deviceAbis.isNotEmpty() -> deviceAbis.first()
            else -> "arm64-v8a"
        }
    }

    fun inspectExistingOrExtract(
        context: Context,
        pkg: InstalledMinecraftPackage,
        logger: LeviLogger? = null,
        forceReextract: Boolean = false
    ): List<ElfInspectionResult> {
        val apkAbis = scanApkAvailableAbis(pkg)
        val abi = selectPreferredAbi(pkg, apkAbis)
        val outDir = targetLibDir(context, pkg, abi)

        // Clean stale version/timestamp caches for this package so old corrupted caches never linger
        pruneStalePackageCaches(context, pkg, outDir, logger)

        if (forceReextract && outDir.exists()) {
            logger?.info("STAGE_B", "Purging Library Cache for clean rebuild at ${outDir.absolutePath}")
            makeTreeWritable(outDir)
            outDir.deleteRecursively()
        }
        outDir.mkdirs()

        val cacheMeta = readCacheMeta(outDir)
        val newMeta = JSONObject().apply {
            put("packageName", pkg.packageName)
            put("versionCode", pkg.versionCode)
            put("lastUpdateTime", pkg.lastUpdateTime)
            put("abi", abi)
        }
        val libCrcJson = JSONObject()

        val resultsByName = linkedMapOf<String, ElfInspectionResult>()

        // Index pre-extracted nativeLibraryDir files first (zero-copy direct loadable on Android 13)
        val nativeDirFiles = indexNativeLibraryDir(pkg)

        // 1. Scan Base APK + Split APKs (handling both compressed & uncompressed STORED entries)
        val apkPaths = buildList {
            if (pkg.sourceDir.isNotBlank()) add(pkg.sourceDir)
            addAll(pkg.splitSourceDirs.filter { it.isNotBlank() })
        }.distinct()

        val libPrefix = "lib/$abi/"
        for (apkPath in apkPaths) {
            val apkFile = File(apkPath)
            if (!apkFile.exists() || !apkFile.canRead()) {
                logger?.warn("STAGE_B", "Cannot read APK archive: $apkPath")
                continue
            }

            try {
                ZipFile(apkFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue
                        if (!entry.name.startsWith(libPrefix) || !entry.name.endsWith(".so")) continue

                        val libName = entry.name.substringAfterLast('/')
                        val destFile = File(outDir, libName)
                        val expectedSize = entry.size
                        val expectedCrc = entry.crc
                        val isStoredUncompressed = entry.method == ZipEntry.STORED
                        val directApkZipPath = "${apkFile.absolutePath}!/${entry.name}"
                        val nativeDirFile = nativeDirFiles[libName]

                        // Reuse validated cache entry only if size + CRC32 + ELF64 all match
                        val cachedCrc = cacheMeta?.optJSONObject("crcs")?.optLong(libName, -1L) ?: -1L
                        val canReuseCache = !forceReextract &&
                            destFile.exists() &&
                            (expectedSize < 0L || destFile.length() == expectedSize) &&
                            (expectedCrc < 0L || cachedCrc == expectedCrc || computeCrc32(destFile) == expectedCrc)

                        if (canReuseCache) {
                            val existingCheck = ElfValidator.validateLibrary(
                                file = destFile,
                                sourceOrigin = apkFile.name,
                                requireMinOneMb = libName == "libminecraftpe.so"
                            )
                            if (existingCheck.isValid) {
                                val resolved = attachBestLoadSource(
                                    baseResult = existingCheck,
                                    nativeDirFile = nativeDirFile,
                                    isStoredInApk = isStoredUncompressed,
                                    directApkZipPath = directApkZipPath,
                                    crc32 = if (expectedCrc >= 0L) expectedCrc else computeCrc32(destFile)
                                )
                                resultsByName[libName] = resolved
                                libCrcJson.put(libName, resolved.crc32)
                                continue
                            }
                        }

                        // Extract atomically to Library Cache so Elf64Parser can inspect DT_NEEDED & ELF headers
                        if (destFile.exists()) {
                            destFile.setWritable(true)
                        }
                        val tmpFile = File(outDir, "$libName.tmp")
                        if (tmpFile.exists()) {
                            tmpFile.setWritable(true)
                            tmpFile.delete()
                        }

                        var copiedBytes = 0L
                        val crc = CRC32()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(tmpFile).use { output ->
                                val buf = ByteArray(BUFFER_SIZE)
                                while (true) {
                                    val n = input.read(buf)
                                    if (n <= 0) break
                                    output.write(buf, 0, n)
                                    crc.update(buf, 0, n)
                                    copiedBytes += n
                                }
                                output.flush()
                                output.fd.sync()
                            }
                        }
                        val actualCrc = crc.value

                        if ((expectedSize >= 0L && copiedBytes != expectedSize) ||
                            (expectedCrc >= 0L && actualCrc != expectedCrc)
                        ) {
                            tmpFile.delete()
                            logger?.error(
                                "STAGE_B",
                                "Integrity mismatch extracting $libName from ${apkFile.name}: size=$copiedBytes/$expectedSize crc=$actualCrc/$expectedCrc"
                            )
                            continue
                        }

                        val tmpCheck = ElfValidator.validateLibrary(
                            file = tmpFile,
                            sourceOrigin = apkFile.name,
                            requireMinOneMb = libName == "libminecraftpe.so"
                        )
                        if (!tmpCheck.isValid) {
                            tmpFile.delete()
                            logger?.error(
                                "STAGE_B",
                                "Validation failed for $libName from ${apkFile.name}: ${tmpCheck.validationNote}"
                            )
                            resultsByName[libName] = tmpCheck.copy(
                                filePath = destFile.absolutePath,
                                loadSource = LibraryLoadSource.MISSING
                            )
                            continue
                        }

                        if (destFile.exists()) {
                            destFile.setWritable(true)
                            destFile.delete()
                        }
                        if (!tmpFile.renameTo(destFile)) {
                            tmpFile.delete()
                            logger?.error("STAGE_B", "Atomic rename failed for $libName")
                            continue
                        }

                        val finalCheck = ElfValidator.validateLibrary(
                            file = destFile,
                            sourceOrigin = apkFile.name,
                            requireMinOneMb = libName == "libminecraftpe.so"
                        )
                        val resolved = attachBestLoadSource(
                            baseResult = finalCheck,
                            nativeDirFile = nativeDirFile,
                            isStoredInApk = isStoredUncompressed,
                            directApkZipPath = directApkZipPath,
                            crc32 = actualCrc
                        )
                        resultsByName[libName] = resolved
                        libCrcJson.put(libName, actualCrc)
                        logger?.success(
                            "STAGE_B",
                            "Verified $libName (${resolved.sizeBytes}B, ${resolved.machineName}, load=${resolved.loadSource.name}) from ${apkFile.name}"
                        )
                    }
                }
            } catch (t: Throwable) {
                logger?.error("STAGE_B", "Failed reading APK $apkPath: ${t.message}", t)
            }
        }

        // 2. Fallback / supplement from nativeLibraryDir if any .so was only pre-extracted by PackageManager
        for ((libName, srcLib) in nativeDirFiles) {
            if (resultsByName[libName]?.isValid == true) continue
            val destFile = File(outDir, libName)
            val tmpFile = File(outDir, "$libName.tmp")
            try {
                if (tmpFile.exists()) {
                    tmpFile.setWritable(true)
                    tmpFile.delete()
                }
                val crc = CRC32()
                FileInputStream(srcLib).use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        val buf = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                            crc.update(buf, 0, n)
                        }
                        output.flush()
                        output.fd.sync()
                    }
                }
                val actualCrc = crc.value
                val tmpCheck = ElfValidator.validateLibrary(
                    file = tmpFile,
                    sourceOrigin = "nativeLibraryDir",
                    requireMinOneMb = libName == "libminecraftpe.so"
                )
                if (tmpCheck.isValid) {
                    if (destFile.exists()) {
                        destFile.setWritable(true)
                        destFile.delete()
                    }
                    if (tmpFile.renameTo(destFile)) {
                        val finalCheck = ElfValidator.validateLibrary(
                            file = destFile,
                            sourceOrigin = "nativeLibraryDir",
                            requireMinOneMb = libName == "libminecraftpe.so"
                        ).copy(
                            loadablePath = srcLib.absolutePath,
                            loadSource = LibraryLoadSource.NATIVE_DIR,
                            crc32 = actualCrc
                        )
                        resultsByName[libName] = finalCheck
                        libCrcJson.put(libName, actualCrc)
                        logger?.success(
                            "STAGE_B",
                            "Verified $libName from nativeLibraryDir (${finalCheck.sizeBytes} bytes)"
                        )
                    } else {
                        tmpFile.delete()
                    }
                } else {
                    tmpFile.delete()
                    resultsByName[libName] = tmpCheck.copy(loadSource = LibraryLoadSource.MISSING)
                }
            } catch (t: Throwable) {
                tmpFile.delete()
                logger?.warn("STAGE_B", "Error reading $libName from nativeLibraryDir: ${t.message}")
            }
        }

        newMeta.put("crcs", libCrcJson)
        writeCacheMeta(outDir, newMeta)

        return resultsByName.values.sortedWith(
            compareByDescending<ElfInspectionResult> { it.isMainLibrary }
                .thenBy { it.fileName }
        )
    }

    private fun attachBestLoadSource(
        baseResult: ElfInspectionResult,
        nativeDirFile: File?,
        isStoredInApk: Boolean,
        directApkZipPath: String,
        crc32: Long
    ): ElfInspectionResult {
        val validNativeDir = nativeDirFile != null &&
            nativeDirFile.exists() &&
            nativeDirFile.length() == baseResult.sizeBytes
        return when {
            validNativeDir -> baseResult.copy(
                loadablePath = nativeDirFile!!.absolutePath,
                loadSource = LibraryLoadSource.NATIVE_DIR,
                crc32 = crc32,
                isUncompressedInApk = isStoredInApk
            )
            isStoredInApk -> baseResult.copy(
                loadablePath = directApkZipPath,
                loadSource = LibraryLoadSource.APK_DIRECT_ZIP,
                crc32 = crc32,
                isUncompressedInApk = true
            )
            else -> baseResult.copy(
                loadablePath = baseResult.filePath,
                loadSource = LibraryLoadSource.EXTRACTED_CACHE,
                crc32 = crc32,
                isUncompressedInApk = false
            )
        }
    }

    private fun indexNativeLibraryDir(pkg: InstalledMinecraftPackage): Map<String, File> {
        if (pkg.nativeLibraryDir.isBlank()) return emptyMap()
        val nativeDir = File(pkg.nativeLibraryDir)
        if (!nativeDir.exists() || !nativeDir.isDirectory) return emptyMap()
        return nativeDir.listFiles { f -> f.isFile && f.name.endsWith(".so") }
            .orEmpty()
            .associateBy { it.name }
    }

    fun buildCacheSummary(
        context: Context,
        pkg: InstalledMinecraftPackage,
        libraries: List<ElfInspectionResult>,
        stalePruned: Int = 0
    ): LibraryCacheSummary {
        val root = cacheRootDir(context)
        val activeDir = targetLibDir(context, pkg, selectPreferredAbi(pkg))
        val (totalBytes, fileCount) = measureDirectory(root)
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        return LibraryCacheSummary(
            cacheRootPath = root.absolutePath,
            activeCacheDirPath = activeDir.absolutePath,
            totalCacheBytes = totalBytes,
            cachedFileCount = fileCount,
            staleDirectoriesPruned = stalePruned,
            crcVerifiedCount = libraries.count { it.isValid && it.crc32 != 0L },
            directLoadableCount = libraries.count {
                it.loadSource == LibraryLoadSource.NATIVE_DIR || it.loadSource == LibraryLoadSource.APK_DIRECT_ZIP
            },
            lastVerifiedTimestamp = ts
        )
    }

    fun pruneStalePackageCaches(
        context: Context,
        pkg: InstalledMinecraftPackage,
        activeAbiDir: File,
        logger: LeviLogger? = null
    ): Int {
        val safePkg = pkg.packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val pkgRoot = File(cacheRootDir(context), safePkg)
        if (!pkgRoot.exists() || !pkgRoot.isDirectory) return 0
        val activeVersionDir = activeAbiDir.parentFile ?: return 0
        var pruned = 0
        pkgRoot.listFiles().orEmpty().forEach { child ->
            if (child.isDirectory && child.absolutePath != activeVersionDir.absolutePath) {
                makeTreeWritable(child)
                if (child.deleteRecursively()) {
                    pruned++
                }
            }
        }
        if (pruned > 0) {
            logger?.info("STAGE_B", "Pruned $pruned stale Library Cache directory(ies) for ${pkg.packageName}")
        }
        return pruned
    }

    fun clearAllCache(context: Context, logger: LeviLogger? = null): Long {
        val root = cacheRootDir(context)
        val (bytesBefore, _) = measureDirectory(root)
        if (root.exists()) {
            makeTreeWritable(root)
            root.deleteRecursively()
        }
        root.mkdirs()
        logger?.info("CACHE", "Cleared all Library Cache ($bytesBefore bytes freed)")
        return bytesBefore
    }

    fun scanApkAvailableAbis(pkg: InstalledMinecraftPackage): List<String> {
        val abis = linkedSetOf<String>()
        val apkPaths = buildList {
            if (pkg.sourceDir.isNotBlank()) add(pkg.sourceDir)
            addAll(pkg.splitSourceDirs.filter { it.isNotBlank() })
        }
        for (path in apkPaths) {
            val file = File(path)
            if (!file.exists() || !file.canRead()) continue
            runCatching {
                ZipFile(file).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val name = entries.nextElement().name
                        if (name.startsWith("lib/") && name.endsWith(".so")) {
                            val parts = name.split('/')
                            if (parts.size >= 3) {
                                abis.add(parts[1])
                            }
                        }
                    }
                }
            }
        }
        return abis.toList()
    }

    private fun computeCrc32(file: File): Long {
        val crc = CRC32()
        FileInputStream(file).use { input ->
            val buf = ByteArray(BUFFER_SIZE)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                crc.update(buf, 0, n)
            }
        }
        return crc.value
    }

    private fun readCacheMeta(dir: File): JSONObject? {
        val file = File(dir, CACHE_META_FILE)
        if (!file.exists()) return null
        return runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    private fun writeCacheMeta(dir: File, json: JSONObject) {
        runCatching {
            File(dir, CACHE_META_FILE).writeText(json.toString(2), Charsets.UTF_8)
        }
    }

    private fun makeTreeWritable(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { makeTreeWritable(it) }
        }
        file.setWritable(true)
    }

    private fun measureDirectory(dir: File): Pair<Long, Int> {
        if (!dir.exists()) return 0L to 0
        var bytes = 0L
        var count = 0
        dir.walkTopDown().forEach { f ->
            if (f.isFile && f.name != CACHE_META_FILE && !f.name.endsWith(".tmp")) {
                bytes += f.length()
                count++
            }
        }
        return bytes to count
    }
}
