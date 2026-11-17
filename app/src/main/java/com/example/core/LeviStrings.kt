package com.example.core

enum class AppLanguage {
    AR,
    EN
}

interface LeviStrings {
    val appTitle: String
    val targetVersionTag: String
    val tabLaunch: String
    val tabProbe: String
    val tabTools: String
    val tabLogs: String
    val toggleLanguageLabel: String
    val shareDiagnosticsLabel: String

    val deviceSummaryTitle: String
    val abiLabel: String
    val androidOsLabel: String
    val discoveredPackagesTitle: String
    val noMinecraftInstalledTitle: String
    val noMinecraftInstalledDesc: String
    val rescanPackagesButton: String
    val packageVersionLabel: String
    val versionCodeLabel: String
    val targetMatchExact: String
    val targetMatchMismatch: String
    val splitApkBadge: String
    val singleApkBadge: String
    val previewBadge: String
    val launchPrimaryButton: String
    val launchInProcessButton: String
    val launchFallbackButton: String
    val extractAndVerifyButton: String
    val inspectExternalApkButton: String
    val externalApkCardTitle: String
    val installExternalApkButton: String

    val gameHealthTitle: String
    val repairReextractButton: String
    val clearCacheButton: String
    val repairingStatus: String
    val noLibrariesExtracted: String
    val elfValidBadge: String
    val elfInvalidBadge: String
    val sizeOverOneMbBadge: String
    val sizeUnderOneMbBadge: String
    val architectureLabel: String
    val sourceOriginLabel: String
    val loadSourceLabel: String
    val cacheSummaryTitle: String
    val cacheSizeLabel: String
    val cacheDirectLoadLabel: String
    val cacheCrcVerifiedLabel: String

    val lastFailureBannerTitle: String
    val fallbackExplanationPrefix: String
    val dismissBanner: String

    val probeTitle: String
    val probeSubtitle: String
    val runProbeButton: String
    val probingInProgress: String
    val probeAutoNotice: String
    val probeSelectPackage: String
    val probeContextStatusTitle: String
    val probeContextSuccess: String
    val probeContextFailed: String
    val probeInheritanceTitle: String
    val probeNativeMethodsTitle: String
    val probeNoNativeMethods: String
    val probeDtNeededTitle: String
    val probeLoadOrderTitle: String
    val probeSystemDepsLabel: String
    val probeMissingDepsLabel: String
    val probeApkPathsTitle: String

    val toolsTitle: String
    val graphicsTitle: String
    val graphicsDesc: String
    val loadOptionsTxtButton: String
    val saveOptionsTxtButton: String
    val exportOptionsTxtButton: String
    val presetBattery: String
    val presetBalanced: String
    val presetCompetitive: String
    val presetUltra: String
    val renderDistanceLabel: String
    val maxFramerateLabel: String
    val msaaLabel: String
    val fovLabel: String
    val fancyGraphicsLabel: String
    val smoothLightingLabel: String
    val fancySkiesLabel: String
    val vsyncLabel: String
    val deferredRenderingLabel: String
    val launcherCompatTitle: String
    val autoVerifyCacheLabel: String
    val preferDirectApkLoadLabel: String

    val worldBackupTitle: String
    val worldBackupDesc: String
    val pickWorldFolderButton: String
    val exportMcWorldButton: String
    val pickRestoreMcWorldButton: String
    val restoreToFolderButton: String
    val openWorldInMinecraftButton: String
    val worldNameLabel: String
    val worldFilesLabel: String
    val levelDatFound: String
    val levelDatMissing: String
    val mcPackImportTitle: String
    val mcPackImportDesc: String
    val pickMcPackButton: String
    val openInMinecraftButton: String
    val packNameLabel: String
    val packTypeLabel: String
    val packVersionLabel: String

    val serverPingerTitle: String
    val serverPingerDesc: String
    val serverNameHint: String
    val serverHostHint: String
    val serverPortHint: String
    val addServerButton: String
    val pingAllButton: String
    val pingButton: String
    val deleteServerDesc: String
    val serverOnline: String
    val serverOffline: String
    val serverPlayersLabel: String
    val serverLatencyLabel: String
    val noServersSaved: String

    val logsTitle: String
    val subTabHistory: String
    val subTabSessionLogs: String
    val noLaunchHistoryYet: String
    val clearHistoryButton: String
    val logsFilePathLabel: String
    val filterAll: String
    val filterErrors: String
    val copyLogsButton: String
    val shareReportButton: String
    val clearLogsButton: String
    val noLogsYet: String
    val copiedToast: String

