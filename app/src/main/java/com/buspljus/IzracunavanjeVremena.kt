package com.buspljus

import org.json.JSONArray
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class IzracunavanjeVremena {

    var vremeDolaska: LocalTime = LocalTime.now()
    var vremeDolaskaFinal = mutableListOf<LocalTime>()

    fun izracunavanjeVremena(autoSTGeoPoint: List<GeoPoint>, gpx: JSONArray, vozilo: MarkerItem, prviSledeciPolazak: LocalTime, rezim: Int): List<LocalTime> {
        var i = 0
        var aStGeoPoint = 0

        val pozicijaVozila = GeoPoint(vozilo.geoPoint.latitude, vozilo.geoPoint.longitude)
        var pozicijaGPX : GeoPoint? = null
        var rastojanjeVozila: Double?
        var rastojanjeStanica: Double?
        var voziloGPXPozicija = 0
        var stanicaGPXPozicija: Int
        val testNiz = mutableListOf<List<Any>>()

        var pronadjenoVozilo = false
        var pronadjenaStanica = false

        fun pojacivacI(x : Double) {
            when {
                x > 5000 -> i += 200
                x > 1500 -> i += 20
                //x > 300 -> i += 5
                else -> i += 1
            }
        }

        fun dodajpolazak() {
            vremeDolaskaFinal.add(LocalTime.parse(vremeDolaska.hour.toString().padStart(2, '0') + ":" +
                    vremeDolaska.minute.toString().padStart(2, '0'), DateTimeFormatter.ofPattern("HH:mm")))
        }

        fun pronadjiVozilo() {
            rastojanjeVozila = pozicijaGPX?.sphericalDistance(pozicijaVozila)

            if (rastojanjeVozila!! < 150) { // Pronadjeno vozilo
                voziloGPXPozicija = i
                pronadjenoVozilo = true
            }

            pojacivacI(rastojanjeVozila!!)
        }

        fun traziZS() {
            rastojanjeStanica = pozicijaGPX?.sphericalDistance(autoSTGeoPoint[aStGeoPoint])
            testNiz.add(listOf(rastojanjeStanica!!, pozicijaGPX as GeoPoint))

            if (rastojanjeStanica!! < 100) {
                if (aStGeoPoint == autoSTGeoPoint.size-1)
                    pronadjenaStanica = true
                else
                    aStGeoPoint += 1
                stanicaGPXPozicija = i

                vremeDolaska = prviSledeciPolazak.plusMinutes(((stanicaGPXPozicija-voziloGPXPozicija)*4/60).toLong())

                dodajpolazak()
            }

            pojacivacI(rastojanjeStanica!!)
        }

        fun promeniPozicijuGPX() {
            pozicijaGPX = GeoPoint(gpx.getJSONObject(i).getDouble("lat"), gpx.getJSONObject(i).getDouble("lon"))
        }

        while (i < gpx.length()) {
            promeniPozicijuGPX()
            if (!pronadjenoVozilo)
                pronadjiVozilo()

            if ( (pronadjenoVozilo) and (!pronadjenaStanica) )
                traziZS()

            if ((pronadjenoVozilo) and (pronadjenaStanica))
                break
        }

        if ((rezim == 0) and ((!pronadjenoVozilo) and (!pronadjenaStanica)))
            vremeDolaskaFinal.add(LocalTime.MIDNIGHT)

        return vremeDolaskaFinal
    }
}