package com.buspljus

import SpisakLinija
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.preference.PreferenceManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.buspljus.Adapteri.PretragaStanica
import com.buspljus.Adapteri.PrikazStanicaTrasa
import com.buspljus.Adapteri.sifraNaziv
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Response
import org.json.JSONObject
import org.oscim.android.MapView
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.android.theme.AssetsRenderTheme
import org.oscim.core.GeoPoint
import org.oscim.core.MapPosition
import org.oscim.event.Gesture
import org.oscim.event.GestureListener
import org.oscim.event.MotionEvent
import org.oscim.layers.Layer
import org.oscim.layers.PathLayer
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol
import org.oscim.layers.tile.bitmap.BitmapTileLayer
import org.oscim.layers.tile.buildings.BuildingLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.map.Map
import org.oscim.theme.styles.LineStyle
import org.oscim.tiling.source.mapfile.MapFileTileSource
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.time.LocalTime
import java.util.Timer
import kotlin.concurrent.schedule

class Glavna : AppCompatActivity(),ItemizedLayer.OnItemGestureListener<MarkerInterface>, Interfejs.trasa {

    companion object {
        lateinit var mapa: Map
        lateinit var adapter : PretragaStanica
        lateinit var putanja : PathLayer
        lateinit var sveStanice : ItemizedLayer
        lateinit var markeriVozila: ItemizedLayer
        lateinit var odabranoStajalisteSloj: ItemizedLayer
        private var tajmer: Timer? = null
        private var posao: Job? = null
        private var odbrojavanje15Sek = false
    }

    private lateinit var pozicijaPesakaSloj: ItemizedLayer
    private lateinit var redvoznjeProzor: ItemizedLayer

    private lateinit var pozicijaPesakaMarker: MarkerItem
    private lateinit var polje: AutoCompleteTextView
    private lateinit var podesavanje: ImageButton
    private lateinit var ucitavanje : ProgressBar
    private lateinit var lista: ListView
    private lateinit var osvezi: ImageButton
    private lateinit var pregledmape: MapView
    private lateinit var promenaunosa: Button
    private lateinit var gpsdugme: FloatingActionButton
    private lateinit var menadzerLokacije: LocationManager
    private lateinit var rvsveln : FloatingActionButton
    private lateinit var stajaliste: MarkerItem

    private var pratilacLokacije: LocationListener? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var baner : LinearLayout? = null
    private var markersimbol: MarkerSymbol? = null
    private var slobodnopomeranjemape = true
    private var pozicija = MapPosition()
    private var prviput: Boolean = true

    private var najbliziAutobusRastojanje = 100000.0
    private var najbliziAutobusMarker : MarkerItem? = null
    private var tastaturasklonjena: Boolean = true
    private var kliknalistu: Boolean = false
    private var omogucenoLociranje = false
    private var boja = 1

    private val markeriVozilaSpisak: MutableList<MarkerInterface> = ArrayList()
    private val vozilaNaMapi: MutableList<MojMarker> = mutableListOf()

    var primljeniString = ""
    var stanicaId: String = ""
    var stanicaNaziv: String = ""
    var trazenjePoBroju: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setContentView(R.layout.glavna)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AlertDialog(this).stackTrace(thread, throwable)

