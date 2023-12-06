package com.buspljus

import android.content.Context
import org.oscim.core.MapPosition
import org.oscim.event.Gesture
import org.oscim.event.GestureListener
import org.oscim.event.MotionEvent
import org.oscim.layers.Layer
import org.oscim.map.Map

class Kliknamapu(map: Map, private val context: Context) : Layer(map), GestureListener {
    companion object {
        var pozicija = MapPosition()
    }

        override fun onGesture(g: Gesture?, e: MotionEvent): Boolean {
            if (g is Gesture.Tap) {
                if (pozicija.zoomLevel>=16) {
                    val p = mMap.viewport().fromScreenPoint(e.x, e.y)
                    SQLcitac(context).pretragabaze_kliknamapu(p.latitude.toString(),p.longitude.toString())
                    return true
                }

            }
            return false
        }
    }