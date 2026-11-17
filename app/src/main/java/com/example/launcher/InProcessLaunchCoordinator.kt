package com.example.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import com.example.core.LeviLogger
import com.example.core.LeviStrings
import com.example.engine.MinecraftPackageDiscovery
import com.example.engine.MinecraftProbeEngine
import com.example.model.InProcessLaunchFailure
import com.example.model.InstalledMinecraftPackage
import com.example.model.LaunchHistoryEntry
import com.example.model.LaunchMode
import com.example.model.LaunchStageStatus
import com.example.model.LaunchStepState
import com.example.model.LibraryLoadSource
import com.example.model.ProbeReport
import dalvik.system.BaseDexClassLoader
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class LaunchExecutionOutcome(
    val mode: LaunchMode,
    val stages: List<LaunchStageStatus>,
    val targetPackage: InstalledMinecraftPackage?,
    val probeReport: ProbeReport?,
    val loadedLibraries: List<String>,
    val mainLibLoadSource: String,
    val gameLaunchedExternally: Boolean,
    val failure: InProcessLaunchFailure?,
    val historyEntry: LaunchHistoryEntry?
)

object InProcessLaunchCoordinator {

    fun executeDirectNativeLaunch(
        activityContext: Context,
        preferredPackageName: String?,
        verifyCacheBeforeLaunch: Boolean,
        strings: LeviStrings,
        logger: LeviLogger,
        onStageUpdate: (List<LaunchStageStatus>) -> Unit
    ): LaunchExecutionOutcome {
        val stages = mutableListOf(
            LaunchStageStatus("A", strings.stageALabel, LaunchStepState.RUNNING, "..."),
            LaunchStageStatus("B", strings.stageBLabel, LaunchStepState.PENDING, ""),
            LaunchStageStatus("C", strings.stageCLabel, LaunchStepState.PENDING, ""),
            LaunchStageStatus("D", strings.stageDLabel, LaunchStepState.PENDING, ""),
            LaunchStageStatus("E", strings.stageELabel, LaunchStepState.PENDING, "")
        )
        onStageUpdate(stages.toList())

        fun updateStage(index: Int, state: LaunchStepState, detail: String) {
            stages[index] = stages[index].copy(state = state, detail = detail)
            onStageUpdate(stages.toList())
        }

        // Stage A: Discover installed Minecraft APK / Split APKs
        val discovered = MinecraftPackageDiscovery.discoverInstalledPackages(activityContext, logger)
        val targetPkg = if (!preferredPackageName.isNullOrBlank()) {
            discovered.firstOrNull { it.packageName == preferredPackageName }
                ?: MinecraftPackageDiscovery.selectBestPackage(discovered)
        } else {
            MinecraftPackageDiscovery.selectBestPackage(discovered)
        }

        if (targetPkg == null) {
            val reason = "No supported Minecraft package found among: ${MinecraftPackageDiscovery.CANDIDATE_PACKAGES.joinToString(", ")}"
            updateStage(0, LaunchStepState.FAILED, reason)
            val failure = buildFailure(strings.stageALabel, reason, null, false)
            return LaunchExecutionOutcome(
                mode = LaunchMode.DIRECT_NATIVE,
                stages = stages.toList(),
                targetPackage = null,
                probeReport = null,
                loadedLibraries = emptyList(),
                mainLibLoadSource = LibraryLoadSource.MISSING.name,
                gameLaunchedExternally = false,
                failure = failure,
                historyEntry = buildHistoryEntry(
                    pkg = null,
                    mode = LaunchMode.DIRECT_NATIVE,
                    succeeded = false,
                    usedFallback = false,
                    mainSource = LibraryLoadSource.MISSING.name,
                    summary = reason
                )
            )
        }

        val splitTag = if (targetPkg.splitSourceDirs.isNotEmpty()) {
            "SplitAPK(${targetPkg.splitSourceDirs.size})"
        } else {
            "SingleAPK"
        }
        updateStage(
            0,
            LaunchStepState.PASSED,
            "${targetPkg.packageName} v${targetPkg.versionName} (${targetPkg.versionCode}) • $splitTag"
        )

        // Stage B & C: Verify libminecraftpe.so, Library Cache & DT_NEEDED
        var probeReport: ProbeReport? = null
        var mainLibSource = LibraryLoadSource.NATIVE_DIR.name

        if (verifyCacheBeforeLaunch) {
            updateStage(1, LaunchStepState.RUNNING, "Checking libminecraftpe.so & Library Cache...")
            val probe = MinecraftProbeEngine.runProbe(
                context = activityContext.applicationContext,
                pkg = targetPkg,
                logger = logger,
                forceReextract = false
            )
            probeReport = probe
            LaunchDiagnosticsBus.publishProbe(probe)

            val mainLib = probe.discoveredLibraries.firstOrNull { it.fileName == "libminecraftpe.so" && it.isValid }
            if (mainLib == null) {
                val note = probe.discoveredLibraries
                    .firstOrNull { it.fileName == "libminecraftpe.so" }
                    ?.validationNote ?: "libminecraftpe.so not found in Base/Split APK or nativeLibraryDir"
                updateStage(1, LaunchStepState.FAILED, note)
                val failure = buildFailure(strings.stageBLabel, note, null, true)
                return LaunchExecutionOutcome(
                    mode = LaunchMode.DIRECT_NATIVE,
                    stages = stages.toList(),
                    targetPackage = targetPkg,
                    probeReport = probe,
                    loadedLibraries = emptyList(),
                    mainLibLoadSource = LibraryLoadSource.MISSING.name,
                    gameLaunchedExternally = false,
                    failure = failure,
                    historyEntry = buildHistoryEntry(
                        pkg = targetPkg,
                        mode = LaunchMode.DIRECT_NATIVE,
                        succeeded = false,
                        usedFallback = false,
                        mainSource = LibraryLoadSource.MISSING.name,
                        summary = note
                    )
                )
            }
            mainLibSource = mainLib.loadSource.name
            updateStage(
                1,
                LaunchStepState.PASSED,
                "libminecraftpe.so (${mainLib.sizeBytes / (1024 * 1024)} MB, ${mainLib.machineName}) • ${mainLib.loadSource.name}"
            )

            val depRes = probe.dependencyResolution
            if (depRes != null) {
                updateStage(
                    2,
                    LaunchStepState.PASSED,
                    "${depRes.bundledLoadOrder.size} bundled ELF64 libs verified (missing=${depRes.missingDependencies.size})"
                )
            } else {
                updateStage(2, LaunchStepState.PASSED, "ELF64 header verified")
            }
        } else {
            updateStage(1, LaunchStepState.PASSED, "Fast-launch mode (cache pre-check skipped)")
            updateStage(2, LaunchStepState.PASSED, "Using installed APK linker namespace")
        }

        // Stage D: Resolve launch Activity ComponentName
        updateStage(3, LaunchStepState.RUNNING, "Resolving installed Minecraft Activity...")
        val launchIntent = buildLaunchIntentForPackage(activityContext, targetPkg)
        if (launchIntent == null) {
            val reason = "No launchable Activity resolved for ${targetPkg.packageName}"
            updateStage(3, LaunchStepState.FAILED, reason)
            val failure = buildFailure(strings.stageDLabel, reason, null, false)
            return LaunchExecutionOutcome(
                mode = LaunchMode.DIRECT_NATIVE,
                stages = stages.toList(),
                targetPackage = targetPkg,
                probeReport = probeReport,
                loadedLibraries = emptyList(),
                mainLibLoadSource = mainLibSource,
                gameLaunchedExternally = false,
                failure = failure,
                historyEntry = buildHistoryEntry(
                    pkg = targetPkg,
                    mode = LaunchMode.DIRECT_NATIVE,
                    succeeded = false,
                    usedFallback = false,
                    mainSource = mainLibSource,
                    summary = reason
                )
            )
        }
        updateStage(
            3,
            LaunchStepState.PASSED,
            "Target Activity: ${launchIntent.component?.className ?: targetPkg.packageName}"
        )

        // Stage E: Launch the installed Minecraft Preview APK itself
        updateStage(4, LaunchStepState.RUNNING, "Starting ${targetPkg.packageName}...")
        return try {
            activityContext.startActivity(launchIntent)
            val msg = "Launched ${targetPkg.packageName} v${targetPkg.versionName} (${launchIntent.component?.shortClassName ?: "MainActivity"})"
            logger.success("LAUNCH", msg)
            updateStage(4, LaunchStepState.PASSED, msg)
            val history = buildHistoryEntry(
                pkg = targetPkg,
                mode = LaunchMode.DIRECT_NATIVE,
                succeeded = true,
                usedFallback = false,
                mainSource = mainLibSource,
                summary = msg
            )
            LaunchDiagnosticsBus.publishFailure(null)
            LaunchExecutionOutcome(
                mode = LaunchMode.DIRECT_NATIVE,
                stages = stages.toList(),
                targetPackage = targetPkg,
                probeReport = probeReport,
                loadedLibraries = emptyList(),
                mainLibLoadSource = mainLibSource,
                gameLaunchedExternally = true,
                failure = null,
                historyEntry = history
            )
        } catch (t: Throwable) {
            val reason = "startActivity failed: ${t.javaClass.simpleName}: ${t.message}"
            updateStage(4, LaunchStepState.FAILED, reason)
            val failure = buildFailure(strings.stageELabel, reason, t, false)
            LaunchDiagnosticsBus.publishFailure(failure)
            LaunchExecutionOutcome(
                mode = LaunchMode.DIRECT_NATIVE,
                stages = stages.toList(),
                targetPackage = targetPkg,
                probeReport = probeReport,
                loadedLibraries = emptyList(),
                mainLibLoadSource = mainLibSource,
                gameLaunchedExternally = false,
                failure = failure,
                historyEntry = buildHistoryEntry(
                    pkg = targetPkg,
                    mode = LaunchMode.DIRECT_NATIVE,
                    succeeded = false,
                    usedFallback = false,
                    mainSource = mainLibSource,
                    summary = reason
                )
            )
        }
    }

