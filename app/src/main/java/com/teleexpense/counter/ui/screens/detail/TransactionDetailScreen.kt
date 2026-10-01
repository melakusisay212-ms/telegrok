package com.teleexpense.counter.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import com.teleexpense.counter.ui.screens.home.categoryLabel
import com.teleexpense.counter.ui.screens.home.formatEtb
import com.teleexpense.counter.ui.theme.EthioGreen
import com.teleexpense.counter.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: MainViewModel,
    transactionId: String,
    onBack: () -> Unit
) {
    val tx = viewModel.getTransaction(transactionId)
    if (tx == null) {
        Box(Modifier.fillMaxSize()) {
            Text("Transaction not found")
        }
        return
    }

    val gDate = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date(tx.gregorianDateTimeMillis))
    val gTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date(tx.gregorianDateTimeMillis))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.excludeTransaction(tx.id)
                        onBack()
                    }) {
                        Icon(Icons.Default.Delete, "Mark as not expense")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EthioGreen)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(formatEtb(tx.amount), style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
                    Text(categoryLabel(tx.category), color = MaterialTheme.colorScheme.onPrimary.copy(0.9f))
                }
            }

            DetailRow("Package", tx.packageName ?: "—")
            DetailRow(
                "Ethiopian date",
                "${EthiopianCalendarConverter.monthName(tx.ethiopianMonth)} ${tx.ethiopianDay}, ${tx.ethiopianYear}"
            )
            DetailRow("Gregorian", "$gDate · $gTime")
            DetailRow("Provider", tx.provider.name.replace('_', ' '))
            DetailRow("Direction", tx.direction.name)
            DetailRow("Recipient", tx.recipient ?: "—")
            DetailRow("Transaction ID", tx.transactionId ?: "—")
            DetailRow("Transfer ID", tx.transferId ?: "—")
            DetailRow("Related SMS", "${tx.sourceEventIds.size}")
            DetailRow("Confidence", "${(tx.confidence * 100).toInt()}%")
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
        }
    }
}
