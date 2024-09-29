package com.buspljus

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

class TekstUBitmap {

    fun setBackground(view: View, background: Drawable?) {
        view.background = background
    }

    fun getBitmapFromTitle(title: String?, context: Glavna, boja: Int): Bitmap {
        val bubbleView = TextView(context)
        setBackground(
            bubbleView,
            VectorDrawableCompat.create(context.resources, R.drawable.oblak, ContextThemeWrapper(context,
                when (boja) {
                    0 -> R.style.Stanica
                    1 -> R.style.Autobus
                    2 -> R.style.Tramvaj
                    3 -> R.style.Trolejbus
                    4 -> R.style.TestMarker
                    else -> {R.style.Autobus}
                }).theme))

        bubbleView.gravity = Gravity.CENTER_VERTICAL
        bubbleView.maxEms = 20
        bubbleView.textSize = 15f
        bubbleView.setPadding(20, 0, 20, 8)
        if (boja == 0)
            bubbleView.setTextColor(Color.BLACK)
        else bubbleView.setTextColor(Color.WHITE)
        bubbleView.text = title

        bubbleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val bitmap = Bitmap.createBitmap(bubbleView.measuredWidth, bubbleView.measuredHeight, Bitmap.Config.ARGB_8888)
        val platno = Canvas(bitmap)
        bubbleView.layout(0,0,bubbleView.measuredWidth,bubbleView.measuredHeight)
        bubbleView.draw(platno)
        return bitmap
    }
}