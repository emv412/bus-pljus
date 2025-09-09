package com.buspljus.Mapa

import android.content.Context
import com.buspljus.Baza.InterakcijaMapa
import com.buspljus.Glavna.Companion.mapa
import com.buspljus.Interfejs
import org.oscim.event.Gesture
import org.oscim.event.GestureListener
import org.oscim.event.MotionEvent
import org.oscim.layers.Layer

class KlikNaMapu(
    private val context: Context,
    private val listener: Interfejs.MapTapListener
) : Layer(mapa), GestureListener {

    override fun onGesture(g: Gesture, e: MotionEvent): Boolean {
        if (g == Gesture.TAP) {
            val p = mapa.viewport().fromScreenPoint(e.x, e.y)

            InterakcijaMapa(context).pretragabaze_kliknamapu(
                p.latitude.toString(),
                p.longitude.toString(),
                object : Interfejs.MapTapListener {
                    override fun podesiTextView(idStanice: String, nazivStanice: String) {
                        listener.podesiTextView(idStanice, nazivStanice)
                    }

                    override fun koloneBGVOZ(lista: List<Any>) {}
                },
                0
            )
        }
        return false
    }
}
