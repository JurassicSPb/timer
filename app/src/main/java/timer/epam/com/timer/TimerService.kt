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
    private var timeToFinish = DEFAULT_TIME
    private var millisLeft = DEFAULT_TIME

    override fun onCreate() {
        super.onCreate()

        notificationHelper = NotificationHelper.instance.apply {
            createNotificationManager(this@TimerService)
            createInitialNotification(this@TimerService)
        }

        formatUtils = FormatUtils.instance
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val hasMillisLeft = intent?.hasExtra(MILLIS_LEFT_KEY)
        if (hasMillisLeft != null && hasMillisLeft) {
            millisLeft = intent.getLongExtra(MILLIS_LEFT_KEY, DEFAULT_TIME)
        }

        cancelJob()

        when (intent?.action) {
            ACTION_STOP -> stopService()
            ACTION_PAUSE -> pauseService()
            ACTION_PLAY -> playService()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun launch() {
        job = GlobalScope.launch(Dispatchers.Default) {

            timeToFinish = System.currentTimeMillis() + millisLeft
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
                    } else {
                        notificationHelper.updateNotification(notificationHelper.updateNotificationBuilder(result)?.build())
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
        millisLeft = timeToFinish - currentTime

        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisLeft) % SEC_MINS

        val hours = TimeUnit.MILLISECONDS.toHours(millisLeft) % HOURS

        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisLeft) % SEC_MINS

        result = formatUtils.formattedTime(hours, minutes, seconds)
    }

    private fun stopService() {
        needToStop = true
        stopForeground(true)
        stopSelf()
    }

    private fun pauseService() {
        job = GlobalScope.launch(Dispatchers.Default) {

            needToPause = true
            needToPlay = false
            val currentTime = System.currentTimeMillis()
            timeToFinish = currentTime + millisLeft

            delay(initialDelay)

            if (currentTime < timeToFinish) {
                showTime(currentTime)
                val notification = notificationHelper.switchButtonBuilder(this@TimerService, PAUSE, PLAY, ACTION_PLAY, result)
                startForeground(1, notification?.build())
            }
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
            putExtra(MILLIS_LEFT_SERVICE_KEY, millisLeft)
            putExtra(PAUSE_KEY, needToPause)
            putExtra(STOP_KEY, needToStop)
        })
        cancelJob()
        super.onDestroy()
    }

    companion object {
        const val PAUSE = "Остановить"
        const val PLAY = "Продолжить"
        const val ACTION_STOP = "actionStop"
        const val ACTION_PAUSE = "actionPause"
        const val ACTION_PLAY = "actionPlay"
        private const val DEFAULT_RESULT = "00:00:00"
        private const val MILLIS_LEFT_KEY = "millisLeftKey"
        private const val PAUSE_KEY = "pauseKey"
        private const val STOP_KEY = "stopKey"
        private const val MILLIS_LEFT_SERVICE_KEY = "millisLeftServiceKey"
        private const val initialDelay = 200L
        private const val delay = 500L
        private const val DEFAULT_TIME = 0L
        private const val HOURS = 24
        private const val SEC_MINS = 60
        @Volatile
        var shouldShow = false
    }
}