package com.buspljus

import android.content.Context
import android.database.Cursor
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.properties.Delegates

class RedVoznje(private val context: Context) {

    private lateinit var sati : JSONObject

    private var brojacDvaPolaska = 0
    private var prethodnoVreme = "-:--"

    private lateinit var dobijenoVreme: String

    private lateinit var prvipol : TextView
    private lateinit var drugipol : TextView
    private lateinit var redvoznje : TextView
    private lateinit var datum_rv : TextView
    private lateinit var sledeciPolasci : TextView
    private lateinit var danunedelji_textview : TextView
    private lateinit var prethodnipol : TextView
    private lateinit var linijarv : TextView
    private lateinit var linijarel : TextView
    private lateinit var garBroj : TextView
    private lateinit var rastojanje : TextView

    val dialog = BottomSheetDialog(context)

    val danunedelji = when (LocalDate.now().dayOfWeek.value) {
        in 1 .. 5 -> 0
        6 -> 1
        7 -> 2
        else -> 0
    }

    val trenutnovreme = LocalTime.now()

    fun redvoznjeKliknaVozilo(item: MarkerItem, odabranoStajalisteMarker: MarkerItem, stanica_id: String) : Boolean {
        val markerItem = item
        val rastojanjedostanice = markerItem.geoPoint.sphericalDistance(
            GeoPoint(
                odabranoStajalisteMarker.geoPoint.latitude,
                odabranoStajalisteMarker.geoPoint.longitude
            )
        ).div(1000)
        val rastojanjeprer = BigDecimal(rastojanjedostanice).setScale(1, RoundingMode.HALF_EVEN).toString() + " km"

        val kr = SQLcitac(context).redvoznjeKliknavozilo(markerItem.title,stanica_id)
        if (kr.count > 0) {
            dialog.setContentView(R.layout.prozor_redvoznje)

            linijarv = dialog.findViewById(R.id.linija_rv)!!
            linijarel = dialog.findViewById(R.id.linija_relacija)!!
            garBroj = dialog.findViewById(R.id.gb_redv)!!
            rastojanje = dialog.findViewById(R.id.rastojanje)!!

            kr.moveToFirst()
            try {
                val relacijaLinije = kr.getString(kr.getColumnIndexOrThrow("od"))+" - "+kr.getString(kr.getColumnIndexOrThrow("do"))
                val polasci = JSONObject(kr.getString(kr.getColumnIndexOrThrow("redvoznje")))

                val okretnica = JSONArray(kr.getString(kr.getColumnIndexOrThrow("stajalista"))).get(0).toString()

                if (SQLcitac(context).pozahtevu_jednastanica(okretnica).sphericalDistance(markerItem.geoPoint) < 100) {
                    sati = polasci.getJSONObject("rv")

                    prvipol = dialog.findViewById(R.id.prvipolazak)!!
                    drugipol = dialog.findViewById(R.id.drugipolazak)!!
                    redvoznje = dialog.findViewById(R.id.redvoznje)!!
                    datum_rv = dialog.findViewById(R.id.datum_rv)!!
                    sledeciPolasci = dialog.findViewById(R.id.polasci_textview)!!
                    danunedelji_textview = dialog.findViewById(R.id.rdsn)!!
                    prethodnipol = dialog.findViewById(R.id.prethodnipol)!!

                    prvipol.visibility = View.VISIBLE
                    drugipol.visibility = View.VISIBLE
                    redvoznje.visibility = View.VISIBLE
                    sledeciPolasci.visibility = View.VISIBLE
                    datum_rv.visibility = View.VISIBLE
                    prethodnipol.visibility = View.VISIBLE
                    danunedelji_textview.visibility = View.VISIBLE

                    when (danunedelji) {
                        0 -> danunedelji_textview.text=context.resources.getString(R.string.radni_dan)
                        1 -> danunedelji_textview.text=context.resources.getString(R.string.subota)
                        2 -> danunedelji_textview.text=context.resources.getString(R.string.nedelja)
                        else -> {}
                    }

                    with (polasci.getJSONArray("datum")) {
                        val datumRedaVoznje = this.getString(0)+". "+this.getString(1)+". "+this.getString(2)+"."
                        datum_rv.text=datumRedaVoznje
                    }

                    for (i in 0 .. sati.length()-1) {
                        val prviPolazak = sati.keys().asSequence().elementAt(0)
                        val sat = sati.keys().asSequence().elementAt(i)
                        for (k in 0 .. sati.getJSONArray(sat).getJSONArray(danunedelji).length()-1) {
                            dobijenoVreme = sat+":"+sati.getJSONArray(sat).getJSONArray(danunedelji)[k]
                            if (LocalTime.parse(dobijenoVreme, DateTimeFormatter.ofPattern("HH:mm")).isBefore(trenutnovreme.minusMinutes(1)) and (brojacDvaPolaska == 0)) {
                                prethodnoVreme = dobijenoVreme
                            }
                            else if (LocalTime.parse(dobijenoVreme, DateTimeFormatter.ofPattern("HH:mm")).isAfter(trenutnovreme.minusMinutes(1)) and (brojacDvaPolaska < 2)) {
                                if (brojacDvaPolaska == 0)
                                    prvipol.text=dobijenoVreme
                                else if (brojacDvaPolaska == 1)
                                    drugipol.text=dobijenoVreme
                                brojacDvaPolaska += 1
                            }
                        }
                        if ((i == sati.length()-1) and (prviPolazak == "00")) {
                            try {
                                val ponoc = prviPolazak+":"+sati.getJSONArray(prviPolazak).getJSONArray(danunedelji)[0]
                                when (brojacDvaPolaska) {
                                    0 -> prvipol.text=ponoc
                                    1 -> drugipol.text=ponoc
                                }
                            }
                            catch(e: Exception) {
                                Log.d(context.resources.getString(R.string.debug),"Greska: "+e)
                            }

                        }
                    }
                    prethodnipol.text=prethodnoVreme
                }

                linijarv.text=markerItem.title
                linijarel.text=relacijaLinije
                garBroj.text=markerItem.description
                rastojanje.text=rastojanjeprer

                dialog.show()
            }
            catch(e:Exception) {
                Log.d(context.resources.getString(R.string.debug),""+e)
            }
            kr.close()
        }
        return true
    }

