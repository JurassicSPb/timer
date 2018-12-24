package timer.epam.com.timer

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_timer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext


class TimerActivity :
        AppCompatActivity(),
        CoroutineScope,
        TimerBroadcast.TimerResultCallback,
        TimePickerFragment.TimePickerCallback {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
    private lateinit var job: Job
    private lateinit var formatUtils: FormatUtils
    private var timeToFinish = DEFAULT_TIME
    private var result = DEFAULT_RESULT

    private var needToPause = false
    private var secondsLeft = 0L
    private var initialTime = DEFAULT_INITIAL_TIME
    private lateinit var timerBroadcastReceiver: TimerBroadcast

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
            timeToFinish = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
            if (currentTime < timeToFinish) {
                showTime(currentTime)
            }
        }

        show_time_dialog.setOnClickListener {
            TimePickerFragment().apply {
                timerPickerCallback = this@TimerActivity
            }.show(supportFragmentManager, SET_TIME)
        }

        formatUtils = FormatUtils.instance
    }


    private fun launch() = launch(coroutineContext + job) {
        checkIfOnPause()
        delay(initialDelay)

        while (true) {
            val currentTime = System.currentTimeMillis()

            if (currentTime < timeToFinish) {
                showTime(currentTime)

                delay(delay)
            } else break // sound
        }
    }

    private fun showTime(currentTime: Long) {
        secondsLeft = TimeUnit.MILLISECONDS.toSeconds(timeToFinish - currentTime)

        val minutes = TimeUnit.SECONDS.toMinutes(secondsLeft) % SEC_MINS

        val hours = TimeUnit.SECONDS.toHours(secondsLeft) % HOURS

        val seconds = secondsLeft % SEC_MINS

        result = formatUtils.formattedTime(hours, minutes, seconds)

        timer.handler.post { timer.text = result }
    }

    private fun checkIfOnPause() {
        if (initialTime != DEFAULT_INITIAL_TIME) {
            timeToFinish = System.currentTimeMillis() + initialTime
            initialTime = DEFAULT_INITIAL_TIME
        } else {
            timeToFinish = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
        }
    }

    override fun onStart() {
        super.onStart()

        job = Job()
        TimerService.shouldShow = false // maybe to intent?
        stopService(Intent(this, TimerService::class.java))
    }

    override fun onStop() {
        if (result != DEFAULT_RESULT) {
            val intent = Intent(this, TimerService::class.java).apply {
                putExtra(SECONDS_LEFT_KEY, secondsLeft)
                action = if (needToPause) ACTION_PAUSE else ACTION_PLAY
            }
            TimerService.shouldShow = true // maybe to intent?
            startService(intent)
            job.cancel()
        }

        super.onStop()
    }

    override fun onTimeSet(hourOfDay: Int, minute: Int, seconds: Int) {
        cancelJob()

        timer.text = formatUtils.formattedTime(hourOfDay.toLong(), minute.toLong(), seconds.toLong())

        initialTime = TimeUnit.HOURS.toMillis(hourOfDay.toLong()) +
                TimeUnit.MINUTES.toMillis(minute.toLong()) +
                TimeUnit.SECONDS.toMillis(seconds.toLong())
    }

    override fun onTimerResult(timeToFinish: Long, onPause: Boolean, onStop: Boolean) {
        if (onStop) {
            this.timeToFinish = DEFAULT_TIME
            needToPause = false
            initialTime = DEFAULT_INITIAL_TIME
            result = DEFAULT_RESULT
            secondsLeft = DEFAULT_TIME
            timer.text = ""
            return
        }

        if (timeToFinish != DEFAULT_TIME) {
            secondsLeft = timeToFinish
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
        private const val SECONDS_LEFT_KEY = "secondsLeftKey"
        private const val SET_TIME = "set_time"
        private const val initialDelay = 200L
        private const val delay = 500L
        private const val DEFAULT_INITIAL_TIME = -1L
        private const val DEFAULT_TIME = 0L
        private const val HOURS = 24
        private const val SEC_MINS = 60
    }
}
