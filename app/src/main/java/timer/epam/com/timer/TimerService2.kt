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
    //    private var intentaActionPause = false
    private var manager: NotificationManager? = null

    private lateinit var builder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()

        manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager?.createNotificationChannel(NotificationChannel(CHANNEL_ID, "channelName", NotificationManager.IMPORTANCE_LOW))
        }

        builder = NotificationCompat.Builder(this, CHANNEL_ID)
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
//                    .setTicker("TICKER")

        formatUtils = FormatUtils.instance
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.hasExtra("finish_key")) secondsLeft = intent.getLongExtra("finish_key", 0L)

        when (intent.action) {
            ACTION_STOP -> stopService()
            ACTION_PAUSE -> {
//                pauseService()
                needToPause = true
                if (job == null) {
                    run()
                }
            }
            ACTION_PLAY -> {
                job?.cancel()
                needToPause = false
//                playService()
                switchButton("Play", "Pause", ACTION_PAUSE)
                run()
            }
            else -> run()
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

        return super.onStartCommand(intent, flags, startId)
    }

    private fun run() {

        job = GlobalScope.launch(Dispatchers.Default) {
            //            try {
            finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)

            while (true) {
//                if (needToPause) {
////                    needToPause = false
//                    builder.addAction(R.drawable.ic_launcher_foreground,
//                            "Play",
//                            PendingIntent.getService(this@TimerService2, 2, Intent(this@TimerService2, TimerService2::class.java).apply {
//                                action = ACTION_PLAY
//                            }, 0))
//                    builder.setContentTitle(result)
//                    startForeground(1, builder.build())
//                    break
//                }
                val currentTime = System.currentTimeMillis()

                if (needToStop) {
                    break
                }

                if (needToPause) {
                    if (currentTime < finishTime) {
                        showTime(currentTime)
                        switchButton("Pause", "Play", ACTION_PLAY)
                    }
                    break
                }


                if (currentTime < finishTime) {
                    showTime(currentTime)

                    delay(500)
                } else {
                    break
                }
            }

            if (result == "00:00:00" && shouldShow) {
                builder.setContentTitle("finished")
                manager?.notify(5, builder.build())
                stopService()
            }
//            } finally {
//                stopService()
//            }
        }
    }

    private fun showTime(currentTime: Long) {

        secondsLeft = TimeUnit.MILLISECONDS.toSeconds(finishTime - currentTime)

        val minutes = TimeUnit.SECONDS.toMinutes(secondsLeft) % 60

        val hours = TimeUnit.SECONDS.toHours(secondsLeft) % 24

        val seconds = secondsLeft % 60

        result = formatUtils.formattedTime(hours, minutes, seconds)

        builder.setContentTitle(result)
        startForeground(1, builder.build())
    }

    private fun playService() {
//        needToPlay = true
        needToPause = false
//        startForeground(1, builder.build())

//        val iterator = builder.mActions.iterator()
//        while (iterator.hasNext()){
//            val element = iterator.next()
//            if (element.title == "Play") {
//                iterator.remove()
//                break
//            }
//        }
//
//        builder.addAction(R.drawable.ic_launcher_foreground,
//                "Pause",
//                PendingIntent.getService(this@TimerService2, 2, Intent(this@TimerService2, TimerService2::class.java).apply {
//                    action = ACTION_PAUSE
//                }, 0))
//        builder.setContentTitle(result)

//        finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)

    }

    private fun pauseService() {
        needToPause = true

//        finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
    }

    private fun switchButton(oldTitle: String, newTitle: String, action: String) {
        val iterator = builder.mActions.iterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element.title == oldTitle) {
                iterator.remove()
                break
            }
        }

        builder.addAction(R.drawable.ic_launcher_foreground,
                newTitle,
                PendingIntent.getService(this@TimerService2, 2, Intent(this@TimerService2, TimerService2::class.java).apply {
                    this.action = action
                }, 0))
        builder.setContentTitle(result)
        startForeground(1, builder.build())
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