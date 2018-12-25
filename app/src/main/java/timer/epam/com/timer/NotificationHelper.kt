package timer.epam.com.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat

class NotificationHelper private constructor() {
    private var manager: NotificationManager? = null
    private var builder: NotificationCompat.Builder? = null

    fun createNotificationManager(context: Context) {
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager?.createNotificationChannel(NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW))
        }
    }

    fun createInitialNotification(context: Context) {
        builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setContentText(CONTENT_TEXT)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    }

    fun switchButtonBuilder(context: Context, newTitle: String, action: String, result: String): NotificationCompat.Builder? {
        builder?.mActions?.clear()

        builder?.addAction(R.drawable.ic_timer,
                STOP,
                PendingIntent.getService(context, 1, Intent(context, TimerService::class.java).apply {
                    this.action = ACTION_STOP
                }, PendingIntent.FLAG_UPDATE_CURRENT))
                ?.addAction(R.drawable.ic_timer,
                        newTitle,
                        PendingIntent.getService(context, 2, Intent(context, TimerService::class.java).apply {
                            this.action = action
                        }, PendingIntent.FLAG_UPDATE_CURRENT))
                ?.setContentTitle(result)
                ?.setContentText(if (action == ACTION_PAUSE) CONTENT_TEXT else CONTENT_TEXT_PAUSE)

        return builder
    }

    fun finishedNotification() {
        val iterator = builder!!.mActions.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }

        builder?.setContentTitle(FINISHED)
        manager?.notify(1, builder?.build())
    }

    fun updateNotificationBuilder(result: String): NotificationCompat.Builder? {
        builder?.setContentTitle(result)
        return builder
    }

    fun getRingtone(context: Context): Ringtone {
        var alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (alert == null) {
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
        }

        return RingtoneManager.getRingtone(context, alert)
    }

    fun updateNotification(build: Notification?) {
        manager?.notify(1, build)
    }

    private object Holder {
        val INSTANCE = NotificationHelper()
    }

    companion object {
        val instance: NotificationHelper by lazy { Holder.INSTANCE }
        private const val CHANNEL_ID = "timerChannelId"
        private const val CHANNEL_NAME = "timerChannel"
        private const val CONTENT_TEXT = "Таймер"
        private const val CONTENT_TEXT_PAUSE = "Таймер приостановлен"
        const val ACTION_PAUSE = "actionPause"
        const val ACTION_STOP = "actionStop"
        private const val STOP = "Cтоп"
        private const val FINISHED = "Готово!"
    }
}