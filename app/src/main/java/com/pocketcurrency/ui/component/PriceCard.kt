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
import com.pocketcurrency.domain.model.ConversionResult
import com.pocketcurrency.domain.model.RateSource
import android.text.format.DateUtils
import java.text.DecimalFormat

@Composable
fun PriceCard(result: ConversionResult) {

    val relativeUpdated = DateUtils.getRelativeTimeSpanString(
        result.rate.lastUpdatedMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )

    val sourceLabel = when (result.rate.source) {
        RateSource.LIVE -> "Live"
        RateSource.SAVED -> "Saved"
        RateSource.MANUAL -> "Manual"
    }
    val timeLabel = "Updated"

    val decimalFormat = DecimalFormat("#,##0.00")
    val formattedFrom = decimalFormat.format(result.from.amount)
    val formattedTo = decimalFormat.format(result.convertedAmount)
    val formattedRate = decimalFormat.format(result.rate.rate)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Converted amount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$formattedTo ${result.toCurrency}",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("From $formattedFrom ${result.from.currency}")
                InfoPill("Rate $formattedRate")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            Text(
                text = "$sourceLabel · $timeLabel $relativeUpdated",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
