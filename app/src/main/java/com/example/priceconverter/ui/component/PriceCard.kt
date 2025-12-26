package com.example.priceconverter.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.priceconverter.domain.model.ConversionResult
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceCard(result: ConversionResult) {

    val lastUpdatedFormatted = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.getDefault()
    ).format(Date(result.rate.lastUpdatedMillis))

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("From: ${result.from.amount} ${result.from.currency}")
            Text("To: ${result.convertedAmount} ${result.toCurrency}")
            Text("Rate: ${result.rate.rate}")
            Text("Last updated: $lastUpdatedFormatted")
        }
    }
}