    val embeddedHostTitle: String
    val embeddedDirectHostTitle: String
    val embeddedRunningStages: String
    val embeddedSuccessHint: String
    val embeddedDirectSuccessHint: String
    val embeddedFailureTitle: String
    val embeddedFallbackMessage: String
    val embeddedOpenExternalButton: String
    val embeddedBackToLabButton: String
    val embeddedShareReportButton: String
    val stageALabel: String
    val stageBLabel: String
    val stageCLabel: String
    val stageDLabel: String
    val stageELabel: String
}

object ArStrings : LeviStrings {
    override val appTitle = "LeviLab"
    override val targetVersionTag = "الهدف: v1.26.70.24"
    override val tabLaunch = "التشغيل"
    override val tabProbe = "الفحص والمكتبات"
    override val tabTools = "الرسوم والأدوات"
    override val tabLogs = "السجل"
    override val toggleLanguageLabel = "EN"
    override val shareDiagnosticsLabel = "مشاركة التقرير"

    override val deviceSummaryTitle = "بيئة الجهاز والهدف"
    override val abiLabel = "المعمارية"
    override val androidOsLabel = "نظام أندرويد"
    override val discoveredPackagesTitle = "مدير إصدارات ماينكرافت المثبّتة"
    override val noMinecraftInstalledTitle = "لم يتم العثور على ماينكرافت مثبّتة"
    override val noMinecraftInstalledDesc =
        "تأكد من تثبيت إحدى الحزم: com.mojang.minecraftpe.preview أو com.mojang.minecraftpe أو com.mojang.minecrafttrialpe أو com.mojang.minecraftpe.beta."
    override val rescanPackagesButton = "إعادة الفحص"
    override val packageVersionLabel = "الإصدار"
    override val versionCodeLabel = "رقم البناء"
    override val targetMatchExact = "يطابق v1.26.70.24"
    override val targetMatchMismatch = "إصدار مختلف عن 1.26.70.24"
    override val splitApkBadge = "Split APK"
    override val singleApkBadge = "Single APK"
    override val previewBadge = "Preview"
    override val launchPrimaryButton = "تشغيل Minecraft Preview الآن"
    override val launchInProcessButton = "مضيف التشغيل المدمج (In-Process)"
    override val launchFallbackButton = "تشغيل الحزمة المثبّتة مباشرة"
    override val extractAndVerifyButton = "فحص وإصلاح المكتبات"
    override val inspectExternalApkButton = "فحص ملف APK خارجي"
    override val externalApkCardTitle = "نتيجة فحص ملف APK الخارجي"
    override val installExternalApkButton = "فتح لتثبيت APK"

    override val gameHealthTitle = "مدير المكتبات وذاكرة الكاش"
    override val repairReextractButton = "إصلاح وإعادة بناء الكاش"
    override val clearCacheButton = "مسح الكاش"
    override val repairingStatus = "جاري فحص المكتبات وإعادة بناء الكاش..."
    override val noLibrariesExtracted = "لم تُفحص المكتبات بعد. اضغط إعادة الفحص أو إصلاح."
    override val elfValidBadge = "ELF64 صالحة"
    override val elfInvalidBadge = "ELF غير صالحة"
    override val sizeOverOneMbBadge = "الحجم > 1MB"
    override val sizeUnderOneMbBadge = "الحجم < 1MB"
    override val architectureLabel = "البنية"
    override val sourceOriginLabel = "المصدر"
    override val loadSourceLabel = "مسار التحميل"
    override val cacheSummaryTitle = "حالة Library Cache"
    override val cacheSizeLabel = "حجم الكاش"
    override val cacheDirectLoadLabel = "تحميل مباشر بدون نسخ"
    override val cacheCrcVerifiedLabel = "ملفات محققة CRC32"

    override val lastFailureBannerTitle = "تقرير آخر محاولة تشغيل مدمج"
    override val fallbackExplanationPrefix = "توقف المضيف المدمج عند:"
    override val dismissBanner = "إخفاء"

