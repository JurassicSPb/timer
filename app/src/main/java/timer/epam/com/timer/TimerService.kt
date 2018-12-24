package timer.epam.com.timer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TimerService : Service() {
    private lateinit var formatUtils: FormatUtils
    private lateinit var notificationHelper: NotificationHelper
    private var result = ""
    private var job: Job? = null
    private var needToPause = false
    private var needToPlay = false
    private var needToStop = false
    private var finishTime = 0L
    private var secondsLeft = 0L

    override fun onCreate() {
        super.onCreate()

        notificationHelper = NotificationHelper.instance
        notificationHelper.apply {
            createNotificationManager(this@TimerService)
            createInitialNotification(this@TimerService)
        }

        formatUtils = FormatUtils.instance
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.hasExtra("finish_key")) {
            secondsLeft = intent.getLongExtra("finish_key", 0L)
        }

        job?.cancel()
        job = null

        when (intent.action) {
            ACTION_STOP -> stopService()
            ACTION_PAUSE -> {
                needToPause = true
                needToPlay = false
                val currentTime = System.currentTimeMillis()
                finishTime = currentTime + TimeUnit.SECONDS.toMillis(secondsLeft)
                if (currentTime < finishTime) {
                    showTime(currentTime)
                    val notification = notificationHelper.switchButtonBuilder(this, "Pause", "Play", ACTION_PLAY, result)
                    startForeground(1, notification?.build())
                }
            }
            ACTION_PLAY -> {
                needToPause = false
                needToPlay = true
                run()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun run() {

        job = GlobalScope.launch(Dispatchers.Default) {

            finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
            delay(200)

            while (true) {
                val currentTime = System.currentTimeMillis()

                if (needToStop) {
                    break
                }

                if (currentTime < finishTime) {
                    showTime(currentTime)
                    if (needToPlay) {
                        val notification = notificationHelper.switchButtonBuilder(this@TimerService, "Play", "Pause", ACTION_PAUSE, result)
                        startForeground(1, notification?.build())
                        needToPlay = false
                    }

                    delay(500)
                } else {
                    break // sound
                }
            }

            if (result == "00:00:00" && shouldShow) {
                notificationHelper.finishedNotification()
                stopService()
            }
        }
    }


    private fun showTime(currentTime: Long) {

        secondsLeft = TimeUnit.MILLISECONDS.toSeconds(finishTime - currentTime)

        val minutes = TimeUnit.SECONDS.toMinutes(secondsLeft) % 60

        val hours = TimeUnit.SECONDS.toHours(secondsLeft) % 24

        val seconds = secondsLeft % 60

        result = formatUtils.formattedTime(hours, minutes, seconds)

        startForeground(1, notificationHelper.updateNotificationBuilder(result)?.build())
    }

    private fun stopService() {
        needToStop = true
        job?.cancel()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        sendBroadcast(Intent(TimerActivity.BROADCAST_TIMER_ACTION).apply {
            putExtra("finish_time_from_service", secondsLeft)
            putExtra("pause_key", needToPause)
            putExtra("stop_key", needToStop)
        })
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "actionStop"
        const val ACTION_PAUSE = "actionPause"
        const val ACTION_PLAY = "actionPlay"
        @Volatile
        var shouldShow = false
    }
}