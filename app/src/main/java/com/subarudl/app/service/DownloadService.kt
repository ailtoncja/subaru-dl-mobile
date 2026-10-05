package com.subarudl.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.subarudl.app.R
import com.subarudl.app.SubaruApp
import com.subarudl.app.data.SettingsStore
import com.subarudl.app.data.Status
import com.subarudl.app.util.formatSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Mantém o app vivo (com uma notificação de progresso) enquanto há downloads rodando. */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observer: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = (application as SubaruApp).manager
        when (intent?.action) {
            ACTION_PAUSE_ALL -> manager.pauseAll()
            ACTION_RESUME_ALL -> manager.resumeAll()
        }
        startForegroundCompat(build(manager))
        wakeLock = wakeLock ?: (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SubaruDL:downloads").apply { acquire(6 * 3600_000L) }
        if (observer == null) {
            observer = scope.launch {
                var idle = 0
                while (true) {
                    delay(1000)
                    if (manager.busy) {
                        idle = 0
                        notify(build(manager))
                    } else if (++idle >= 3) {
                        stopSelf()
                        break
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun build(manager: DownloadManager): Notification {
        val items = manager.items.value
        val active = items.filter { it.active }
        val waiting = items.count { it.status == Status.WAITING }
        val speed = active.sumOf { it.speed }
        val first = active.firstOrNull()
        val title = when {
            active.isEmpty() && waiting > 0 -> "Na fila: $waiting"
            active.size == 1 -> first!!.title
            else -> "${active.size} downloads em andamento"
        }
        val net = manager.net.value
        val text = when {
            active.isEmpty() -> if (!net.online) "Sem internet" else if (!net.wifi && SettingsStore.value.wifiOnly) "Aguardando Wi-Fi" else "Preparando…"
            else -> "${first!!.stage.ifEmpty { "Baixando" }} • ${formatSpeed(speed)}" + if (waiting > 0) " • +$waiting na fila" else ""
        }
        val progress = if (active.isEmpty()) 0 else (active.map { it.progress }.average() * 100).toInt()
        val pause = PendingIntent.getService(
            this, 1, Intent(this, DownloadService::class.java).setAction(ACTION_PAUSE_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, Notifier.CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, progress, active.isEmpty() || (first?.progress ?: 0f) <= 0f)
            .setContentIntent(Notifier.openApp(this, "queue"))
            .addAction(0, "Pausar tudo", pause)
            .build()
    }

    private fun startForegroundCompat(n: Notification) {
        startForeground(Notifier.ID_FOREGROUND, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun notify(n: Notification) {
        runCatching { getSystemService(NotificationManager::class.java).notify(Notifier.ID_FOREGROUND, n) }
    }

    override fun onDestroy() {
        (application as SubaruApp).manager.serviceStopped()
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PAUSE_ALL = "com.subarudl.app.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.subarudl.app.RESUME_ALL"
    }
}
