package com.buspljus.Trasa

import android.content.Context
import com.buspljus.Baza.Linije
import com.buspljus.Baza.Stanice
import org.json.JSONArray
import org.oscim.layers.marker.MarkerItem

class UpitZaTrasuUBazi(private val context: Context) {
    fun pribaviTrasu(linijaId: String, stanicaId: String, smer: String?): JSONArray {
        return Linije(context).upitTrasa(linijaId, stanicaId, smer).second
    }
    fun pribaviPreostaleStanice(staniceJson: JSONArray, markerStanica: MarkerItem): List<List<Any>> {
        return Stanice(context).staniceDoKrajaTrase(staniceJson, markerStanica)
    }
}