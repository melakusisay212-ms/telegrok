package com.teleexpense.counter.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleexpense.counter.ui.theme.EthioGreen
import com.teleexpense.counter.ui.viewmodel.ScanUiState

@Composable
fun OnboardingScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onScanThisMonth: () -> Unit,
    onStartFromToday: () -> Unit,
    scanState: ScanUiState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        when (scanState) {
            is ScanUiState.Scanning -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = EthioGreen)
                    Spacer(Modifier.height(16.dp))
                    Text("Analyzing telecom messages…", style = MaterialTheme.typography.titleMedium)
                }
            }
            is ScanUiState.Done -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${scanState.result.relevantMessages} relevant messages",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${scanState.result.transactionEvents} transaction events",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${scanState.result.expensesCreated} telecom expenses",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = EthioGreen
                    )
                }
            }
            is ScanUiState.Error -> {
                Text(
                    scanState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(48.dp))
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = EthioGreen
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Welcome to Tele Expense",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your telecom spending,\nautomatically tracked.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = EthioGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Everything stays on your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = EthioGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "No account. No cloud. No upload.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    if (!hasPermission) {
                        Text(
                            "Tele Expense needs SMS access to detect telecom purchases and recharges automatically. Your messages are processed locally on your phone.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EthioGreen)
                        ) {
                            Text("Grant SMS Permission", fontSize = 16.sp)
                        }
                    } else {
                        Text(
                            "Build your starting history?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "We can scan telecom transaction messages from the current month to create your starting spending history.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onScanThisMonth,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EthioGreen)
                        ) {
                            Text("Scan This Month", fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onStartFromToday,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Start From Today", fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
