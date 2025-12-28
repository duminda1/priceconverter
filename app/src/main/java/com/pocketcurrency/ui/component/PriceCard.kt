package com.pocketcurrency.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.pocketcurrency.domain.model.ConversionResult
import com.pocketcurrency.domain.model.RateSource
import android.text.format.DateUtils
import java.text.DecimalFormat
import kotlin.math.abs

@Composable
fun PriceCard(modifier: Modifier = Modifier, result: ConversionResult) {

    val relativeUpdated = DateUtils.getRelativeTimeSpanString(
        result.rate.lastUpdatedMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )

    val sourceLabel = when (result.rate.source) {
        RateSource.LIVE -> "Live"
        RateSource.SAVED -> "Saved"
        RateSource.MANUAL -> "Offline"
    }
    val timeLabel = "Updated"

    val formattedFrom = formatDisplayAmount(result.from.amount)
    val formattedTo = formatDisplayAmount(result.convertedAmount)
    val formattedRate = formatDisplayAmount(result.rate.rate)
    val readableAmount = formatLargeAmount(result.convertedAmount)
    val reassuranceColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Converted amount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$formattedTo ${result.toCurrency}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (readableAmount != null) {
                Text(
                    text = "≈ $readableAmount ${result.toCurrency} ($formattedTo)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("From $formattedFrom ${result.from.currency}")
                InfoPill("Rate $formattedRate")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            Text(
                text = "$sourceLabel · $timeLabel $relativeUpdated",
                style = MaterialTheme.typography.bodyMedium,
                color = reassuranceColor
            )
        }
    }
}

internal fun formatDisplayAmount(value: Double): String {
    val absValue = abs(value)
    val formatter = DecimalFormat("#,##0.00")
    if (absValue < 1) {
        formatter.maximumFractionDigits = 6
    }
    return formatter.format(value)
}

private fun formatLargeAmount(value: Double): String? {
    val absValue = abs(value)
    val (scaled, unit) = when {
        absValue >= 1_000_000_000_000 -> absValue / 1_000_000_000_000 to "trillion"
        absValue >= 1_000_000_000 -> absValue / 1_000_000_000 to "billion"
        absValue >= 1_000_000 -> absValue / 1_000_000 to "million"
        else -> return null
    }

    val formatted = DecimalFormat("#,##0.#").format(scaled)
    val sign = if (value < 0) "-" else ""
    return "$sign$formatted $unit"
}

@Composable
private fun InfoPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
