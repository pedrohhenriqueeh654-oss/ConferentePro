package com.conferentepro.app.worker

import android.content.Context
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Keeps the monthly CD shipment cycle independent from product registrations.
 * A cycle refers to expired products from the previous calendar month.
 */
object MonthlyCdCycle {
    private const val PREFS = "monthly_cd_cycle"
    private fun closedKey(month: YearMonth) = "closed_month_$month"
    private const val KEY_LAST_NOTIFICATION_DAY = "last_notification_day"

    data class State(
        val month: YearMonth,
        val closed: Boolean,
        val overdue: Boolean,
        val daysRemaining: Long
    ) {
        val title: String get() = "Envio de vencidos ao CD"
        val description: String
            get() = when {
                closed -> "Ciclo de ${month.format(DateTimeFormatter.ofPattern("MM/yyyy"))} encerrado."
                overdue -> "Prazo ultrapassado. O envio ainda não foi marcado como concluído."
                else -> "Envie ao CD os produtos vencidos em ${month.format(DateTimeFormatter.ofPattern("MM/yyyy"))}. Prazo: dias 1 a 15 deste mês."
            }
    }

    fun currentState(context: Context, today: LocalDate = LocalDate.now()): State {
        val cycleMonth = YearMonth.from(today).minusMonths(1)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val closed = prefs.getBoolean(closedKey(cycleMonth), false)
        val overdue = today.dayOfMonth > 15
        return State(cycleMonth, closed, overdue, (16L - today.dayOfMonth).coerceAtLeast(0L))
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
