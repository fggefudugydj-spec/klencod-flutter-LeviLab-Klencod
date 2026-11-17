package com.example.model

import com.example.core.AppLanguage

enum class LeviTab {
    LAUNCH,
    PROBE,
    TOOLS,
    LOGS
}

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}

enum class LaunchMode {
    DIRECT_NATIVE,
    IN_PROCESS_EXPERIMENTAL
}

enum class LibraryLoadSource {
    NATIVE_DIR,
    APK_DIRECT_ZIP,
    EXTRACTED_CACHE,
    MISSING
}

enum class GraphicsPreset {
    BATTERY_SAVER,
    BALANCED,
    COMPETITIVE_120FPS,
    ULTRA_VISUALS
}

enum class LogsSubTab {
    HISTORY,
    SESSION_LOGS
}

data class LogEntry(
    val id: Long,
    val timestamp: String,
    val level: LogLevel,
    val stage: String,
    val message: String
)

data class InstalledMinecraftPackage(
    val packageName: String,
    val appLabel: String,
    val versionName: String,
    val versionCode: Long,
    val lastUpdateTime: Long = 0L,
    val sourceDir: String,
    val splitSourceDirs: List<String>,
    val splitNames: List<String> = emptyList(),
    val nativeLibraryDir: String,
    val supportedAbis: List<String>,
    val matchesTargetVersion: Boolean,
    val isPreviewPackage: Boolean = packageName == "com.mojang.minecraftpe.preview",
    val extractNativeLibs: Boolean = true,
    val launchActivityName: String? = null,
    val targetSdkVersion: Int = 0,
    val minSdkVersion: Int = 0
)

data class ExternalApkInspection(
    val uriString: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val matchesTargetVersion: Boolean,
    val availableAbis: List<String>,
    val hasArm64MainLib: Boolean,
    val mainLibSizeBytes: Long,
    val mainLibIsValidElf64: Boolean,
    val errorMessage: String? = null
)

data class ElfInspectionResult(
    val fileName: String,
    val filePath: String,
    val loadablePath: String = filePath,
    val loadSource: LibraryLoadSource = LibraryLoadSource.EXTRACTED_CACHE,
    val crc32: Long = 0L,
    val isUncompressedInApk: Boolean = false,
    val sourceOrigin: String,
    val sizeBytes: Long,
    val hasElfMagic: Boolean,
    val isElf64: Boolean,
    val isAArch64: Boolean,
    val machineName: String,
    val meetsMinSize: Boolean,
    val isMainLibrary: Boolean,
    val isValid: Boolean,
    val validationNote: String
)

data class LibraryCacheSummary(
    val cacheRootPath: String,
    val activeCacheDirPath: String,
    val totalCacheBytes: Long,
    val cachedFileCount: Int,
    val staleDirectoriesPruned: Int,
    val crcVerifiedCount: Int,
    val directLoadableCount: Int,
    val lastVerifiedTimestamp: String
)

data class Elf64DynamicInfo(
    val libraryName: String,
    val soname: String?,
    val dtNeeded: List<String>,
    val parseError: String? = null
)

data class DependencyResolutionResult(
    val bundledLoadOrder: List<String>,
    val systemDependencies: List<String>,
    val missingDependencies: List<String>,
    val perLibraryNeeded: Map<String, List<String>>
)

data class NativeMethodDescriptor(
    val declaringClass: String,
    val methodName: String,
    val signature: String
)

data class ProbeReport(
    val packageInfo: InstalledMinecraftPackage,
    val contextCreated: Boolean,
    val contextError: String?,
    val mainActivityClass: String?,
    val inheritanceChain: List<String>,
    val gameActivityDetected: Boolean,
    val gameActivityClassName: String?,
    val nativeMethods: List<NativeMethodDescriptor>,
    val probedClasses: List<String>,
    val availableApkAbis: List<String>,
    val preferredAbi: String = "arm64-v8a",
    val discoveredLibraries: List<ElfInspectionResult>,
    val mainElfDynamicInfo: Elf64DynamicInfo?,
    val dependencyResolution: DependencyResolutionResult?,
    val cacheSummary: LibraryCacheSummary? = null,
    val probeTimestamp: String
)

enum class LaunchStepState {
    PENDING,
    RUNNING,
    PASSED,
    FAILED
}

data class LaunchStageStatus(
    val stageKey: String,
    val title: String,
    val state: LaunchStepState,
    val detail: String
)