    override val probeTitle = "فحص الحزمة والواجهة البرمجية (Probe)"
    override val probeSubtitle = "تحليل APK و Split APK و ELF64 و JNI"
    override val runProbeButton = "تشغيل Probe الآن"
    override val probingInProgress = "جاري فحص الـ APK و JNI و ELF64..."
    override val probeAutoNotice = "تم الفحص التلقائي اعتماداً على الحزمة المكتشفة وإصدارها."
    override val probeSelectPackage = "الحزم المثبّتة"
    override val probeContextStatusTitle = "تحميل كلاسات اللعبة (createPackageContext)"
    override val probeContextSuccess = "تم تحميل Package Context بنجاح"
    override val probeContextFailed = "تعذر تحميل Package Context"
    override val probeInheritanceTitle = "سلسلة وراثة MainActivity"
    override val probeNativeMethodsTitle = "دوال native المكتشفة"
    override val probeNoNativeMethods = "لم يتم العثور على دوال native معلنة في سلسلة الوراثة."
    override val probeDtNeededTitle = "قسم DT_NEEDED من libminecraftpe.so"
    override val probeLoadOrderTitle = "ترتيب التحميل الطوبولوجي للمكتبات المجمّعة"
    override val probeSystemDepsLabel = "مكتبات النظام"
    override val probeMissingDepsLabel = "مكتبات مفقودة"
    override val probeApkPathsTitle = "مسارات Base APK و Split APKs"

    override val toolsTitle = "الرسوم والأداء والعوالم"
    override val graphicsTitle = "إعدادات الرسوم والأداء (options.txt)"
    override val graphicsDesc =
        "اقرأ وعدّل ملف options.txt الحقيقي للعبة عبر SAF أو جهّز ملف إعدادات بأوضاع أداء جاهزة دون المساس بالعوالم."
    override val loadOptionsTxtButton = "استيراد options.txt"
    override val saveOptionsTxtButton = "حفظ التعديلات في الملف"
    override val exportOptionsTxtButton = "تصدير options.txt"
    override val presetBattery = "توفير الطاقة"
    override val presetBalanced = "متوازن"
    override val presetCompetitive = "أداء 120 FPS"
    override val presetUltra = "جودة فائقة"
    override val renderDistanceLabel = "مسافة الرؤية (Chunks)"
    override val maxFramerateLabel = "الحد الأقصى للإطارات (FPS)"
    override val msaaLabel = "تنعيم الحواف (MSAA)"
    override val fovLabel = "زاوية الرؤية (FOV)"
    override val fancyGraphicsLabel = "رسوم متقدمة (Fancy Graphics)"
    override val smoothLightingLabel = "إضاءة ناعمة (Smooth Lighting)"
    override val fancySkiesLabel = "سماء عالية الدقة (Fancy Skies)"
    override val vsyncLabel = "مزامنة عمودية (VSync)"
    override val deferredRenderingLabel = "تظليل متقدم (Deferred / Vibrant)"
    override val launcherCompatTitle = "خيارات توافق المشغّل"
    override val autoVerifyCacheLabel = "التحقق من libminecraftpe.so والكاش قبل التشغيل"
    override val preferDirectApkLoadLabel = "تفضيل التحميل المباشر من APK!/lib لتجاوز قيود Android 13"

    override val worldBackupTitle = "نسخ احتياطي واستعادة العوالم (.mcworld)"
    override val worldBackupDesc =
        "انسخ مجلد العالم إلى ملف .mcworld مضغوط، أو استعد ملف .mcworld إلى مجلد عوالم، أو افتحه داخل ماينكرافت."
    override val pickWorldFolderButton = "اختر مجلد العالم (نسخ)"
    override val exportMcWorldButton = "حفظ كـ .mcworld"
    override val pickRestoreMcWorldButton = "اختر .mcworld للاستعادة"
    override val restoreToFolderButton = "استعادة إلى مجلد (SAF)"
    override val openWorldInMinecraftButton = "فتح العالم في ماينكرافت"
    override val worldNameLabel = "اسم العالم"
    override val worldFilesLabel = "عدد الملفات"
    override val levelDatFound = "level.dat موجود"
    override val levelDatMissing = "تحذير: level.dat غير موجود"
    override val mcPackImportTitle = "استيراد حزمة موارد (.mcpack / .mcaddon)"
    override val mcPackImportDesc =
        "افحص manifest.json داخل الحزمة ثم افتحها مباشرة داخل ماينكرافت عبر Intent."
    override val pickMcPackButton = "اختر ملف .mcpack"
    override val openInMinecraftButton = "فتح في ماينكرافت"
    override val packNameLabel = "اسم الحزمة"
    override val packTypeLabel = "النوع"
    override val packVersionLabel = "الإصدار"

