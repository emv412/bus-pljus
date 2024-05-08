package com.buspljus

import android.content.Context
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONArray
import org.json.JSONObject
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class RedVoznje(private val context: Context) {
    companion object {
        val danunedelji = when (LocalDate.now().dayOfWeek.value) {
            in 1 .. 5 -> 0
            6 -> 1
            7 -> 2
            else -> 0
        }
    }

    private lateinit var sati : JSONObject

    private var brojacDvaPolaska = 0
    private var zadovoljenUslov = false
    private var prethodnoVreme = "-:--"

    private lateinit var dobijenoVreme_lt : LocalTime
    lateinit var prvipol : TextView
    lateinit var drugipol : TextView
    lateinit var redvoznje_textview : TextView
    lateinit var datum_rv : TextView
    lateinit var sledeciPolasci : TextView
    lateinit var danunedelji_textview : TextView
    lateinit var prethodnipol : TextView
    lateinit var linijarv : TextView
    lateinit var linijarel : TextView
    lateinit var garBroj : TextView
    lateinit var rastojanje : TextView
    lateinit var prosirena_sekcija : ConstraintLayout
    lateinit var stanicepadajucalista : Spinner
    lateinit var presedanjebgvoz : Button
    lateinit var prosirivalista : ExpandableListView
    lateinit var ime_okretnice : TextView

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
        val dobijenaLista = SQLcitac(context).redvoznjeKliknavozilo(markerItem.title, stanica_id)

        try {
            if (dobijenaLista.isNotEmpty()) {
                with (dialog) {
                    setContentView(R.layout.prozor_redvoznje)
                    linijarv = findViewById(R.id.linija_rv)!!
                    linijarel = findViewById(R.id.linija_relacija)!!
                    garBroj = findViewById(R.id.gb_redv)!!
                    rastojanje = findViewById(R.id.rastojanje)!!
                    presedanjebgvoz = findViewById(R.id.presedanjebgvoz)!!
                }

                val pol = dobijenaLista[0]
                val odr = dobijenaLista[1]

                val sveStaniceLinije = JSONArray(dobijenaLista[2])
                val polasci = JSONObject(dobijenaLista[3])
                val datum = JSONArray(dobijenaLista[4])

                val relacijaLinije = "$pol - $odr"

                val sveStaniceLinije_lista = mutableListOf<String>()
                val zeleznickeStaniceZaListu = mutableMapOf<String,List<String>>()

                for (b in 0 until sveStaniceLinije.length()) {
                    sveStaniceLinije_lista.add(sveStaniceLinije[b].toString())
                }

                for (c in sveStaniceLinije_lista.indexOf(stanica_id) until sveStaniceLinije_lista.size) {
                    val jednaKoordinata = SQLcitac(context).pozahtevu_jednastanica(sveStaniceLinije_lista[c])
                    SQLcitac(context).pretragabaze_kliknamapu(jednaKoordinata.latitude.toString(),jednaKoordinata.longitude.toString(),object: SQLcitac.Callback {
                        override fun korak(s: String) {
                        }

                        override fun koloneBGVOZ(lista: List<String>) {
                            zeleznickeStaniceZaListu[lista[0]] = listOf(lista[1],lista[2])
                            if (SQLcitac(context).preradaRVJSON(JSONObject(lista[2]), null, null).isNotEmpty())
                                zadovoljenUslov = true
                        }
                    },1)
                }

                if ((zeleznickeStaniceZaListu.isNotEmpty()) and (zadovoljenUslov)) {
                    presedanjebgvoz.visibility = View.VISIBLE

                    presedanjebgvoz.setOnClickListener {
                        prosirena_sekcija = dialog.findViewById(R.id.prosirena_sekcija)!!
                        if (prosirena_sekcija.visibility == View.VISIBLE) {
                            prosirena_sekcija.visibility = View.GONE
                        } else {
                            with(dialog) {
                                stanicepadajucalista = findViewById(R.id.stanicepadajucalista)!!
                                prosirena_sekcija.visibility = View.VISIBLE
                            }

                            val adapter = ArrayAdapter(context, R.layout.spinneritem, zeleznickeStaniceZaListu.map { it.value[0] } )
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            stanicepadajucalista.adapter = adapter

                            stanicepadajucalista.onItemSelectedListener =
                                object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                        pregledPolaskaVozova(zeleznickeStaniceZaListu.map { it.value[0] }[position],
                                            zeleznickeStaniceZaListu.map { it.value[1] }[position], 1)
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {
                                        Toster(context).toster("nista nije izabrano")
                                    }

                                }
                        }
                    }
                }

                // Provera da li se vozilo nalazi na do 100 metara vazdusno od okursoretnice
                if (SQLcitac(context).pozahtevu_jednastanica(sveStaniceLinije.get(0).toString())
                        .sphericalDistance(markerItem.geoPoint) < 100
                ) {
                    sati = polasci.getJSONObject("rv")

                    with (dialog) {
                        prvipol = findViewById(R.id.prvipolazak)!!
                        drugipol = findViewById(R.id.drugipolazak)!!
                        redvoznje_textview = findViewById(R.id.redvoznje_textview)!!
                        datum_rv = findViewById(R.id.datum_rv)!!
                        sledeciPolasci = findViewById(R.id.polasci_textview)!!
                        danunedelji_textview = findViewById(R.id.rdsn)!!
                        prethodnipol = findViewById(R.id.prethodnipol)!!
                    }

                    val view = listOf(prvipol,drugipol,redvoznje_textview,sledeciPolasci,datum_rv,prethodnipol,danunedelji_textview)
                    for (j in view)
                        j.visibility = View.VISIBLE

                    when (danunedelji) {
                        0 -> danunedelji_textview.text = context.resources.getString(R.string.radni_dan)
                        1 -> danunedelji_textview.text = context.resources.getString(R.string.subota)
                        2 -> danunedelji_textview.text = context.resources.getString(R.string.nedelja)

                        else -> {}
                    }

                    with(datum) {
                        val datumRedaVoznje = this.getString(0) + ". " + this.getString(1) + ". " + this.getString(2) + "."
                        datum_rv.text = datumRedaVoznje
                    }

                    for (i in 0..sati.length() - 1) {
                        val prviPolazak = sati.keys().asSequence().elementAt(0)
                        val sat = sati.keys().asSequence().elementAt(i)
                        for (k in 0 until sati.getJSONArray(sat).getJSONArray(danunedelji).length()) {
                            dobijenoVreme_lt = LocalTime.parse(sat + ":" + sati.getJSONArray(sat).getJSONArray(danunedelji)[k], DateTimeFormatter.ofPattern("HH:mm"))
                            if (dobijenoVreme_lt.isBefore(LocalTime.now().minusMinutes(1)) and (brojacDvaPolaska == 0)
                            ) {
                                prethodnoVreme = dobijenoVreme_lt.toString()
                            } else if (dobijenoVreme_lt.isAfter(LocalTime.now().minusMinutes(1)) and (brojacDvaPolaska < 2)
                            ) {
                                if (brojacDvaPolaska == 0)
                                    prvipol.text = dobijenoVreme_lt.toString()
                                else if (brojacDvaPolaska == 1)
                                    drugipol.text = dobijenoVreme_lt.toString()
                                brojacDvaPolaska += 1
                            }
                        }
                        if ((i == sati.length() - 1) and (prviPolazak == "00") and (sati.getJSONArray(prviPolazak).getJSONArray(danunedelji).length() > 0)) {
                            try {
                                val ponoc = prviPolazak + ":" + sati.getJSONArray(prviPolazak).getJSONArray(danunedelji)[0]
                                when (brojacDvaPolaska) {
                                    0 -> prvipol.text = ponoc
                                    1 -> drugipol.text = ponoc
                                }
                            } catch (e: Exception) {
                                Log.d(context.resources.getString(R.string.debug), "Greska: " + e)
                            }

                        }
                    }
                    prethodnipol.text = prethodnoVreme
                }

                linijarv.text = markerItem.title
                linijarel.text = relacijaLinije
                garBroj.text = markerItem.description
                rastojanje.text = rastojanjeprer

                if (!dialog.isShowing)
                    dialog.show()
            }
        } catch (e:Exception) {
            Log.d(context.resources.getString(R.string.debug),""+e)
        }
        return true
    }

    fun pregledPolaskaVozova(imestanice: String, rv: String, pozivodfn: Int) {
        with (dialog) {
            behavior.state=BottomSheetBehavior.STATE_EXPANDED
            if (pozivodfn == 0)
                setContentView(R.layout.prozor_bgvoz)
            ime_okretnice = findViewById(R.id.ime_okretnice)!!

            with (ime_okretnice) {
                text = if (pozivodfn == 0) imestanice else null
                visibility = if (pozivodfn != 0) View.GONE else View.VISIBLE
            }

            if (!isShowing)
                show()

            prosirivalista = findViewById(R.id.prosirenje)!!
            if (pozivodfn == 0)
                prosirivalista.layoutParams.height= WindowManager.LayoutParams.WRAP_CONTENT
            behavior.isDraggable=false
        }

        val rezultat = SQLcitac(context).preradaRVJSON(JSONObject(rv), null,null)

        if (rezultat.size > 0)
            prosirivalista.setAdapter(KursorAdapterVoz(context, rezultat.sortedBy { it[1] },prosirivalista))

    }
}