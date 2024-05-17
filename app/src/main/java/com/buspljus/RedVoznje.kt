package com.buspljus

import android.content.Context
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONArray
import org.json.JSONObject
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol
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
    lateinit var vozilostizeoko : TextView
    lateinit var prosirena_sekcija : ConstraintLayout
    lateinit var stanicepadajucalista : Spinner
    lateinit var presedanjebgvoz : Button
    lateinit var trasaDugme : Button
    lateinit var prosirivalista : ExpandableListView
    lateinit var ime_okretnice : TextView
    lateinit var jednaKoordinata : GeoPoint
    lateinit var prikazilokstanice : ImageButton

    var vremeDolaska : LocalTime? = null
    val dialog = BottomSheetDialog(context)

    fun zaokruzivanje(broj: Double, brojDecimala: Int): Double {
        return BigDecimal(broj).setScale(brojDecimala, RoundingMode.HALF_EVEN).toDouble()
    }

    fun redvoznjeKliknaVozilo(item: MarkerItem, odabranoStajalisteMarker: MarkerItem) : Boolean {
        val markerItem = item
        val rastojanjedostanice = markerItem.geoPoint.sphericalDistance(
            GeoPoint(
                odabranoStajalisteMarker.geoPoint.latitude,
                odabranoStajalisteMarker.geoPoint.longitude
            )
        ).div(1000)

        val rastojanjeprer = zaokruzivanje(rastojanjedostanice,1).toString() + " km"
        val dobijenaLista = SQLcitac(context).redvoznjeKliknavozilo(markerItem.title, odabranoStajalisteMarker.title)

        try {
            if (dobijenaLista.isNotEmpty()) {
                with (dialog) {
                    setContentView(R.layout.prozor_redvoznje)
                    linijarv = findViewById(R.id.linija_rv)!!
                    linijarel = findViewById(R.id.linija_relacija)!!
                    garBroj = findViewById(R.id.gb_redv)!!
                    rastojanje = findViewById(R.id.rastojanje)!!
                    presedanjebgvoz = findViewById(R.id.presedanjebgvoz)!!
                    trasaDugme = findViewById(R.id.prikaztrase)!!
                    vozilostizeoko = findViewById(R.id.vozilostizetextview)!!
                    prikazilokstanice = findViewById(R.id.prikazilokstanice)!!
                }

                val pol = dobijenaLista[0]
                val odr = dobijenaLista[1]

                val sveStaniceLinije = JSONArray(dobijenaLista[2])
                val polasci = JSONObject(dobijenaLista[3])
                val datum = JSONArray(dobijenaLista[4])

                val relacijaLinije = "$pol - $odr"

                val zeleznickeStaniceZaListu = mutableMapOf<String,List<Any>>()

                fun izracunavanjeVremena(autoSTGeoPoint : GeoPoint, sifraSt : String) {
                    var i = 0

                    fun pojacivacI(x : Double) {
                        if (x > 4000)
                            i += 200
                        else if (x > 1000)
                            i += 50
                        else if (x > 700)
                            i += 10
                        else i += 1
                    }

                    val gpx = crtanjeTrase(markerItem, odabranoStajalisteMarker, null)
                    val pozicijaVozila = GeoPoint(item.geoPoint.latitude, item.geoPoint.longitude)
                    var pozicijaGPX : GeoPoint
                    var rastojanjeVozila : Double
                    var rastojanjeStanica : Double
                    var voziloGPXPozicija = 0
                    var stanicaGPXPozicija = 0


                    var pronadjenoVozilo = false
                    var pronadjenaStanica = false

                    while (i < gpx[1].length()) {
                        pozicijaGPX = GeoPoint(gpx[1].getJSONObject(i).getDouble("lat"), gpx[1].getJSONObject(i).getDouble("lon"))
                        if (!pronadjenoVozilo) {
                            rastojanjeVozila = pozicijaGPX.sphericalDistance(pozicijaVozila)

                            pojacivacI(rastojanjeVozila)

                            if (rastojanjeVozila < 35) { // Pronadjeno vozilo
                                voziloGPXPozicija = i
                                pronadjenoVozilo = true
                            }
                        }

                        if ((pronadjenoVozilo) and (!pronadjenaStanica)) {
                            rastojanjeStanica = pozicijaGPX.sphericalDistance(autoSTGeoPoint)

                            pojacivacI(rastojanjeStanica)

                            if (rastojanjeStanica < 45) {
                                stanicaGPXPozicija = i
                                vremeDolaska = LocalTime.now().plusMinutes(((stanicaGPXPozicija-voziloGPXPozicija)*4/60).toLong())
                                vozilostizeoko.text = StringBuilder(context.resources.getString(R.string.vozilostizeoko) +
                                        " "+LocalTime.parse(vremeDolaska?.hour.toString().padStart(2, '0') + ":" +
                                        vremeDolaska?.minute.toString().padStart(2, '0'), DateTimeFormatter.ofPattern("HH:mm"))+", "+
                                        context.resources.getString(R.string.stanicarb)+" "+ sifraSt
                                )
                                pronadjenaStanica = true
                            }
                        }
                        if ((pronadjenoVozilo) and (pronadjenaStanica))
                            break
                    }
                }

                var prolazUIf = false

                for (brojac in 0 until sveStaniceLinije.length()) {
                    if ((sveStaniceLinije[brojac] == odabranoStajalisteMarker.title) and (!prolazUIf)) {
                        prolazUIf = true
                        continue
                    }
                    if (prolazUIf) {
                        jednaKoordinata = SQLcitac(context).pozahtevu_jednastanica(sveStaniceLinije[brojac].toString())
                        SQLcitac(context).pretragabaze_kliknamapu(jednaKoordinata.latitude.toString(),jednaKoordinata.longitude.toString(), object: SQLcitac.Callback {
                            override fun korak(s: String) {
                            }

                            override fun koloneBGVOZ(lista: List<String>) {
                                zeleznickeStaniceZaListu[lista[0]] = listOf(lista[1], lista[2], jednaKoordinata, sveStaniceLinije[brojac])
                                if (!zadovoljenUslov)
                                    if (SQLcitac(context).preradaRVJSON(JSONObject(lista[2]), null, null, 1, null).isNotEmpty())
                                    zadovoljenUslov = true
                            }
                        },1)
                    }
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

                                        prikazilokstanice.setOnClickListener {
                                            val xy = zeleznickeStaniceZaListu.map {it.value[2]}[position] as GeoPoint
                                            Glavna.mapa.setMapPosition(xy.latitude, xy.longitude, 80000.0)
                                            crtanjeTrase(markerItem, odabranoStajalisteMarker, zeleznickeStaniceZaListu.map { it.value[3] }[position] as String)
                                            dialog.behavior.state=BottomSheetBehavior.STATE_COLLAPSED
                                        }

                                        izracunavanjeVremena(zeleznickeStaniceZaListu.map { it.value[2] }[position] as GeoPoint,
                                            zeleznickeStaniceZaListu.map { it.value[3] }[position] as String)
                                        pregledPolaskaVozova(
                                            zeleznickeStaniceZaListu.map { it.value[0] }[position].toString(),
                                            zeleznickeStaniceZaListu.map { it.value[1] }[position].toString(), 1)
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {
                                        Toster(context).toster("nista nije izabrano")
                                    }

                                }
                        }
                    }
                }

                // Provera da li se vozilo nalazi na do 100 metara vazdusno od okretnice
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

                    val view = listOf(prvipol, drugipol, redvoznje_textview, sledeciPolasci, datum_rv, prethodnipol, danunedelji_textview)
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
                                Log.d(context.resources.getString(R.string.debug), "Greska: $e")
                            }

                        }
                    }
                    prethodnipol.text = prethodnoVreme
                }

                linijarv.text = markerItem.title
                linijarel.text = relacijaLinije
                garBroj.text = markerItem.description
                rastojanje.text = rastojanjeprer

                trasaDugme.setOnClickListener {
                    crtanjeTrase(markerItem, odabranoStajalisteMarker, null)
                }

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

        val rezultat = SQLcitac(context).preradaRVJSON(JSONObject(rv), null,null, 0, vremeDolaska)

        if (rezultat.size > 0)
            prosirivalista.setAdapter(KursorAdapterVoz(context, rezultat.sortedBy { it[1] },prosirivalista))

    }

    fun crtanjeTrase(markerVozilo: MarkerItem, markerStanica: MarkerItem, presedackaSt : String?): List<JSONArray> {
        Glavna.putanja.clearPath()
        Glavna.sveStanice.removeAllItems()
        val trasa = SQLcitac(context).prikaziTrasu(markerVozilo.title, markerStanica.title)

        for (i in 0 until trasa[0].length()) {
            val koordinateStanice = SQLcitac(context).pozahtevu_jednastanica(trasa[0][i].toString())
            val marker = MarkerItem(null, trasa[0][i].toString(),null, GeoPoint(koordinateStanice.latitude, koordinateStanice.longitude))
            marker.marker = MarkerSymbol(
                AndroidBitmap(VectorDrawableCompat.create(
                    context.resources, if ((presedackaSt != null) and (presedackaSt == trasa[0][i].toString())) R.drawable.crvena_tacka else R.drawable.plava_tacka, context.theme)!!.toBitmap()), MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true)
            Glavna.sveStanice.addItem(marker)
        }

        for (i in 0 until trasa[1].length()) {
            Glavna.putanja.addPoints(listOf(GeoPoint(
                trasa[1].getJSONObject(i)["lat"].toString().toDouble(),
                trasa[1].getJSONObject(i)["lon"].toString().toDouble()
            )))
        }
        return trasa
    }
}