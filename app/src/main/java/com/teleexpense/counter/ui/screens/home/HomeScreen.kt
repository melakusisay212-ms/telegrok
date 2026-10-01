package com.teleexpense.counter.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import com.teleexpense.counter.domain.model.Category
import com.teleexpense.counter.domain.model.TelecomTransaction
import com.teleexpense.counter.ui.theme.EthioGreen
import com.teleexpense.counter.ui.viewmodel.MainViewModel
import com.teleexpense.counter.ui.viewmodel.Period
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onTransactionClick: (String) -> Unit
) {
    val summary = viewModel.currentMonthSummary()
    val recent = viewModel.transactions.collectAsState().value.take(8)
    val eth = EthiopianCalendarConverter.today()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Tele Expense",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Where is your money going?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            // Total card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EthioGreen)
            ) {
                Column(Modifier = Modifier.padding(20.dp)) {
                    Text(
                        EthiopianCalendarConverter.monthLabel(eth.year, eth.month),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This Month",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formatEtb(summary.totalAmount),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${summary.transactionCount} transactions",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Text(
                "Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(8.dp))
            if (summary.breakdown.isEmpty()) {
                Text("No expenses this month yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                CategoryBreakdownChart(summary.breakdown)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        if (recent.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No telecom expenses yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(recent) { tx ->
                TransactionCard(tx, onClick = { onTransactionClick(tx.id) })
            }
        }
    }
}

@Composable
fun CategoryBreakdownChart(breakdown: List<com.teleexpense.counter.domain.model.CategoryBreakdown>) {
    val colors = listOf(
        Color(0xFF008C45), // data
        Color(0xFF2196F3), // airtime
        Color(0xFFFF9800), // sms
        Color(0xFF9C27B0), // other
        Color(0xFF607D8B)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            breakdown.forEachIndexed { index, item ->
                val icon = when (item.category) {
                    Category.DATA_PACKAGE, Category.MIXED_PACKAGE -> "📶"
                    Category.AIRTIME, Category.AIRTIME_RECHARGE -> "📞"
                    Category.SMS_PACKAGE -> "💬"
                    Category.VOICE_PACKAGE -> "🎙️"
                    else -> "📦"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[index % colors.size], CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(icon, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        categoryLabel(item.category),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        formatEtb(item.amount),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionCard(tx: TelecomTransaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (tx.category) {
                Category.DATA_PACKAGE, Category.MIXED_PACKAGE -> "📶"
                Category.AIRTIME, Category.AIRTIME_RECHARGE -> "📞"
                Category.SMS_PACKAGE -> "💬"
                else -> "📦"
            }
            Text(icon, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.packageName ?: categoryLabel(tx.category),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Text(
                    "${EthiopianCalendarConverter.monthName(tx.ethiopianMonth)} ${tx.ethiopianDay}, ${tx.ethiopianYear}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatEtb(tx.amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = EthioGreen
            )
        }
    }
}

fun formatEtb(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US)
    nf.maximumFractionDigits = 2
    nf.minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    return "${nf.format(amount)} ETB"
}

fun categoryLabel(c: Category): String = when (c) {
    Category.AIRTIME, Category.AIRTIME_RECHARGE -> "Airtime"
    Category.DATA_PACKAGE, Category.MIXED_PACKAGE -> "Data"
    Category.SMS_PACKAGE -> "SMS"
    Category.VOICE_PACKAGE -> "Voice"
    Category.FREE_PACKAGE -> "Free"
    Category.PACKAGE_ACTIVATION -> "Activation"
    else -> "Other"
}
