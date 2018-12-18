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
        get() = Dispatchers.Default + job
    private lateinit var job: Job
    private lateinit var formatUtils: FormatUtils
    private var finishTime = 0L
    private var result = DEFAULT_RESULT

    private var needToPause = false
    //    private var dataFromService = false
//    private var needToBreak = false
    private var secondsLeft = 0L
    private var initialTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        val filter = IntentFilter(BROADCAST_TIMER_ACTION)
        registerReceiver(TimerBroadcast().apply {
            timerResultCallback = this@TimerActivity
        }, filter)

        start_timer.setOnClickListener {
            //            val currentTimeSeconds = System.currentTimeMillis()
            checkIfOnPause()
            launch()
        }

        stop_timer.setOnClickListener {
            needToPause = true
//            needToBreak = true
        }

        show_time_dialog.setOnClickListener {
            TimePickerFragment().apply {
                timerPickerCallback = this@TimerActivity
            }.show(supportFragmentManager, "choose_time")
        }

        formatUtils = FormatUtils.instance
    }


    private fun launch() = launch {

        while (true) {
//            if (needToBreak) {
//                needToBreak = false
//                break
//            }

            val currentTime = System.currentTimeMillis()

            if (currentTime < finishTime) {
                secondsLeft = TimeUnit.MILLISECONDS.toSeconds(finishTime - currentTime)

                val minutes = TimeUnit.SECONDS.toMinutes(secondsLeft) % 60

                val hours = TimeUnit.SECONDS.toHours(secondsLeft) % 24

                val seconds = secondsLeft % 60

                result = formatUtils.formattedTime(hours, minutes, seconds)

                timer.handler.post { timer.text = result }

                delay(500)
            } else break

            if (needToPause) {
                break
            }
        }
    }

    private fun checkIfOnPause() {
        if (!needToPause) {
            finishTime = System.currentTimeMillis() + initialTime
        } else {
            finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
            needToPause = false
//            needToPause = false
        }
    }

//    private fun checkIfFromServer(): Boolean {
//        if (dataFromService) {
//            finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
//            return true
//        }
//
//        return false
//    }

    override fun onStart() {
        super.onStart()

        job = Job()
        TimerService2.shouldShow = false // maybe to intent?
        stopService(Intent(this, TimerService2::class.java))
    }

    override fun onStop() {
        if (result != DEFAULT_RESULT) {
            val intent = Intent(this, TimerService2::class.java).apply {
                //                putExtra("finish_key", finishTime)
                putExtra("finish_key", secondsLeft)
//                putExtra("pause_key", needToPause)
                action = if (needToPause) ACTION_PAUSE else ACTION_PLAY
            }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TimerService2.shouldShow = true // maybe to intent?
            startService(intent)
            job.cancel()
//        } else {
//            startService(intent)
//        }
        }

        super.onStop()
    }

    override fun onTimeSet(hourOfDay: Int, minute: Int, seconds: Int) {

        timer.text = formatUtils.formattedTime(hourOfDay.toLong(), minute.toLong(), seconds.toLong())

        initialTime = TimeUnit.HOURS.toMillis(hourOfDay.toLong()) +
                TimeUnit.MINUTES.toMillis(minute.toLong()) +
                TimeUnit.SECONDS.toMillis(seconds.toLong())
    }

    override fun onTimerResult(finishedTime: Long, onPause: Boolean, onStop: Boolean) {
        if (onStop) {
            finishTime = 0L
            needToPause = false
            finishTime = 0L
            result = DEFAULT_RESULT
            secondsLeft = 0L
            timer.text = ""
            return
        }

        if (finishedTime != 0L) {
//            finishTime = finishedTime
            secondsLeft = finishedTime
//            dataFromService = true
            needToPause = onPause
            finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
            launch()
//            if (onPause) stop_timer.callOnClick()
//            start_timer.callOnClick()
//            needToBreak = onPause
//            finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secondsLeft)
//            launch()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private const val ACTION_PAUSE = "actionPause"
        private const val ACTION_PLAY = "actionPlay"
        const val BROADCAST_TIMER_ACTION = "timer_action"
        private const val DEFAULT_RESULT = "0 0 0"
    }
}
