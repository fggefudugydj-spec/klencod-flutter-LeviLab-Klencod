package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.LeviStrings
import com.example.model.GraphicsPreset
import com.example.model.LeviUiState
import com.example.model.MinecraftGraphicsOptions
import kotlin.math.roundToInt

@Composable
fun GraphicsSettingsCard(
    state: LeviUiState,
    strings: LeviStrings,
    onLoadOptionsTxt: (Uri) -> Unit,
    onUpdateOptions: ((MinecraftGraphicsOptions) -> MinecraftGraphicsOptions) -> Unit,
    onApplyPreset: (GraphicsPreset) -> Unit,
    onSaveOptionsTxt: () -> Unit,
    onExportOptionsTxt: (Uri) -> Unit,
    onToggleAutoVerifyCache: (Boolean) -> Unit,
    onTogglePreferDirectApk: (Boolean) -> Unit
) {
    val options = state.graphicsOptions

    val pickOptionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onLoadOptionsTxt(uri)
    }

    val exportOptionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) onExportOptionsTxt(uri)
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = strings.graphicsTitle,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = strings.graphicsTitle,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = strings.graphicsDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Performance Presets Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onApplyPreset(GraphicsPreset.BATTERY_SAVER) },
                    label = { Text(strings.presetBattery) },
                    modifier = Modifier.testTag("preset_battery")
                )
                AssistChip(
                    onClick = { onApplyPreset(GraphicsPreset.BALANCED) },
                    label = { Text(strings.presetBalanced) },
                    modifier = Modifier.testTag("preset_balanced")
                )
                AssistChip(
                    onClick = { onApplyPreset(GraphicsPreset.COMPETITIVE_120FPS) },
                    label = { Text(strings.presetCompetitive) },
                    modifier = Modifier.testTag("preset_120fps")
                )
                AssistChip(
                    onClick = { onApplyPreset(GraphicsPreset.ULTRA_VISUALS) },
                    label = { Text(strings.presetUltra) },
                    modifier = Modifier.testTag("preset_ultra")
                )
            }

            // Render Distance
            Column {
                Text(
                    text = "${strings.renderDistanceLabel}: ${options.renderDistanceChunks}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = options.renderDistanceChunks.toFloat(),
                    onValueChange = { v ->
                        onUpdateOptions { it.copy(renderDistanceChunks = v.roundToInt()) }
                    },
                    valueRange = 4f..32f,
                    steps = 27
                )
            }

            // Max Framerate
            Column {
                Text(
                    text = "${strings.maxFramerateLabel}: ${options.maxFramerate}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = options.maxFramerate.toFloat(),
                    onValueChange = { v ->
                        val snapped = (v / 10f).roundToInt() * 10
                        onUpdateOptions { it.copy(maxFramerate = snapped.coerceIn(30, 240)) }
                    },
                    valueRange = 30f..240f,
                    steps = 20
                )
            }

            // FOV
            Column {
                Text(
                    text = "${strings.fovLabel}: ${options.fieldOfView.roundToInt()}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = options.fieldOfView,
                    onValueChange = { v ->
                        onUpdateOptions { it.copy(fieldOfView = v.roundToInt().toFloat()) }
                    },
                    valueRange = 40f..110f
                )
            }

            GraphicsSwitchRow(
                label = strings.fancyGraphicsLabel,
                checked = options.fancyGraphics,
                onCheckedChange = { c -> onUpdateOptions { it.copy(fancyGraphics = c) } }
            )
            GraphicsSwitchRow(
                label = strings.smoothLightingLabel,
                checked = options.smoothLighting,
                onCheckedChange = { c -> onUpdateOptions { it.copy(smoothLighting = c) } }
            )
            GraphicsSwitchRow(
                label = strings.fancySkiesLabel,
                checked = options.fancySkies,
                onCheckedChange = { c -> onUpdateOptions { it.copy(fancySkies = c) } }
            )
            GraphicsSwitchRow(
                label = strings.vsyncLabel,
                checked = options.vsyncEnabled,
                onCheckedChange = { c -> onUpdateOptions { it.copy(vsyncEnabled = c) } }
            )
            GraphicsSwitchRow(
                label = strings.deferredRenderingLabel,
                checked = options.deferredRendering,
                onCheckedChange = { c -> onUpdateOptions { it.copy(deferredRendering = c) } }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { pickOptionsLauncher.launch(arrayOf("text/plain", "*/*")) },
                    enabled = !state.isLoadingGraphicsOptions,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("load_options_txt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = strings.loadOptionsTxtButton,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(strings.loadOptionsTxtButton)
                }
                OutlinedButton(
                    onClick = { exportOptionsLauncher.launch("options.txt") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_options_txt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = strings.exportOptionsTxtButton,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(strings.exportOptionsTxtButton)
                }
            }

            if (options.fileUriString != null) {
                Button(
                    onClick = onSaveOptionsTxt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_options_txt_button")
                ) {
                    Text(strings.saveOptionsTxtButton)
                }
            }

            state.graphicsStatusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = strings.launcherCompatTitle,
                style = MaterialTheme.typography.titleMedium
            )
            GraphicsSwitchRow(
                label = strings.autoVerifyCacheLabel,
                checked = state.autoVerifyCacheBeforeLaunch,
                onCheckedChange = onToggleAutoVerifyCache
            )
            GraphicsSwitchRow(
                label = strings.preferDirectApkLoadLabel,
                checked = state.preferDirectApkNativeLoad,
                onCheckedChange = onTogglePreferDirectApk
            )
        }
    }
}

@Composable
private fun GraphicsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