data class InProcessLaunchFailure(
    val failedStage: String,
    val reason: String,
    val exceptionClass: String,
    val stackTracePreview: String,
    val fallbackAvailable: Boolean,
    val fallbackTriggered: Boolean,
    val timestamp: String
)

data class LaunchHistoryEntry(
    val id: String,
    val timestamp: String,
    val packageName: String,
    val versionName: String,
    val mode: LaunchMode,
    val succeeded: Boolean,
    val usedFallback: Boolean,
    val mainLibLoadSource: String,
    val summary: String
)

data class MinecraftGraphicsOptions(
    val fileUriString: String? = null,
    val fileName: String = "options.txt",
    val renderDistanceChunks: Int = 12,
    val maxFramerate: Int = 60,
    val fancyGraphics: Boolean = true,
    val smoothLighting: Boolean = true,
    val fancySkies: Boolean = true,
    val vsyncEnabled: Boolean = true,
    val msaaSamples: Int = 2,
    val fieldOfView: Float = 60f,
    val deferredRendering: Boolean = false,
    val uiProfileClassic: Boolean = false,
    val rawLineCount: Int = 0,
    val rawLines: List<String> = emptyList()
)

data class SavedServer(
    val id: String,
    val label: String,
    val host: String,
    val port: Int = 19132
)

data class ServerPingResult(
    val serverId: String,
    val isOnline: Boolean,
    val motd: String,
    val subMotd: String,
    val versionName: String,
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val gameMode: String,
    val latencyMs: Long,
    val errorMessage: String? = null
)

data class WorldFolderSummary(
    val treeUriString: String,
    val displayName: String,
    val levelName: String,
    val fileCount: Int,
    val totalBytes: Long,
    val hasLevelDat: Boolean
)

data class RestorableMcWorldInfo(
    val fileUriString: String,
    val fileName: String,
    val levelName: String,
    val entryCount: Int,
    val uncompressedBytes: Long,
    val hasLevelDat: Boolean
)

data class McPackManifestInfo(
    val fileUriString: String,
    val fileName: String,
    val packName: String,
    val packDescription: String,
    val packUuid: String,
    val packVersion: String,
    val packType: String
)

data class LeviUiState(
    val language: AppLanguage = AppLanguage.AR,
    val currentTab: LeviTab = LeviTab.LAUNCH,
    val logsSubTab: LogsSubTab = LogsSubTab.HISTORY,
    val preferredLaunchMode: LaunchMode = LaunchMode.DIRECT_NATIVE,
    val autoVerifyCacheBeforeLaunch: Boolean = true,
    val preferDirectApkNativeLoad: Boolean = true,
    val isScanningPackages: Boolean = false,
    val isProbing: Boolean = false,
    val isExtracting: Boolean = false,
    val isRepairing: Boolean = false,
    val isClearingCache: Boolean = false,
    val isInspectingExternalApk: Boolean = false,
    val installedPackages: List<InstalledMinecraftPackage> = emptyList(),
    val selectedPackage: InstalledMinecraftPackage? = null,
    val externalApkInspection: ExternalApkInspection? = null,
    val probeReport: ProbeReport? = null,
    val extractedLibraries: List<ElfInspectionResult> = emptyList(),
    val cacheSummary: LibraryCacheSummary? = null,
    val lastLaunchFailure: InProcessLaunchFailure? = null,
    val launchHistory: List<LaunchHistoryEntry> = emptyList(),
    val graphicsOptions: MinecraftGraphicsOptions = MinecraftGraphicsOptions(),
    val isLoadingGraphicsOptions: Boolean = false,
    val graphicsStatusMessage: String? = null,
    val savedServers: List<SavedServer> = emptyList(),
    val serverPingResults: Map<String, ServerPingResult> = emptyMap(),
    val pingingServerIds: Set<String> = emptySet(),
    val selectedWorldFolder: WorldFolderSummary? = null,
    val selectedRestoreWorldFile: RestorableMcWorldInfo? = null,
    val isScanningWorld: Boolean = false,
    val isBackingUpWorld: Boolean = false,
    val isRestoringWorld: Boolean = false,
    val worldBackupStatusMessage: String? = null,
    val selectedMcPack: McPackManifestInfo? = null,
    val isInspectingPack: Boolean = false,
    val packImportStatusMessage: String? = null,
    val logs: List<LogEntry> = emptyList(),
    val logFilePath: String = "",
    val logLevelFilter: LogLevel? = null,
    val deviceAbiList: List<String> = emptyList(),
    val androidVersionLabel: String = "",
    val statusBannerMessage: String? = null
)
