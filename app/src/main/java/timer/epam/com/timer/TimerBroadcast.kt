package timer.epam.com.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerBroadcast : BroadcastReceiver(){ // make singleton?

    var timerResultCallback: TimerResultCallback? = null

    override fun onReceive(context: Context, intent: Intent) {
        timerResultCallback?.onTimerResult(intent.getLongExtra("finish_time_from_service", 0L),
                intent.getBooleanExtra("pause_key", false),
                intent.getBooleanExtra("stop_key", false))
    }

    interface TimerResultCallback{
        fun onTimerResult(finishedTime: Long, onPause: Boolean, onStop: Boolean)
    }
}