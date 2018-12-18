package timer.epam.com.timer

import java.util.concurrent.TimeUnit

class TimerHelper private constructor() {
    var finishTime = 0L
    var result = ""

    fun startTimer(duration: Int) {
        val currentTimeSeconds = System.currentTimeMillis()
        finishTime = currentTimeSeconds + duration

        while (System.currentTimeMillis() < finishTime) {
            val secondsLeft = TimeUnit.MILLISECONDS.toSeconds(finishTime - System.currentTimeMillis())

            val minutesLeft = TimeUnit.SECONDS.toMinutes(secondsLeft)

            val hoursLeft = TimeUnit.SECONDS.toHours(secondsLeft) % 24

            result = "${if (hoursLeft == 0L) 24 else hoursLeft} ${minutesLeft % 60} ${secondsLeft % 60}"
        }
    }

    interface TimerResultCallback{
        fun onTimerResult(result: String)
    }

    private object Holder {
        val INSTANCE = TimerHelper()
    }

    companion object {
        val instance: TimerHelper by lazy { Holder.INSTANCE }
    }
}