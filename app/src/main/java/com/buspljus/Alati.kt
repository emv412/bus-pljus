package com.buspljus

import android.content.Context
import com.buspljus.Baza.Stanice
import org.json.JSONArray
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime

class Alati(private val context: Context) {
    fun proveriPolazak(voziloNaOkretnici: Boolean, prviSledeciPolazak: LocalTime): LocalTime {
        var lokalVreme = LocalTime.now()
        if (voziloNaOkretnici) {
            if (prviSledeciPolazak.isAfter(LocalTime.now()))
                lokalVreme = prviSledeciPolazak
        }
        return lokalVreme
    }

    fun proveraVozilaNaOkretnici(sveStaniceLinije: JSONArray, linija: MojMarker): Boolean {
        // provera blizine vozila u odnosu na okretnicu
        return if (Stanice(context).idStaniceuGeoPoint(sveStaniceLinije.get(0).toString()).sphericalDistance(linija.polozajVozila) < 100)
            true
        else
            false
    }

    fun zaokruzivanje(broj: Double, brojDecimala: Int): Double {
        return BigDecimal(broj).setScale(brojDecimala, RoundingMode.HALF_EVEN).toDouble()
    }

    fun prikaziDatumRV(datum: JSONArray): String {
        with(datum) {
            return this.getString(0) + ". " + this.getString(1) + ". " + this.getString(2) + "."
        }
    }

}