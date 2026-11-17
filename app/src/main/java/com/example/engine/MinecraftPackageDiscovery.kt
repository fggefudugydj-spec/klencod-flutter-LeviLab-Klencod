package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.pm.PackageInfoCompat
import com.example.core.LeviLogger
import com.example.model.ExternalApkInspection
import com.example.model.InstalledMinecraftPackage
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object MinecraftPackageDiscovery {

    const val TARGET_VERSION_NAME = "1.26.70.24"
    const val PREVIEW_PACKAGE_NAME = "com.mojang.minecraftpe.preview"

    val CANDIDATE_PACKAGES = listOf(
        PREVIEW_PACKAGE_NAME,
        "com.mojang.minecraftpe",
        "com.mojang.minecrafttrialpe",
        "com.mojang.minecraftpe.beta"
    )

    fun discoverInstalledPackages(
        context: Context,
        logger: LeviLogger? = null
    ): List<InstalledMinecraftPackage> {
        val pm = context.packageManager
        val found = mutableListOf<InstalledMinecraftPackage>()

        logger?.info(
            "STAGE_A",
            "Scanning installed packages among: ${CANDIDATE_PACKAGES.joinToString(", ")}"
        )

        for (pkg in CANDIDATE_PACKAGES) {
            val info = getPackageInfoCompat(pm, pkg)
            if (info != null) {
                val appInfo = info.applicationInfo
                val versionName = info.versionName ?: "unknown"
                val versionCode = PackageInfoCompat.getLongVersionCode(info)
                val appLabel = appInfo?.loadLabel(pm)?.toString() ?: pkg
                val sourceDir = appInfo?.sourceDir ?: ""
                val splits = appInfo?.splitSourceDirs?.toList() ?: emptyList()
                val splitNames = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo?.splitNames?.toList() ?: emptyList()
                } else {
                    emptyList()
                }
                val nativeDir = appInfo?.nativeLibraryDir ?: ""
                val abis = Build.SUPPORTED_ABIS?.toList() ?: emptyList()
                val matchesTarget = isTargetVersionMatch(versionName)
                val extractNative = appInfo != null &&
                    (appInfo.flags and ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) != 0
                val launchActivity = resolveLaunchActivityName(pm, pkg)
                val targetSdk = appInfo?.targetSdkVersion ?: 0
                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo?.minSdkVersion ?: 0
                } else {
                    0
                }

                val item = InstalledMinecraftPackage(
                    packageName = pkg,
                    appLabel = appLabel,
                    versionName = versionName,
                    versionCode = versionCode,
                    lastUpdateTime = info.lastUpdateTime,
                    sourceDir = sourceDir,
                    splitSourceDirs = splits,
                    splitNames = splitNames,
                    nativeLibraryDir = nativeDir,
                    supportedAbis = abis,
                    matchesTargetVersion = matchesTarget,
                    isPreviewPackage = pkg == PREVIEW_PACKAGE_NAME,
                    extractNativeLibs = extractNative,
                    launchActivityName = launchActivity,
                    targetSdkVersion = targetSdk,
                    minSdkVersion = minSdk
                )
                found.add(item)
                logger?.success(
                    "STAGE_A",
                    "Found $pkg | v$versionName ($versionCode) | targetMatch=$matchesTarget | splits=${splits.size} | extractNativeLibs=$extractNative | activity=${launchActivity ?: "none"}"
                )
            } else {
                logger?.info("STAGE_A", "Package not installed: $pkg")
            }
        }

        return found
    }

    fun selectBestPackage(packages: List<InstalledMinecraftPackage>): InstalledMinecraftPackage? {
        if (packages.isEmpty()) return null
        return packages.firstOrNull { it.matchesTargetVersion && it.isPreviewPackage }
            ?: packages.firstOrNull { it.matchesTargetVersion }
            ?: packages.firstOrNull { it.isPreviewPackage }
            ?: packages.first()
    }

    fun isTargetVersionMatch(versionName: String): Boolean {
        val normalized = versionName.trim()
        return normalized == TARGET_VERSION_NAME ||
            normalized.startsWith("$TARGET_VERSION_NAME.") ||
            normalized.startsWith("$TARGET_VERSION_NAME-") ||
            normalized.contains(TARGET_VERSION_NAME)
    }

    fun resolveLaunchActivityName(pm: PackageManager, packageName: String): String? {
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        val fromLaunchIntent = launchIntent?.component?.className
        if (!fromLaunchIntent.isNullOrBlank()) return fromLaunchIntent

        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val resolved = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        }.getOrNull().orEmpty()
        return resolved.firstOrNull()?.activityInfo?.name
    }

    fun inspectExternalApk(
        context: Context,
        uri: Uri,
        logger: LeviLogger? = null
    ): ExternalApkInspection {
        val fileName = queryDisplayName(context, uri) ?: "selected.apk"
        val tempApk = File(context.cacheDir, "inspect_external_${System.currentTimeMillis()}.apk")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempApk).use { out ->
                    input.copyTo(out, 64 * 1024)
                    out.flush()
                }
            } ?: error("Cannot open APK stream")

            val fileSize = tempApk.length()
            val pm = context.packageManager
            val archiveInfo = getPackageArchiveInfoCompat(pm, tempApk.absolutePath)
            val pkgName = archiveInfo?.packageName
            val verName = archiveInfo?.versionName
            val verCode = archiveInfo?.let { PackageInfoCompat.getLongVersionCode(it) }
            val matches = verName != null && isTargetVersionMatch(verName)

            val abis = linkedSetOf<String>()
            var hasArm64Main = false
            var mainLibSize = 0L
            var mainLibValidElf64 = false

            ZipFile(tempApk).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue
                    if (entry.name.startsWith("lib/") && entry.name.endsWith(".so")) {
                        val parts = entry.name.split('/')
                        if (parts.size >= 3) {
                            abis.add(parts[1])
                        }
                        if (entry.name == "lib/arm64-v8a/libminecraftpe.so") {
                            hasArm64Main = true
                            mainLibSize = entry.size
                            val header = ByteArray(20)
                            val read = zip.getInputStream(entry).use { it.read(header) }
                            mainLibValidElf64 = read >= 20 &&
                                ElfValidator.isElfMagic(header) &&
                                (header[4].toInt() and 0xFF) == 2 &&
                                entry.size > ElfValidator.MIN_MAIN_LIBRARY_BYTES
                        }
                    }
                }
            }

            logger?.info(
                "VERSION_MGR",
                "Inspected external APK $fileName: pkg=$pkgName, v=$verName ($verCode), abis=$abis, arm64Main=$hasArm64Main"
            )

            ExternalApkInspection(
                uriString = uri.toString(),
                fileName = fileName,
                fileSizeBytes = fileSize,
                packageName = pkgName,
                versionName = verName,
                versionCode = verCode,
                matchesTargetVersion = matches,
                availableAbis = abis.toList(),
                hasArm64MainLib = hasArm64Main,
                mainLibSizeBytes = mainLibSize,
                mainLibIsValidElf64 = mainLibValidElf64
            )
        } catch (t: Throwable) {
            logger?.error("VERSION_MGR", "External APK inspection failed: ${t.message}", t)
            ExternalApkInspection(
                uriString = uri.toString(),
                fileName = fileName,
                fileSizeBytes = 0L,
                packageName = null,
                versionName = null,
                versionCode = null,
                matchesTargetVersion = false,
                availableAbis = emptyList(),
                hasArm64MainLib = false,
                mainLibSizeBytes = 0L,
                mainLibIsValidElf64 = false,
                errorMessage = "${t.javaClass.simpleName}: ${t.message}"
            )
        } finally {
            tempApk.delete()
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private fun getPackageInfoCompat(pm: PackageManager, pkg: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val flags = PackageManager.PackageInfoFlags.of(
                    (PackageManager.GET_META_DATA or PackageManager.GET_ACTIVITIES).toLong()
                )
                pm.getPackageInfo(pkg, flags)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(
                    pkg,
                    PackageManager.GET_META_DATA or PackageManager.GET_ACTIVITIES
                )
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun getPackageArchiveInfoCompat(pm: PackageManager, archivePath: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    archivePath,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(archivePath, PackageManager.GET_META_DATA)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
