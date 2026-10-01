package com.teleexpense.counter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.teleexpense.counter.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives new SMS after tracking has started.
 * Only processes messages that arrive after trackerStartedAt.
 * Full body is parsed in memory and never persisted.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ExpenseRepository(context.applicationContext)
                val started = repo.getTrackerStartedAt() ?: return@launch
                // Process only new messages since last processed
                repo.scanNewMessages()
            } finally {
                pending.finish()
            }
        }
    }
}
