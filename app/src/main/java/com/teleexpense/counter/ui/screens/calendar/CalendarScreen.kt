package com.teleexpense.counter.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import com.teleexpense.counter.domain.model.TelecomTransaction
import com.teleexpense.counter.ui.screens.home.TransactionCard
import com.teleexpense.counter.ui.screens.home.formatEtb
import com.teleexpense.counter.ui.theme.EthioGreen
import com.teleexpense.counter.ui.viewmodel.MainViewModel

@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onDayClick: (Int) -> Unit
) {
    val today = EthiopianCalendarConverter.today()
    var year by remember { mutableIntStateOf(today.year) }
    var month by remember { mutableIntStateOf(today.month) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    val txs = viewModel.transactions.collectAsState().value
    val monthTxs = txs.filter { it.ethiopianYear == year && it.ethiopianMonth == month }
    val daysWithSpend = monthTxs.groupBy { it.ethiopianDay }
    val daysInMonth = EthiopianCalendarConverter.daysInMonth(year, month)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (month == 1) {
                    month = 13
                    year--
                } else month--
                selectedDay = null
            }) {
                Icon(Icons.Default.ChevronLeft, null)
            }
            Text(
                EthiopianCalendarConverter.monthLabel(year, month),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(onClick = {
                if (month == 13) {
                    month = 1
                    year++
                } else month++
                selectedDay = null
            }) {
                Icon(Icons.Default.ChevronRight, null)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Weekday headers
        Row(Modifier.fillMaxWidth()) {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { d ->
                Text(
                    d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Simple grid: Ethiopian months start on varying weekdays;
        // for v1 we start day 1 in the first cell (no weekday offset calculation for brevity).
        // A production version would compute the weekday of Meskerem 1 etc.
        val rows = (daysInMonth + 6) / 7
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val day = row * 7 + col + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .then(
                                if (day <= daysInMonth) {
                                    Modifier.clickable { selectedDay = day }
                                } else Modifier
                            )
                            .background(
                                when {
                                    day == selectedDay -> EthioGreen
                                    day == today.day && year == today.year && month == today.month ->
                                        EthioGreen.copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day <= daysInMonth) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$day",
                                    color = if (day == selectedDay) Color.White
                                    else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = if (daysWithSpend.containsKey(day)) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (daysWithSpend.containsKey(day)) {
                                    Box(
                                        Modifier
                                            .size(5.dp)
                                            .background(
                                                if (day == selectedDay) Color.White else EthioGreen,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (selectedDay != null) {
            val dayTxs = daysWithSpend[selectedDay] ?: emptyList()
            val total = dayTxs.sumOf { it.amount }
            Text(
                "${EthiopianCalendarConverter.monthName(month)} $selectedDay, $year",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "Total: ${formatEtb(total)}",
                style = MaterialTheme.typography.bodyLarge,
                color = EthioGreen
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayTxs) { tx ->
                    TransactionCard(tx, onClick = {})
                }
            }
        } else {
            val monthTotal = monthTxs.sumOf { it.amount }
            Text(
                "Month total: ${formatEtb(monthTotal)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
