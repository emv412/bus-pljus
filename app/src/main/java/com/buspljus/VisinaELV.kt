package com.buspljus

import android.view.View
import android.widget.ExpandableListView

class VisinaELV {
    fun podesivisinuELV(listView: ExpandableListView) {
        val adapter = listView.expandableListAdapter
        var totalHeight = 0
        val desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.EXACTLY)
        val desiredHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        for (i in 0 until adapter.groupCount) {
            val groupItem = adapter.getGroupView(i, true, null, listView)
            groupItem.measure(desiredWidth, desiredHeight)
            totalHeight += groupItem.measuredHeight

            val innerCount = adapter.getChildrenCount(i)
            for (j in 0 until innerCount) {
                val childItem = adapter.getChildView(i, j, false, null, listView)
                childItem.measure(desiredWidth, desiredHeight)
                totalHeight += childItem.measuredHeight
            }
        }

        val params = listView.layoutParams
        params.height = totalHeight + listView.dividerHeight * (adapter.groupCount - 1)
        listView.layoutParams = params
        listView.requestLayout()
    }
}