    fun executeInProcessSequence(
        context: Context,
        preferredPackageName: String?,
        preferDirectApkNativeLoad: Boolean,
        strings: LeviStrings,
        logger: LeviLogger,
        onStageUpdate: (List<LaunchStageStatus>) -> Unit
    ): LaunchExecutionOutcome {
        val stages = mutableListOf(
            LaunchStageStatus("A", strings.stageALabel, LaunchStepState.RUNNING, "..."),
            LaunchStageStatus("B", strings.stageBLabel, LaunchStepState.PENDING, ""),
            LaunchStageStatus("C", strings.stageCLabel, LaunchStepState.PENDING, ""),
            LaunchStageStatus("D", strings.stageDLabel, LaunchStepState.PENDING, ""),
            LaunchStageStatus("E", strings.stageELabel, LaunchStepState.PENDING, "")
        )
        onStageUpdate(stages.toList())

        fun updateStage(index: Int, state: LaunchStepState, detail: String) {
            stages[index] = stages[index].copy(state = state, detail = detail)
            onStageUpdate(stages.toList())
        }

        fun failOutcome(
            stageIndex: Int,
            stageName: String,
            reason: String,
            throwable: Throwable? = null,
            pkg: InstalledMinecraftPackage? = null,
            probe: ProbeReport? = null,
            loadedLibs: List<String> = emptyList(),
            mainSource: String = LibraryLoadSource.MISSING.name
        ): LaunchExecutionOutcome {
            updateStage(stageIndex, LaunchStepState.FAILED, reason)
            logger.error("STAGE_${stages[stageIndex].stageKey}", "$stageName failed: $reason", throwable)

            val launchIntent = pkg?.let { buildLaunchIntentForPackage(context, it) }
            val fallbackAvail = launchIntent != null
            updateStage(
                4,
                if (fallbackAvail) LaunchStepState.PASSED else LaunchStepState.FAILED,
                if (fallbackAvail) {
                    "Installed ${pkg?.packageName} Activity ready for direct launch"
                } else {
                    "No launchable Activity found"
                }
            )

            val failure = buildFailure(stageName, reason, throwable, fallbackAvail)
            LaunchDiagnosticsBus.publishFailure(failure)
            return LaunchExecutionOutcome(
                mode = LaunchMode.IN_PROCESS_EXPERIMENTAL,
                stages = stages.toList(),
                targetPackage = pkg,
                probeReport = probe,
                loadedLibraries = loadedLibs,
                mainLibLoadSource = mainSource,
                gameLaunchedExternally = false,
                failure = failure,
                historyEntry = buildHistoryEntry(
                    pkg = pkg,
                    mode = LaunchMode.IN_PROCESS_EXPERIMENTAL,
                    succeeded = false,
                    usedFallback = false,
                    mainSource = mainSource,
                    summary = reason
                )
            )
        }

        // Stage A: Discover installed packages
        val discovered = MinecraftPackageDiscovery.discoverInstalledPackages(context, logger)
        val targetPkg = if (!preferredPackageName.isNullOrBlank()) {
            discovered.firstOrNull { it.packageName == preferredPackageName }
                ?: MinecraftPackageDiscovery.selectBestPackage(discovered)
        } else {
            MinecraftPackageDiscovery.selectBestPackage(discovered)
        }

        if (targetPkg == null) {
            return failOutcome(
                stageIndex = 0,
                stageName = strings.stageALabel,
                reason = "No supported Minecraft package found among: ${MinecraftPackageDiscovery.CANDIDATE_PACKAGES.joinToString(", ")}"
            )
        }

        updateStage(
            0,
            LaunchStepState.PASSED,
            "${targetPkg.packageName} (v${targetPkg.versionName}, code=${targetPkg.versionCode}, match=${targetPkg.matchesTargetVersion})"
        )

        // Stage B: Probe + Native Libs + Library Cache validation
        updateStage(1, LaunchStepState.RUNNING, "Verifying libminecraftpe.so, Split APKs & Library Cache...")
        val probe = MinecraftProbeEngine.runProbe(
            context = context,
            pkg = targetPkg,
            logger = logger,
            forceReextract = false
        )
        LaunchDiagnosticsBus.publishProbe(probe)

        val validLibs = probe.discoveredLibraries.filter { it.isValid }
        val mainLib = validLibs.firstOrNull { it.fileName == "libminecraftpe.so" }
        if (mainLib == null) {
            val mainFailureNote = probe.discoveredLibraries
                .firstOrNull { it.fileName == "libminecraftpe.so" }
                ?.validationNote ?: "libminecraftpe.so not found in APK, splits, or nativeLibraryDir"
            return failOutcome(
                stageIndex = 1,
                stageName = strings.stageBLabel,
                reason = mainFailureNote,
                pkg = targetPkg,
                probe = probe
            )
        }

        updateStage(
            1,
            LaunchStepState.PASSED,
            "Verified ${validLibs.size} ELF64 libs (libminecraftpe.so = ${mainLib.sizeBytes / (1024 * 1024)} MB via ${mainLib.loadSource.name})"
        )

        // Stage C: Parse DT_NEEDED & resolve topological load order + linker path
        updateStage(2, LaunchStepState.RUNNING, "Resolving DT_NEEDED & linker paths...")
        val depRes = probe.dependencyResolution
        if (depRes == null || depRes.bundledLoadOrder.isEmpty()) {
            val parseErr = probe.mainElfDynamicInfo?.parseError ?: "No DT_NEEDED resolution available"
            return failOutcome(
                stageIndex = 2,
                stageName = strings.stageCLabel,
                reason = parseErr,
                pkg = targetPkg,
                probe = probe,
                mainSource = mainLib.loadSource.name
            )
        }

        // Prepare Stage D context & bridge before System.load
        updateStage(3, LaunchStepState.RUNNING, "Preparing PackageContext, AssetManager & ClassLoader...")
        val mcContext = try {
            context.createPackageContext(
                targetPkg.packageName,
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )
        } catch (t: Throwable) {
            return failOutcome(
                stageIndex = 3,
                stageName = strings.stageDLabel,
                reason = "createPackageContext failed: ${t.javaClass.simpleName}: ${t.message}",
                throwable = t,
                pkg = targetPkg,
                probe = probe,
                mainSource = mainLib.loadSource.name
            )
        }

        // Attempt AssetManager mounting for Base + Split APKs
        val mountedAssets = attemptMountApkAssets(targetPkg, logger)
        // Attempt ClassLoader DEX + NativeLibDir bridge
        val extraNativeDirs = buildList {
            if (targetPkg.nativeLibraryDir.isNotBlank()) add(File(targetPkg.nativeLibraryDir))
            validLibs.firstOrNull()?.filePath?.let { File(it).parentFile }?.let { add(it) }
        }.distinct()
        val bridgeError = attemptClassLoaderBridge(
            appClassLoader = context.classLoader,
            mcClassLoader = mcContext.classLoader,
            extraNativeDirs = extraNativeDirs,
            pkg = targetPkg,
            logger = logger
        )

        // Load bundled libraries in topological DT_NEEDED order using best loadablePath
        val libsByName = validLibs.associateBy { it.fileName }
        val loadedLibs = mutableListOf<String>()
        for (libName in depRes.bundledLoadOrder) {
            val libEntry = libsByName[libName]
                ?: return failOutcome(
                    stageIndex = 2,
                    stageName = strings.stageCLabel,
                    reason = "Library $libName listed in load order was not found on disk",
                    pkg = targetPkg,
                    probe = probe,
                    loadedLibs = loadedLibs,
                    mainSource = mainLib.loadSource.name
                )

            val candidatePaths = if (preferDirectApkNativeLoad) {
                listOf(libEntry.loadablePath, libEntry.filePath).distinct()
            } else {
                listOf(libEntry.filePath, libEntry.loadablePath).distinct()
            }

            var lastLoadError: Throwable? = null
            var loadedThis = false
            for (path in candidatePaths) {
                try {
                    logger.info("STAGE_C", "System.load($path)")
                    @Suppress("UnsafeDynamicallyLoadedCode")
                    System.load(path)
                    loadedLibs.add(libName)
                    logger.success("STAGE_C", "Loaded $libName from $path")
                    loadedThis = true
                    break
                } catch (t: Throwable) {
                    lastLoadError = t
                    logger.warn("STAGE_C", "Load attempt failed for $libName at $path: ${t.message}")
                }
            }

            if (!loadedThis) {
                val targetStageIndex = if (libName == "libminecraftpe.so") 3 else 2
                val targetStageName = if (libName == "libminecraftpe.so") strings.stageDLabel else strings.stageCLabel
                return failOutcome(
                    stageIndex = targetStageIndex,
                    stageName = targetStageName,
                    reason = "Failed loading $libName across ${candidatePaths.size} paths: ${lastLoadError?.javaClass?.simpleName}: ${lastLoadError?.message}",
                    throwable = lastLoadError,
                    pkg = targetPkg,
                    probe = probe,
                    loadedLibs = loadedLibs,
                    mainSource = mainLib.loadSource.name
                )
            }
        }

        updateStage(
            2,
            LaunchStepState.PASSED,
            "Loaded ${loadedLibs.size} libs in DT_NEEDED order (assetsMounted=$mountedAssets)"
        )

        // Stage D: Verify GameActivity & attempt safe MainActivity instantiation check
        val gameActivityClass = probe.gameActivityClassName
        if (!probe.gameActivityDetected || gameActivityClass == null) {
            return failOutcome(
                stageIndex = 3,
                stageName = strings.stageDLabel,
                reason = "Probe did not detect GameActivity in MainActivity chain (${probe.inheritanceChain.joinToString(" -> ")})",
                pkg = targetPkg,
                probe = probe,
                loadedLibs = loadedLibs,
                mainSource = mainLib.loadSource.name
            )
        }

        val resolvedMainCls = runCatching {
            mcContext.classLoader.loadClass(targetPkg.launchActivityName ?: "com.mojang.minecraftpe.MainActivity")
        }.getOrNull()

        val reason = buildString {
            append("Loaded ${loadedLibs.size} native libraries (main=${mainLib.loadSource.name}) ")
            append("and resolved ${resolvedMainCls?.name ?: gameActivityClass} (${probe.nativeMethods.size} JNI methods, assetsMounted=$mountedAssets). ")
            if (bridgeError != null) {
                append("Android 13 ClassLoader/Hidden-API bridge note: $bridgeError. ")
            }
            append("Android OS isolates ${targetPkg.packageName}'s ActivityThread, AMS window token, and C++ GameActivity mNativeHandle from external app processes; launching the installed package directly ensures full Main Menu, Xbox Live, and Worlds access.")
        }

        return failOutcome(
            stageIndex = 3,
            stageName = strings.stageDLabel,
            reason = reason,
            pkg = targetPkg,
            probe = probe,
            loadedLibs = loadedLibs,
            mainSource = mainLib.loadSource.name
        )
    }

