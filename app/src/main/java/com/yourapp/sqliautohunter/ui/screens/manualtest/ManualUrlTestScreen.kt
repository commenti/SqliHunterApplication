package com.yourapp.sqliautohunter.ui.screens.manualtest

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.sqliautohunter.ui.components.ConfidenceBadge
import com.yourapp.sqliautohunter.ui.theme.TextPrimary
import com.yourapp.sqliautohunter.ui.theme.TextSecondary
import com.yourapp.sqliautohunter.ui.theme.VulnerableRed

@Composable
fun ManualUrlTestScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResults: () -> Unit,
    viewModel: ManualUrlTestViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    val context = LocalContext.current

    LaunchedEffect(state.url) {
        textFieldValue = textFieldValue.copy(text = state.url)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Header with back button
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
                text = "Manual URL Test",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter a URL to test for SQL injection vulnerabilities",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // URL input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    viewModel.onUrlChanged(newValue.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "https://example.com/page.php?id=1",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                        innerTextField()
                    }
                }
            )

            IconButton(
                onClick = {
                    val clipboardText = clipboardManager.getText()?.text ?: ""
                    viewModel.onUrlChanged(clipboardText)
                    textFieldValue = textFieldValue.copy(text = clipboardText)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    tint = TextSecondary
                )
            }

            if (textFieldValue.text.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearUrl() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Already tested message
        if (state.isAlreadyTested) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "This URL was already tested. Previous result: ${state.previousResult}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Test buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.quickTest() },
                modifier = Modifier.weight(1f),
                enabled = !isTesting && textFieldValue.text.isNotEmpty()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Quick Test")
                }
            }

            Button(
                onClick = { viewModel.testUrl() },
                modifier = Modifier.weight(1f),
                enabled = !isTesting && textFieldValue.text.isNotEmpty()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Full Test")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Results
        if (state.isVulnerable != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (state.isVulnerable == true) VulnerableRed.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isVulnerable == true) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = "Result",
                            tint = if (state.isVulnerable == true) VulnerableRed else MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.size(8.dp))

                        Text(
                            text = if (state.isVulnerable == true) "VULNERABLE" else "NOT VULNERABLE",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.isVulnerable == true) VulnerableRed else MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        state.confidence?.let { confidence ->
                            ConfidenceBadge(confidence)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vulnerability types
                    if (state.vulnerabilityTypes.isNotEmpty()) {
                        Text(
                            text = "Types: ${state.vulnerabilityTypes.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    // Detection results
                    if (state.detectionResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Detection results:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        state.detectionResults.forEach { result ->
                            Text(
                                text = "• ${result.type}: ${result.payload} (${"%.2f".format(result.confidence)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Error message
        state.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { viewModel.clearError() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onNavigateToResults
            ) {
                Text("View All Results")
            }
        }
    }
}