    override val serverPingerTitle = "فاحص سيرفرات Bedrock (UDP 19132)"
    override val serverPingerDesc = "فحص حقيقي بحزم RakNet Unconnected Ping على المنفذ 19132."
    override val serverNameHint = "اسم السيرفر (اختياري)"
    override val serverHostHint = "العنوان (مثال: play.cubecraft.net)"
    override val serverPortHint = "المنفذ"
    override val addServerButton = "إضافة"
    override val pingAllButton = "فحص الكل"
    override val pingButton = "فحص"
    override val deleteServerDesc = "حذف السيرفر"
    override val serverOnline = "متصل"
    override val serverOffline = "غير متصل"
    override val serverPlayersLabel = "اللاعبون"
    override val serverLatencyLabel = "التأخير"
    override val noServersSaved = "لا توجد سيرفرات مضافة بعد. أضف عنوان سيرفر Bedrock لفحصه."

    override val logsTitle = "سجل التشغيل والتشخيص"
    override val subTabHistory = "سجل عمليات التشغيل"
    override val subTabSessionLogs = "السجل المباشر"
    override val noLaunchHistoryYet = "لم يتم تسجيل أي عملية تشغيل بعد."
    override val clearHistoryButton = "مسح سجل التشغيل"
    override val logsFilePathLabel = "ملف السجل الدائم (filesDir)"
    override val filterAll = "الكل"
    override val filterErrors = "الأخطاء فقط"
    override val copyLogsButton = "نسخ السجل"
    override val shareReportButton = "مشاركة تقرير التشخيص"
    override val clearLogsButton = "مسح"
    override val noLogsYet = "لا توجد سجلات مسجلة حالياً."
    override val copiedToast = "تم النسخ إلى الحافظة"

    override val embeddedHostTitle = "مضيف التشغيل — LeviLab"
    override val embeddedDirectHostTitle = "التحقق المسبق وتشغيل Minecraft Preview"
    override val embeddedRunningStages = "جاري تنفيذ مراحل التحقق والتشغيل بالترتيب..."
    override val embeddedSuccessHint = "تم تحميل المكتبات وتجهيز بيئة التشغيل."
    override val embeddedDirectSuccessHint = "تم التحقق من الحزمة و libminecraftpe.so وتشغيل Minecraft Preview بنجاح."
    override val embeddedFailureTitle = "توقف التشغيل المدمج — تشخيص صريح"
    override val embeddedFallbackMessage =
        "توقف التشغيل المدمج داخل نفس العملية للسبب الموضح أعلاه. يمكنك تشغيل النسخة المثبّتة مباشرة للوصول إلى القائمة الرئيسية والعوالم."
    override val embeddedOpenExternalButton = "تشغيل Minecraft Preview المثبّتة"
    override val embeddedBackToLabButton = "العودة إلى LeviLab"
    override val embeddedShareReportButton = "مشاركة سبب الفشل"
    override val stageALabel = "أ. اكتشاف الحزمة ومطابقة 1.26.70.24 و ARM64"
    override val stageBLabel = "ب. فحص libminecraftpe.so و APK/Split و Library Cache"
    override val stageCLabel = "ج. تحليل DT_NEEDED وحل مسار التحميل"
    override val stageDLabel = "د. تجهيز بيئة التشغيل / الجسر البرمجي"
    override val stageELabel = "هـ. تشغيل Minecraft Bedrock Preview"
}

object EnStrings : LeviStrings {
    override val appTitle = "LeviLab"
    override val targetVersionTag = "Target: v1.26.70.24"
    override val tabLaunch = "Launch"
    override val tabProbe = "Probe & Libs"
    override val tabTools = "Graphics & Tools"
    override val tabLogs = "Logs"
    override val toggleLanguageLabel = "AR"
    override val shareDiagnosticsLabel = "Share Report"

