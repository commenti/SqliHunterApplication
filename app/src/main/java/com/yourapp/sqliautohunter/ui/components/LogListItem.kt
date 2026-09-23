package com.yourapp.sqliautohunter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourapp.sqliautohunter.ui.theme.TextPrimary
import com.yourapp.sqliautohunter.ui.theme.TextSecondary

@Composable
fun LogListItem(
    log: com.yourapp.sqliautohunter.data.local.database.entity.CrashLogEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            )
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

@Composable
fun Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
        content = content
    )
}