    private fun attemptMountApkAssets(pkg: InstalledMinecraftPackage, logger: LeviLogger): Boolean {
        return try {
            val am = AssetManager::class.java.getDeclaredConstructor().newInstance()
            val addAssetPath = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
            var mounted = 0
            if (pkg.sourceDir.isNotBlank()) {
                val cookie = addAssetPath.invoke(am, pkg.sourceDir) as? Int ?: 0
                if (cookie != 0) mounted++
            }
            for (split in pkg.splitSourceDirs) {
                val cookie = addAssetPath.invoke(am, split) as? Int ?: 0
                if (cookie != 0) mounted++
            }
            am.close()
            logger.info("STAGE_D", "Mounted $mounted APK asset container(s) via AssetManager.addAssetPath")
            mounted > 0
        } catch (t: Throwable) {
            logger.warn("STAGE_D", "AssetManager.addAssetPath unavailable: ${t.message}")
            false
        }
    }

    private fun attemptClassLoaderBridge(
        appClassLoader: ClassLoader,
        mcClassLoader: ClassLoader,
        extraNativeDirs: List<File>,
        pkg: InstalledMinecraftPackage,
        logger: LeviLogger
    ): String? {
        return try {
            if (appClassLoader !is BaseDexClassLoader || mcClassLoader !is BaseDexClassLoader) {
                return "ClassLoaders are not BaseDexClassLoader"
            }
            val pathListField = BaseDexClassLoader::class.java.getDeclaredField("pathList")
            pathListField.isAccessible = true
            val appPathList = pathListField.get(appClassLoader)
            val mcPathList = pathListField.get(mcClassLoader)

            val dexElementsField = appPathList.javaClass.getDeclaredField("dexElements")
            dexElementsField.isAccessible = true
            val appElements = dexElementsField.get(appPathList) as Array<*>
            val mcElements = dexElementsField.get(mcPathList) as Array<*>

            val elementClass = appElements.javaClass.componentType
                ?: return "Missing dexElements componentType"
            val combined = java.lang.reflect.Array.newInstance(
                elementClass,
                appElements.size + mcElements.size
            ) as Array<Any?>
            System.arraycopy(appElements, 0, combined, 0, appElements.size)
            System.arraycopy(mcElements, 0, combined, appElements.size, mcElements.size)
            dexElementsField.set(appPathList, combined)

            // Also attempt to add native library directories for DT_NEEDED sibling lookup
            runCatching {
                val nativeDirsField = appPathList.javaClass.getDeclaredField("nativeLibraryDirectories")
                nativeDirsField.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val existingDirs = (nativeDirsField.get(appPathList) as? List<File>).orEmpty()
                val updatedDirs = (extraNativeDirs + existingDirs).distinct()
                nativeDirsField.set(appPathList, updatedDirs)
            }

            logger.success(
                "STAGE_D",
                "Merged ${mcElements.size} DEX elements & ${extraNativeDirs.size} native dirs from ${pkg.packageName}"
            )
            null
        } catch (t: Throwable) {
            val msg = "${t.javaClass.simpleName}: ${t.message}"
            logger.warn("STAGE_D", "ClassLoader bridge restricted by Android 13 hidden-API policy: $msg")
            msg
        }
    }

