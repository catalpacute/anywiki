package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.wikipedia.util.ResourceUtil
import work.czzzz.anywiki.R

open class WikiSwipeRefreshLayout(context: Context, attrs: AttributeSet?) : SwipeRefreshLayout(context, attrs) {

    var scrollableChild: View? = null

    init {
        setColorSchemeResources(ResourceUtil.getThemedAttributeId(context, R.attr.progressive_color))
    }

    override fun canChildScrollUp(): Boolean {
        return if (scrollableChild == null) {
            super.canChildScrollUp()
        } else scrollableChild!!.scrollY > 0
    }
}
