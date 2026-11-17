package com.example.engine

import android.content.Context
import com.example.core.LeviLogger
import com.example.model.Elf64DynamicInfo
import com.example.model.InstalledMinecraftPackage
import com.example.model.NativeMethodDescriptor
import com.example.model.ProbeReport
import java.io.File
import java.lang.reflect.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MinecraftProbeEngine {

    private val candidateJniClasses = listOf(
        "com.mojang.minecraftpe.MainActivity",
        "com.mojang.minecraftpe.GameActivity",
        "com.google.androidgamesdk.GameActivity",
        "com.mojang.minecraftpe.HardwareInformation",
        "com.mojang.minecraftpe.store.NativeStoreListener",
        "com.mojang.minecraftpe.Webview.MinecraftWebview",
        "org.fmod.FMOD"
    )

    fun runProbe(
        context: Context,
        pkg: InstalledMinecraftPackage,
        logger: LeviLogger? = null,
        forceReextract: Boolean = false
    ): ProbeReport {
        logger?.info("PROBE", "Starting Probe on ${pkg.packageName} (v${pkg.versionName}, code=${pkg.versionCode})")

        val availableAbis = NativeLibraryManager.scanApkAvailableAbis(pkg)
        val preferredAbi = NativeLibraryManager.selectPreferredAbi(pkg, availableAbis)
        logger?.info(
            "PROBE",
            "APK ABIs found: ${availableAbis.ifEmpty { listOf("none") }.joinToString(", ")} | Selected ABI: $preferredAbi"
        )

        val discoveredLibs = NativeLibraryManager.inspectExistingOrExtract(
            context = context,
            pkg = pkg,
            logger = logger,
            forceReextract = forceReextract
        )
        val cacheSummary = NativeLibraryManager.buildCacheSummary(
            context = context,
            pkg = pkg,
            libraries = discoveredLibs
        )

        val validExtractedMap = discoveredLibs
            .filter { it.isValid }
            .associate { it.fileName to File(it.filePath) }

        val mainLibFile = validExtractedMap["libminecraftpe.so"]
        val mainDynamicInfo: Elf64DynamicInfo? = if (mainLibFile != null) {
            Elf64Parser.parseDynamicInfo(mainLibFile).also { info ->
                if (info.parseError != null) {
                    logger?.error("STAGE_C", "ELF64 DT_NEEDED parse error: ${info.parseError}")
                } else {
                    logger?.success(
                        "STAGE_C",
                        "libminecraftpe.so DT_NEEDED (${info.dtNeeded.size}): ${info.dtNeeded.joinToString(", ")}"
                    )
                }
            }
        } else {
            logger?.error("STAGE_C", "Valid libminecraftpe.so (>1MB ELF64) not found; cannot parse DT_NEEDED")
            null
        }

        val depResolution = if (validExtractedMap.isNotEmpty()) {
            Elf64Parser.resolveLoadOrder("libminecraftpe.so", validExtractedMap).also { res ->
                logger?.info(
                    "STAGE_C",
                    "Bundled load order (${res.bundledLoadOrder.size}): ${res.bundledLoadOrder.joinToString(" -> ")}"
                )
                if (res.missingDependencies.isNotEmpty()) {
                    logger?.warn(
                        "STAGE_C",
                        "Unresolved non-system dependencies: ${res.missingDependencies.joinToString(", ")}"
                    )
                }
            }
        } else {
            null
        }

        var contextCreated = false
        var contextError: String? = null
        var mainActivityClassName: String? = null
        val inheritanceChain = mutableListOf<String>()
        var gameActivityDetected = false
        var gameActivityClassName: String? = null
        val nativeMethods = mutableListOf<NativeMethodDescriptor>()
        val probedClasses = mutableListOf<String>()

        try {
            val mcContext = context.createPackageContext(
                pkg.packageName,
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )
            contextCreated = true
            val cl = mcContext.classLoader
            logger?.success("PROBE", "createPackageContext succeeded for ${pkg.packageName}")

            val mainClass = runCatching {
                cl.loadClass(pkg.launchActivityName ?: "com.mojang.minecraftpe.MainActivity")
            }.onFailure { err ->
                logger?.warn("PROBE", "Could not load MainActivity: ${err.message}")
            }.getOrNull()

            if (mainClass != null) {
                mainActivityClassName = mainClass.name
                var current: Class<*>? = mainClass
                while (current != null) {
                    val cname = current.name
                    inheritanceChain.add(cname)
                    if (cname.endsWith(".GameActivity") || cname == "com.google.androidgamesdk.GameActivity") {
                        gameActivityDetected = true
                        gameActivityClassName = cname
                    }
                    collectNativeMethods(current, nativeMethods)
                    current = current.superclass
                }
                logger?.info(
                    "PROBE",
                    "MainActivity inheritance: ${inheritanceChain.joinToString(" -> ")}"
                )
                logger?.info(
                    "PROBE",
                    "GameActivity detected=$gameActivityDetected (${gameActivityClassName ?: "none"}), native methods=${nativeMethods.size}"
                )
            }

            for (className in candidateJniClasses) {
                runCatching {
                    val cls = cl.loadClass(className)
                    probedClasses.add(cls.name)
                    if (cls.name != mainActivityClassName && cls.name !in inheritanceChain) {
                        collectNativeMethods(cls, nativeMethods)
                    }
                }
            }
        } catch (t: Throwable) {
            contextError = "${t.javaClass.simpleName}: ${t.message}"
            logger?.error("PROBE", "createPackageContext failed for ${pkg.packageName}: $contextError", t)
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return ProbeReport(
            packageInfo = pkg,
            contextCreated = contextCreated,
            contextError = contextError,
            mainActivityClass = mainActivityClassName,
            inheritanceChain = inheritanceChain,
            gameActivityDetected = gameActivityDetected,
            gameActivityClassName = gameActivityClassName,
            nativeMethods = nativeMethods.distinctBy { "${it.declaringClass}#${it.methodName}${it.signature}" },
            probedClasses = probedClasses,
            availableApkAbis = availableAbis,
            preferredAbi = preferredAbi,
            discoveredLibraries = discoveredLibs,
            mainElfDynamicInfo = mainDynamicInfo,
            dependencyResolution = depResolution,
            cacheSummary = cacheSummary,
            probeTimestamp = timestamp
        )
    }

    private fun collectNativeMethods(
        cls: Class<*>,
        out: MutableList<NativeMethodDescriptor>
    ) {
        val methods = runCatching { cls.declaredMethods }.getOrNull() ?: return
        for (method in methods) {
            if (Modifier.isNative(method.modifiers)) {
                val params = method.parameterTypes.joinToString(", ") { it.simpleName }
                val ret = method.returnType.simpleName
                out.add(
                    NativeMethodDescriptor(
                        declaringClass = cls.name,
                        methodName = method.name,
                        signature = "($params) -> $ret"
                    )
                )
            }
        }
    }
}
