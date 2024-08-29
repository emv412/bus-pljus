package com.buspljus

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
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
import com.buspljus.Adapteri.SpisakLinijaAdapter
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

class VoziloInfo(private val context: Context) {

    companion object {
        val danunedelji = when (LocalDate.now().dayOfWeek.value) {
            in 1 .. 5 -> 0
            6 -> 1
            7 -> 2
            else -> 0
        }
        private var pozivTrasaInterface: Interfejs.trasa? = null

        var preostaleStanice = mutableListOf<List<Any>>()

        var voziloCache = mutableListOf("0","0")
        lateinit var trasaLinije : Triple<JSONArray, JSONArray, MutableList<GeoPoint>>

        val zeleznickeStaniceZaListu = mutableMapOf<String,List<Any>>()
        val stNiz = mutableListOf<sifraNaziv>()
        val gpNiz = mutableListOf<GeoPoint>()

    }

    private lateinit var sati : JSONObject

    private var izvrsiUI = Handler(Looper.getMainLooper())
    private var voziloNaOkretnici = false
    private var samoBGVBoolean = false
    private var rezultat = mutableListOf(listOf<String>())

    private lateinit var vreme : String
    private lateinit var vozoviPosleVremena : LocalTime
    private lateinit var prviSledeciPolazak : LocalTime
    private lateinit var polazakU : TextView
    private lateinit var polasci_textview : TextView
    private lateinit var linijarv : TextView
    private lateinit var linijarel : TextView
    private lateinit var garBroj : TextView
    private lateinit var rastojanje : TextView
    private lateinit var nemaPolazaka : TextView
    private lateinit var vozilostizeoko : TextView
    private lateinit var krenuo_u_textview : TextView
    private lateinit var krenuo_u_vreme : TextView
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
        for (brojac in 0 until sveStaniceLinije.length()) {
            if (sveStaniceLinije[brojac] == odabranoStajalisteMarker.title) {
                for (novib in brojac until sveStaniceLinije.length()) {
                    preostaleStanice.add(listOf(sveStaniceLinije[novib].toString(), SQLcitac(context).idStaniceuGeoPoint(sveStaniceLinije[novib].toString())))
                }
                break
            }
        }
    }

    fun proveriPolazak(): LocalTime {
        var lokalVreme = LocalTime.now()
        if (voziloNaOkretnici) {
            if (prviSledeciPolazak.isAfter(LocalTime.now()))
                lokalVreme = prviSledeciPolazak
        }
        return lokalVreme
    }

    fun prikaziDatumRV(datum: JSONArray): String {
        with(datum) {
            return this.getString(0) + ". " + this.getString(1) + ". " + this.getString(2) + "."
        }
    }

    fun redvoznjeKliknaVozilo(linija: MojMarker, odabranoStajalisteMarker: MarkerItem) : Boolean {
        val asinhrono = CoroutineScope(Dispatchers.Main)
        vreme = linija.vremePolaska
        val rastojanjedostanice = linija.polozajVozila.sphericalDistance(
            GeoPoint(
                odabranoStajalisteMarker.geoPoint.latitude,
                odabranoStajalisteMarker.geoPoint.longitude
            )
        ).div(1000)

        val rastojanjeprer = zaokruzivanje(rastojanjedostanice,1).toString() + " km"

        fun obradilistu(dobijenaLista: List<String>) {
            try {
                if (dobijenaLista.isNotEmpty()) {
                    (context as Activity).runOnUiThread {
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
                            krenuo_u_textview = findViewById(R.id.krenuo_u)!!
                            krenuo_u_vreme = findViewById(R.id.krenuo_u_vreme)!!

                            ucitavanjePresedanja.visibility = View.VISIBLE

                            show()
                        }

                        val pol = dobijenaLista[0]
                        val odr = dobijenaLista[1]

                        val sveStaniceLinije = JSONArray(dobijenaLista[2])
                        val polasci = JSONObject(dobijenaLista[3])

                        val relacijaLinije = "$pol - $odr"

                        var staroRastojanje = 1000.0
                        var novoRastojanje: Double

                        fun pretresiZS() {
                            try {
                                if ((voziloCache[0] != linija.brojLinije) or (voziloCache[1] != odabranoStajalisteMarker.title)) {
                                    zeleznickeStaniceZaListu.clear()
                                    staniceDoKrajaTrase(sveStaniceLinije, odabranoStajalisteMarker)

                                    for (brojac in 1 until preostaleStanice.size) {
                                        jednaKoordinata = preostaleStanice[brojac][1] as GeoPoint
                                        SQLcitac(context).pretragabaze_kliknamapu(jednaKoordinata.latitude.toString(),jednaKoordinata.longitude.toString(), object: Interfejs.Callback {
                                            override fun korak(s: String) {
                                            }

                                            override fun koloneBGVOZ(lista: List<Any>) {
                                                novoRastojanje = lista[3] as Double
                                                if (novoRastojanje < staroRastojanje) {
                                                    zeleznickeStaniceZaListu[lista[0] as String] = listOf(lista[1], lista[2], jednaKoordinata, preostaleStanice[brojac][0])
                                                    staroRastojanje = lista[3] as Double
                                                }
                                            }
                                        },1)
                                    }
                                }
                                izvrsiUI.post { ucitavanjePresedanja.visibility = View.GONE }
                            }
                            catch (e: Exception) {
                                Toster(context).toster(e.toString())
                            }

                        }

                        fun ucitajStanice() {
                            prosirenaSekcija = dialog.findViewById(R.id.prosirena_sekcija)!!
                            if (prosirenaSekcija.visibility == View.VISIBLE) {
                                izvrsiUI.post { prosirenaSekcija.visibility = View.GONE }
                            } else {
                                with (dialog) {
                                    stanicepadajucalista = findViewById(R.id.stanicepadajucalista)!!
                                    izvrsiUI.post { prosirenaSekcija.visibility = View.VISIBLE }
                                }

                                val adapter = ArrayAdapter(context, R.layout.spinneritem, zeleznickeStaniceZaListu.map { it.value[0] } )
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                stanicepadajucalista.adapter = adapter

                                stanicepadajucalista.onItemSelectedListener =
                                    object : AdapterView.OnItemSelectedListener {
                                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                            izvrsiUI.post { ucitavamStanice.visibility = View.VISIBLE }

                                            val autoSTGeoPoint = zeleznickeStaniceZaListu.map { it.value[2] }[position] as GeoPoint
                                            val izlaznaSt = SQLcitac(context).idStaniceUNaziv(zeleznickeStaniceZaListu.map { it.value[3] }[position] as String)

                                            prikazilokstanice.setOnClickListener {
                                                try {
                                                    val xy = zeleznickeStaniceZaListu.map {it.value[2]}[position] as GeoPoint
                                                    Glavna.mapa.setMapPosition(xy.latitude, xy.longitude, 80000.0)
                                                    crtanjeTrase(linija, odabranoStajalisteMarker, zeleznickeStaniceZaListu.map { it.value[3] }[position] as String, proveriPolazak(), false)

                                                    dialog.dismiss()
                                                }
                                                catch (g: Exception) {
                                                    AlertDialog(context).prikaziGresku(g)
                                                }

                                            }

                                            try {
                                                vozoviPosleVremena = IzracunavanjeVremena().izracunavanjeVremena(
                                                    listOf(autoSTGeoPoint),
                                                    SQLcitac(context).prikaziTrasu(linija.brojLinije, odabranoStajalisteMarker.title, null).second,
                                                    linija,
                                                    proveriPolazak()
                                                )[0]
                                            }
                                            catch (e: Exception) {
                                                AlertDialog(context).prikaziGresku(e)
                                            }

                                            if (::vozoviPosleVremena.isInitialized) {
                                                vozilostizeoko.text = StringBuilder(context.resources.getString(R.string.dolazaknastajaliste) +
                                                        " "+ izlaznaSt + " " + context.resources.getString(R.string.oko) +" "+ vozoviPosleVremena)
                                                pregledPolaskaVozova(
                                                    zeleznickeStaniceZaListu.map { it.value[0] }[position].toString(),
                                                    zeleznickeStaniceZaListu.map { it.value[1] }[position].toString(), 1)

                                            }

                                            izvrsiUI.post { ucitavamStanice.visibility = View.GONE }
                                        }

                                        override fun onNothingSelected(parent: AdapterView<*>?) {
                                        }

                                    }
                            }
                        }

                        fun prikaziZSDugme() {
                            if (zeleznickeStaniceZaListu.isNotEmpty()) {
                                izvrsiUI.post { presedanjebgvoz.visibility = View.VISIBLE }

                                presedanjebgvoz.setOnClickListener {
                                    ucitajStanice()
                                }
                            }
                        }

                        fun proveraOkretnica() {
                            // Provera da li se vozilo nalazi na do 100 metara vazdusno od okretnice
                            if (SQLcitac(context).idStaniceuGeoPoint(sveStaniceLinije.get(0).toString()).sphericalDistance(linija.polozajVozila) < 100) {
                                voziloNaOkretnici = true
                                sati = polasci.getJSONObject("rv")

                                with (dialog) {
                                    polazakU = findViewById(R.id.prvipolazak)!!
                                    polasci_textview = findViewById(R.id.polasci_textview)!!
                                }

                                val view = listOf(polazakU, polasci_textview)
                                for (j in view)
                                    izvrsiUI.post { j.visibility = View.VISIBLE }

                                prviSledeciPolazak = LocalTime.parse(vreme, DateTimeFormatter.ofPattern("HH:mm"))
                                izvrsiUI.post { polazakU.text = prviSledeciPolazak.toString() }
                            }
                            else {
                                izvrsiUI.post {
                                    krenuo_u_textview.visibility = View.VISIBLE
                                    krenuo_u_vreme.visibility = View.VISIBLE
                                    krenuo_u_vreme.text = linija.vremePolaska
                                }
                            }

                            izvrsiUI.post {
                                linijarv.text = linija.brojLinije
                                linijarel.text = relacijaLinije
                                garBroj.text = if (linija.garazniBMenjan == null) linija.garazniBOriginal else linija.garazniBMenjan
                                rastojanje.text = rastojanjeprer
                            }
                        }

                        trasaDugme.setOnClickListener {
                            try {
                                crtanjeTrase(linija, odabranoStajalisteMarker, null, proveriPolazak(), false)
                                dialog.dismiss()
                            }
                            catch (g: Exception) {
                                izvrsiUI.post { AlertDialog(context).prikaziGresku(g) }
                            }

                        }

                        asinhrono.launch {
                            try {
                                proveraOkretnica()
                                pretresiZS()
                                prikaziZSDugme()
                            }
                            catch (g: Exception) {
                                izvrsiUI.post { AlertDialog(context).prikaziGresku(g) }
                            }
                        }
                    }

                }
            } catch (e:Exception) {
                izvrsiUI.post { AlertDialog(context).prikaziGresku(e) }
            }
        }

        SQLcitac(context).kliknavozilo(linija.title, odabranoStajalisteMarker.title, linija.description, object: Interfejs.vracenaLista {
            override fun vratiListu(lista: List<String>) {
                obradilistu(lista)
            }

        })

        return true
    }

    fun pregledPolaskaVozova(imestanice: String, rv: String, pozivodfn: Int) {
        with (dialog) {
            behavior.state=BottomSheetBehavior.STATE_EXPANDED
            if (pozivodfn == 0)
                setContentView(R.layout.prozor_bgvoz)

            imeOkretnice = findViewById(R.id.ime_okretnice)!!
            samoBGVCheckBox = findViewById(R.id.samoBGVoz)!!
            nemaPolazaka = findViewById(R.id.nemapolazaka)!!

            with (imeOkretnice) {
                text = if (pozivodfn == 0) imestanice else null
                visibility = if (pozivodfn != 0) View.GONE else View.VISIBLE
            }

            prosirivalista = findViewById(R.id.prosirenje)!!

            if (!isShowing)
                show()

            behavior.isDraggable = false

            if (pozivodfn == 0)
                prosirivalista.layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
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
            if (rezultat.size == 0) {
                nemaPolazaka.visibility = View.VISIBLE
            }
            else
                nemaPolazaka.visibility = View.GONE
        }

        izlistajVozove()

        samoBGVCheckBox.setOnClickListener {
            samoBGVBoolean = samoBGVCheckBox.isChecked
            izlistajVozove()
        }
    }

    fun crtanjeTrase(markerVozilo: MarkerItem, markerStanica: MarkerItem, presedackaSt : String?, polazak: LocalTime, sveStanice: Boolean) {
        stNiz.clear()
        if ((voziloCache[0] != markerVozilo.title) or (voziloCache[1] != markerStanica.title) or (sveStanice)) {
            Glavna().izbrisiTrasu()
            gpNiz.clear()

            trasaLinije = SQLcitac(context).prikaziTrasu(markerVozilo.title, markerStanica.title, if (sveStanice) SpisakLinijaAdapter.odabranaLinija[2].toString() else null)

            fun dodajPreostaleStanice() {
                for (b in 0 until preostaleStanice.size) {
                    gpNiz.add(preostaleStanice[b][1] as GeoPoint)
                }
            }

            for (i in 0 until trasaLinije.second.length()) {
                Glavna.putanja.addPoints(listOf(GeoPoint(
                    trasaLinije.second.getJSONObject(i)["lat"].toString().toDouble(),
                    trasaLinije.second.getJSONObject(i)["lon"].toString().toDouble()
                )))
            }

            if (sveStanice) {
                markerVozilo.geoPoint = Glavna.putanja.points[0]
                staniceDoKrajaTrase(SpisakLinijaAdapter.odabranaLinija[1] as JSONArray, markerStanica)
            }

            dodajPreostaleStanice()

            for (i in 0 until trasaLinije.third.size) {
                val ltlg = trasaLinije.third[i]
                val marker = MarkerItem(null, trasaLinije.first[i].toString(),null, GeoPoint(ltlg.latitude, ltlg.longitude))
                marker.marker = MarkerSymbol(
                    AndroidBitmap(VectorDrawableCompat.create(
                        context.resources, if ((presedackaSt != null) and (presedackaSt == trasaLinije.first[i].toString())) R.drawable.crvena_tacka else
                            R.drawable.plava_tacka, context.theme)!!.toBitmap()), MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true)
                Glavna.sveStanice.addItem(marker)
            }

            with (voziloCache) {
                this[0] = markerVozilo.title
                this[1] = markerStanica.title
            }

        }

        if (gpNiz.size > 0) {
            val listLocalTime = IzracunavanjeVremena().izracunavanjeVremena(gpNiz, trasaLinije.second, markerVozilo, polazak)

            for (b in 0 until preostaleStanice.size) {
                stNiz.add(sifraNaziv(
                    preostaleStanice[b][0] as String, SQLcitac(context).idStaniceUNaziv(
                        preostaleStanice[b][0] as String), if (listLocalTime.size == preostaleStanice.size) listLocalTime[b] else null, gpNiz[b]
                ))
            }
        }

        Glavna.putanja.isEnabled = true
        Glavna.sveStanice.isEnabled = true
        pozivTrasaInterface?.prikazTrase(markerVozilo.title, stNiz)

    }
}