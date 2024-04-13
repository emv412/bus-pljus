package com.buspljus

import android.content.Context
import android.database.Cursor
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.buspljus.SQLcitac.Companion.kursor
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

    private lateinit var sati : JSONObject

    private var brojacDvaPolaska = 0
    private var prethodnoVreme = "-:--"

    private lateinit var dobijenoVreme: String
    private lateinit var dobijenoVreme_lt : LocalTime

    val dialog = BottomSheetDialog(context)

    val danunedelji = when (LocalDate.now().dayOfWeek.value) {
        in 1 .. 5 -> 0
        6 -> 1
        7 -> 2
        else -> 0
    }

    val trenutnovreme = LocalTime.now()

    fun redvoznjeKliknaVozilo(item: MarkerItem, odabranoStajalisteMarker: MarkerItem, stanica_id: String) : Boolean {
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
        lateinit var odaberistanicu_textview : TextView
        lateinit var samobgvoz : CheckBox
        lateinit var stanicepadajucalista : Spinner
        lateinit var presedanjebgvoz : Button
        lateinit var zs : Cursor

        val markerItem = item
        val rastojanjedostanice = markerItem.geoPoint.sphericalDistance(
            GeoPoint(
                odabranoStajalisteMarker.geoPoint.latitude,
                odabranoStajalisteMarker.geoPoint.longitude
            )
        ).div(1000)

        val rastojanjeprer = BigDecimal(rastojanjedostanice).setScale(1, RoundingMode.HALF_EVEN).toString() + " km"
        val rvKursor = SQLcitac(context).redvoznjeKliknavozilo(markerItem.title, stanica_id)

        try {
            if (rvKursor.count > 0) {
                dialog.setContentView(R.layout.prozor_redvoznje)

                linijarv = dialog.findViewById(R.id.linija_rv)!!
                linijarel = dialog.findViewById(R.id.linija_relacija)!!
                garBroj = dialog.findViewById(R.id.gb_redv)!!
                rastojanje = dialog.findViewById(R.id.rastojanje)!!
                presedanjebgvoz = dialog.findViewById(R.id.presedanjebgvoz)!!

                rvKursor.moveToFirst()

                var relacijaLinije: String
                var polasci: JSONObject
                var sveStaniceLinije: JSONArray
                var datum : JSONArray
                rvKursor.use {
                    val pol = it.getString(it.getColumnIndexOrThrow("od"))
                    val odr = it.getString(it.getColumnIndexOrThrow("do"))
                    relacijaLinije = "$pol - $odr"
                    sveStaniceLinije = JSONArray(it.getString(it.getColumnIndexOrThrow("stajalista")))
                    polasci = JSONObject(it.getString(it.getColumnIndexOrThrow("redvoznje")))
                    datum = JSONArray(it.getString(it.getColumnIndexOrThrow("datumrv")))
                }

                val sveStaniceLinije_lista = mutableListOf<String>()
                val zeleznickeStaniceZaListu = mutableMapOf<String,List<String>>()
                for (b in 0 until sveStaniceLinije.length()) {
                    sveStaniceLinije_lista.add(sveStaniceLinije[b].toString())
                }
                for (c in sveStaniceLinije_lista.indexOf(stanica_id) until sveStaniceLinije_lista.size) {
                    val jednaKoordinata = SQLcitac(context).pozahtevu_jednastanica(sveStaniceLinije_lista[c])
                    zs = SQLcitac(context).SQLzahtev("bgvoz",arrayOf("_id","naziv","redvoznje"),"round(lt,?) = round(?,?) and round(lg,?) = round(?,?)",
                        arrayOf("2", jednaKoordinata.latitude.toString(), "2", "2", jednaKoordinata.longitude.toString(), "2"),null)
                    if (zs.count > 0) {
                        zs.moveToFirst()
                        zs.use {
                            val id = it.getString(it.getColumnIndexOrThrow("_id"))
                            val naziv = it.getString(it.getColumnIndexOrThrow("naziv"))
                            val redvoznje = it.getString(it.getColumnIndexOrThrow("redvoznje"))
                            zeleznickeStaniceZaListu.put(id,listOf(naziv,redvoznje))
                        }
                    }
                }

                if (zeleznickeStaniceZaListu.size > 0) {
                    presedanjebgvoz.visibility = View.VISIBLE

                    presedanjebgvoz.setOnClickListener {
                        prosirena_sekcija = dialog.findViewById(R.id.prosirena_sekcija)!!
                        if (prosirena_sekcija.visibility == View.VISIBLE) {
                            prosirena_sekcija.visibility = View.GONE
                        } else {
                            with(dialog) {
                                odaberistanicu_textview = findViewById(R.id.odaberistanicu_textview)!!
                                stanicepadajucalista = findViewById(R.id.stanicepadajucalista)!!
                                samobgvoz = findViewById(R.id.samobgvoz)!!
                                prosirena_sekcija.visibility = View.VISIBLE
                            }

                            val adapter = ArrayAdapter(context, R.layout.spinneritem, zeleznickeStaniceZaListu.map { it.value[0] } )
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            stanicepadajucalista.adapter = adapter

                            stanicepadajucalista.onItemSelectedListener =
                                object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                        pregledPolaskaVozova(zeleznickeStaniceZaListu.map { it.value[0] }[position],
                                            JSONObject(zeleznickeStaniceZaListu.map { it.value[1] }[position]).getJSONObject(danunedelji.toString()),1)
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
                        0 -> danunedelji_textview.text =
                            context.resources.getString(R.string.radni_dan)

                        1 -> danunedelji_textview.text =
                            context.resources.getString(R.string.subota)

                        2 -> danunedelji_textview.text =
                            context.resources.getString(R.string.nedelja)

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
                            if (dobijenoVreme_lt.isBefore(trenutnovreme.minusMinutes(1)) and (brojacDvaPolaska == 0)
                            ) {
                                prethodnoVreme = dobijenoVreme_lt.toString()
                            } else if (dobijenoVreme_lt.isAfter(trenutnovreme.minusMinutes(1)) and (brojacDvaPolaska < 2)
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
        finally {
            rvKursor.close()
        }
        return true
    }

    fun redVoznjeKliknaStanicu() {
        dialog.setContentView(R.layout.prozor_bgvoz)
        pregledPolaskaVozova(kursor.getString(kursor.getColumnIndexOrThrow("naziv")),JSONObject(
            kursor.getString(kursor.getColumnIndexOrThrow("redvoznje"))).getJSONObject(danunedelji.toString()),0)
    }

    fun pregledPolaskaVozova(imestanice: String, rv: JSONObject, pozivodfn: Int) {
        lateinit var ime_okretnice : TextView
        lateinit var prvi_polazak_odrediste : TextView
        lateinit var prvi_polazak_vreme : TextView
        lateinit var drugi_polazak_odrediste : TextView
        lateinit var drugi_polazak_vreme : TextView
        lateinit var treci_polazak_odrediste : TextView
        lateinit var treci_polazak_vreme : TextView
        lateinit var cetvrti_polazak_odrediste : TextView
        lateinit var cetvrti_polazak_vreme : TextView

        with (dialog) {
            ime_okretnice = findViewById(R.id.ime_okretnice)!!
            prvi_polazak_odrediste = findViewById(R.id.linija1_odrediste)!!
            prvi_polazak_vreme = findViewById(R.id.linija1_polazak)!!
            drugi_polazak_odrediste = findViewById(R.id.linija2_odrediste)!!
            drugi_polazak_vreme = findViewById(R.id.linija2_polazak)!!
            treci_polazak_odrediste = findViewById(R.id.linija3_odrediste)!!
            treci_polazak_vreme = findViewById(R.id.linija3_polazak)!!
            cetvrti_polazak_odrediste = findViewById(R.id.linija4_odrediste)!!
            cetvrti_polazak_vreme = findViewById(R.id.linija4_polazak)!!
        }


        val rezultat = mutableListOf<List<String>>()

        /*
        val lista = dialog.findViewById<ExpandableListView>(R.id.lista)

        val ladapter = SimpleExpandableListAdapter()

         */

        for (sifra_odredisne_stanice in rv.keys().iterator()) {
            with (SQLcitac(context).SQLzahtev("bgvoz",arrayOf("naziv"),"_id = ?",arrayOf(sifra_odredisne_stanice),null)) {
                if (this.count > 0) {
                    moveToFirst()
                    use {
                        for (satnica in rv.getJSONObject(sifra_odredisne_stanice).keys().iterator()) {
                            for (minutaza in 0 until rv.getJSONObject(sifra_odredisne_stanice).getJSONArray(satnica).length()) {
                                dobijenoVreme = satnica + ":" + rv.getJSONObject(sifra_odredisne_stanice).getJSONArray(satnica).getJSONObject(minutaza).keys().next()
                                rezultat.add(listOf(it.getString(0), dobijenoVreme))
                            }
                        }
                    }
                }
            }
        }

        val rezultat2 = rezultat.sortedBy { it[1] }
        brojacDvaPolaska = 0

        for (n in 0 until rezultat2.size) {
            dobijenoVreme_lt = LocalTime.parse(rezultat2[n][1], DateTimeFormatter.ofPattern("HH:mm"))
            if (dobijenoVreme_lt.isBefore(trenutnovreme.minusMinutes(1)) and (brojacDvaPolaska == 0)) {
                prvi_polazak_odrediste.text=rezultat2[n][0]
                prvi_polazak_vreme.text=rezultat2[n][1]
            }
            else if (brojacDvaPolaska < 3) {
                with (rezultat2[n][0]) {
                    when (brojacDvaPolaska) {
                        0 -> drugi_polazak_odrediste.text = this
                        1 -> treci_polazak_odrediste.text = this
                        2 -> cetvrti_polazak_odrediste.text = this
                    }
                }
                with (rezultat2[n][1]) {
                    when (brojacDvaPolaska) {
                        0 -> drugi_polazak_vreme.text=this
                        1 -> treci_polazak_vreme.text=this
                        2 -> cetvrti_polazak_vreme.text=this
                    }
                }
                brojacDvaPolaska += 1
            }
            else break
        }
        if (pozivodfn == 0) {
            ime_okretnice.text=imestanice
            if (!dialog.isShowing)
                dialog.show()
        }
        else {
            ime_okretnice.visibility = View.GONE
        }

    }
}