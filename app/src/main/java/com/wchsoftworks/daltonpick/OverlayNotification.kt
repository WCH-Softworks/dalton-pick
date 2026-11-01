package com.wchsoftworks.daltonpick

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.wchsoftworks.daltonpick.ui.Screen

/** The persistent notification with Hide/Show and Settings actions (spec §5.5). */
object OverlayNotification {
    const val ID = 1
    private const val CHANNEL_ID = "overlay"

    fun build(context: Context, tabVisible: Boolean, withStopAction: Boolean): Notification {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(context, 0, MainActivity.intent(context), PendingIntent.FLAG_IMMUTABLE)
        val settings = PendingIntent.getActivity(
            context, 3, MainActivity.intent(context, Screen.SETTINGS), PendingIntent.FLAG_IMMUTABLE,
        )
        val toggle = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, OverlayActionReceiver::class.java).setAction(OverlayActionReceiver.ACTION_TOGGLE_TAB),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val toggleLabel = context.getString(if (tabVisible) R.string.notif_hide else R.string.notif_show)
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lens)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(if (tabVisible) R.string.notif_text_shown else R.string.notif_text_hidden))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, toggleLabel, toggle).build())
            .addAction(Notification.Action.Builder(null, context.getString(R.string.notif_settings), settings).build())
        if (withStopAction) {
            val stop = PendingIntent.getService(
                context, 2,
                Intent(context, ProjectionOverlayService::class.java).setAction(ProjectionOverlayService.ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(Notification.Action.Builder(null, context.getString(R.string.notif_stop), stop).build())
        }
        return builder.build()
    }
}
