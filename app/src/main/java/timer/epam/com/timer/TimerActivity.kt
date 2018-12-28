package timer.epam.com.timer

import android.content.Intent
import android.content.IntentFilter
import android.media.Ringtone
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_timer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext


class TimerActivity :
        AppCompatActivity(),
        CoroutineScope,
        TimerBroadcast.TimerResultCallback,
        TimePickerFragment.TimePickerCallback {

    override val coroutineContext: CoroutineContext
        get() = coroutineDispatcher
    private lateinit var job: Job
    private val executorService = Executors.newSingleThreadExecutor()!!
    private val coroutineDispatcher = executorService.asCoroutineDispatcher()
    private lateinit var formatUtils: FormatUtils
    private lateinit var notificationHelper: NotificationHelper
    private var timeToFinish = DEFAULT_TIME
    private var result = DEFAULT_RESULT

    private var needToPause = false
    private var millisLeft = DEFAULT_TIME
    private var initialTime = DEFAULT_TIME
    private lateinit var timerBroadcastReceiver: TimerBroadcast
    private var ringtone: Ringtone? = null
    private var initialTimeSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        val filter = IntentFilter(BROADCAST_TIMER_ACTION)

        timerBroadcastReceiver = TimerBroadcast()
        registerReceiver(timerBroadcastReceiver.apply {
            timerResultCallback = this@TimerActivity
        }, filter)

        start_timer.setOnClickListener {
            cancelJob()
            needToPause = false
            launch()
        }

        stop_timer.setOnClickListener {
            cancelJob()
            needToPause = true
            val currentTime = System.currentTimeMillis()
            timeToFinish = System.currentTimeMillis() + millisLeft
            if (currentTime < timeToFinish) {
                showTime(currentTime)
            }
            // stop rington on cancel button when time is off
        }

        show_time_dialog.setOnClickListener {
            TimePickerFragment().apply {
                timerPickerCallback = this@TimerActivity
            }.show(supportFragmentManager, SET_TIME)
        }

        formatUtils = FormatUtils.instance
        notificationHelper = NotificationHelper.instance
    }


    private fun launch() = launch(coroutineContext + job) {
        when {
            initialTime == DEFAULT_TIME && initialTimeSet -> {
                initialTimeSet = false
                return@launch
            }
            initialTime != DEFAULT_TIME && initialTimeSet -> {
                initialTimeSet = false
                timeToFinish = System.currentTimeMillis() + initialTime
                initialTime = DEFAULT_TIME
            }
            else -> timeToFinish = System.currentTimeMillis() + millisLeft
        }

        while (true) {
            val currentTime = System.currentTimeMillis()

            if (currentTime < timeToFinish) {
                showTime(currentTime)

                delay(delay)
            } else {
                timer.handler.post { timer.text = "" }
                ringtone = notificationHelper.getRingtone(this@TimerActivity)
                ringtone?.play()
                break
            }
        }
    }

    private fun showTime(currentTime: Long) {
        millisLeft = timeToFinish - currentTime

        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisLeft) % SEC_MINS

        val hours = TimeUnit.MILLISECONDS.toHours(millisLeft) % HOURS

        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisLeft) % SEC_MINS

        result = formatUtils.formattedTime(hours, minutes, seconds)

        timer.handler.post { timer.text = result }
    }

    override fun onStart() {
        super.onStart()

        job = Job()
        TimerService.shouldShow = false // maybe to intent?
        stopService(Intent(this, TimerService::class.java))
    }

    override fun onStop() {
        if (result != DEFAULT_RESULT && !initialTimeSet) {
            val intent = Intent(this, TimerService::class.java).apply {
                putExtra(MILLIS_LEFT_KEY, millisLeft)
                action = if (needToPause) ACTION_PAUSE else ACTION_PLAY
            }
            TimerService.shouldShow = true // maybe to intent?
            startService(intent)
        }
        job.cancel()

        super.onStop()
    }

    override fun onTimeSet(hourOfDay: Int, minute: Int, seconds: Int) {
        ringtone?.stop()

        cancelJob()

        if (hourOfDay + minute + seconds > DEFAULT_TIME) {
            timer.text = formatUtils.formattedTime(hourOfDay.toLong(), minute.toLong(), seconds.toLong())

            initialTime = TimeUnit.HOURS.toMillis(hourOfDay.toLong()) +
                    TimeUnit.MINUTES.toMillis(minute.toLong()) +
                    TimeUnit.SECONDS.toMillis(seconds.toLong())

            initialTimeSet = true
        }
    }

    override fun onTimerResult(timeToFinish: Long, onPause: Boolean, onStop: Boolean) {
        if (onStop) {
            this.timeToFinish = DEFAULT_TIME
            needToPause = false
            initialTime = DEFAULT_TIME
            result = DEFAULT_RESULT
            millisLeft = DEFAULT_TIME
            timer.text = ""
            return
        }

        if (timeToFinish != DEFAULT_TIME) {
            millisLeft = timeToFinish
            if (!onPause) {
                start_timer.performClick()
            } else {
                stop_timer.performClick()
            }
        }
    }

    private fun cancelJob() {
        job.cancel()
        job = Job()
    }

    override fun onDestroy() {
        unregisterReceiver(timerBroadcastReceiver)

        super.onDestroy()
    }

    companion object {
        private const val ACTION_PAUSE = "actionPause"
        private const val ACTION_PLAY = "actionPlay"
        const val BROADCAST_TIMER_ACTION = "timer_action"
        private const val DEFAULT_RESULT = "00:00:00"
        private const val MILLIS_LEFT_KEY = "millisLeftKey"
        private const val SET_TIME = "set_time"
        private const val delay = 500L
        private const val DEFAULT_TIME = 0L
        private const val HOURS = 24
        private const val SEC_MINS = 60
    }
}
