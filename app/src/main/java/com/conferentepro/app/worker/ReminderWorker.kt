package com.conferentepro.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.conferentepro.app.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ReminderWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(ctx)
        val demandas = database.demandaDao().getAllDemandas().first()
        val agora = System.currentTimeMillis()
        val atrasadas = demandas.filter { !it.concluida && it.dataPrazoFinal < agora }

        if (atrasadas.isNotEmpty()) {
            val channelId = "CONFERENTE_ALERTAS"
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(channelId, "Alertas Conferente", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            val notif = NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Atenção Conferente: Demandas Atrasadas")
                .setContentText("Você possui ${atrasadas.size} pendência(s) vencidas!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            manager.notify(101, notif)
        }
        return Result.success()
    }

    companion object {
        fun agendarVerificacaoDiaria(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("ConferenteVerificacao", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
