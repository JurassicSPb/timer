package timer.epam.com.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TimerService2 : Service() {
    private lateinit var formatUtils: FormatUtils
    private var result = ""
    private var job: Job? = null
    private var needToPause = false
    private var needToStop = false
//    private var needToPlay = false
    private var finishTime = 0L
    private var secondsLeft = 0L
    private var intentaActionPause = false

    override fun onCreate() {
        super.onCreate()

        formatUtils = FormatUtils.instance
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.hasExtra("finish_key")) secondsLeft = intent.getLongExtra("finish_key", 0L)

        when (intent.action) {
            ACTION_STOP -> stopService()
            ACTION_PAUSE -> pauseService()
            ACTION_PLAY -> playService()
        }

//        intentaActionPause = intent.action == ACTION_PAUSE

//        if (!needToPlay) {
////            finishTime = intent.getLongExtra("finish_key", 0L)
//            secondsLeft = intent.getLongExtra("finish_key", 0L)
//            finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
//            if (intent.action != ACTION_PAUSE) needToPause = intent.getBooleanExtra("pause_key", false)
//        } else if (needToPlay) {
//            finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
//            needToPlay = false
//        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("contentTitle")
                .setContentText("contentText")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setStyle(NotificationCompat.BigTextStyle()
//                        .bigText("contentText")
//                        .setBigContentTitle("contentTitle")
//                )
                .addAction(R.drawable.ic_launcher_foreground,
                        "Stop",
                        PendingIntent.getService(this, 0, Intent(this, TimerService2::class.java).apply {
                            action = ACTION_STOP
                        }, 0))
                .addAction(R.drawable.ic_launcher_foreground,
                        "Pause",
                        PendingIntent.getService(this, 1, Intent(this, TimerService2::class.java).apply {
                            action = ACTION_PAUSE
                        }, 0))
//                    .setTicker("TICKER")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager?.createNotificationChannel(NotificationChannel(CHANNEL_ID, "channelName", NotificationManager.IMPORTANCE_LOW
            ))
        }

        job = GlobalScope.launch(Dispatchers.Default) {
            //            try {
            finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)

            while (true) {
                if (needToStop) {
                    break
                }

                val currentTime = System.currentTimeMillis()

                if (currentTime < finishTime) {
                    secondsLeft = TimeUnit.MILLISECONDS.toSeconds(finishTime - currentTime)

                    val minutes = TimeUnit.SECONDS.toMinutes(secondsLeft) % 60

                    val hours = TimeUnit.SECONDS.toHours(secondsLeft) % 24

                    val seconds = secondsLeft % 60

                    result = formatUtils.formattedTime(hours, minutes, seconds)

                    builder.setContentTitle(result)
                    startForeground(1, builder.build())

                    delay(500)
                } else break

                if (needToPause) {
//                    needToPause = false
                    builder.addAction(R.drawable.ic_launcher_foreground,
                            "Play",
                            PendingIntent.getService(this@TimerService2, 2, Intent(this@TimerService2, TimerService2::class.java).apply {
                                action = ACTION_PLAY
                            }, 0))
                    builder.setContentTitle(result)
                    startForeground(1, builder.build())
                    break
                }
            }

            if (result == "0 0 0" && shouldShow) {
                builder.setContentTitle("finished")
                manager?.notify(5, builder.build())
                stopService()
            }
//            } finally {
//                stopService()
//            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun playService() {
//        needToPlay = true
        needToPause = false
//        finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)

    }

    private fun pauseService() {
        needToPause = true
//        finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
    }

    private fun stopService() {
        needToStop = true
        job?.cancel()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        sendBroadcast(Intent(TimerActivity.BROADCAST_TIMER_ACTION).apply {
//            putExtra("finish_time_from_service", finishTime)
            putExtra("finish_time_from_service", secondsLeft)
            putExtra("pause_key", needToPause)
            putExtra("stop_key", needToStop)
        })
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "actionStop"
        private const val ACTION_PAUSE = "actionPause"
        private const val ACTION_PLAY = "actionPlay"
        private const val CHANNEL_ID = "timerChannelId"
        @Volatile
        var shouldShow = false
    }
}