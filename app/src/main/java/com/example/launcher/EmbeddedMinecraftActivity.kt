package com.example.launcher

import android.content.Intent
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.example.core.AppLanguage
import com.example.core.LeviLogger
import com.example.core.LeviPreferences
import com.example.core.LeviStrings
import com.example.core.stringsFor
import com.example.model.LaunchMode
import com.example.model.LaunchStageStatus
import com.example.model.LaunchStepState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LeviLabTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmbeddedMinecraftActivity : ComponentActivity() {

    private var stagesState by mutableStateOf<List<LaunchStageStatus>>(emptyList())
    private var outcomeState by mutableStateOf<LaunchExecutionOutcome?>(null)
    private var isExecuting by mutableStateOf(true)
    private var surfaceReady by mutableStateOf(false)
    private var autoFallbackCancelled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val logger = LeviLogger.getInstance(applicationContext)
        val preferences = LeviPreferences(applicationContext)
        val langName = intent.getStringExtra(EXTRA_LANGUAGE) ?: AppLanguage.AR.name
        val language = runCatching { AppLanguage.valueOf(langName) }.getOrDefault(AppLanguage.AR)
        val preferredPkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val modeName = intent.getStringExtra(EXTRA_LAUNCH_MODE) ?: LaunchMode.DIRECT_NATIVE.name
        val launchMode = runCatching { LaunchMode.valueOf(modeName) }.getOrDefault(LaunchMode.DIRECT_NATIVE)
        val verifyCache = intent.getBooleanExtra(EXTRA_VERIFY_CACHE, true)
        val preferDirectApk = intent.getBooleanExtra(EXTRA_PREFER_DIRECT_APK, true)
        val strings = stringsFor(language)

        logger.info("HOST", "EmbeddedMinecraftActivity started (mode=${launchMode.name}, pkg=${preferredPkg ?: "auto"})")

        setContent {
            LeviLabTheme(language = language) {
                EmbeddedGameHostScreen(
                    strings = strings,
                    launchMode = launchMode,
                    stages = stagesState,
                    outcome = outcomeState,
                    isExecuting = isExecuting,
                    surfaceReady = surfaceReady,
                    autoFallbackCancelled = autoFallbackCancelled,
                    onSurfaceCreated = { surfaceReady = true },
                    onCancelAutoFallback = { autoFallbackCancelled = true },
                    onOpenExternalFallback = {
                        val pkg = outcomeState?.targetPackage
                        val pkgName = pkg?.packageName ?: preferredPkg
                        if (!pkgName.isNullOrBlank()) {
                            triggerExternalFallback(pkgName, logger, strings, preferences)
                        }
                    },
                    onShareFailure = {
                        shareFailureReport(outcomeState, logger)
                    },
                    onReturnToLab = { finish() }
                )
            }
        }

        lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                if (launchMode == LaunchMode.DIRECT_NATIVE) {
                    InProcessLaunchCoordinator.executeDirectNativeLaunch(
                        activityContext = this@EmbeddedMinecraftActivity,
                        preferredPackageName = preferredPkg,
                        verifyCacheBeforeLaunch = verifyCache,
                        strings = strings,
                        logger = logger,
                        onStageUpdate = { updated -> stagesState = updated }
                    )
                } else {
                    InProcessLaunchCoordinator.executeInProcessSequence(
                        context = applicationContext,
                        preferredPackageName = preferredPkg,
                        preferDirectApkNativeLoad = preferDirectApk,
                        strings = strings,
                        logger = logger,
                        onStageUpdate = { updated -> stagesState = updated }
                    )
                }
            }
            outcomeState = outcome
            isExecuting = false
            outcome.historyEntry?.let { entry ->
                preferences.appendLaunchHistory(entry)
            }
        }
    }

    private fun triggerExternalFallback(
        packageName: String,
        logger: LeviLogger,
        strings: LeviStrings,
        preferences: LeviPreferences
    ) {
        autoFallbackCancelled = true
        val targetPkg = outcomeState?.targetPackage
        val opened = if (targetPkg != null) {
            InProcessLaunchCoordinator.launchInstalledPackage(this, targetPkg, logger)
        } else {
            InProcessLaunchCoordinator.launchInstalledFallback(this, packageName, logger)
        }
        if (opened) {
            outcomeState?.failure?.let { currentFailure ->
                LaunchDiagnosticsBus.publishFailure(currentFailure.copy(fallbackTriggered = true))
            }
            lifecycleScope.launch {
                preferences.appendLaunchHistory(
                    InProcessLaunchCoordinator.buildHistoryEntry(
                        pkg = targetPkg,
                        mode = outcomeState?.mode ?: LaunchMode.IN_PROCESS_EXPERIMENTAL,
                        succeeded = true,
                        usedFallback = true,
                        mainSource = outcomeState?.mainLibLoadSource ?: "NATIVE_DIR",
                        summary = "Launched installed $packageName via handoff"
                    )
                )
            }
            Toast.makeText(this, strings.embeddedDirectSuccessHint, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "getLaunchIntentForPackage($packageName) returned null",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun shareFailureReport(outcome: LaunchExecutionOutcome?, logger: LeviLogger) {
        val failure = outcome?.failure
        val report = buildString {
            appendLine("=== LeviLab Launch Diagnostic Report ===")
            appendLine("Mode: ${outcome?.mode?.name ?: "unknown"}")
            appendLine("Package: ${outcome?.targetPackage?.packageName ?: "unknown"}")
            appendLine("Version: ${outcome?.targetPackage?.versionName ?: "unknown"}")
            appendLine("Failed Stage: ${failure?.failedStage ?: "unknown"}")
            appendLine("Exception: ${failure?.exceptionClass ?: "N/A"}")
            appendLine("Reason: ${failure?.reason ?: "N/A"}")
            appendLine()
            appendLine("Stacktrace:")
            appendLine(failure?.stackTracePreview ?: "N/A")
            appendLine()
            appendLine("Recent Logs:")
            appendLine(logger.readFullFileText().lines().takeLast(60).joinToString("\n"))
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "LeviLab Launch Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Launch Report"))
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_LANGUAGE = "extra_language"
        const val EXTRA_LAUNCH_MODE = "extra_launch_mode"
        const val EXTRA_VERIFY_CACHE = "extra_verify_cache"
        const val EXTRA_PREFER_DIRECT_APK = "extra_prefer_direct_apk"
    }
}

@Composable
private fun EmbeddedGameHostScreen(
    strings: LeviStrings,
    launchMode: LaunchMode,
    stages: List<LaunchStageStatus>,
    outcome: LaunchExecutionOutcome?,
    isExecuting: Boolean,
    surfaceReady: Boolean,
    autoFallbackCancelled: Boolean,
    onSurfaceCreated: () -> Unit,
    onCancelAutoFallback: () -> Unit,
    onOpenExternalFallback: () -> Unit,
    onShareFailure: () -> Unit,
    onReturnToLab: () -> Unit
) {
    val failure = outcome?.failure
    val directSuccess = outcome?.gameLaunchedExternally == true && failure == null
    var countdown by remember { mutableIntStateOf(3) }

    LaunchedEffect(failure, autoFallbackCancelled) {
        if (failure != null && failure.fallbackAvailable && !autoFallbackCancelled) {
            countdown = 3
            while (countdown > 0 && !autoFallbackCancelled) {
                delay(1000L)
                countdown -= 1
            }
            if (!autoFallbackCancelled) {
                onOpenExternalFallback()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback2 {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            onSurfaceCreated()
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                        }

                        override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (launchMode == LaunchMode.DIRECT_NATIVE) {
                                strings.embeddedDirectHostTitle
                            } else {
                                strings.embeddedHostTitle
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (surfaceReady) "SurfaceHolder ready" else "Initializing SurfaceView...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    OutlinedButton(
                        onClick = onReturnToLab,
                        modifier = Modifier.testTag("embedded_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.embeddedBackToLabButton,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.embeddedBackToLabButton)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isExecuting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(
                                text = strings.embeddedRunningStages,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        stages.forEach { stage ->
                            StageRow(stage = stage)
                        }
                    }
                }

                if (directSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = strings.embeddedDirectSuccessHint,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Button(
                                onClick = onOpenExternalFallback,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Launch,
                                    contentDescription = strings.embeddedOpenExternalButton,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(strings.embeddedOpenExternalButton)
                            }
                        }
                    }
                }

                if (failure != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = strings.embeddedFailureTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "${failure.failedStage} — ${failure.exceptionClass}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = failure.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Text(
                                text = strings.embeddedFallbackMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (failure.fallbackAvailable) {
                                    Button(
                                        onClick = onOpenExternalFallback,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("embedded_fallback_launch_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Launch,
                                            contentDescription = strings.embeddedOpenExternalButton,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        val label = if (!autoFallbackCancelled && countdown > 0) {
                                            "${strings.embeddedOpenExternalButton} (${countdown}s)"
                                        } else {
                                            strings.embeddedOpenExternalButton
                                        }
                                        Text(label)
                                    }
                                }
                                OutlinedButton(
                                    onClick = onShareFailure,
                                    modifier = Modifier.testTag("embedded_share_failure_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = strings.embeddedShareReportButton,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(strings.embeddedShareReportButton)
                                }
                            }

                            if (failure.fallbackAvailable && !autoFallbackCancelled && countdown > 0) {
                                OutlinedButton(
                                    onClick = onCancelAutoFallback,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (strings.toggleLanguageLabel == "EN") {
                                            "إيقاف الفتح التلقائي والبقاء في التقرير"
                                        } else {
                                            "Cancel Auto-Launch & Stay Here"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageRow(stage: LaunchStageStatus) {
    val icon = when (stage.state) {
        LaunchStepState.PENDING -> Icons.Default.RadioButtonUnchecked
        LaunchStepState.RUNNING -> Icons.Default.HourglassTop
        LaunchStepState.PASSED -> Icons.Default.CheckCircle
        LaunchStepState.FAILED -> Icons.Default.Error
    }
    val tint = when (stage.state) {
        LaunchStepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        LaunchStepState.RUNNING -> MaterialTheme.colorScheme.secondary
        LaunchStepState.PASSED -> MaterialTheme.colorScheme.primary
        LaunchStepState.FAILED -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stage.title,
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stage.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (stage.detail.isNotBlank()) {
                Text(
                    text = stage.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
