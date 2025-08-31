package com.buspljus

import android.content.Context
import com.buspljus.Baza.Stanice

object LinijePoStanici {
    lateinit var linijePoStanici: Map<String, List<LineInfo>>

    fun initialize(context: Context) {
        linijePoStanici = Stanice(context).ucitajLinijePoStanicama()
    }
}
