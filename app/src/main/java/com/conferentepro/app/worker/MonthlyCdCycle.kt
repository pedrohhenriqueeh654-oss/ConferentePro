package com.conferentepro.app.worker

import android.content.Context
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

/**
 * Monthly shipment cycle: products expired in the previous month must be sent
 * to the CD by the 15th day of the current month.
 */
object MonthlyCdCycle {
    private const val PREFS = "monthly_cd_cycle"
    private fun closedKey(month: YearMonth) = "closed_month_$month"
    private const val KEY_LAST_NOTIFICATION_DAY = "last_notification_day"

    data class State(
        val month: YearMonth,
        val deadline: LocalDate,
        val closed: Boolean,
        val overdue: Boolean,
        val daysRemaining: Long
    ) {
        val title: String get() = "Envio de vencidos ao CD"
        val description: String
            get() = when {
                closed -> "Envio dos produtos vencidos em ${month.format(DateTimeFormatter.ofPattern("MM/yyyy"))} concluído."
                overdue -> "O prazo para envio terminou. Regularize o envio ao CD."
                else -> "Produtos vencidos em ${month.format(DateTimeFormatter.ofPattern("MM/yyyy"))} devem ser enviados até 15/${deadline.format(DateTimeFormatter.ofPattern("MM/yyyy"))}."
            }
    }

    fun currentState(context: Context, today: LocalDate = LocalDate.now()): State {
        val cycleMonth = YearMonth.from(today).minusMonths(1)
        val deadline = YearMonth.from(today).atDay(15)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val closed = prefs.getBoolean(closedKey(cycleMonth), false)
        val overdue = today.isAfter(deadline)
        val daysRemaining = if (overdue) 0L else ChronoUnit.DAYS.between(today, deadline).coerceAtLeast(0L)
        return State(cycleMonth, deadline, closed, overdue, daysRemaining)
    }

    fun closeCurrentCycle(context: Context, today: LocalDate = LocalDate.now()) {
        val cycleMonth = YearMonth.from(today).minusMonths(1)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(closedKey(cycleMonth), true)
            .apply()
    }

    fun shouldNotifyToday(context: Context, today: LocalDate = LocalDate.now()): Boolean {
        val state = currentState(context, today)
        if (state.closed) return false
        val todayKey = today.toString()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_NOTIFICATION_DAY, null) == todayKey) return false
        prefs.edit().putString(KEY_LAST_NOTIFICATION_DAY, todayKey).apply()
        return true
    }
}
