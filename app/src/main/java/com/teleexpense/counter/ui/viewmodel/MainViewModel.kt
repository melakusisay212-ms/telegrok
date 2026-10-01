package com.teleexpense.counter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teleexpense.counter.data.repository.ExpenseRepository
import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import com.teleexpense.counter.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.GregorianCalendar

class MainViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _onboardingDone = MutableStateFlow(false)
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    val transactions: StateFlow<List<TelecomTransaction>> = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPeriod = MutableStateFlow(Period.THIS_MONTH)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod.asStateFlow()

    init {
        viewModelScope.launch {
            _onboardingDone.value = repository.isOnboardingDone()
        }
    }

    fun scanThisMonth() {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning
            try {
                val result = repository.scanCurrentMonth()
                _scanState.value = ScanUiState.Done(result)
                _onboardingDone.value = true
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun startFromToday() {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning
            repository.startFromToday()
            _scanState.value = ScanUiState.Done(
                ExpenseRepository.ScanResult(0, 0, 0, emptyList())
            )
            _onboardingDone.value = true
        }
    }

    fun scanNewMessages() {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning
            try {
                val result = repository.scanNewMessages()
                _scanState.value = ScanUiState.Done(result)
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun setPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    fun currentMonthSummary(): PeriodSummary {
        val eth = EthiopianCalendarConverter.today()
        val txs = transactions.value.filter {
            it.ethiopianYear == eth.year && it.ethiopianMonth == eth.month && !it.isExcluded
        }
        return buildSummary(txs, EthiopianCalendarConverter.monthLabel(eth.year, eth.month))
    }

    fun summaryForPeriod(period: Period): PeriodSummary {
        val now = System.currentTimeMillis()
        val cal = GregorianCalendar()
        val (from, to, label) = when (period) {
            Period.TODAY -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                Triple(start, now, "Today")
            }
            Period.THIS_WEEK -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Triple(cal.timeInMillis, now, "This Week")
            }
            Period.THIS_MONTH -> {
                val eth = EthiopianCalendarConverter.today()
                val start = EthiopianCalendarConverter.startOfEthiopianMonthMillis(eth.year, eth.month)
                Triple(start, now, EthiopianCalendarConverter.monthLabel(eth.year, eth.month))
            }
            Period.THIS_YEAR -> {
                val eth = EthiopianCalendarConverter.today()
                val start = EthiopianCalendarConverter.startOfEthiopianMonthMillis(eth.year, 1)
                Triple(start, now, "Year ${eth.year}")
            }
            Period.CUSTOM -> Triple(0L, now, "Custom")
        }
        val txs = transactions.value.filter {
            !it.isExcluded && it.gregorianDateTimeMillis in from until to
        }
        return buildSummary(txs, label)
    }

    private fun buildSummary(txs: List<TelecomTransaction>, label: String): PeriodSummary {
        val total = txs.sumOf { it.amount }
        val byCat = txs.groupBy { simplifyCategory(it.category) }
            .map { (cat, list) ->
                val amt = list.sumOf { it.amount }
                CategoryBreakdown(cat, amt, if (total > 0) ((amt / total) * 100).toFloat() else 0f, list.size)
            }
            .sortedByDescending { it.amount }
        val days = 1 // simplified
        return PeriodSummary(label, label, total, txs.size, total / days.coerceAtLeast(1), if (txs.isNotEmpty()) total / txs.size else 0.0, byCat)
    }

    private fun simplifyCategory(c: Category): Category = when (c) {
        Category.AIRTIME, Category.AIRTIME_RECHARGE -> Category.AIRTIME
        Category.DATA_PACKAGE, Category.MIXED_PACKAGE -> Category.DATA_PACKAGE
        Category.SMS_PACKAGE -> Category.SMS_PACKAGE
        Category.VOICE_PACKAGE -> Category.VOICE_PACKAGE
        else -> Category.OTHER
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }

    fun excludeTransaction(id: String) {
        viewModelScope.launch { repository.excludeTransaction(id) }
    }

    fun updateTransaction(tx: TelecomTransaction) {
        viewModelScope.launch { repository.updateTransaction(tx) }
    }

    fun getTransaction(id: String): TelecomTransaction? =
        transactions.value.find { it.id == id }
}

enum class Period { TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM }

sealed class ScanUiState {
    data object Idle : ScanUiState()
    data object Scanning : ScanUiState()
    data class Done(val result: ExpenseRepository.ScanResult) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

class MainViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository) as T
    }
}
