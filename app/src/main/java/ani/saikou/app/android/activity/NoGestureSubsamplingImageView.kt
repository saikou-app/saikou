package ani.saikou.app.android.activity

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class NoGestureSubsamplingImageView(context: Context?, attr: AttributeSet?) :
    SubsamplingScaleImageView(context, attr) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }
}