package timer.epam.com.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat


class NotificationHelper private constructor() {
    private var manager: NotificationManager? = null
    private var builder: NotificationCompat.Builder? = null
    private var finishedBuilder: NotificationCompat.Builder? = null

    fun createNotificationManager(context: Context) {
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
            }

            val finishedChannel = NotificationChannel(CHANNEL_ID_FINISHED, CHANNEL_NAME_FINISHED, NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(getRingtoneUri(), AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build())
            }

            manager?.apply {
                createNotificationChannel(channel)
                createNotificationChannel(finishedChannel)
            }
        }

        builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            createCommonBuilderSettings(context, this)
        }

        finishedBuilder = NotificationCompat.Builder(context, CHANNEL_ID_FINISHED).apply {
            createCommonBuilderSettings(context, this)
            setContentTitle(FINISHED)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                setSound(getRingtoneUri())
            }
        }
    }

    private fun createCommonBuilderSettings(context: Context, builder: NotificationCompat.Builder) {
        builder.setSmallIcon(R.drawable.ic_timer)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setContentText(CONTENT_TEXT)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    fun switchButtonBuilder(context: Context, newTitle: String, action: String, result: String): NotificationCompat.Builder? {
        builder?.mActions?.clear()

        builder?.addAction(R.drawable.ic_stop,
                STOP,
                PendingIntent.getService(context, 1, Intent(context, TimerService::class.java).apply {
                    this.action = ACTION_STOP
                }, PendingIntent.FLAG_UPDATE_CURRENT))
                ?.addAction(if (action == ACTION_PAUSE) R.drawable.ic_pause else R.drawable.ic_play,
                        newTitle,
                        PendingIntent.getService(context, 2, Intent(context, TimerService::class.java).apply {
                            this.action = action
                        }, PendingIntent.FLAG_UPDATE_CURRENT))
                ?.setContentTitle(result)
                ?.setContentText(if (action == ACTION_PAUSE) CONTENT_TEXT else CONTENT_TEXT_PAUSE)

        return builder
    }

    fun finishedNotification() {
        manager?.cancel(1)
        val notification = finishedBuilder?.build()
        notification?.flags = notification?.flags?.or(Notification.FLAG_INSISTENT)

        manager?.notify(3, notification)
    }

    fun updateNotificationBuilder(result: String): NotificationCompat.Builder? {
        builder?.setContentTitle(result)
        return builder
    }

    private fun getRingtoneUri(): Uri {
        var alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (alert == null) {
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
        }
        return alert
    }

    fun getRingtone(context: Context): Ringtone = RingtoneManager.getRingtone(context, getRingtoneUri())

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
        private const val CHANNEL_ID_FINISHED = "timerChannelId2"
        private const val CHANNEL_NAME_FINISHED = "timerChannelFinished"
        private const val CONTENT_TEXT = "Таймер"
        private const val CONTENT_TEXT_PAUSE = "Таймер приостановлен"
        const val ACTION_PAUSE = "actionPause"
        const val ACTION_STOP = "actionStop"
        private const val STOP = "Cтоп"
        private const val FINISHED = "Готово!"
    }
}