            finish()
        }

        Podesavanja.deljenapodesavanja = PreferenceManager.getDefaultSharedPreferences(this)
        SQLcitac.inicijalizacija(this)

        pregledmape = findViewById(R.id.mapa)
        ucitavanje = findViewById(R.id.napredak)
        polje = findViewById(R.id.polje_za_unos)
        lista = findViewById(R.id.lista)
        osvezi = findViewById(R.id.osvezi)
        podesavanje = findViewById(R.id.podesavanja)
        gpsdugme = findViewById(R.id.gps)
        promenaunosa = findViewById(R.id.promenaunosa)
        baner = findViewById(R.id.baner)
        rvsveln = findViewById(R.id.rv_sveln)

        mapa = pregledmape.map()

        prikazSacuvanihStanica()

        VoziloInfo(this).registracijaCallback(this)

        lista.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            adapter.cursor?.apply {
                moveToPosition(position)
                stanicaId = getString(getColumnIndexOrThrow("_id"))
                stanicaNaziv = getString(getColumnIndexOrThrow(SQLcitac.CIR_KOLONA))
                close()
            }
            if (!trazenjePoBroju)
                promenaunosa.callOnClick()
            kliknalistu = true
            ukucanastanica(this@Glavna, stanicaId, odabranoStajalisteSloj, stajaliste, true)
            podesiNaziv()
            prikaziListu(0)
        }

        gpsdugme.setOnClickListener {
            menadzerLokacije = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                if (omogucenoLociranje) {
                    omogucenoLociranje = false
                    gpsdugme.backgroundTintList=AppCompatResources.getColorStateList(this, R.color.siva)
                    slobodnopomeranjemape = true
                    pozicijaPesakaSloj.removeAllItems()
                    pratilacLokacije?.let { it1 -> menadzerLokacije.removeUpdates(it1) }
                }
                else {
                    if (menadzerLokacije.isProviderEnabled(GPS_PROVIDER)) {
                        omogucenoLociranje = true
                        slobodnopomeranjemape = false
                        gpsdugme.backgroundTintList=AppCompatResources.getColorStateList(this, R.color.crvena)

                        pratilacLokacije = LocationListener { location ->
                            with (gpsdugme) {
                                backgroundTintList=AppCompatResources.getColorStateList(this@Glavna,R.color.plava)
                                setOnLongClickListener {
                                    mapa.setMapPosition(location.latitude,location.longitude,70000.0)
                                    slobodnopomeranjemape = false
                                    true
                                }
                            }

                            if (!slobodnopomeranjemape)
                                mapa.setMapPosition(location.latitude,location.longitude,70000.0)

                            pozicijaPesakaMarker.geoPoint=GeoPoint(location.latitude,location.longitude)
                            pozicijaPesakaSloj.addItem(pozicijaPesakaMarker)
                            mapa.updateMap()
                        }
                        menadzerLokacije.requestLocationUpdates(GPS_PROVIDER, 5000, 0F, pratilacLokacije!!
                        )
                    }
                    else Toster(this).toster(resources.getString(R.string.ukljucigps))
                }
                mapa.updateMap()
            }
            else {
                Toster(this).toster(resources.getString(R.string.nije_omoguceno_lociranje))
                ActivityCompat.requestPermissions(this,arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION),99)
            }
        }

        podesavanje.setOnClickListener {
            startActivity(Intent(this, Podesavanja::class.java))
        }

        osvezi.setOnClickListener {
            ukucanastanica(
                this@Glavna,
                stanicaId,
                odabranoStajalisteSloj,
                stajaliste,
                false
            )
        }

        promenaunosa.setOnClickListener {
            with (polje) {
                filters = (if (trazenjePoBroju) arrayOf() else arrayOf(InputFilter.LengthFilter(4)))
                inputType = (if (trazenjePoBroju) TYPE_CLASS_TEXT else TYPE_CLASS_NUMBER)
                hint = (if (trazenjePoBroju) resources.getString(R.string.naziv_stanice) else resources.getString(R.string.broj_stanice))
                text.clear()
            }
            promenaunosa.text = (if (trazenjePoBroju) "A" else "1")
            trazenjePoBroju = (!trazenjePoBroju)
            prikaziTastaturu()
        }

        polje.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                prikaziListu(1)
                tastaturasklonjena = false
                if (polje.length() > 0) {
                    adapter.changeCursor(SQLcitac(this@Glavna).dobavisifre(s.toString(), trazenjePoBroju))
                    lista.setSelection(0)

                    if ((trazenjePoBroju) and //Trazenje po broju, a ne imenu stanice
                        (adapter.cursor.count == 1) and // U listi je ostala jedna stanica
                        (!kliknalistu) and // Na stanicu nije kliknuto na listi, vec se odabira samo kucanjem
                        (Podesavanja.deljenapodesavanja.getBoolean("automatskiunos", true)))
                    { //Podesavanje ukljuceno/iskljuceno

                        with (adapter.cursor) {
                            moveToFirst()
                            stanicaId = getString(getColumnIndexOrThrow("_id"))
                            stanicaNaziv = getString(getColumnIndexOrThrow(SQLcitac.CIR_KOLONA))
                            close()
                        }

                        ukucanastanica(this@Glavna, stanicaId, odabranoStajalisteSloj, stajaliste, true)

                        podesiNaziv()
                    }
                    kliknalistu = false
                } else
                    prikaziListu(0)
            }

            override fun afterTextChanged(s: Editable?) {
                return
            }
        }
        )

        fun prikazListe() {
            if ((polje.length() == 0)) {
                prikazSacuvanihStanica()
                if (lista.adapter.count > 0)
                    prikaziListu(1)
            }
        }

        fun fokusKlik() {
            prikaziTastaturu()
            prikazListe()
            sakrijTrasu()
        }

        rvsveln.setOnClickListener {
            SpisakLinija(this@Glavna).nacrtajDugmad()
        }

        polje.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                fokusKlik()
            }
        }

        polje.setOnClickListener {
            fokusKlik()
        }
        otvoriMapu()
    }

    fun ukucanastanica(context: Context, stanica: String, stajalisteSloj: ItemizedLayer, stajaliste: MarkerItem, animacija: Boolean) {
        try {
            sklonitastaturu()

            reset()

            prviput = true

            if (tajmer != null)
                stopTajmera()

            with (stajaliste) {
                title = stanica
                geoPoint = SQLcitac(this@Glavna).idStaniceuGeoPoint(stanica)
                marker = (MarkerSymbol(
                    AndroidBitmap(TekstUBitmap().getBitmapFromTitle(stajaliste.getTitle(), this@Glavna, 0)),
                    MarkerSymbol.HotspotPlace.NONE, true
                ))
            }

            tajmer = Timer()

            if (ProveraDostupnostiInterneta(this@Glavna)) {
                zahtevZaPozicijuVozila()
                dugmezaosvezavanje(1, 0)
            }
            else { // Nema interneta
                if (Podesavanja.deljenapodesavanja.getBoolean("prikazporv", false)) {
                    Toster(this).toster(resources.getString(R.string.offline_rezim))
                    tajmer?.schedule(0, 4000) {
                        izbrisiSveMarkere()
                        crtanjeSpecMarkera()
                        markeriVozila.addItems(vozilaNaMapi as Collection<MarkerInterface>)
                        mapa.updateMap()
                    }
                }
                else {
                    Toster(this@Glavna).toster(resources.getString(R.string.nema_interneta))
                }
                dugmezaosvezavanje(0, 1)
            }

            if (animacija) {
                mapa.mapPosition = MapPosition(stajaliste.geoPoint.latitude, stajaliste.geoPoint.longitude, 2000.0)
                if (stajalisteSloj.size() == 0) {
                    mapa.animator().animateZoom(2000, 22.0, 0F, 0F)
                }
            }
            if (stajalisteSloj.size() != 0)
                stajalisteSloj.removeItem(0)

            stajalisteSloj.addItem(stajaliste)

            mapa.updateMap()
        } catch (e: Exception) {
            Toster(context).toster(e.toString())
            Log.d(resources.getString(R.string.debug), "" + e)
        }
    }

    private fun otvoriMapu() {
        try {
            class Kliknamapu : Layer(mapa), GestureListener {
                override fun onGesture(g: Gesture, e: MotionEvent): Boolean {
                    if (g == Gesture.TAP) {
                        val p = mMap.viewport().fromScreenPoint(e.x, e.y)
                        SQLcitac(this@Glavna).pretragabaze_kliknamapu(p.latitude.toString(),p.longitude.toString(), object: Interfejs.Callback {
                            override fun korak(s: String) {
                                stanicaId = s
                                stanicaNaziv = ""
                                podesiNaziv()
                                ukucanastanica(this@Glavna, s, odabranoStajalisteSloj, stajaliste, false)
                                sakrijTrasu()
                            }

                            override fun koloneBGVOZ(lista: List<Any>) {
                            }
                        },0)
                    }
                    return false
                }
            }

            val tileSource = MapFileTileSource()
            val fis = contentResolver.openInputStream(Uri.fromFile((File(filesDir, "beograd.map")))) as FileInputStream?

            tileSource.setMapFileInputStream(fis)

            val tileLayer = mapa.setBaseMap(tileSource)

            markeriVozila = ItemizedLayer(mapa, markeriVozilaSpisak, markersimbol, this)
            redvoznjeProzor = ItemizedLayer(mapa, markersimbol)

            odabranoStajalisteSloj = ItemizedLayer(mapa, markersimbol)
            stajaliste = MarkerItem(null, null, null, null)
            pozicijaPesakaSloj = ItemizedLayer(mapa,markersimbol)
            pozicijaPesakaMarker = MarkerItem(null,null,null,null)
            sveStanice = ItemizedLayer(mapa, markersimbol)

            pozicijaPesakaMarker.marker=MarkerSymbol(
                AndroidBitmap(VectorDrawableCompat.create(
                    resources,R.drawable.glisa,theme)!!.toBitmap()),
                MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true)

            putanja = PathLayer(mapa,LineStyle(resources.getColor(R.color.crvena),5.0F))

            with (mapa.layers()) {
                val listaSlojeva = listOf(
                    BitmapTileLayer(mapa, tileSource),
                    BuildingLayer(mapa, tileLayer),
                    LabelLayer(mapa, tileLayer),Kliknamapu(),
                    putanja,
                    sveStanice,
                    markeriVozila,
                    odabranoStajalisteSloj,
                    redvoznjeProzor,
                    pozicijaPesakaSloj
                )
                for (d in listaSlojeva)
                    add(d)
            }

            mapa.events.bind(Map.UpdateListener { e, mapPosition ->

                pozicija = mapPosition

                if (e == Map.MOVE_EVENT) {
                    sklonitastaturu()
                    prikaziListu(0)
                    slobodnopomeranjemape = true
                    polje.clearFocus()
                }
            })

            mapa.setTheme(AssetsRenderTheme(assets, "", "osmarender.xml"))
            mapa.setMapPosition(44.821, 20.471, 2500.0)

            if (adapter.cursor.count > 0)
                prikaziListu(1)

            prikaziTastaturu()

        } catch (e: Exception) {
            Log.d(resources.getString(R.string.debug), "" + e)
        }
    }

    fun crtanjeSpecMarkera() {
        SQLcitac(this).ubacivanjeTestMarkera(stajaliste.title, object: Interfejs.specMarker {
            override fun crtanjespecMarkera(id: String, smer: String, g: List<Pair<GeoPoint, LocalTime>>) {
                val listaPolazaka = mutableListOf<Pair<String,String>>()

                fun specMarker(marker: Int) {
                    if ((g[marker].first.sphericalDistance(stajaliste.geoPoint) < 2500) or (vozilaNaMapi.size < 3)) {
                        val specMarker = MojMarker(id, smer, "/", null, g[marker].second.toString(), g[marker].first)
                        specMarker.marker = (MarkerSymbol(
                            AndroidBitmap(TekstUBitmap().getBitmapFromTitle(
                                specMarker.title, this@Glavna, 4)),
                            MarkerSymbol.HotspotPlace.BOTTOM_CENTER, true
                        ))
                        vozilaNaMapi.add(specMarker)
                    }
                }

                if (ProveraDostupnostiInterneta(this@Glavna)) {
                    for (marker in 0 until vozilaNaMapi.size) {
                        with (vozilaNaMapi[marker]) {
                            listaPolazaka.add(Pair(brojLinije, vremePolaska))
                        }
                    }

                    for (marker in g.indices) {
                        if (!listaPolazaka.contains(Pair(id, g[marker].second.toString()))) {
                            specMarker(marker)
                        }
                    }
                }
                else {
                    for (marker in g.indices) {
                        specMarker(marker)
                    }
                }
            }
        })
    }

    fun izbrisiSveMarkere() {
        vozilaNaMapi.clear()
        markeriVozila.removeAllItems()
    }

    fun crtanjemarkera(odgovor: String) {
        try {
            najbliziAutobusRastojanje = 100000.0
            val json = JSONObject(odgovor)
            val linije = json.keys()

            izbrisiSveMarkere()

            while (linije.hasNext()) {
                val linija = linije.next()
                val maximumI = json.getJSONArray(linija).length()
                for (i in 0 until maximumI) {
                    val jsonObject = json.getJSONArray(linija).getJSONObject(i)
                    val vozilo = MojMarker(
                        linija.replace(" ",""),
                        null,
                        jsonObject.getString("g"),
                        null,
                        jsonObject.getString("p"),
                        GeoPoint((jsonObject.getDouble("lt")), jsonObject.getDouble("lg")
                        ))

                    if ((vozilo.garazniBOriginal.startsWith("P9")) or (vozilo.garazniBOriginal.startsWith("P8"))) {
                        if (vozilo.garazniBOriginal.startsWith("P9"))
                            boja = 1
                        if (vozilo.garazniBOriginal.startsWith("P8")) {
                            boja =
                                if (vozilo.garazniBOriginal[2] == '0' || vozilo.garazniBOriginal[2] == '1')
                                    2 //tramvaj
                                else 3 //trolejbus

                            if (vozilo.garazniBOriginal[2] == '0')
                                vozilo.garazniBMenjan = vozilo.garazniBOriginal.replaceFirst("0","2")
                        }
                        if (vozilo.garazniBOriginal[2] == '0')
                            vozilo.garazniBMenjan = vozilo.garazniBOriginal.drop(3)
                        else vozilo.garazniBMenjan = vozilo.garazniBOriginal.drop(2)
                    } else boja = 1

                    vozilo.marker = (MarkerSymbol(
                        AndroidBitmap(TekstUBitmap().getBitmapFromTitle(
                            when (Podesavanja.deljenapodesavanja.getBoolean("prikazgb", false)) {
                                true -> vozilo.brojLinije + " (" + if (vozilo.garazniBMenjan == null) vozilo.garazniBOriginal else vozilo.garazniBMenjan + ")"
                                false -> vozilo.brojLinije
                            }, this, boja)),
                        MarkerSymbol.HotspotPlace.BOTTOM_CENTER, true
                    ))

                    vozilaNaMapi.add(vozilo)

                    if ((stajaliste.geoPoint.sphericalDistance(GeoPoint(vozilo.polozajVozila.latitude, vozilo.polozajVozila.longitude)) < najbliziAutobusRastojanje)
                        and ((stajaliste.geoPoint.sphericalDistance(GeoPoint(vozilo.polozajVozila.latitude, vozilo.polozajVozila.longitude)) > 100))) {

                        najbliziAutobusRastojanje =
                            stajaliste.geoPoint.sphericalDistance(
                                GeoPoint(
                                    vozilo.polozajVozila.latitude,
                                    vozilo.polozajVozila.longitude
                                )
                            )
                        najbliziAutobusMarker = MarkerItem(
                            null,
                            null,
                            GeoPoint(vozilo.polozajVozila.latitude, vozilo.polozajVozila.longitude)
                        )
                    }
                }
            }

            dugmezaosvezavanje(0, 0)
            if ((prviput) and (najbliziAutobusMarker?.geoPoint != null)) {
                odzumirajMapu()
                prviput = false
            }

            tajmer?.schedule(15000) { zahtevZaPozicijuVozila() }

            animacijaUcitavanja()

        } catch (e: Exception) {
            with(Toster(this@Glavna)) {
                if (odgovor == "0") {
                    this.toster(resources.getString(R.string.nema_vozila))
                    izbrisiSveMarkere()
                }
                else {
                    AlertDialog(this@Glavna).prikaziGresku(e)
                }
            }
            dugmezaosvezavanje(0, 1)
        }
        finally {
            runOnUiThread {
                if (Podesavanja.deljenapodesavanja.getBoolean("prikazporv", false)) {
                    crtanjeSpecMarkera()
                }
                markeriVozila.addItems(vozilaNaMapi as Collection<MarkerInterface>?)
                mapa.updateMap()
            }
        }
    }

    fun animacijaUcitavanja() {
        runOnUiThread {
            odbrojavanje15Sek = true
            with (ucitavanje) {
                visibility = View.VISIBLE
                isIndeterminate = false
                progress = 100
            }
            posao = scope.launch {
                while (odbrojavanje15Sek) {
                    delay(150)
                    ucitavanje.progress -= 1
                }
            }
        }
    }

    override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
        val markerItem = item as MojMarker
        VoziloInfo(this@Glavna).redvoznjeKliknaVozilo(markerItem, stajaliste)
        return true
    }

    override fun onItemLongPress(index: Int, item: MarkerInterface?): Boolean {
        return true
    }

    private fun odzumirajMapu() {
        if (najbliziAutobusRastojanje != 100000.0) {
            runOnUiThread {
                mapa.setMapPosition(
                    (stajaliste.geoPoint.latitude + najbliziAutobusMarker?.geoPoint?.latitude!!) / 2,
                    (stajaliste.geoPoint.longitude + najbliziAutobusMarker?.geoPoint?.longitude!!) / 2,
                    1 / najbliziAutobusRastojanje * 25000000
                )
            }
        }
    }

    private fun sklonitastaturu() {
        if (!tastaturasklonjena) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                currentFocus?.windowToken,
                0
            )
            tastaturasklonjena = true
        }
    }

    private fun prikaziTastaturu() {
        polje.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        tastaturasklonjena = false
    }

    fun dugmezaosvezavanje(animiranaTraka: Int, taster: Int) {
        runOnUiThread {
            osvezi.visibility = (if (taster == 0) View.INVISIBLE else View.VISIBLE)

            with (ucitavanje) {
                visibility = (if (animiranaTraka == 0) View.INVISIBLE else View.VISIBLE)
                isIndeterminate = true
            }

            odbrojavanje15Sek = false
        }
    }

    private fun zahtevZaPozicijuVozila() {
        dugmezaosvezavanje(1, 0)
        Internet().zahtevPremaInternetu(stanicaId, null, 1, object : Interfejs.odgovorSaInterneta {
            override fun uspesanOdgovor(response: Response) {
                if (response.isSuccessful) {
                    primljeniString = response.body!!.string()
                    crtanjemarkera(primljeniString)
                }
            }

            override fun neuspesanOdgovor(e: IOException) {
                if (Internet.zahtev?.isCanceled() == false)
                    Toster(this@Glavna).toster(resources.getString(R.string.greska_sa_vezom))
                dugmezaosvezavanje(0, 1)
            }
        })
    }

    fun podesiNaziv() {
        with (polje) {
            hint=(if (stanicaNaziv.isEmpty()) stanicaId else StringBuilder("$stanicaId - $stanicaNaziv"))
            text.clear()
            clearFocus()
        }
    }

    private fun stopTajmera() {
        Internet.zahtev?.cancel()

        odbrojavanje15Sek = false

        with (tajmer) {
            this?.cancel()
            this?.purge()
            tajmer = null
        }
    }

    private fun prikaziListu(prikaz: Int) {
        runOnUiThread {
            lista.visibility = (if (prikaz == 0) View.GONE else View.VISIBLE)
            gpsdugme.visibility = (if (prikaz == 0) View.VISIBLE else View.GONE)
            rvsveln.visibility = (if (prikaz == 0) View.VISIBLE else View.GONE)
        }
    }

    private fun prikazSacuvanihStanica() {
        adapter = PretragaStanica(this, SQLcitac(this).SQLzahtev("stanice", arrayOf("_id","naziv_cir","staju","sacuvana"),"sacuvana = ?", arrayOf("1"),null))
        lista.adapter = adapter
    }

    override fun prikazTrase(linijarv: String, listasifraNaziv: MutableList<sifraNaziv>) {
        val linija = findViewById<TextView>(R.id.ln)!!
        val lista = findViewById<ListView>(R.id.ls)!!
        val zatvori = findViewById<ImageButton>(R.id.zatvori)!!
        linija.text = linijarv
        lista.adapter = PrikazStanicaTrasa(this, listasifraNaziv)

        baner?.visibility = View.VISIBLE

        zatvori.setOnClickListener {
            sakrijTrasu()
        }
    }

    fun sakrijTrasu() {
        putanja.isEnabled=false
        sveStanice.isEnabled=false
        ukloniBaner()
    }

    fun izbrisiTrasu() {
        putanja.clearPath()
        sveStanice.removeAllItems()
    }

    fun ukloniBaner() {
        baner?.visibility = View.GONE
        mapa.updateMap()
    }

    fun reset() {
        sveStanice.removeAllItems()
        markeriVozila.removeAllItems()
        odabranoStajalisteSloj.removeAllItems()
        putanja.clearPath()

        VoziloInfo.voziloCache = mutableListOf("0","0")
        stopTajmera()

        ukloniBaner()
    }

    override fun onStop() {
        stopTajmera()
        super.onStop()
    }

    override fun onResume() {
        if (stanicaId.isNotEmpty())
            dugmezaosvezavanje(0, 1)
        if (((System.currentTimeMillis().div(1000)) - (Podesavanja.deljenapodesavanja.getLong("zatvoren", 0)) > 300)
            or (markeriVozila.itemList.size > 10)) {
            reset()
        }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Podesavanja.deljenapodesavanja.edit().putLong("zatvoren", System.currentTimeMillis().div(1000)).apply()
        pratilacLokacije?.let { it1 -> menadzerLokacije.removeUpdates(it1) }
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.cursor.close()
    }
}