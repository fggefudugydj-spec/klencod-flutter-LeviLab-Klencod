package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.stringsFor
import com.example.launcher.EmbeddedMinecraftActivity
import com.example.model.LaunchMode
import com.example.model.LeviTab
import com.example.ui.screens.LaunchHealthScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.ProbeScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.LeviLabTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeviApp(
    viewModel: LeviViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = stringsFor(state.language)
    val context = LocalContext.current

    fun shareDiagnosticReport() {
        val report = viewModel.buildDiagnosticReportText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "LeviLab Diagnostic Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        context.startActivity(Intent.createChooser(intent, strings.shareDiagnosticsLabel))
    }

    fun copyLogsToClipboard() {
        val text = viewModel.logger.readFullFileText()
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("LeviLab Logs", text))
        Toast.makeText(context, strings.copiedToast, Toast.LENGTH_SHORT).show()
    }

    fun startLaunchActivity(mode: LaunchMode) {
        val launchIntent = Intent(context, EmbeddedMinecraftActivity::class.java).apply {
            putExtra(
                EmbeddedMinecraftActivity.EXTRA_PACKAGE_NAME,
                state.selectedPackage?.packageName
            )
            putExtra(
                EmbeddedMinecraftActivity.EXTRA_LANGUAGE,
                state.language.name
            )
            putExtra(
                EmbeddedMinecraftActivity.EXTRA_LAUNCH_MODE,
                mode.name
            )
            putExtra(
                EmbeddedMinecraftActivity.EXTRA_VERIFY_CACHE,
                state.autoVerifyCacheBeforeLaunch
            )
            putExtra(
                EmbeddedMinecraftActivity.EXTRA_PREFER_DIRECT_APK,
                state.preferDirectApkNativeLoad
            )
        }
        context.startActivity(launchIntent)
    }

    LeviLabTheme(language = state.language) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = strings.targetVersionTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = { viewModel.toggleLanguage() },
                                modifier = Modifier.testTag("toggle_language_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = strings.toggleLanguageLabel,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(strings.toggleLanguageLabel)
                            }
                            IconButton(
                                onClick = { shareDiagnosticReport() },
                                modifier = Modifier.testTag("topbar_share_diagnostics")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = strings.shareDiagnosticsLabel
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                selected = state.currentTab == LeviTab.LAUNCH,
                                onClick = { viewModel.selectTab(LeviTab.LAUNCH) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.RocketLaunch,
                                        contentDescription = strings.tabLaunch
                                    )
                                },
                                label = { Text(strings.tabLaunch) },
                                modifier = Modifier.testTag("nav_launch")
                            )
                            NavigationBarItem(
                                selected = state.currentTab == LeviTab.PROBE,
                                onClick = { viewModel.selectTab(LeviTab.PROBE) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = strings.tabProbe
                                    )
                                },
                                label = { Text(strings.tabProbe) },
                                modifier = Modifier.testTag("nav_probe")
                            )
                            NavigationBarItem(
                                selected = state.currentTab == LeviTab.TOOLS,
                                onClick = { viewModel.selectTab(LeviTab.TOOLS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = strings.tabTools
                                    )
                                },
                                label = { Text(strings.tabTools) },
                                modifier = Modifier.testTag("nav_tools")
                            )
                            NavigationBarItem(
                                selected = state.currentTab == LeviTab.LOGS,
                                onClick = { viewModel.selectTab(LeviTab.LOGS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = strings.tabLogs
                                    )
                                },
                                label = { Text(strings.tabLogs) },
                                modifier = Modifier.testTag("nav_logs")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            NavigationRailItem(
                                selected = state.currentTab == LeviTab.LAUNCH,
                                onClick = { viewModel.selectTab(LeviTab.LAUNCH) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.RocketLaunch,
                                        contentDescription = strings.tabLaunch
                                    )
                                },
                                label = { Text(strings.tabLaunch) },
                                modifier = Modifier.testTag("nav_launch")
                            )
                            NavigationRailItem(
                                selected = state.currentTab == LeviTab.PROBE,
                                onClick = { viewModel.selectTab(LeviTab.PROBE) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = strings.tabProbe
                                    )
                                },
                                label = { Text(strings.tabProbe) },
                                modifier = Modifier.testTag("nav_probe")
                            )
                            NavigationRailItem(
                                selected = state.currentTab == LeviTab.TOOLS,
                                onClick = { viewModel.selectTab(LeviTab.TOOLS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = strings.tabTools
                                    )
                                },
                                label = { Text(strings.tabTools) },
                                modifier = Modifier.testTag("nav_tools")
                            )
                            NavigationRailItem(
                                selected = state.currentTab == LeviTab.LOGS,
                                onClick = { viewModel.selectTab(LeviTab.LOGS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = strings.tabLogs
                                    )
                                },
                                label = { Text(strings.tabLogs) },
                                modifier = Modifier.testTag("nav_logs")
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (state.currentTab) {
                            LeviTab.LAUNCH -> LaunchHealthScreen(
                                state = state,
                                strings = strings,
                                onRescan = { viewModel.scanPackagesAndAutoProbe() },
                                onSelectPackage = { viewModel.selectPackageAndProbe(it) },
                                onLaunchPrimary = { startLaunchActivity(LaunchMode.DIRECT_NATIVE) },
                                onLaunchInProcess = { startLaunchActivity(LaunchMode.IN_PROCESS_EXPERIMENTAL) },
                                onLaunchDirectFallback = { viewModel.launchInstalledDirectly() },
                                onRepairLibraries = { viewModel.repairLibraries() },
                                onClearCache = { viewModel.clearLibraryCache() },
                                onInspectExternalApk = { viewModel.inspectExternalApk(it) },
                                onInstallExternalApk = { viewModel.openExternalApkInstaller(it) },
                                onDismissFailure = { viewModel.dismissLastFailure() }
                            )

                            LeviTab.PROBE -> ProbeScreen(
                                state = state,
                                strings = strings,
                                onSelectPackage = { viewModel.selectPackageAndProbe(it) },
                                onRunProbe = { viewModel.runProbeForSelected(forceReextract = false) },
                                onRepairCache = { viewModel.repairLibraries() },
                                onClearCache = { viewModel.clearLibraryCache() }
                            )

                            LeviTab.TOOLS -> ToolsScreen(
                                state = state,
                                strings = strings,
                                onLoadOptionsTxt = { viewModel.onOptionsTxtPicked(it) },
                                onUpdateOptions = { viewModel.updateGraphicsOptions(it) },
                                onApplyPreset = { viewModel.applyGraphicsPreset(it) },
                                onSaveOptionsTxt = { viewModel.saveGraphicsOptionsToLoadedFile() },
                                onExportOptionsTxt = { viewModel.exportGraphicsOptionsToUri(it) },
                                onToggleAutoVerifyCache = { viewModel.setAutoVerifyCache(it) },
                                onTogglePreferDirectApk = { viewModel.setPreferDirectApkLoad(it) },
                                onAddServer = { label, host, port ->
                                    viewModel.addServer(label, host, port)
                                },
                                onDeleteServer = { viewModel.deleteServer(it) },
                                onPingServer = { viewModel.pingServer(it) },
                                onPingAll = { viewModel.pingAllServers() },
                                onWorldFolderPicked = { viewModel.onWorldFolderPicked(it) },
                                onExportWorldDestPicked = { viewModel.onExportWorldDestinationPicked(it) },
                                onRestoreMcWorldPicked = { viewModel.onRestoreMcWorldPicked(it) },
                                onRestoreTargetFolderPicked = { viewModel.onRestoreWorldTargetFolderPicked(it) },
                                onOpenRestoreWorldInMc = { viewModel.openSelectedRestoreWorldInMinecraft() },
                                onMcPackPicked = { viewModel.onMcPackPicked(it) },
                                onOpenSelectedPack = { viewModel.openSelectedPackInMinecraft() }
                            )

                            LeviTab.LOGS -> LogsScreen(
                                state = state,
                                strings = strings,
                                onSelectSubTab = { viewModel.selectLogsSubTab(it) },
                                onSetFilter = { viewModel.setLogFilter(it) },
                                onCopyLogs = { copyLogsToClipboard() },
                                onShareDiagnosticReport = { shareDiagnosticReport() },
                                onClearLogs = { viewModel.clearLogs() },
                                onClearHistory = { viewModel.clearLaunchHistory() }
                            )
                        }
                    }
                }
            }
        }
    }
}
