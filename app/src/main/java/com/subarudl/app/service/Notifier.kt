package com.subarudl.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.subarudl.app.MainActivity
import com.subarudl.app.R
import com.subarudl.app.data.QueueItem
import com.subarudl.app.data.SettingsStore

object Notifier {
    const val CH_PROGRESS = "progresso"
    const val CH_DONE = "concluidos"
    const val CH_DONE_SOUND = "concluidos_som"
    const val ID_FOREGROUND = 1
    const val EXTRA_TAB = "tab"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, "Downloads em andamento", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Mostra o progresso enquanto baixa"
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Downloads concluídos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisa quando um download termina ou falha"
                setSound(null, null)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE_SOUND, "Downloads concluídos (com som)", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisa com som quando um download termina"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
                )
            },
        )
    }

    fun canNotify(ctx: Context) =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun openApp(ctx: Context, tab: String = "queue"): PendingIntent =
        PendingIntent.getActivity(
            ctx, tab.hashCode(),
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(EXTRA_TAB, tab),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun finished(ctx: Context, item: QueueItem, ok: Boolean) {
        if (!SettingsStore.value.notifyDone || !canNotify(ctx)) return
        val channel = if (SettingsStore.value.soundDone) CH_DONE_SOUND else CH_DONE
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(if (ok) "Download concluído" else "Download falhou")
            .setContentText(if (ok) item.title else (item.error ?: item.title))
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (ok) item.title else "${item.title}\n${item.error ?: ""}"))
            .setContentIntent(openApp(ctx, if (ok) "queue-done" else "queue-fail"))
            .setAutoCancel(true)
            .build()
        runCatching { ctx.getSystemService(NotificationManager::class.java).notify(item.id.hashCode(), n) }
    }
}
