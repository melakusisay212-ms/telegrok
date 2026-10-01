package com.teleexpense.counter.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teleexpense.counter.domain.model.Category
import com.teleexpense.counter.ui.screens.home.categoryLabel
import com.teleexpense.counter.ui.screens.home.formatEtb
import com.teleexpense.counter.ui.theme.EthioGreen
import com.teleexpense.counter.ui.viewmodel.MainViewModel
import com.teleexpense.counter.ui.viewmodel.Period

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    var period by remember { mutableStateOf(Period.THIS_MONTH) }
    val summary = viewModel.summaryForPeriod(period)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Where is my money going?",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            // Period chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Period.TODAY to "Today",
                    Period.THIS_WEEK to "Week",
                    Period.THIS_MONTH to "Month",
                    Period.THIS_YEAR to "Year"
                ).forEach { (p, label) ->
                    FilterChip(
                        selected = period == p,
                        onClick = { period = p },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EthioGreen)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(summary.ethiopianLabel, color = Color.White.copy(0.9f))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatEtb(summary.totalAmount),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${summary.transactionCount} transactions",
                        color = Color.White.copy(0.85f)
                    )
                }
            }
        }

        item {
            Text("By category", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            if (summary.breakdown.isEmpty()) {
                Text("No data for this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                summary.breakdown.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(categoryLabel(item.category), fontWeight = FontWeight.Medium)
                                Text(
                                    "${item.count} tx · ${"%.0f".format(item.percentage)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                formatEtb(item.amount),
                                fontWeight = FontWeight.Bold,
                                color = EthioGreen
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Averages", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Per day", formatEtb(summary.averagePerDay), Modifier.weight(1f))
                StatCard("Per tx", formatEtb(summary.averagePerTransaction), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
