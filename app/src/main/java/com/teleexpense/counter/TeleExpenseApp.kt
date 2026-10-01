package com.teleexpense.counter

import android.app.Application
import com.teleexpense.counter.data.repository.ExpenseRepository

class TeleExpenseApp : Application() {
    lateinit var repository: ExpenseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ExpenseRepository(this)
    }
}
