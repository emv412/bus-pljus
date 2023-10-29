package com.buspljus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.TextView

class tekstubitmap {

    fun setBackground(view: View, background: Drawable?) {
        view.background = background
    }

    fun getBitmapFromTitle(title: String?, context: Context, boja: Int): Bitmap {
        val bubbleView = TextView(context)
        if (boja == 0) {
            setBackground(
                bubbleView,
                context.getDrawable(R.drawable.oblak)
            )
        }
        else if (boja == 1) {
            setBackground(
                bubbleView,
                context.getDrawable(R.drawable.oblak_crveni)
            )
        }

        bubbleView.gravity = Gravity.CENTER
        bubbleView.maxEms = 20
        bubbleView.textSize = 15f
        bubbleView.setPadding(20, 0, 20, 15)
        bubbleView.setTextColor(Color.WHITE)
        bubbleView.text = title

        bubbleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val bitmap = Bitmap.createBitmap(bubbleView.measuredWidth, bubbleView.measuredHeight, Bitmap.Config.ARGB_8888)
        val platno = Canvas(bitmap)
        bubbleView.layout(0,0,bubbleView.measuredWidth,bubbleView.measuredHeight)
        bubbleView.draw(platno)
        return bitmap
    }
}