    override val deviceSummaryTitle = "Device & Target Environment"
    override val abiLabel = "ABI"
    override val androidOsLabel = "Android OS"
    override val discoveredPackagesTitle = "Installed Minecraft Versions"
    override val noMinecraftInstalledTitle = "No Minecraft Package Installed"
    override val noMinecraftInstalledDesc =
        "Install one of: com.mojang.minecraftpe.preview, com.mojang.minecraftpe, com.mojang.minecrafttrialpe, or com.mojang.minecraftpe.beta."
    override val rescanPackagesButton = "Rescan"
    override val packageVersionLabel = "Version"
    override val versionCodeLabel = "Version Code"
    override val targetMatchExact = "Matches v1.26.70.24"
    override val targetMatchMismatch = "Version differs from 1.26.70.24"
    override val splitApkBadge = "Split APK"
    override val singleApkBadge = "Single APK"
    override val previewBadge = "Preview"
    override val launchPrimaryButton = "Launch Minecraft Preview"
    override val launchInProcessButton = "In-Process Host (Experimental)"
    override val launchFallbackButton = "Launch Installed Package Directly"
    override val extractAndVerifyButton = "Verify & Repair Libraries"
    override val inspectExternalApkButton = "Inspect External APK"
    override val externalApkCardTitle = "External APK Inspection"
    override val installExternalApkButton = "Open APK Installer"

    override val gameHealthTitle = "Native Libraries & Cache Manager"
    override val repairReextractButton = "Repair & Rebuild Cache"
    override val clearCacheButton = "Clear Cache"
    override val repairingStatus = "Verifying libraries and rebuilding cache..."
    override val noLibrariesExtracted = "No libraries verified yet. Tap Rescan or Repair."
    override val elfValidBadge = "ELF64 Valid"
    override val elfInvalidBadge = "Invalid ELF"
    override val sizeOverOneMbBadge = "Size > 1MB"
    override val sizeUnderOneMbBadge = "Size < 1MB"
    override val architectureLabel = "Arch"
    override val sourceOriginLabel = "Origin"
    override val loadSourceLabel = "Load Path"
    override val cacheSummaryTitle = "Library Cache Status"
    override val cacheSizeLabel = "Cache Size"
    override val cacheDirectLoadLabel = "Zero-Copy Direct Load"
    override val cacheCrcVerifiedLabel = "CRC32 Verified"

    override val lastFailureBannerTitle = "Last In-Process Host Report"
    override val fallbackExplanationPrefix = "In-process host stopped at:"
    override val dismissBanner = "Dismiss"

    override val probeTitle = "APK, Split & JNI Probe"
    override val probeSubtitle = "Analyze Base/Split APKs, ELF64, and GameActivity JNI"
    override val runProbeButton = "Run Probe"
    override val probingInProgress = "Probing APK, JNI classes, and ELF64..."
    override val probeAutoNotice = "Auto-probed based on discovered package & versionName."
    override val probeSelectPackage = "Installed Packages"
    override val probeContextStatusTitle = "Game Classes (createPackageContext)"
    override val probeContextSuccess = "Loaded Package Context"
    override val probeContextFailed = "Failed to load Package Context"
    override val probeInheritanceTitle = "MainActivity Inheritance Chain"
    override val probeNativeMethodsTitle = "Discovered Native Methods"
    override val probeNoNativeMethods = "No declared native methods found in hierarchy."
    override val probeDtNeededTitle = "libminecraftpe.so DT_NEEDED"
    override val probeLoadOrderTitle = "Topological Bundled Load Order"
    override val probeSystemDepsLabel = "System Libs"
    override val probeMissingDepsLabel = "Missing Libs"
    override val probeApkPathsTitle = "Base APK & Split APK Paths"

    override val toolsTitle = "Graphics, Performance & Worlds"
    override val graphicsTitle = "Graphics & Performance (options.txt)"
    override val graphicsDesc =
        "Load and edit Minecraft's real options.txt via SAF or export a tuned preset without touching your worlds."
    override val loadOptionsTxtButton = "Load options.txt"
    override val saveOptionsTxtButton = "Save to Loaded File"
    override val exportOptionsTxtButton = "Export options.txt"
    override val presetBattery = "Battery"
    override val presetBalanced = "Balanced"
    override val presetCompetitive = "120 FPS"
    override val presetUltra = "Ultra"
    override val renderDistanceLabel = "Render Distance (Chunks)"
    override val maxFramerateLabel = "Max Framerate (FPS)"
    override val msaaLabel = "Anti-Aliasing (MSAA)"
    override val fovLabel = "Field of View (FOV)"
    override val fancyGraphicsLabel = "Fancy Graphics"
    override val smoothLightingLabel = "Smooth Lighting"
    override val fancySkiesLabel = "Fancy Skies"
    override val vsyncLabel = "VSync"
    override val deferredRenderingLabel = "Deferred / Vibrant Visuals"
    override val launcherCompatTitle = "Launcher Compatibility Options"
    override val autoVerifyCacheLabel = "Verify libminecraftpe.so & cache before launch"
    override val preferDirectApkLoadLabel = "Prefer direct APK!/lib load (Android 13 safe)"

