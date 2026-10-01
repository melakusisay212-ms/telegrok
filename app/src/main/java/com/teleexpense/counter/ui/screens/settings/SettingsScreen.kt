package com.teleexpense.counter.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teleexpense.counter.ui.theme.EthioGreen
import com.teleexpense.counter.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Card(shape = RoundedCornerShape(14.dp)) {
            ListItem(
                headlineContent = { Text("Scan new messages") },
                supportingContent = { Text("Process SMS received after tracking started") },
                leadingContent = { Icon(Icons.Default.Refresh, null, tint = EthioGreen) },
                modifier = Modifier.fillMaxWidth(),
                trailingContent = {
                    TextButton(onClick = { viewModel.scanNewMessages() }) {
                        Text("Scan")
                    }
                }
            )
        }

        Card(shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = EthioGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Privacy", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "• No account required\n" +
                            "• No backend or cloud database\n" +
                            "• SMS never leave your phone\n" +
                            "• Only extracted transaction data is stored\n" +
                            "• Full SMS bodies are not retained",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, tint = EthioGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("About", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tele Expense Counter v1.0\nOffline-first Ethiopian telecom expense tracker.\nProcesses SMS locally with a rule-based parser.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
