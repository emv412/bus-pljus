package com.buspljus

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONArray
import org.json.JSONObject
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.properties.Delegates

class RedVoznje(private val context: Context) {

    private lateinit var sati : JSONObject

    private var brojacDvaPolaska by Delegates.notNull<Int>()
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
            val trenutnovreme = LocalTime.now()

            val danunedelji = when (LocalDate.now().dayOfWeek.value) {
                in 1 .. 5 -> 0
                6 -> 1
                7 -> 2
                else -> 0
            }

            dialog.setContentView(R.layout.prozor_redvoznje)

            linijarv = dialog.findViewById(R.id.linija_rv)!!
            linijarel = dialog.findViewById(R.id.linija_relacija)!!
            garBroj = dialog.findViewById(R.id.gb_redv)!!
            rastojanje = dialog.findViewById(R.id.rastojanje)!!

            kr.moveToFirst()
            try {
                var relacijaLinije = kr.getString(kr.getColumnIndexOrThrow("od"))+" - "+kr.getString(kr.getColumnIndexOrThrow("do"))
                val polasci = JSONObject(kr.getString(kr.getColumnIndexOrThrow("redvoznje")))

                val okretnica = JSONArray(kr.getString(kr.getColumnIndexOrThrow("stajalista"))).get(0).toString()

                if (okretnica == stanica_id)
                    relacijaLinije = kr.getString(kr.getColumnIndexOrThrow("do"))+" - "+kr.getString(kr.getColumnIndexOrThrow("od"))

                if (SQLcitac(context).pozahtevu_jednastanica(okretnica).sphericalDistance(markerItem.geoPoint) < 100) {

                    sati = polasci.getJSONObject("rv")
                    brojacDvaPolaska = 0

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

}