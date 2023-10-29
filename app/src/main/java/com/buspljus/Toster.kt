package com.buspljus

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class Toster(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun toster(poruka: String) {
        mainHandler.post {
            Toast.makeText(context, poruka, Toast.LENGTH_SHORT).show()
        }
    }
}
