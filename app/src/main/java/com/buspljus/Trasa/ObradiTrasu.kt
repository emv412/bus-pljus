package com.buspljus.Trasa

import android.content.Context
import com.buspljus.Baza.PosrednikBaze
import com.buspljus.IzracunavanjeVremena
import com.buspljus.sifraNaziv
import org.json.JSONArray
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import java.time.LocalTime

class ObradiTrasu(private val context: Context) {

    val trasaNizTacaka = mutableListOf<GeoPoint>()

    fun jsonArrayuGeoPoint(j: JSONArray): List<GeoPoint> {
        trasaNizTacaka.clear()
        for (gp in 0 until j.length()) {
            trasaNizTacaka.add(GeoPoint(j.getJSONObject(gp).getDouble("lat"),(j.getJSONObject(gp).getDouble("lon"))))
        }
        return trasaNizTacaka
    }

    fun preradiTrasu(
        trasaJson: JSONArray,
        preostaleStanice: List<List<Any>>,
        markerVozilo: MarkerItem,
        polazak: LocalTime
    ): Pair<List<GeoPoint>, List<sifraNaziv>> {

        val procenjenaVremenaDolazaka = preostaleStanice.map { it[1] as GeoPoint }
        val nizStajalista = mutableListOf<sifraNaziv>()

        jsonArrayuGeoPoint(trasaJson)

        if (procenjenaVremenaDolazaka.isNotEmpty()) {
            val listLocalTime = IzracunavanjeVremena().izracunavanjeVremena(
                procenjenaVremenaDolazaka, trasaNizTacaka, markerVozilo, polazak
            )

            for (b in preostaleStanice.indices) {
                nizStajalista.add(
                    sifraNaziv(
                        preostaleStanice[b][0] as String,
                        PosrednikBaze(context).idStaniceUNaziv(preostaleStanice[b][0] as String),
                        listLocalTime.getOrNull(b),
                        procenjenaVremenaDolazaka[b]
                    )
                )
            }
        }
        return Pair(trasaNizTacaka, nizStajalista)
    }
}