    override val worldBackupTitle = "World Backup & Restore (.mcworld)"
    override val worldBackupDesc =
        "Archive a world folder into .mcworld, restore a .mcworld backup to a folder, or open it in Minecraft."
    override val pickWorldFolderButton = "Pick World Folder (Backup)"
    override val exportMcWorldButton = "Export .mcworld"
    override val pickRestoreMcWorldButton = "Pick .mcworld to Restore"
    override val restoreToFolderButton = "Restore to Folder (SAF)"
    override val openWorldInMinecraftButton = "Open World in Minecraft"
    override val worldNameLabel = "World Name"
    override val worldFilesLabel = "Files"
    override val levelDatFound = "level.dat present"
    override val levelDatMissing = "Warning: level.dat missing"
    override val mcPackImportTitle = "Import Resource Pack (.mcpack / .mcaddon)"
    override val mcPackImportDesc =
        "Inspect manifest.json inside the pack and launch it into Minecraft via Intent."
    override val pickMcPackButton = "Pick .mcpack File"
    override val openInMinecraftButton = "Open in Minecraft"
    override val packNameLabel = "Pack Name"
    override val packTypeLabel = "Type"
    override val packVersionLabel = "Version"

    override val serverPingerTitle = "Bedrock Server Pinger (UDP 19132)"
    override val serverPingerDesc = "Live RakNet Unconnected Ping packets over UDP port 19132."
    override val serverNameHint = "Label (optional)"
    override val serverHostHint = "Host (e.g. play.cubecraft.net)"
    override val serverPortHint = "Port"
    override val addServerButton = "Add"
    override val pingAllButton = "Ping All"
    override val pingButton = "Ping"
    override val deleteServerDesc = "Delete server"
    override val serverOnline = "Online"
    override val serverOffline = "Offline"
    override val serverPlayersLabel = "Players"
    override val serverLatencyLabel = "Latency"
    override val noServersSaved = "No servers saved yet. Add a Bedrock server host to ping."

    override val logsTitle = "Launch History & Diagnostics"
    override val subTabHistory = "Launch History"
    override val subTabSessionLogs = "Live Logs"
    override val noLaunchHistoryYet = "No launches recorded yet."
    override val clearHistoryButton = "Clear History"
    override val logsFilePathLabel = "Persistent Log File (filesDir)"
    override val filterAll = "All"
    override val filterErrors = "Errors Only"
    override val copyLogsButton = "Copy Logs"
    override val shareReportButton = "Share Diagnostic Report"
    override val clearLogsButton = "Clear"
    override val noLogsYet = "No log entries recorded yet."
    override val copiedToast = "Copied to clipboard"

    override val embeddedHostTitle = "Game Host — LeviLab"
    override val embeddedDirectHostTitle = "Pre-Flight Check & Launch Minecraft Preview"
    override val embeddedRunningStages = "Executing ordered launch stages..."
    override val embeddedSuccessHint = "Native libraries loaded and environment ready."
    override val embeddedDirectSuccessHint = "Package, libminecraftpe.so, and cache verified. Minecraft Preview launched."
    override val embeddedFailureTitle = "In-Process Host Stopped — Honest Diagnosis"
    override val embeddedFallbackMessage =
        "In-process embedding hit an Android 13 boundary shown above. Launch the installed Minecraft Preview directly to reach the main menu and worlds."
    override val embeddedOpenExternalButton = "Launch Installed Minecraft Preview"
    override val embeddedBackToLabButton = "Return to LeviLab"
    override val embeddedShareReportButton = "Share Failure Report"
    override val stageALabel = "A. Package, Version 1.26.70.24 & ARM64 Check"
    override val stageBLabel = "B. libminecraftpe.so, Split APK & Library Cache"
    override val stageCLabel = "C. DT_NEEDED Graph & Load Path Resolution"
    override val stageDLabel = "D. Runtime Host / JNI Bridge Preparation"
    override val stageELabel = "E. Launch Minecraft Bedrock Preview"
}

fun stringsFor(language: AppLanguage): LeviStrings = when (language) {
    AppLanguage.AR -> ArStrings
    AppLanguage.EN -> EnStrings
}
