package com.teleexpense.counter.calendar

import com.google.common.truth.Truth.assertThat
import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import org.junit.Test

class EthiopianCalendarTest {

    @Test
    fun knownDate_september11_2026_isMeskerem1_2019() {
        // Ethiopian New Year 2019 EE falls on 11 Sep 2026 (non-leap Gregorian)
        val eth = EthiopianCalendarConverter.toEthiopian(2026, 9, 11)
        assertThat(eth.year).isEqualTo(2019)
        assertThat(eth.month).isEqualTo(1) // Meskerem
        assertThat(eth.day).isEqualTo(1)
    }

    @Test
    fun september17_2026_convertsCorrectly() {
        val eth = EthiopianCalendarConverter.toEthiopian(2026, 9, 17)
        assertThat(eth.year).isEqualTo(2019)
        assertThat(eth.month).isEqualTo(1)
        assertThat(eth.day).isEqualTo(7)
    }

    @Test
    fun monthNames() {
        assertThat(EthiopianCalendarConverter.monthName(1)).isEqualTo("Meskerem")
        assertThat(EthiopianCalendarConverter.monthName(13)).isEqualTo("Pagume")
    }

    @Test
    fun leapYear_pagumeHas6Days() {
        assertThat(EthiopianCalendarConverter.isEthiopianLeapYear(2015)).isTrue()
        assertThat(EthiopianCalendarConverter.daysInMonth(2015, 13)).isEqualTo(6)
        assertThat(EthiopianCalendarConverter.daysInMonth(2019, 13)).isEqualTo(5)
    }

    @Test
    fun daysInRegularMonth_is30() {
        for (m in 1..12) {
            assertThat(EthiopianCalendarConverter.daysInMonth(2019, m)).isEqualTo(30)
        }
    }
}
