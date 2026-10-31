package com.conferentepro.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.conferentepro.app.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ReminderWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(ctx)
        val demandas = database.demandaDao().getAllDemandas().first()
        val agora = System.currentTimeMillis()
        val atrasadas = demandas.filter { !it.concluida && it.dataPrazoFinal < agora }

        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "CONFERENTE_ALERTAS"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Alertas Conferente", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        if (atrasadas.isNotEmpty()) {
            val notif = NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Atenção Conferente: Demandas Atrasadas")
                .setContentText("Você possui ${atrasadas.size} pendência(s) vencidas!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify(101, notif)
        }

        // This reminder is calendar-based and does not depend on any product being registered.
        val today = LocalDate.now()
        val cdState = MonthlyCdCycle.currentState(ctx, today)
        if (!cdState.closed && MonthlyCdCycle.shouldNotifyToday(ctx, today)) {
            val text = if (cdState.overdue) {
                "Prazo ultrapassado: o envio dos vencidos de ${cdState.month.monthValue.toString().padStart(2, '0')}/${cdState.month.year} ainda não foi encerrado."
            } else {
                "Envie ao CD os produtos vencidos no mês anterior. Prazo até o dia 15."
            }
            val notification = NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Envio de vencidos ao CD")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify(102, notification)
        }
        return Result.success()
    }

    companion object {
        fun agendarVerificacaoDiaria(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "ConferenteVerificacao",
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }
}
