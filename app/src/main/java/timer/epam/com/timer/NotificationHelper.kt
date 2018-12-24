package timer.epam.com.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat

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
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentText(CONTENT_TEXT)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_launcher_foreground,
                        STOP,
                        PendingIntent.getService(context, 0, Intent(context, TimerService::class.java).apply {
                            action = TimerService.ACTION_STOP
                        }, 0))
    }

    fun switchButtonBuilder(context: Context, oldTitle: String, newTitle: String, action: String, result: String): NotificationCompat.Builder? {
        val iterator = builder!!.mActions.iterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element.title == oldTitle) {
                iterator.remove()
                break
            }
        }

        builder?.addAction(R.drawable.ic_launcher_foreground,
                newTitle,
                PendingIntent.getService(context, 2, Intent(context, TimerService::class.java).apply {
                    this.action = action
                }, 0))
        builder?.setContentTitle(result)

        return builder
    }

    fun finishedNotification(){
        builder?.setContentTitle(FINISHED)
        manager?.notify(5, builder?.build())
    }

    fun updateNotificationBuilder(result: String) : NotificationCompat.Builder?{
        builder?.setContentTitle(result)
        return builder
    }

    private object Holder {
        val INSTANCE = NotificationHelper()
    }

    companion object {
        val instance: NotificationHelper by lazy { Holder.INSTANCE }
        private const val CHANNEL_ID = "timerChannelId"
        private const val CHANNEL_NAME = "timerChannel"

        private const val CONTENT_TEXT = "Таймер"
        private const val STOP = "Cтоп"
        private const val FINISHED = "Готово!"
    }
}