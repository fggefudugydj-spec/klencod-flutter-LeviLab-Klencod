package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.LeviStrings
import com.example.model.GraphicsPreset
import com.example.model.LeviUiState
import com.example.model.MinecraftGraphicsOptions
import com.example.model.SavedServer
import com.example.model.ServerPingResult
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun ToolsScreen(
    state: LeviUiState,
    strings: LeviStrings,
    onLoadOptionsTxt: (Uri) -> Unit,
    onUpdateOptions: ((MinecraftGraphicsOptions) -> MinecraftGraphicsOptions) -> Unit,
    onApplyPreset: (GraphicsPreset) -> Unit,
    onSaveOptionsTxt: () -> Unit,
    onExportOptionsTxt: (Uri) -> Unit,
    onToggleAutoVerifyCache: (Boolean) -> Unit,
    onTogglePreferDirectApk: (Boolean) -> Unit,
    onAddServer: (label: String, host: String, port: String) -> Unit,
    onDeleteServer: (String) -> Unit,
    onPingServer: (SavedServer) -> Unit,
    onPingAll: () -> Unit,
    onWorldFolderPicked: (Uri) -> Unit,
    onExportWorldDestPicked: (Uri) -> Unit,
    onRestoreMcWorldPicked: (Uri) -> Unit,
    onRestoreTargetFolderPicked: (Uri) -> Unit,
    onOpenRestoreWorldInMc: () -> Unit,
    onMcPackPicked: (Uri) -> Unit,
    onOpenSelectedPack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var serverLabelInput by remember { mutableStateOf("") }
    var serverHostInput by remember { mutableStateOf("") }
    var serverPortInput by remember { mutableStateOf("19132") }

    val pickWorldLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onWorldFolderPicked(uri)
    }

    val createMcWorldLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) onExportWorldDestPicked(uri)
    }

    val pickRestoreMcWorldLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRestoreMcWorldPicked(uri)
    }

    val pickRestoreTargetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onRestoreTargetFolderPicked(uri)
    }

    val pickMcPackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onMcPackPicked(uri)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Graphics & Performance Settings (options.txt)
        item(key = "graphics_settings_card") {
            GraphicsSettingsCard(
                state = state,
                strings = strings,
                onLoadOptionsTxt = onLoadOptionsTxt,
                onUpdateOptions = onUpdateOptions,
                onApplyPreset = onApplyPreset,
                onSaveOptionsTxt = onSaveOptionsTxt,
                onExportOptionsTxt = onExportOptionsTxt,
                onToggleAutoVerifyCache = onToggleAutoVerifyCache,
                onTogglePreferDirectApk = onTogglePreferDirectApk
            )
        }

        // 2. World Backup & Restore (.mcworld) via SAF
        item(key = "world_backup_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = strings.worldBackupTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.worldBackupTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = strings.worldBackupDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pickWorldLauncher.launch(null) },
                            enabled = !state.isScanningWorld && !state.isBackingUpWorld,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_world_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = strings.pickWorldFolderButton,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(strings.pickWorldFolderButton)
                        }
                        OutlinedButton(
                            onClick = { pickRestoreMcWorldLauncher.launch(arrayOf("*/*")) },
                            enabled = !state.isScanningWorld && !state.isRestoringWorld,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_restore_mcworld_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = strings.pickRestoreMcWorldButton,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(strings.pickRestoreMcWorldButton)
                        }
                    }

                    state.selectedWorldFolder?.let { world ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${strings.worldNameLabel}: ${world.levelName}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${strings.worldFilesLabel}: ${world.fileCount} (${formatBytes(world.totalBytes)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (world.hasLevelDat) strings.levelDatFound else strings.levelDatMissing,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (world.hasLevelDat) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val safeName = world.levelName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                                createMcWorldLauncher.launch("${safeName.ifBlank { "world" }}.mcworld")
                            },
                            enabled = !state.isBackingUpWorld && world.fileCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_mcworld_button")
                        ) {
                            if (state.isBackingUpWorld) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                            }
                            Text(strings.exportMcWorldButton)
                        }
                    }

                    state.selectedRestoreWorldFile?.let { restore ->
                        HorizontalDivider()
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${strings.worldNameLabel}: ${restore.levelName} (${restore.fileName})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${strings.worldFilesLabel}: ${restore.entryCount} (${formatBytes(restore.uncompressedBytes)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (restore.hasLevelDat) strings.levelDatFound else strings.levelDatMissing,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (restore.hasLevelDat) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { pickRestoreTargetLauncher.launch(null) },
                                enabled = !state.isRestoringWorld && restore.entryCount > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_to_folder_button")
                            ) {
                                Text(strings.restoreToFolderButton)
                            }
                            OutlinedButton(
                                onClick = onOpenRestoreWorldInMc,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.openWorldInMinecraftButton)
                            }
                        }
                    }

                    state.worldBackupStatusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // 3. Resource Pack (.mcpack / .mcaddon) Importer
        item(key = "mcpack_import_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = strings.mcPackImportTitle,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = strings.mcPackImportTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = strings.mcPackImportDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { pickMcPackLauncher.launch(arrayOf("*/*")) },
                        enabled = !state.isInspectingPack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_mcpack_button")
                    ) {
                        Text(strings.pickMcPackButton)
                    }

                    state.selectedMcPack?.let { pack ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${strings.packNameLabel}: ${pack.packName}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${strings.packTypeLabel}: ${pack.packType} | ${strings.packVersionLabel}: ${pack.packVersion.ifBlank { "N/A" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (pack.packDescription.isNotBlank()) {
                                    Text(
                                        text = pack.packDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onOpenSelectedPack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_mcpack_in_mc_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Launch,
                                contentDescription = strings.openInMinecraftButton,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(strings.openInMinecraftButton)
                        }
                    }

                    state.packImportStatusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // 4. Bedrock Server Pinger (UDP 19132)
        item(key = "server_pinger_card") {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = strings.serverPingerTitle,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = strings.serverPingerTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (state.savedServers.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onPingAll,
                                modifier = Modifier.testTag("ping_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NetworkPing,
                                    contentDescription = strings.pingAllButton,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(strings.pingAllButton)
                            }
                        }
                    }

                    Text(
                        text = strings.serverPingerDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = serverLabelInput,
                        onValueChange = { serverLabelInput = it },
                        label = { Text(strings.serverNameHint) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_label_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = serverHostInput,
                            onValueChange = { serverHostInput = it },
                            label = { Text(strings.serverHostHint) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(2f)
                                .testTag("server_host_input")
                        )
                        OutlinedTextField(
                            value = serverPortInput,
                            onValueChange = { serverPortInput = it },
                            label = { Text(strings.serverPortHint) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("server_port_input")
                        )
                    }

                    Button(
                        onClick = {
                            if (serverHostInput.isNotBlank()) {
                                onAddServer(serverLabelInput, serverHostInput, serverPortInput)
                                serverLabelInput = ""
                                serverHostInput = ""
                                serverPortInput = "19132"
                            }
                        },
                        enabled = serverHostInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_server_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = strings.addServerButton,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.addServerButton)
                    }

                    if (state.savedServers.isEmpty()) {
                        Text(
                            text = strings.noServersSaved,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(
            items = state.savedServers,
            key = { it.id }
        ) { server ->
            ServerPingCard(
                server = server,
                pingResult = state.serverPingResults[server.id],
                isPinging = server.id in state.pingingServerIds,
                strings = strings,
                onPing = { onPingServer(server) },
                onDelete = { onDeleteServer(server.id) }
            )
        }
    }
}

@Composable
private fun ServerPingCard(
    server: SavedServer,
    pingResult: ServerPingResult?,
    isPinging: Boolean,
    strings: LeviStrings,
    onPing: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${server.host}:${server.port}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onPing,
                        enabled = !isPinging
                    ) {
                        if (isPinging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(strings.pingButton)
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = strings.deleteServerDesc,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (pingResult != null) {
                if (pingResult.isOnline) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.serverOnline} • ${pingResult.motd}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${pingResult.latencyMs} ms",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${strings.serverPlayersLabel}: ${pingResult.onlinePlayers}/${pingResult.maxPlayers} | v${pingResult.versionName.ifBlank { "?" }} ${pingResult.gameMode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "${strings.serverOffline}: ${pingResult.errorMessage ?: "timeout"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
