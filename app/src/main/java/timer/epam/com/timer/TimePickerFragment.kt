package timer.epam.com.timer

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.ikovac.timepickerwithseconds.MyTimePickerDialog

class TimePickerFragment : DialogFragment(), MyTimePickerDialog.OnTimeSetListener {
    var timerPickerCallback: TimePickerCallback? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MyTimePickerDialog(
                activity,
                R.style.MyTimePickerDialogStyle,
                this,
                0,
                0,
                0,
                true
        ).apply {
            setButton(MyTimePickerDialog.BUTTON_POSITIVE, "Применить", this)
            setButton(MyTimePickerDialog.BUTTON_NEGATIVE, "Отмена", null as? DialogInterface.OnClickListener?)
        }
    }

    override fun onTimeSet(view: com.ikovac.timepickerwithseconds.TimePicker, hourOfDay: Int, minute: Int, seconds: Int) {
        timerPickerCallback?.onTimeSet(hourOfDay, minute, seconds)
        Toast.makeText(activity, "$hourOfDay $minute $seconds", Toast.LENGTH_SHORT).show()
    }

    interface TimePickerCallback {
        fun onTimeSet(hourOfDay: Int, minute: Int, seconds: Int)
    }
}