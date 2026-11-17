package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.model.LaunchHistoryEntry
import com.example.model.LeviUiState
import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.LogsSubTab
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun LogsScreen(
    state: LeviUiState,
    strings: LeviStrings,
    onSelectSubTab: (LogsSubTab) -> Unit,
    onSetFilter: (LogLevel?) -> Unit,
    onCopyLogs: () -> Unit,
    onShareDiagnosticReport: () -> Unit,
    onClearLogs: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredLogs = if (state.logLevelFilter == null) {
        state.logs
    } else {
        state.logs.filter { it.level == state.logLevelFilter }
    }.asReversed()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "logs_header") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.logsTitle,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.logsSubTab == LogsSubTab.HISTORY,
                            onClick = { onSelectSubTab(LogsSubTab.HISTORY) },
                            label = { Text("${strings.subTabHistory} (${state.launchHistory.size})") },
                            modifier = Modifier.testTag("subtab_history")
                        )
                        FilterChip(
                            selected = state.logsSubTab == LogsSubTab.SESSION_LOGS,
                            onClick = { onSelectSubTab(LogsSubTab.SESSION_LOGS) },
                            label = { Text("${strings.subTabSessionLogs} (${state.logs.size})") },
                            modifier = Modifier.testTag("subtab_session_logs")
                        )
                    }

                    if (state.logsSubTab == LogsSubTab.SESSION_LOGS) {
                        Text(
                            text = "${strings.logsFilePathLabel}:\n${state.logFilePath}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = state.logLevelFilter == null,
                                onClick = { onSetFilter(null) },
                                label = { Text("${strings.filterAll} (${state.logs.size})") }
                            )
                            FilterChip(
                                selected = state.logLevelFilter == LogLevel.ERROR,
                                onClick = {
                                    onSetFilter(
                                        if (state.logLevelFilter == LogLevel.ERROR) null else LogLevel.ERROR
                                    )
                                },
                                label = {
                                    val errCount = state.logs.count { it.level == LogLevel.ERROR }
                                    Text("${strings.filterErrors} ($errCount)")
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCopyLogs,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_logs_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = strings.copyLogsButton,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(strings.copyLogsButton)
                            }

                            Button(
                                onClick = onShareDiagnosticReport,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_report_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = strings.shareReportButton,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(strings.shareReportButton)
                            }
                        }

                        OutlinedButton(
                            onClick = onClearLogs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = strings.clearLogsButton,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(strings.clearLogsButton)
                        }
                    } else if (state.launchHistory.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = strings.clearHistoryButton,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(strings.clearHistoryButton)
                        }
                    }
                }
            }
        }

        if (state.logsSubTab == LogsSubTab.HISTORY) {
            if (state.launchHistory.isEmpty()) {
                item(key = "no_history") {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(
                            text = strings.noLaunchHistoryYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(
                    items = state.launchHistory,
                    key = { it.id }
                ) { entry ->
                    LaunchHistoryRow(entry = entry)
                }
            }
        } else {
            if (filteredLogs.isEmpty()) {
                item(key = "no_logs") {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(
                            text = strings.noLogsYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(
                    items = filteredLogs,
                    key = { it.id }
                ) { entry ->
                    LogEntryRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LaunchHistoryRow(entry: LaunchHistoryEntry) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            if (entry.succeeded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (entry.succeeded) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = entry.packageName,
                tint = if (entry.succeeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${entry.packageName} v${entry.versionName}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = entry.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${entry.mode.name} • lib=${entry.mainLibLoadSource}${if (entry.usedFallback) " • handoff" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = entry.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val accent = when (entry.level) {
        LogLevel.INFO -> MaterialTheme.colorScheme.secondary
        LogLevel.SUCCESS -> MaterialTheme.colorScheme.primary
        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "[${entry.level.name}] [${entry.stage}]",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = accent
                )
                Text(
                    text = entry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = entry.message,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
