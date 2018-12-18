package timer.epam.com.timer

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class TimerService : IntentService("timerService") {
    private var result = ""
    private var job: Job? = null

    override fun onHandleIntent(intent: Intent) {

        when {
            intent.action == ACTION_STOP -> {
                stopService()
                return
            }
        }

        val finishTime = intent.getLongExtra("finish_key", 0L)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("contentTitle")
                .setContentText("contentText")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("contentText")
                        .setBigContentTitle("contentTitle"))
                .addAction(R.drawable.ic_launcher_foreground,
                        "Stop",
                        PendingIntent.getService(this, 0, Intent(this, TimerService::class.java).apply {
                            action = ACTION_STOP
                        }, 0))
//                    .setTicker("TICKER")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager?.createNotificationChannel(NotificationChannel(CHANNEL_ID, "channelName", NotificationManager.IMPORTANCE_LOW
            ))
        }

        job = GlobalScope.launch(Dispatchers.Default) {
            try {
                while (System.currentTimeMillis() < finishTime) {
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(finishTime - System.currentTimeMillis())

                    val minutesLeft = TimeUnit.SECONDS.toMinutes(seconds) % 60

                    val hoursLeft = TimeUnit.SECONDS.toHours(seconds) % 24

                    val secondsLeft = seconds % 60

                    result = "$hoursLeft $minutesLeft $secondsLeft"

                    builder.setContentTitle(Thread.currentThread().name)
                    startForeground(1, builder.build())

                    try {
                        delay(500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }

                if (result == "0 0 0" && shouldShow) {
                    builder.setContentTitle("finished")
                    manager?.notify(5, builder.build())
                }
            } finally {
                stopService()
            }
        }
    }

    private fun stopService() {
        job?.cancel()
        stopForeground(true)
        stopSelf()
    }

    companion object {
        private const val ACTION_STOP = "actionStop"
        private const val CHANNEL_ID = "timerChannelId"
        @Volatile
        var shouldShow = false
    }
}