package ani.saikou.tv.components

import android.view.View
import androidx.leanback.widget.ListRowPresenter

class CustomListRowPresenter(focusZoomFactor: Int, useFocusDimmer: Boolean) :
    ListRowPresenter(focusZoomFactor, useFocusDimmer) {

    override fun isUsingDefaultListSelectEffect(): Boolean {
        return false
    }

    override fun applySelectLevelToChild(rowViewHolder: ViewHolder?, childView: View?) {
        super.applySelectLevelToChild(rowViewHolder, childView)
        //Custom dim here
    }

}