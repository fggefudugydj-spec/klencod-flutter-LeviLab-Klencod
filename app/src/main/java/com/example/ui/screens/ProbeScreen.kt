package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.LeviStrings
import com.example.model.InstalledMinecraftPackage
import com.example.model.LeviUiState
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun ProbeScreen(
    state: LeviUiState,
    strings: LeviStrings,
    onSelectPackage: (InstalledMinecraftPackage) -> Unit,
    onRunProbe: () -> Unit,
    onRepairCache: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    val probe = state.probeReport

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "probe_header") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.probeTitle,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = strings.probeSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onRunProbe,
                            enabled = !state.isProbing,
                            modifier = Modifier.testTag("run_probe_button")
                        ) {
                            if (state.isProbing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = strings.runProbeButton,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(if (state.isProbing) strings.probingInProgress else strings.runProbeButton)
                        }
                    }

                    if (state.installedPackages.isNotEmpty()) {
                        Text(
                            text = strings.probeSelectPackage,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.installedPackages.forEach { pkg ->
                                FilterChip(
                                    selected = state.selectedPackage?.packageName == pkg.packageName,
                                    onClick = { onSelectPackage(pkg) },
                                    label = { Text("${pkg.packageName} (${pkg.versionName})") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Library Cache & File Manager Card
        state.cacheSummary?.let { cache ->
            item(key = "probe_cache_mgr") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = strings.cacheSummaryTitle,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.cacheSummaryTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "Root: ${cache.cacheRootPath}\nActive: ${cache.activeCacheDirPath}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${strings.cacheSizeLabel}: ${formatBytes(cache.totalCacheBytes)} • Files: ${cache.cachedFileCount} • Direct Load: ${cache.directLoadableCount} • CRC32: ${cache.crcVerifiedCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRepairCache,
                                enabled = !state.isRepairing && !state.isProbing,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = strings.repairReextractButton,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(strings.repairReextractButton)
                            }
                            OutlinedButton(
                                onClick = onClearCache,
                                enabled = !state.isClearingCache
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

        if (probe != null) {
            item(key = "probe_context_card") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (probe.contextCreated) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = strings.probeContextStatusTitle,
                                tint = if (probe.contextCreated) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = strings.probeContextStatusTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = if (probe.contextCreated) {
                                "${strings.probeContextSuccess} (${probe.packageInfo.packageName})"
                            } else {
                                "${strings.probeContextFailed}: ${probe.contextError ?: "unknown"}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Preferred ABI: ${probe.preferredAbi} | APK ABIs: ${probe.availableApkAbis.joinToString(", ").ifBlank { "none" }}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (probe.gameActivityDetected) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "GameActivity: ${probe.gameActivityClassName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "probe_inheritance") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = strings.probeInheritanceTitle,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = strings.probeInheritanceTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (probe.inheritanceChain.isEmpty()) {
                            Text(
                                text = "MainActivity not loaded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            probe.inheritanceChain.forEachIndexed { idx, cls ->
                                Text(
                                    text = "${"  ".repeat(idx)}↳ $cls",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item(key = "probe_dt_needed") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.probeDtNeededTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val dyn = probe.mainElfDynamicInfo
                        if (dyn == null) {
                            Text(text = "N/A", style = MaterialTheme.typography.labelSmall)
                        } else if (dyn.parseError != null) {
                            Text(
                                text = dyn.parseError,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = dyn.dtNeeded.joinToString("\n") { "• $it" },
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        probe.dependencyResolution?.let { dep ->
                            Text(
                                text = strings.probeLoadOrderTitle,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Text(
                                text = dep.bundledLoadOrder.joinToString(" → "),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (dep.missingDependencies.isNotEmpty()) {
                                Text(
                                    text = "${strings.probeMissingDepsLabel}: ${dep.missingDependencies.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            item(key = "probe_native_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = strings.probeNativeMethodsTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${strings.probeNativeMethodsTitle} (${probe.nativeMethods.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (probe.nativeMethods.isEmpty()) {
                item(key = "probe_no_native") {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(
                            text = strings.probeNoNativeMethods,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(
                    items = probe.nativeMethods,
                    key = { "${it.declaringClass}.${it.methodName}${it.signature}" }
                ) { method ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${method.methodName}${method.signature}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = method.declaringClass,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "probe_apk_paths") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = strings.probeApkPathsTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "base: ${probe.packageInfo.sourceDir}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        probe.packageInfo.splitSourceDirs.forEachIndexed { idx, split ->
                            val splitName = probe.packageInfo.splitNames.getOrNull(idx) ?: "split#$idx"
                            Text(
                                text = "$splitName: $split",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "nativeDir: ${probe.packageInfo.nativeLibraryDir}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
