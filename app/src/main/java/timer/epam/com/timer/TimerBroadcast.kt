package timer.epam.com.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerBroadcast : BroadcastReceiver() {

    var timerResultCallback: TimerResultCallback? = null

    override fun onReceive(context: Context, intent: Intent) {
        timerResultCallback?.onTimerResult(intent.getLongExtra(SECONDS_LEFT_SERVICE_KEY, 0L),
                intent.getBooleanExtra(PAUSE_KEY, false),
                intent.getBooleanExtra(STOP_KEY, false))
    }

    interface TimerResultCallback {
        fun onTimerResult(timeToFinish: Long, onPause: Boolean, onStop: Boolean)
    }

    companion object {
        private const val PAUSE_KEY = "pauseKey"
        private const val STOP_KEY = "stopKey"
        private const val SECONDS_LEFT_SERVICE_KEY = "secondsLeftServiceKey"
    }
}