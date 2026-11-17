package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.LeviStrings
import com.example.model.ElfInspectionResult
import com.example.model.InstalledMinecraftPackage
import com.example.model.LeviUiState
import com.example.model.LibraryLoadSource
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LaunchHealthScreen(
    state: LeviUiState,
    strings: LeviStrings,
    onRescan: () -> Unit,
    onSelectPackage: (InstalledMinecraftPackage) -> Unit,
    onLaunchPrimary: () -> Unit,
    onLaunchInProcess: () -> Unit,
    onLaunchDirectFallback: () -> Unit,
    onRepairLibraries: () -> Unit,
    onClearCache: () -> Unit,
    onInspectExternalApk: (Uri) -> Unit,
    onInstallExternalApk: (String) -> Unit,
    onDismissFailure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pickExternalApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onInspectExternalApk(uri)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        state.lastLaunchFailure?.let { failure ->
            item(key = "last_failure_banner") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = strings.lastFailureBannerTitle,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = strings.lastFailureBannerTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            IconButton(onClick = onDismissFailure) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.dismissBanner,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Text(
                            text = "${strings.fallbackExplanationPrefix} ${failure.failedStage}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = failure.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Device Environment Hero Card
        item(key = "device_env_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = strings.deviceSummaryTitle,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = strings.deviceSummaryTitle,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            AssistChip(
                                onClick = onRescan,
                                enabled = !state.isScanningPackages,
                                label = { Text(strings.rescanPackagesButton) },
                                leadingIcon = {
                                    if (state.isScanningPackages) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = strings.rescanPackagesButton,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("rescan_packages_button")
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InfoPill("${strings.androidOsLabel}: ${state.androidVersionLabel}")
                            InfoPill(
                                "${strings.abiLabel}: ${state.deviceAbiList.joinToString(", ").ifBlank { "unknown" }}"
                            )
                            InfoPill(label = strings.targetVersionTag, highlight = true)
                        }
                    }
                }
            }
        }

        // Version Manager & Launch Controls
        item(key = "packages_section") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.discoveredPackagesTitle,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { pickExternalApkLauncher.launch(arrayOf("*/*")) },
                            enabled = !state.isInspectingExternalApk,
                            modifier = Modifier.testTag("inspect_external_apk_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = strings.inspectExternalApkButton,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(strings.inspectExternalApkButton)
                        }
                    }

                    state.externalApkInspection?.let { ext ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${strings.externalApkCardTitle}: ${ext.fileName}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (ext.errorMessage != null) {
                                    Text(
                                        text = ext.errorMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        InfoPill(ext.packageName ?: "unknown")
                                        InfoPill("v${ext.versionName ?: "?"} (${ext.versionCode ?: 0})")
                                        InfoPill(
                                            label = if (ext.hasArm64MainLib && ext.mainLibIsValidElf64) {
                                                "arm64 libminecraftpe.so (${formatBytes(ext.mainLibSizeBytes)})"
                                            } else {
                                                "arm64 libminecraftpe.so missing"
                                            },
                                            highlight = ext.hasArm64MainLib && ext.mainLibIsValidElf64
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onInstallExternalApk(ext.uriString) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(strings.installExternalApkButton)
                                    }
                                }
                            }
                        }
                    }

                    if (state.installedPackages.isEmpty() && !state.isScanningPackages) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = strings.noMinecraftInstalledTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = strings.noMinecraftInstalledDesc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        state.installedPackages.forEach { pkg ->
                            val selected = state.selectedPackage?.packageName == pkg.packageName
                            PackageCardItem(
                                pkg = pkg,
                                isSelected = selected,
                                strings = strings,
                                onClick = { onSelectPackage(pkg) }
                            )
                        }
                    }

                    if (state.selectedPackage != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Button(
                            onClick = onLaunchPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("launch_primary_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = strings.launchPrimaryButton
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = strings.launchPrimaryButton,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onLaunchDirectFallback,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("launch_direct_fallback_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Launch,
                                    contentDescription = strings.launchFallbackButton,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = strings.launchFallbackButton,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = onLaunchInProcess,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("launch_in_process_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = strings.launchInProcessButton,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = strings.launchInProcessButton,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Game Health & Library Cache Summary
        item(key = "health_header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = strings.gameHealthTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.gameHealthTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                state.cacheSummary?.let { cache ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.cacheSummaryTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                InfoPill("${strings.cacheSizeLabel}: ${formatBytes(cache.totalCacheBytes)}")
                                InfoPill(
                                    "${strings.cacheCrcVerifiedLabel}: ${cache.crcVerifiedCount}",
                                    highlight = cache.crcVerifiedCount > 0
                                )
                                InfoPill(
                                    "${strings.cacheDirectLoadLabel}: ${cache.directLoadableCount}",
                                    highlight = cache.directLoadableCount > 0
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onRepairLibraries,
                                    enabled = state.selectedPackage != null && !state.isRepairing && !state.isProbing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("repair_libraries_button")
                                ) {
                                    if (state.isRepairing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = strings.repairReextractButton,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        if (state.isRepairing) strings.repairingStatus else strings.repairReextractButton,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = onClearCache,
                                    enabled = !state.isClearingCache,
                                    modifier = Modifier.testTag("clear_cache_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = strings.clearCacheButton,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(strings.clearCacheButton)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.extractedLibraries.isEmpty()) {
            item(key = "no_libs") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        text = strings.noLibrariesExtracted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(
                items = state.extractedLibraries,
                key = { it.fileName }
            ) { lib ->
                LibraryHealthCard(lib = lib, strings = strings)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackageCardItem(
    pkg: InstalledMinecraftPackage,
    isSelected: Boolean,
    strings: LeviStrings,
    onClick: () -> Unit
) {
    val border = if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = MaterialTheme.shapes.medium,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("package_card_${pkg.packageName}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pkg.appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pkg.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Icon(
                    imageVector = if (pkg.matchesTargetVersion) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = pkg.versionName,
                    tint = if (pkg.matchesTargetVersion) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InfoPill("${strings.packageVersionLabel}: ${pkg.versionName}")
                InfoPill("${strings.versionCodeLabel}: ${pkg.versionCode}")
                if (pkg.isPreviewPackage) {
                    InfoPill(strings.previewBadge, highlight = true)
                }
                InfoPill(
                    if (pkg.splitSourceDirs.isNotEmpty()) {
                        "${strings.splitApkBadge} (${pkg.splitSourceDirs.size})"
                    } else {
                        strings.singleApkBadge
                    }
                )
                InfoPill(
                    label = if (pkg.matchesTargetVersion) strings.targetMatchExact else strings.targetMatchMismatch,
                    highlight = pkg.matchesTargetVersion
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryHealthCard(
    lib: ElfInspectionResult,
    strings: LeviStrings
) {
    val statusColor = if (lib.isValid) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lib.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatBytes(lib.sizeBytes),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = statusColor
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InfoPill(
                    label = if (lib.hasElfMagic) strings.elfValidBadge else strings.elfInvalidBadge,
                    highlight = lib.hasElfMagic
                )
                InfoPill(
                    label = if (lib.sizeBytes > 1_048_576L) {
                        strings.sizeOverOneMbBadge
                    } else {
                        strings.sizeUnderOneMbBadge
                    },
                    highlight = lib.sizeBytes > 1_048_576L
                )
                InfoPill("${strings.architectureLabel}: ${lib.machineName}")
                InfoPill(
                    label = "${strings.loadSourceLabel}: ${lib.loadSource.name}",
                    highlight = lib.loadSource == LibraryLoadSource.NATIVE_DIR ||
                        lib.loadSource == LibraryLoadSource.APK_DIRECT_ZIP
                )
                InfoPill("${strings.sourceOriginLabel}: ${lib.sourceOrigin}")
            }

            Text(
                text = lib.validationNote,
                style = MaterialTheme.typography.labelSmall,
                color = if (lib.isValid) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun InfoPill(
    label: String,
    highlight: Boolean = false
) {
    Surface(
        color = if (highlight) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_048_576L -> String.format(java.util.Locale.US, "%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
