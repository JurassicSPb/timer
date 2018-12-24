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
    private var timeToFinish = 0L
    private var secondsLeft = 0L

    override fun onCreate() {
        super.onCreate()

        notificationHelper = NotificationHelper.instance.apply {
            createNotificationManager(this@TimerService)
            createInitialNotification(this@TimerService)
        }

        formatUtils = FormatUtils.instance
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.hasExtra(SECONDS_LEFT_KEY)) {
            secondsLeft = intent.getLongExtra(SECONDS_LEFT_KEY, 0L)
        }

        cancelJob()

        when (intent.action) {
            ACTION_STOP -> stopService()
            ACTION_PAUSE -> pauseService()
            ACTION_PLAY -> playService()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun launch() {
        job = GlobalScope.launch(Dispatchers.Default) {

            timeToFinish = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
            delay(initialDelay)

            while (true) {
                val currentTime = System.currentTimeMillis()

                if (needToStop || needToPause) {
                    break
                }

                if (currentTime < timeToFinish) {
                    showTime(currentTime)
                    if (needToPlay) {
                        val notification = notificationHelper.switchButtonBuilder(this@TimerService, PLAY, PAUSE, ACTION_PAUSE, result)
                        startForeground(1, notification?.build())
                        needToPlay = false
                    }

                    delay(delay)
                } else {
                    break // sound
                }
            }

            if (result == DEFAULT_RESULT && shouldShow) {
                notificationHelper.finishedNotification()
                stopService()
            }
        }
    }


    private fun showTime(currentTime: Long) {
        secondsLeft = TimeUnit.MILLISECONDS.toSeconds(timeToFinish - currentTime)

        val minutes = TimeUnit.SECONDS.toMinutes(secondsLeft) % 60

        val hours = TimeUnit.SECONDS.toHours(secondsLeft) % 24

        val seconds = secondsLeft % 60

        result = formatUtils.formattedTime(hours, minutes, seconds)

        startForeground(1, notificationHelper.updateNotificationBuilder(result)?.build())
    }

    private fun stopService() {
        needToStop = true
        cancelJob()
        stopForeground(true)
        stopSelf()
    }

    private fun pauseService() {
        needToPause = true
        needToPlay = false
        val currentTime = System.currentTimeMillis()
        timeToFinish = currentTime + TimeUnit.SECONDS.toMillis(secondsLeft)
        if (currentTime < timeToFinish) {
            showTime(currentTime)
            val notification = notificationHelper.switchButtonBuilder(this, PAUSE, PLAY, ACTION_PLAY, result)
            startForeground(1, notification?.build())
        }
    }

    private fun playService() {
        needToPause = false
        needToPlay = true
        launch()
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }

    override fun onDestroy() {
        sendBroadcast(Intent(TimerActivity.BROADCAST_TIMER_ACTION).apply {
            putExtra(SECONDS_LEFT_SERVICE_KEY, secondsLeft)
            putExtra(PAUSE_KEY, needToPause)
            putExtra(STOP_KEY, needToStop)
        })
        super.onDestroy()
    }

    companion object {
        const val PAUSE = "Остановить"
        const val PLAY = "Продолжить"
        const val ACTION_STOP = "actionStop"
        const val ACTION_PAUSE = "actionPause"
        const val ACTION_PLAY = "actionPlay"
        private const val DEFAULT_RESULT = "00:00:00"
        private const val SECONDS_LEFT_KEY = "secondsLeftKey"
        private const val PAUSE_KEY = "pauseKey"
        private const val STOP_KEY = "stopKey"
        private const val SECONDS_LEFT_SERVICE_KEY = "secondsLeftServiceKey"
        private const val initialDelay = 200L
        private const val delay = 500L
        @Volatile
        var shouldShow = false
    }
}