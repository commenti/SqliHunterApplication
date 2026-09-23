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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourapp.sqliautohunter.data.local.database.entity.VulnerabilityResultEntity
import com.yourapp.sqliautohunter.ui.theme.TextPrimary
import com.yourapp.sqliautohunter.ui.theme.TextSecondary
import com.yourapp.sqliautohunter.ui.theme.VulnerableRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultListItem(
    result: VulnerabilityResultEntity,
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
            // Confidence badge
            ConfidenceBadge(result.confidence)

            Spacer(modifier = Modifier.size(8.dp))

            // URL and date
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = result.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        .format(Date(result.discoveredAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Vulnerability type
            Text(
                text = result.vulnType.name,
                style = MaterialTheme.typography.bodySmall,
                color = VulnerableRed
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Payload and keyword
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Payload: ${result.payloadUsed.take(30)}${if (result.payloadUsed.length > 30) "..." else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = result.keywordSource,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Response snippet preview
        if (result.responseSnippet.isNotEmpty()) {
            Text(
                text = result.responseSnippet.take(100) + if (result.responseSnippet.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Divider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
