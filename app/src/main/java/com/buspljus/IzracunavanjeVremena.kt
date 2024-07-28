package com.buspljus

import org.json.JSONArray
import org.json.JSONObject
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class IzracunavanjeVremena {

    var vremeDolaska: LocalTime = LocalTime.now()
    var vremeDolaskaFinal = mutableListOf<LocalTime>()
    val spic = ((vremeDolaska.hour > 6) and (vremeDolaska.hour < 9)) or ((vremeDolaska.hour > 13) and (vremeDolaska.hour < 18))
    private val mnozilac = when (VoziloInfo.danunedelji) {
        0 -> if (spic) 4.3 else 4.0
        else -> 4.0
    }

    fun tranziranjeRV(rv: String, trasa: String): List<GeoPoint> {
        val listaGP = mutableListOf<GeoPoint>()
        var latLNG: JSONObject
        val rvJSON = JSONObject(rv).getJSONObject("rv")
        val trasaJSON = JSONArray(trasa)
        val potrebnoVremeZaPrelazakTrase = trasaJSON.length()*4
        for (sat in rvJSON.keys().iterator()) {
            val minut = JSONArray(rvJSON[sat].toString()).getJSONArray(VoziloInfo.danunedelji)
            for (c in 0 until minut.length()) {
                if (LocalTime.parse(sat + ":" + minut[c]).isBefore(LocalTime.now())) {
                    val vremenskoRastojanje = Duration.between(LocalTime.parse(sat+":"+minut[c] as String, DateTimeFormatter.ofPattern("HH:mm")), LocalTime.now()).toSeconds()
                    if (vremenskoRastojanje < potrebnoVremeZaPrelazakTrase) {
                        latLNG = JSONObject(trasaJSON[vremenskoRastojanje.div(4).toInt()].toString())
                        listaGP.add(GeoPoint(latLNG.getDouble("lat"), latLNG.getDouble("lon")))
                    }
                }
            }
        }
        return listaGP
    }

    fun izracunavanjeVremena(autoSTGeoPoint: List<GeoPoint>, gpx: JSONArray, vozilo: MarkerItem, prviSledeciPolazak: LocalTime): List<LocalTime> {
        var i = 0
        var aStGeoPoint = 0

        val pozicijaVozila = GeoPoint(vozilo.geoPoint.latitude, vozilo.geoPoint.longitude)
        var pozicijaGPX : GeoPoint? = null
        var rastojanjeVozila: Double?
        var rastojanjeStanica: Double?
        var voziloGPXPozicija = 0
        var stanicaGPXPozicija: Int
        var rastojanjeVozilaStart = 80

        var pronadjenoVozilo = false
        var pronadjenaStanica = false

        fun dodajpolazak() {
            vremeDolaskaFinal.add(LocalTime.parse(vremeDolaska.hour.toString().padStart(2, '0') + ":" +
                    vremeDolaska.minute.toString().padStart(2, '0'), DateTimeFormatter.ofPattern("HH:mm")))
        }

        fun pronadjiVozilo() {
            rastojanjeVozila = pozicijaGPX?.sphericalDistance(pozicijaVozila)

            if (rastojanjeVozila!! < rastojanjeVozilaStart) { // Pronadjeno vozilo
                voziloGPXPozicija = i
                pronadjenoVozilo = true
            }

            i += 1
        }

        fun traziZS() {
            rastojanjeStanica = pozicijaGPX?.sphericalDistance(autoSTGeoPoint[aStGeoPoint])

            if (rastojanjeStanica!! < 100) {
                if (aStGeoPoint == autoSTGeoPoint.size-1)
                    pronadjenaStanica = true
                else
                    aStGeoPoint += 1
                stanicaGPXPozicija = i

                vremeDolaska = prviSledeciPolazak.plusMinutes(((stanicaGPXPozicija-voziloGPXPozicija)*mnozilac/60).toLong())

                dodajpolazak()
            }

            i += 1
        }

        fun promeniPozicijuGPX() {
            pozicijaGPX = GeoPoint(gpx.getJSONObject(i).getDouble("lat"), gpx.getJSONObject(i).getDouble("lon"))
        }

        fun pokreni() {
            while (i < gpx.length()) {
                promeniPozicijuGPX()
                if (!pronadjenoVozilo)
                    pronadjiVozilo()

                if ( (pronadjenoVozilo) and (!pronadjenaStanica) )
                    traziZS()

                if ((pronadjenoVozilo) and (pronadjenaStanica))
                    break
            }
        }

        for (n in listOf(60, 100, 140, 180, 300)) {
            i = 0
            rastojanjeVozilaStart = n
            pokreni()
            if (vremeDolaskaFinal.size > 0)
                break
        }

        return vremeDolaskaFinal
    }
}