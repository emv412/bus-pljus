package com.buspljus

import org.json.JSONArray
import org.json.JSONObject
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class IzracunavanjeVremena {

    companion object {
        var mnozilac = 4.0
    }

    var vremeDolaska: LocalTime = LocalTime.now()
    var vremeDolaskaFinal = mutableListOf<LocalTime>()
    val spic = ((vremeDolaska.hour > 6) and (vremeDolaska.hour < 9)) or ((vremeDolaska.hour > 13) and (vremeDolaska.hour < 18))
    private val mnozilac = when (VoziloInfo.danunedelji) {
        0 -> if (spic) 4.3 else 4.0
        else -> 3.9
    }

    fun tranziranjeRV(rv: String, trasa: String): List<Pair<GeoPoint, LocalTime>> {
        val GPnaVR = mutableListOf<Pair<GeoPoint, LocalTime>>()
        var latLNG: JSONObject
        val rvJSON = JSONObject(rv).getJSONObject("rv")
        val trasaJSON = JSONArray(trasa)
        val potrebnoVremeZaPrelazakTrase = trasaJSON.length()*mnozilac
        for (sat in rvJSON.keys().iterator()) {
            val minut = JSONArray(rvJSON[sat].toString()).getJSONArray(VoziloInfo.danunedelji)
            for (c in 0 until minut.length()) {
                var vreme: LocalTime?
                if (sat == "24")
                    vreme = LocalTime.parse("00"+":"+minut[c])
                else
                    vreme = LocalTime.parse(sat+":"+minut[c])
                if (vreme.isBefore(LocalTime.now())) {
                    val vremenskoRastojanje = Duration.between(vreme, LocalTime.now()).toSeconds()
                    if (vremenskoRastojanje < potrebnoVremeZaPrelazakTrase) {
                        latLNG = JSONObject(trasaJSON[vremenskoRastojanje.div(mnozilac).toInt()].toString())
                        GPnaVR.add(Pair(GeoPoint(latLNG.getDouble("lat"), latLNG.getDouble("lon")), vreme))
                    }
                }
            }
        }
        return GPnaVR
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