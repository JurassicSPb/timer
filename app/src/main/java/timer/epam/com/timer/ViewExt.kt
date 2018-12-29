package timer.epam.com.timer

import android.view.View

fun View.isClickableAndFocusable(state: Boolean) {
    isClickable = state
    isFocusable = state
}
