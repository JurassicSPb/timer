package timer.epam.com.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerBroadcast : BroadcastReceiver() {

    var timerResultCallback: TimerResultCallback? = null

    override fun onReceive(context: Context, intent: Intent?) {
        timerResultCallback?.onTimerResult(intent?.getLongExtra(MILLIS_LEFT_SERVICE_KEY, 0L) ?: 0L,
                intent?.getBooleanExtra(PAUSE_KEY, false) ?: false,
                intent?.getBooleanExtra(STOP_KEY, false) ?: false,
                intent?.getIntExtra(MAX_PROGRESS_KEY, 0) ?: 0)
    }

    interface TimerResultCallback {
        fun onTimerResult(millisLeft: Long, onPause: Boolean, onStop: Boolean, maxProgress: Int)
    }

    companion object {
        private const val PAUSE_KEY = "pauseKey"
        private const val STOP_KEY = "stopKey"
        private const val MILLIS_LEFT_SERVICE_KEY = "millisLeftServiceKey"
        private const val MAX_PROGRESS_KEY = "maxProgressKey"
    }
}