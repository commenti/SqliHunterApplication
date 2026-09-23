package com.yourapp.sqliautohunter.ui.screens.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.sqliautohunter.ui.theme.TextPrimary
import com.yourapp.sqliautohunter.ui.theme.TextSecondary

@Composable
fun CrashLogScreen(
    onNavigateBack: () -> Unit,
    viewModel: CrashLogViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedModule by viewModel.selectedModule.collectAsState()
    val selectedSeverity by viewModel.selectedSeverity.collectAsState()

    var searchExpanded by remember { mutableStateOf(false) }
    var moduleExpanded by remember { mutableStateOf(false) }
    var severityExpanded by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    // Get unique modules and severities
    val modules = remember(state.logs) {
        state.logs.map { it.moduleName }.distinct().sorted()
    }

    val severities = remember(state.logs) {
        state.logs.map { it.severity }.distinct().sorted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "Crash Logs",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Total: ${state.totalCount}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Text(
                text = "Filtered: ${state.filteredCount}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextSecondary
                            )

                            Spacer(modifier = Modifier.size(8.dp))

                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search logs...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            innerTextField()

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onSearchQueryChanged("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // Module filter
            Box {
                Button(
                    onClick = { moduleExpanded = !moduleExpanded },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = selectedModule ?: "Module",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }

                DropdownMenu(
                    expanded = moduleExpanded,
                    onDismissRequest = { moduleExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            viewModel.onModuleSelected(null)
                            moduleExpanded = false
                        }
                    )

                    modules.forEach { module ->
                        DropdownMenuItem(
                            text = { Text(module) },
                            onClick = {
                                viewModel.onModuleSelected(module)
                                moduleExpanded = false
                            }
                        )
                    }
                }
            }

            // Severity filter
            Box {
                Button(
                    onClick = { severityExpanded = !severityExpanded },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = selectedSeverity ?: "Severity",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }

                DropdownMenu(
                    expanded = severityExpanded,
                    onDismissRequest = { severityExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            viewModel.onSeveritySelected(null)
                            severityExpanded = false
                        }
                    )

                    severities.forEach { severity ->
                        DropdownMenuItem(
                            text = { Text(severity) },
                            onClick = {
                                viewModel.onSeveritySelected(severity)
                                severityExpanded = false
                            }
                        )
                    }
                }
            }

            // Clear filters
            IconButton(
                onClick = { viewModel.clearFilters() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear Filters"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logs list
        if (state.logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No logs found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.logs) { log ->
                    LogListItem(
                        log = log,
                        onClick = { clipboardManager.setText(AnnotatedString(log.stackTrace)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.clearAllLogs() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear"
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Clear All")
            }

            Button(
                onClick = { viewModel.exportLogs() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Export"
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Export")
            }
        }
    }
}

@Composable
fun LogListItem(
    log: com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Severity indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            when (log.severity) {
                                "Fatal" -> MaterialTheme.colorScheme.error
                                "Error" -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                "Warning" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            shape = MaterialTheme.shapes.extraSmall
                        )
                )

                Spacer(modifier = Modifier.size(8.dp))

                // Module and timestamp
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = log.moduleName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    Text(
                        text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(log.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Severity text
                Text(
                    text = log.severity,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (log.severity) {
                        "Fatal" -> MaterialTheme.colorScheme.error
                        "Error" -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        "Warning" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            Text(
                text = log.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stack trace preview
            if (log.stackTrace.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraSmall
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = log.stackTrace.take(200) + if (log.stackTrace.length > 200) "..." else "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = TextSecondary
                    )
                }
            }

            // URL if available
            log.urlBeingProcessed?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "URL: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
