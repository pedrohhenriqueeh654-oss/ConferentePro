package com.conferentepro.app

import android.app.Application
import com.conferentepro.app.worker.ReminderWorker

class ConferenteProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderWorker.agendarVerificacaoDiaria(this)
    }
}
