package com.buspljus

import android.content.Context
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ExpandableListView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.buspljus.Adapteri.PrikazZStanica
import com.buspljus.Adapteri.sifraNaziv
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        private var pozivTrasaInterface: Interfejs.trasa? = null

        var preostaleStanice : MutableList<String> = mutableListOf()

        var voziloCache = mutableListOf<String>("0","0","0")

        val zeleznickeStaniceZaListu = mutableMapOf<String,List<Any>>()
        val stNiz = mutableListOf<sifraNaziv>()
        val gpNiz = mutableListOf<GeoPoint>()
    }

    private lateinit var sati : JSONObject

    private var voziloNaOkretnici = false
    private var samoBGVBoolean = false
    private var rezultat = mutableListOf(listOf<String>())

    private lateinit var vreme : String
    private lateinit var vozoviPosleVremena : LocalTime
    private lateinit var prviSledeciPolazak : LocalTime
    private lateinit var prvipol : TextView
    private lateinit var redvoznjeTextview : TextView
    private lateinit var datumRv : TextView
    private lateinit var sledeciPolasci : TextView
    private lateinit var danunedeljiTextview : TextView
    private lateinit var linijarv : TextView
    private lateinit var linijarel : TextView
    private lateinit var garBroj : TextView
    private lateinit var rastojanje : TextView
    private lateinit var vozilostizeoko : TextView
    private lateinit var prosirenaSekcija : ConstraintLayout
    private lateinit var stanicepadajucalista : Spinner
    private lateinit var presedanjebgvoz : Button
    private lateinit var trasaDugme : Button
    private lateinit var samoBGVCheckBox : CheckBox
    private lateinit var prosirivalista : ExpandableListView
    private lateinit var imeOkretnice : TextView
    private lateinit var ucitavanjePresedanja : ProgressBar
    private lateinit var ucitavamStanice : ProgressBar
    lateinit var jednaKoordinata : GeoPoint
    lateinit var prikazilokstanice : ImageButton

    private val dialog = BottomSheetDialog(context)

    fun registracijaCallback(povratniPZ: Interfejs.trasa) {
        pozivTrasaInterface = povratniPZ
    }

    fun zaokruzivanje(broj: Double, brojDecimala: Int): Double {
        return BigDecimal(broj).setScale(brojDecimala, RoundingMode.HALF_EVEN).toDouble()
    }

    fun staniceDoKrajaTrase(sveStaniceLinije: JSONArray, odabranoStajalisteMarker: MarkerItem) {
        preostaleStanice.clear()
        var pronadjenoStajaliste = false
        for (brojac in 0 until sveStaniceLinije.length()) {
            if ((sveStaniceLinije[brojac] == odabranoStajalisteMarker.title) or (pronadjenoStajaliste)) {
                if (!pronadjenoStajaliste) {
                    pronadjenoStajaliste = true
                    continue
                }
                else {
                    preostaleStanice.add(sveStaniceLinije[brojac].toString())
                }
            }
        }
    }

    fun prikaziDatumRV(datum: JSONArray): String {
        with(datum) {
            return this.getString(0) + ". " + this.getString(1) + ". " + this.getString(2) + "."
        }
    }

    fun redvoznjeKliknaVozilo(linija: MarkerItem, odabranoStajalisteMarker: MarkerItem) : Boolean {
        val asinhrono = CoroutineScope(Dispatchers.Main)
        vreme = Regex("(?<= ).+").find(linija.description)?.value.toString()
        val rastojanjedostanice = linija.geoPoint.sphericalDistance(
            GeoPoint(
                odabranoStajalisteMarker.geoPoint.latitude,
                odabranoStajalisteMarker.geoPoint.longitude
            )
        ).div(1000)

        val rastojanjeprer = zaokruzivanje(rastojanjedostanice,1).toString() + " km"
        val dobijenaLista = SQLcitac(context).kliknavozilo(linija.title, odabranoStajalisteMarker.title)

        try {
            if (dobijenaLista.isNotEmpty()) {
                with (dialog) {
                    setContentView(R.layout.info_o_vozilu)
                    linijarv = findViewById(R.id.linija_rv)!!
                    linijarel = findViewById(R.id.linija_relacija)!!
                    garBroj = findViewById(R.id.gb_redv)!!
                    rastojanje = findViewById(R.id.rastojanje)!!
                    presedanjebgvoz = findViewById(R.id.presedanjebgvoz)!!
                    trasaDugme = findViewById(R.id.prikaztrase)!!
                    vozilostizeoko = findViewById(R.id.vozilostizetextview)!!
                    prikazilokstanice = findViewById(R.id.prikazilokstanice)!!
                    samoBGVCheckBox = findViewById(R.id.samoBGVoz)!!
                    ucitavanjePresedanja = findViewById(R.id.ucitavanjePresedanja)!!
                    ucitavamStanice = findViewById(R.id.ucitavanjeStanica)!!

                    ucitavanjePresedanja.visibility = View.VISIBLE

                    show()
                }

                val pol = dobijenaLista[0]
                val odr = dobijenaLista[1]

                val sveStaniceLinije = JSONArray(dobijenaLista[2])
                val polasci = JSONObject(dobijenaLista[3])
                val datum = JSONArray(dobijenaLista[4])

                val relacijaLinije = "$pol - $odr"

                var staroRastojanje = 1000.0
                var novoRastojanje: Double

                fun pretresiZS() {
                    if ((voziloCache[0] != linija.title) or (voziloCache[1] != odabranoStajalisteMarker.title) or (voziloCache[2] != vreme)) {
                        zeleznickeStaniceZaListu.clear()
                        staniceDoKrajaTrase(sveStaniceLinije, odabranoStajalisteMarker)

                        for (brojac in 0 until preostaleStanice.size) {
                            jednaKoordinata = SQLcitac(context).idStaniceuGeoPoint(preostaleStanice[brojac])
                            SQLcitac(context).pretragabaze_kliknamapu(jednaKoordinata.latitude.toString(),jednaKoordinata.longitude.toString(), object: Interfejs.Callback {
                                override fun korak(s: String) {
                                }

                                override fun koloneBGVOZ(lista: List<Any>) {
                                    novoRastojanje = lista[3] as Double
                                    if (novoRastojanje < staroRastojanje) {
                                        zeleznickeStaniceZaListu[lista[0] as String] = listOf(lista[1], lista[2], jednaKoordinata, preostaleStanice[brojac])
                                        staroRastojanje = lista[3] as Double
                                    }
                                }
                            },1)
                        }
                        ucitavanjePresedanja.visibility = View.GONE
                    }
                }

                fun asinhronoUcitajStanice() {
                    prosirenaSekcija = dialog.findViewById(R.id.prosirena_sekcija)!!
                    if (prosirenaSekcija.visibility == View.VISIBLE) {
                        prosirenaSekcija.visibility = View.GONE
                    } else {
                        with (dialog) {
                            stanicepadajucalista = findViewById(R.id.stanicepadajucalista)!!
                            prosirenaSekcija.visibility = View.VISIBLE
                        }

                        val adapter = ArrayAdapter(context, R.layout.spinneritem, zeleznickeStaniceZaListu.map { it.value[0] } )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        stanicepadajucalista.adapter = adapter

                        stanicepadajucalista.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                    ucitavamStanice.visibility = View.VISIBLE

                                    val autoSTGeoPoint = zeleznickeStaniceZaListu.map { it.value[2] }[position] as GeoPoint
                                    val izlaznaSt = SQLcitac(context).idStaniceUNaziv(zeleznickeStaniceZaListu.map { it.value[3] }[position] as String)

                                    prikazilokstanice.setOnClickListener {
                                        val xy = zeleznickeStaniceZaListu.map {it.value[2]}[position] as GeoPoint
                                        Glavna.mapa.setMapPosition(xy.latitude, xy.longitude, 80000.0)
                                        crtanjeTrase(linija, odabranoStajalisteMarker, zeleznickeStaniceZaListu.map { it.value[3] }[position] as String)

                                        dialog.dismiss()
                                    }

                                    vozoviPosleVremena = IzracunavanjeVremena().izracunavanjeVremena(
                                        listOf(autoSTGeoPoint),
                                        SQLcitac(context).prikaziTrasu(linija.title, odabranoStajalisteMarker.title).second,
                                        linija,
                                        if (voziloNaOkretnici) prviSledeciPolazak else LocalTime.now(),
                                        0
                                    )[0]

                                    vozilostizeoko.text = StringBuilder(context.resources.getString(R.string.dolazaknastajaliste) +
                                            " "+ izlaznaSt + " " + context.resources.getString(R.string.oko) +" "+ vozoviPosleVremena)
                                    pregledPolaskaVozova(
                                        zeleznickeStaniceZaListu.map { it.value[0] }[position].toString(),
                                        zeleznickeStaniceZaListu.map { it.value[1] }[position].toString(), 1)

                                    ucitavamStanice.visibility = View.GONE
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                }


                            }
                        }
                }

                fun prikaziZSDugme() {
                    if (zeleznickeStaniceZaListu.isNotEmpty()) {
                        presedanjebgvoz.visibility = View.VISIBLE

                        presedanjebgvoz.setOnClickListener {
                            asinhrono.launch {
                                asinhronoUcitajStanice()
                            }
                        }
                    }
                }

                fun proveraOkretnica() {
                    // Provera da li se vozilo nalazi na do 100 metara vazdusno od okretnice
                    if (SQLcitac(context).idStaniceuGeoPoint(sveStaniceLinije.get(0).toString()).sphericalDistance(linija.geoPoint) < 100) {
                        voziloNaOkretnici = true
                        sati = polasci.getJSONObject("rv")

                        with (dialog) {
                            prvipol = findViewById(R.id.prvipolazak)!!
                            redvoznjeTextview = findViewById(R.id.redvoznje_textview)!!
                            datumRv = findViewById(R.id.datum_rv)!!
                            sledeciPolasci = findViewById(R.id.polasci_textview)!!
                            danunedeljiTextview = findViewById(R.id.rdsn)!!
                        }

                        val view = listOf(prvipol, redvoznjeTextview, sledeciPolasci, datumRv, danunedeljiTextview)
                        for (j in view)
                            j.visibility = View.VISIBLE

                        when (danunedelji) {
                            0 -> danunedeljiTextview.text = context.resources.getString(R.string.radni_dan)
                            1 -> danunedeljiTextview.text = context.resources.getString(R.string.subota)
                            2 -> danunedeljiTextview.text = context.resources.getString(R.string.nedelja)

                            else -> {}
                        }

                        datumRv.text = prikaziDatumRV(datum)

                        for (i in 0..sati.length() - 1) {
                            val sat = sati.keys().asSequence().elementAt(i)
                            for (k in 0 until sati.getJSONArray(sat).getJSONArray(danunedelji).length()) {
                                prviSledeciPolazak = LocalTime.parse(vreme, DateTimeFormatter.ofPattern("HH:mm"))
                                prvipol.text = prviSledeciPolazak.toString()
                            }
                        }
                    }

                    linijarv.text = linija.title
                    linijarel.text = relacijaLinije
                    garBroj.text = linija.description
                    rastojanje.text = rastojanjeprer
                }

                trasaDugme.setOnClickListener {
                    crtanjeTrase(linija, odabranoStajalisteMarker, null)
                    Glavna.mapa.setMapPosition(odabranoStajalisteMarker.geoPoint.latitude, odabranoStajalisteMarker.geoPoint.longitude, 80000.0)
                    dialog.dismiss()
                }

                asinhrono.launch {
                    proveraOkretnica()
                    pretresiZS()
                    prikaziZSDugme()
                }

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
                setContentView(R.layout.redvoznje_zs)
            imeOkretnice = findViewById(R.id.ime_okretnice)!!
            samoBGVCheckBox = findViewById(R.id.samoBGVoz)!!

            with (imeOkretnice) {
                text = if (pozivodfn == 0) imestanice else null
                visibility = if (pozivodfn != 0) View.GONE else View.VISIBLE
            }

            prosirivalista = findViewById(R.id.prosirenje)!!

            if (!isShowing)
                show()

            behavior.isDraggable=false

            if (pozivodfn == 0)
                prosirivalista.layoutParams.height=WindowManager.LayoutParams.WRAP_CONTENT
        }

        fun izlistajVozove() {
            if (samoBGVBoolean) {
                val iterator = rezultat.iterator()
                while (iterator.hasNext()) {
                    val sl = iterator.next()
                    if (sl[5] == "false")
                        iterator.remove()
                }
            }
            else {
                rezultat = SQLcitac(context).preradaRVJSON(JSONObject(rv), null, null, 0,
                    if (pozivodfn == 0) LocalTime.now() else vozoviPosleVremena)
            }

            prosirivalista.setAdapter(PrikazZStanica(context, rezultat.sortedBy { it[1] }, prosirivalista))
        }

        izlistajVozove()

        samoBGVCheckBox.setOnClickListener {
            samoBGVBoolean = samoBGVCheckBox.isChecked
            izlistajVozove()
        }
    }

    fun crtanjeTrase(markerVozilo: MarkerItem, markerStanica: MarkerItem, presedackaSt : String?) {
        if ((voziloCache[0] != markerVozilo.title) or (voziloCache[1] != markerStanica.title) or (voziloCache[2] != vreme)) {
            Glavna().izbrisiTrasu()
            gpNiz.clear()
            stNiz.clear()

            val trasa = SQLcitac(context).prikaziTrasu(markerVozilo.title, markerStanica.title)

            for (element in preostaleStanice) {
                gpNiz.add(SQLcitac(context).idStaniceuGeoPoint(element))
            }

            if (gpNiz.size > 0) {
                val listLocalTime = IzracunavanjeVremena().izracunavanjeVremena(gpNiz, trasa.second, markerVozilo,
                    if (voziloNaOkretnici) prviSledeciPolazak else LocalTime.now(), 1)
                if (listLocalTime.size == preostaleStanice.size) {
                    for (b in listLocalTime.indices) {
                        stNiz.add(sifraNaziv(
                            preostaleStanice[b], SQLcitac(context).idStaniceUNaziv(
                                preostaleStanice[b]), listLocalTime[b],
                            presedackaSt == preostaleStanice[b]
                        ))
                    }
                }
            }

            for (i in 0 until trasa.second.length()) {
                Glavna.putanja.addPoints(listOf(GeoPoint(
                    trasa.second.getJSONObject(i)["lat"].toString().toDouble(),
                    trasa.second.getJSONObject(i)["lon"].toString().toDouble()
                )))
            }

            for (i in 0 until trasa.third.size) {
                val ltlg = trasa.third[i]
                val marker = MarkerItem(null, trasa.first[i].toString(),null, GeoPoint(ltlg.latitude, ltlg.longitude))
                marker.marker = MarkerSymbol(
                    AndroidBitmap(VectorDrawableCompat.create(
                        context.resources, if ((presedackaSt != null) and (presedackaSt == trasa.first[i].toString())) R.drawable.crvena_tacka else
                            R.drawable.plava_tacka, context.theme)!!.toBitmap()), MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true)
                Glavna.sveStanice.addItem(marker)
            }

            with (voziloCache) {
                this[0] = markerVozilo.title
                this[1] = markerStanica.title
                this[2] = vreme
            }
        }

        Glavna.putanja.isEnabled=true
        Glavna.sveStanice.isEnabled=true
        pozivTrasaInterface?.prikazTrase(markerVozilo.title, stNiz)
    }
}