    fun redVoznjeKliknaStanicu(kursor: Cursor) {
        kursor.moveToFirst()
        dialog.setContentView(R.layout.prozor_stredvoznje)
        val ime_okretnice = dialog.findViewById<TextView>(R.id.ime_okretnice)
        val prvi_polazak_odrediste = dialog.findViewById<TextView>(R.id.linija1_odrediste)
        val prvi_polazak_vreme = dialog.findViewById<TextView>(R.id.linija1_polazak)
        val drugi_polazak_odrediste = dialog.findViewById<TextView>(R.id.linija2_odrediste)
        val drugi_polazak_vreme = dialog.findViewById<TextView>(R.id.linija2_polazak)
        val treci_polazak_odrediste = dialog.findViewById<TextView>(R.id.linija3_odrediste)
        val treci_polazak_vreme = dialog.findViewById<TextView>(R.id.linija3_polazak)

        ime_okretnice?.text=kursor.getString(1)

        // Potrebna optimizacija

        val rv = JSONObject(kursor.getString(kursor.getColumnIndexOrThrow("redvoznje"))).getJSONObject(danunedelji.toString())
        val rezultat = mutableListOf<List<String>>()

        for (sifra_odredisne_stanice in rv.keys().iterator()) {
            val desifrovana_st = SQLcitac(context).SQLzahtev(6, arrayOf(sifra_odredisne_stanice))
            desifrovana_st.moveToFirst()
            for (satnica in rv.getJSONObject(sifra_odredisne_stanice).keys().iterator()) {
                for (minutaza in 0 until rv.getJSONObject(sifra_odredisne_stanice).getJSONArray(satnica).length()) {
                    dobijenoVreme = satnica + ":" + rv.getJSONObject(sifra_odredisne_stanice).getJSONArray(satnica).getJSONObject(minutaza).keys().next()
                    rezultat.add(listOf(desifrovana_st.getString(0), dobijenoVreme))
                }
            }
        }

        val rezultat2 = rezultat.sortedBy { it[1] }

        for (n in 0 until rezultat2.size) {
            if (LocalTime.parse(rezultat2[n][1], DateTimeFormatter.ofPattern("HH:mm")).isBefore(trenutnovreme.minusMinutes(1)) and (brojacDvaPolaska == 0)) {
                prvi_polazak_odrediste?.text=rezultat2[n][0]
                prvi_polazak_vreme?.text=rezultat2[n][1]
            }
            else {
                with (rezultat2[n][0]) {
                    if (brojacDvaPolaska == 0)
                        drugi_polazak_odrediste?.text=this
                    else if (brojacDvaPolaska == 1)
                        treci_polazak_odrediste?.text=this
                }
                with (rezultat2[n][1]) {
                    if (brojacDvaPolaska == 0)
                        drugi_polazak_vreme?.text=this
                    else if (brojacDvaPolaska == 1)
                        treci_polazak_vreme?.text=this
                }
                brojacDvaPolaska += 1
            }
        }

        kursor.close()
        dialog.show()
    }
}