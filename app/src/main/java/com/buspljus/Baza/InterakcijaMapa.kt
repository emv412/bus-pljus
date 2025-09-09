package com.buspljus.Baza

import android.content.Context
import android.database.Cursor
import com.buspljus.BgVozStation
import com.buspljus.Glavna
import com.buspljus.Interfejs
import com.buspljus.VoziloInfo
import com.buspljus.grafika.PopupProzor
import org.oscim.core.GeoPoint

class InterakcijaMapa(context: Context) : UpitUBazu(context) {

    fun pretragabaze_kliknamapu(lat: String, lng: String, mapTapListener: Interfejs.MapTapListener, pozivOdFunkcije: Int) {
        val tackaGeoPoint = GeoPoint(lat.toDouble(), lng.toDouble())

        when (pozivOdFunkcije) {
            0 -> handleMapClickSearch(tackaGeoPoint, mapTapListener)
            1 -> handleProximitySearch(tackaGeoPoint, mapTapListener)
        }
    }

    private fun handleMapClickSearch(tackaGeoPoint: GeoPoint, mapTapListener: Interfejs.MapTapListener) {
        val nearbyBgVozStations = findNearbyBgVozStations(tackaGeoPoint, 50.0)

        nearbyBgVozStations.forEach { station ->
            VoziloInfo(context).pregledPolaskaVozova(station.naziv, station.redvoznje, 0)
        }

        if (nearbyBgVozStations.isEmpty() && Glavna.mapa.mapPosition.zoomLevel >= 14) {
            findNearbyRegularStations(tackaGeoPoint, mapTapListener)
        }
    }

    private fun handleProximitySearch(tackaGeoPoint: GeoPoint, mapTapListener: Interfejs.MapTapListener) {
        val nearbyStations = findNearbyBgVozStations(tackaGeoPoint, 400.0)

        if (nearbyStations.isNotEmpty()) {
            nearbyStations.forEach { station ->
                mapTapListener.koloneBGVOZ(listOf(station.id, station.naziv, station.redvoznje, station.distance))
            }
        } else {
            val broaderSearch = findBroaderBgVozSearch(tackaGeoPoint)
            processBroaderSearchResults(broaderSearch, tackaGeoPoint, mapTapListener)
        }
    }

    private fun findNearbyBgVozStations(tackaGeoPoint: GeoPoint, maxDistance: Double): List<BgVozStation> {
        val precision = if (maxDistance == 50.0) "1" else "3"
        val tacka = arrayOf(
            precision, tackaGeoPoint.latitude.toString(), precision,
            precision, tackaGeoPoint.longitude.toString(), precision
        )

        return queryAndProcess(
            PosrednikBaze.BGVOZ_TABLE,
            arrayOf("*"),
            "round(lt,?) = round(?,?) and round(lg,?) = round(?,?)",
            tacka
        ) { cursor -> processBgVozResults(cursor, tackaGeoPoint, maxDistance) }
    }

    private fun processBgVozResults(cursor: Cursor, tackaGeoPoint: GeoPoint, maxDistance: Double): List<BgVozStation> {
        val results = mutableListOf<BgVozStation>()

        while (cursor.moveToNext()) {
            val station = extractBgVozStation(cursor, tackaGeoPoint)
            if (station.distance < maxDistance) {
                results.add(station)
            }
        }

        return results
    }

    private fun extractBgVozStation(cursor: Cursor, tackaGeoPoint: GeoPoint): BgVozStation {
        val bazaGeoPoint = GeoPoint(
            cursor.getDouble(cursor.getColumnIndexOrThrow("lt")),
            cursor.getDouble(cursor.getColumnIndexOrThrow("lg"))
        )
        val rastojanje = bazaGeoPoint.sphericalDistance(tackaGeoPoint)

        return BgVozStation(
            id = cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.ID_KOLONA)),
            naziv = cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.STANICABGVOZNAZIV)),
            redvoznje = cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.RED_VOZNJE)),
            distance = rastojanje
        )
    }

    private fun findBroaderBgVozSearch(tackaGeoPoint: GeoPoint): Cursor {
        val precision = "2"
        val tacka = arrayOf(
            precision, tackaGeoPoint.latitude.toString(), precision,
            precision, tackaGeoPoint.longitude.toString(), precision
        )
        return SQLzahtev(
            PosrednikBaze.BGVOZ_TABLE,
            arrayOf("*"),
            "round(lt,?) = round(?,?) or round(lg,?) = round(?,?)",
            tacka,
            null
        )
    }

    private fun processBroaderSearchResults(cursor: Cursor, tackaGeoPoint: GeoPoint, mapTapListener: Interfejs.MapTapListener) {
        cursor.use {
            while (it.moveToNext()) {
                val station = extractBgVozStation(it, tackaGeoPoint)
                if (station.distance < 400) {
                    mapTapListener.koloneBGVOZ(listOf(station.id, station.naziv, station.redvoznje, station.distance))
                }
            }
        }
    }

    private fun findNearbyRegularStations(tackaGeoPoint: GeoPoint, mapTapListener: Interfejs.MapTapListener) {
        val precision = "3"
        val tacka = arrayOf(
            precision, tackaGeoPoint.latitude.toString(), precision,
            precision, tackaGeoPoint.longitude.toString(), precision
        )

        val cursor = SQLzahtev(
            PosrednikBaze.STANICE_TABLE,
            arrayOf("*"),
            "round(lt,?) = round(?,?) and round(lg,?) = round(?,?)",
            tacka,
            null
        )

        PopupProzor(context).pronadjeneStaniceAlertDialog(cursor, mapTapListener)
    }
}