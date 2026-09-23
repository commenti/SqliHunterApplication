package com.yourapp.sqliautohunter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourapp.sqliautohunter.domain.model.ConfidenceLevel
import com.yourapp.sqliautohunter.ui.theme.Success
import com.yourapp.sqliautohunter.ui.theme.VulnerableRed
import com.yourapp.sqliautohunter.ui.theme.Warning

@Composable
fun ConfidenceBadge(
    confidence: ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (confidence) {
        ConfidenceLevel.HIGH -> VulnerableRed to "HIGH"
        ConfidenceLevel.MEDIUM -> Warning to "MEDIUM"
        ConfidenceLevel.LOW -> Color(0xFF9E9E9E) to "LOW"
    }

    Box(
        modifier = modifier
            .background(
                color.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