    fun buildLaunchIntentForPackage(context: Context, pkg: InstalledMinecraftPackage): Intent? {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(pkg.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            return launchIntent
        }
        val activityName = pkg.launchActivityName
            ?: MinecraftPackageDiscovery.resolveLaunchActivityName(pm, pkg.packageName)
            ?: return null
        return Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(pkg.packageName, activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    }

    fun launchInstalledPackage(
        context: Context,
        pkg: InstalledMinecraftPackage,
        logger: LeviLogger
    ): Boolean {
        val intent = buildLaunchIntentForPackage(context, pkg)
        return if (intent != null) {
            logger.success(
                "STAGE_E",
                "Launching installed ${pkg.packageName} (${intent.component?.className ?: "default"})"
            )
            context.startActivity(intent)
            true
        } else {
            logger.error("STAGE_E", "Could not resolve launch intent for ${pkg.packageName}")
            false
        }
    }

    fun launchInstalledFallback(context: Context, packageName: String, logger: LeviLogger): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            logger.info("STAGE_E", "Launching installed package via getLaunchIntentForPackage($packageName)")
            context.startActivity(intent)
            true
        } else {
            logger.error("STAGE_E", "getLaunchIntentForPackage($packageName) returned null")
            false
        }
    }

    private fun buildFailure(
        stageName: String,
        reason: String,
        throwable: Throwable?,
        fallbackAvailable: Boolean
    ): InProcessLaunchFailure {
        val trace = if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            sw.toString().lines().take(12).joinToString("\n")
        } else {
            reason
        }
        val exClass = throwable?.javaClass?.name ?: "LaunchDiagnosticNotice"
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return InProcessLaunchFailure(
            failedStage = stageName,
            reason = reason,
            exceptionClass = exClass,
            stackTracePreview = trace,
            fallbackAvailable = fallbackAvailable,
            fallbackTriggered = false,
            timestamp = ts
        )
    }

    fun buildHistoryEntry(
        pkg: InstalledMinecraftPackage?,
        mode: LaunchMode,
        succeeded: Boolean,
        usedFallback: Boolean,
        mainSource: String,
        summary: String
    ): LaunchHistoryEntry {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return LaunchHistoryEntry(
            id = UUID.randomUUID().toString(),
            timestamp = ts,
            packageName = pkg?.packageName ?: "none",
            versionName = pkg?.versionName ?: "unknown",
            mode = mode,
            succeeded = succeeded,
            usedFallback = usedFallback,
            mainLibLoadSource = mainSource,
            summary = summary
        )
    }
}
