package com.buspljus

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
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.buspljus.Adapteri.PretragaStanica
import com.buspljus.Adapteri.StaniceBottomSheetDialog
import com.buspljus.Baza.DatabaseManager
import com.buspljus.Baza.InterakcijaMapa
import com.buspljus.Baza.Linije
import com.buspljus.Baza.PosrednikBaze
import com.buspljus.Baza.Stanice
import com.buspljus.Trasa.ObradiTrasu
import com.buspljus.Trasa.TrasaRender
import com.buspljus.grafika.Podesavanja
import com.buspljus.grafika.PopupProzor
import com.buspljus.grafika.SpisakLinija
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class Glavna : AppCompatActivity(),ItemizedLayer.OnItemGestureListener<MarkerInterface>, Interfejs.novaTrasa {

    companion object {
        lateinit var mapa: Map
        lateinit var adapter : PretragaStanica
        lateinit var putanja : PathLayer
        lateinit var sveStanice : ItemizedLayer
        lateinit var markeriVozila: ItemizedLayer
        lateinit var odabranoStajalisteSloj: ItemizedLayer
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
    private lateinit var redVoznjeSveLinije : FloatingActionButton
    private lateinit var stajaliste: MarkerItem

    // prikaz trase
    private lateinit var linijaPrikazTrase : TextView
    private lateinit var listaPrikazTrase : ListView
    private lateinit var zatvoriPrikazTrase : ImageButton

    private var pratilacLokacije: LocationListener? = null

    private var baner : LinearLayout? = null
    private var markersimbol: MarkerSymbol? = null
    private var slobodnopomeranjemape = true
    private var pozicija = MapPosition()
    private var prviput: Boolean = true
    private var listaPrikazana = false
    private var timersParentJob = SupervisorJob()
    private var sviTajmeri = CoroutineScope(Dispatchers.Main + timersParentJob)
    private var crtanjeJob: Job? = null
    private var tajmeriPokrenuti = false
    private var animacijaJob: Job? = null

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
            PopupProzor(this).stackTrace(thread, throwable)

            finish()
        }

        Podesavanja.deljenapodesavanja = PreferenceManager.getDefaultSharedPreferences(this)
        DatabaseManager.getInstance(this)

        pregledmape = findViewById(R.id.mapa)
        ucitavanje = findViewById(R.id.napredak)
        polje = findViewById(R.id.polje_za_unos)
        lista = findViewById(R.id.lista)
        osvezi = findViewById(R.id.osvezi)
        podesavanje = findViewById(R.id.podesavanja)
        gpsdugme = findViewById(R.id.gps)
        promenaunosa = findViewById(R.id.promenaunosa)
        baner = findViewById(R.id.baner)
        redVoznjeSveLinije = findViewById(R.id.rv_sveln)
        linijaPrikazTrase = findViewById(R.id.ln)
        listaPrikazTrase = findViewById(R.id.ls)
        zatvoriPrikazTrase = findViewById(R.id.zatvori)

        mapa = pregledmape.map()

        LinijePoStanici.initialize(this)
        prikazSacuvanihStanica()

        lista.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            adapter.cursor?.apply {
                moveToPosition(position)
                stanicaId = getString(getColumnIndexOrThrow("_id"))
                stanicaNaziv = getString(getColumnIndexOrThrow(PosrednikBaze.CIR_KOLONA))
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
                    val noviKursor = PosrednikBaze(this@Glavna).dobavisifre(s.toString(), trazenjePoBroju)

                    val oldCursor = adapter.swapCursor(noviKursor)
                    oldCursor?.close()
                    
                    lista.setSelection(0)

                    if (trazenjePoBroju &&
                        adapter.cursor.count == 1 &&
                        !kliknalistu &&
                        Podesavanja.deljenapodesavanja.getBoolean("automatskiunos", true)
                    ) {
                        val c = adapter.cursor
                        if (c.moveToFirst()) {
                            stanicaId = c.getString(c.getColumnIndexOrThrow("_id"))
                            stanicaNaziv = c.getString(c.getColumnIndexOrThrow(PosrednikBaze.CIR_KOLONA))
                        }

                        ukucanastanica(this@Glavna, stanicaId, odabranoStajalisteSloj, stajaliste, true)
                        podesiNaziv()
                        prikaziListu(0)
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

        redVoznjeSveLinije.setOnClickListener {
            SpisakLinija(this) { vozilo, stanica, presedacka, time ->
                reset()
                pokreniCrtanjeTrase(vozilo, stanica, presedacka, time)
            }.nacrtajDugmad()
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
            Internet.zahtev?.cancel()
            sklonitastaturu()
            reset()

            prviput = true

            with (stajaliste) {
                title = stanica
                geoPoint = PosrednikBaze(this@Glavna).idStaniceuGeoPoint(stanica)
                marker = (MarkerSymbol(
                    AndroidBitmap(TekstUBitmap().getBitmapFromTitle(stajaliste.getTitle(), this@Glavna, 0)),
                    MarkerSymbol.HotspotPlace.NONE, true
                ))
            }

            if (ProveraDostupnostiInterneta(this@Glavna)) {
                startCrtanjeMarkera()
            }
            else { // Nema interneta
                if (Podesavanja.deljenapodesavanja.getBoolean("prikazporv", false)) {
                    Toster(this).toster(resources.getString(R.string.offline_rezim))
                    startCrtanjeSpecMarkera()
                }
                else {
                    Toster(this@Glavna).toster(resources.getString(R.string.nema_interneta))
                }
                lifecycleScope.launch {
                    dugmezaosvezavanje(0, 1)
                }
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
            PopupProzor(context).prikaziGresku(e)
        }
    }

    private fun otvoriMapu() {
        try {
            class Kliknamapu : Layer(mapa), GestureListener {
                override fun onGesture(g: Gesture, e: MotionEvent): Boolean {
                    if (g == Gesture.TAP) {
                        val p = mMap.viewport().fromScreenPoint(e.x, e.y)
                        InterakcijaMapa(this@Glavna).pretragabaze_kliknamapu(p.latitude.toString(),p.longitude.toString(), object: Interfejs.MapTapListener {
                            override fun podesiTextView(idStanice: String, nazivStanice: String) {
                                stanicaId = idStanice
                                stanicaNaziv = nazivStanice
                                podesiNaziv()
                                ukucanastanica(this@Glavna, stanicaId, odabranoStajalisteSloj, stajaliste, false)
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
                    if (!tastaturasklonjena)
                        sklonitastaturu()
                    if (polje.isFocused)
                        polje.clearFocus()
                    if (!listaPrikazana)
                        prikaziListu(0)
                    slobodnopomeranjemape = true
                }
            })

            mapa.setTheme(AssetsRenderTheme(assets, "", "osmarender.xml"))
            mapa.setMapPosition(44.821, 20.471, 2500.0)
            VoziloInfo.proveriPraznik(this)

            if (adapter.cursor.count > 0)
                prikaziListu(1)

            prikaziTastaturu()

        } catch (e: Exception) {
            Log.d(resources.getString(R.string.debug), "" + e)
        }
    }

    fun crtanjeCrnihMarkera() {
        Linije(this).neprijavljenoVozilo(stajaliste.title, object : Interfejs.specMarker {
            override fun crtanjespecMarkera(
                id: String,
                smer: String,
                g: List<Pair<GeoPoint, LocalTime>>
            ) {
                val listaPolazaka = mutableListOf<Pair<String, String>>()

                fun crniMarker(vozilo: Int) {
                    if (g[vozilo].first.sphericalDistance(stajaliste.geoPoint) < 3500) {
                        val specMarker = MojMarker(
                            id,
                            smer,
                            "/",
                            null,
                            g[vozilo].second.toString(),
                            g[vozilo].first,
                            null
                        )
                        specMarker.marker = (MarkerSymbol(
                            AndroidBitmap(
                                TekstUBitmap().getBitmapFromTitle(
                                    specMarker.title, this@Glavna, 4
                                )
                            ),
                            MarkerSymbol.HotspotPlace.BOTTOM_CENTER, true
                        ))
                        vozilaNaMapi.add(specMarker)
                    }
                }

                if (ProveraDostupnostiInterneta(this@Glavna)) {
                    for (marker in 0 until vozilaNaMapi.size) {
                        with(vozilaNaMapi[marker]) {
                            listaPolazaka.add(Pair(brojLinije, vremePolaska))
                        }
                    }

                    for (marker in g.indices) {
                        if (!listaPolazaka.contains(Pair(id, g[marker].second.toString()))) {
                            crniMarker(marker)
                        }
                    }
                } else {
                    for (marker in g.indices) {
                        crniMarker(marker)
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
                        GeoPoint((jsonObject.getDouble("lt")), jsonObject.getDouble("lg")),
                        jsonObject.getString("sl"))

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
                                true -> vozilo.brojLinije + " (" + (if (vozilo.garazniBMenjan == null) vozilo.garazniBOriginal else vozilo.garazniBMenjan) + ")"
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

            lifecycleScope.launch {
                dugmezaosvezavanje(0, 0)
            }
            
            if ((prviput) and (najbliziAutobusMarker?.geoPoint != null)) {
                odzumirajMapu()
                prviput = false
            }

            //startAnimacijaUcitavanja()

            Log.d("BUSPLJUS!","JSON: "+odgovor)

        } catch (e: Exception) {
            with(Toster(this@Glavna)) {
                if (odgovor == "0") {
                    this.toster(resources.getString(R.string.nema_vozila))
                    izbrisiSveMarkere()
                }
                else {
                    PopupProzor(this@Glavna).prikaziGresku(e)
                }
            }
            lifecycleScope.launch {
                dugmezaosvezavanje(0, 1)
                sviTajmeri.cancel()
            }
        }
        finally {
            lifecycleScope.launch {
                if (Podesavanja.deljenapodesavanja.getBoolean("prikazporv", false)) {
                    crtanjeCrnihMarkera()
                }
                (vozilaNaMapi as? Collection<MarkerInterface>)?.let {
                    markeriVozila.addItems(it)
                }
                mapa.updateMap()
            }
        }
    }

    fun tajmerProgressBar(
        totalMs: Long = 15_000,
        intervalMs: Long = 150,
        action: suspend () -> Unit
    ): Flow<Int> = flow {
        while (currentCoroutineContext().isActive) {
            val startTime = System.currentTimeMillis()

            action()

            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = (totalMs - elapsed).coerceAtLeast(0)
                val progress = ((remaining.toDouble() / totalMs) * 100).toInt()
                emit(progress)

                if (remaining <= 0) break
                delay(intervalMs)
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
            lifecycleScope.launch {
                mapa.setMapPosition(
                    (stajaliste.geoPoint.latitude + najbliziAutobusMarker?.geoPoint?.latitude!!) / 2,
                    (stajaliste.geoPoint.longitude + najbliziAutobusMarker?.geoPoint?.longitude!!) / 2,
                    1 / najbliziAutobusRastojanje * 25000000
                )
            }
        }
    }

    private fun sklonitastaturu() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            currentFocus?.windowToken,
            0
        )
        tastaturasklonjena = true
    }

    private fun prikaziTastaturu() {
        polje.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        tastaturasklonjena = false
    }

    suspend fun dugmezaosvezavanje(animiranaTraka: Int, taster: Int) {
        withContext(Dispatchers.Main) {
            osvezi.visibility = if (taster == 0) View.INVISIBLE else View.VISIBLE

            with(ucitavanje) {
                visibility = if (animiranaTraka == 0) View.INVISIBLE else View.VISIBLE
                isIndeterminate = true
            }

            odbrojavanje15Sek = false
        }
    }

    private suspend fun zahtevZaPozicijuVozila() {
        dugmezaosvezavanje(animiranaTraka = 1, taster = 0) // sakrij dugme, prikaži traku
        try {
            Internet().downloadAsFlow(stanicaId, null, 1).collect { result ->
                if (result is Internet.DownloadResult.Text) {
                    primljeniString = result.content
                }
            }
        } catch (_: IOException) {
            if (Internet.zahtev?.isCanceled() == false) {
                Toster(this@Glavna)
                    .toster(getString(R.string.greska_sa_vezom))
            }
            dugmezaosvezavanje(animiranaTraka = 0, taster = 1)
        }
    }

    fun podesiNaziv() {
        with (polje) {
            hint=(if (stanicaNaziv.isEmpty()) stanicaId else StringBuilder("$stanicaId - $stanicaNaziv"))
            text.clear()
            clearFocus()
        }
    }

    private fun prikaziListu(prikaz: Int) {
        lifecycleScope.launch {
            lista.visibility = (if (prikaz == 0) View.GONE else View.VISIBLE)
            gpsdugme.visibility = (if (prikaz == 0) View.VISIBLE else View.GONE)
            redVoznjeSveLinije.visibility = (if (prikaz == 0) View.VISIBLE else View.GONE)
            listaPrikazana = (if (prikaz == 0) true else false)
        }
    }

    private fun prikazSacuvanihStanica() {
        adapter = PretragaStanica(this, Stanice(this).sacuvaneStaniceCursor())
        lista.adapter = adapter
    }

    fun pokreniCrtanjeTrase(
        voziloMarker: MarkerItem,
        stanicaMarker: MarkerItem,
        presedackaSt: String?,
        polazak: LocalTime
    ) {
        val upit = Linije(this).upitTrasa(voziloMarker.title,stanicaMarker.title, null)

        val preostaleStanice = Stanice(this).staniceDoKrajaTrase(upit.first,stanicaMarker)
        val pribavljenaTrasa = upit.second

        val (koordinateTrase, spisakStajalista) =
            ObradiTrasu(this).preradiTrasu(pribavljenaTrasa, preostaleStanice, voziloMarker, polazak)

        val render = TrasaRender(this, this) // 'this' implements Interfejs.trasa
        render.crtanjeTrase(
            brojLinije = voziloMarker,
            koordinateTrase = koordinateTrase, // niz tacaka trase
            spisakStajalista = spisakStajalista,
            presedackaSt = presedackaSt
        )
    }

    override fun prikazTrase(
        voziloMarker: MarkerItem,
        listaStajalista: List<MarkerItem>,
        listaTacakaTrase: List<GeoPoint>,
        listaSifraNaziv: List<sifraNaziv>
    ) {
        izbrisiTrasu()

        sveStanice.addItems(listaStajalista as Collection<MarkerInterface>)
        putanja.addPoints(listaTacakaTrase)

        putanja.isEnabled = true
        sveStanice.isEnabled = true

        linijaPrikazTrase.text = voziloMarker.title
        listaPrikazTrase.adapter = StaniceBottomSheetDialog(this, listaSifraNaziv)

        baner?.visibility = View.VISIBLE

        zatvoriPrikazTrase.setOnClickListener {
            sakrijTrasu()
        }

        mapa.updateMap()
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

        stopTajmera()
        restartTajmera()

        ukloniBaner()
    }

    fun startCrtanjeSpecMarkera() {
        if (crtanjeJob?.isActive == true) return
        crtanjeJob = sviTajmeri.launch {
            while (isActive) {
                delay(2000)
                izbrisiSveMarkere()
                crtanjeCrnihMarkera()
                markeriVozila.addItems(vozilaNaMapi as Collection<MarkerInterface>)
                mapa.updateMap()
            }
        }
    }

    fun startCrtanjeMarkera() {
        if (crtanjeJob?.isActive == true) return
        crtanjeJob = sviTajmeri.launch {
            tajmerProgressBar(
                totalMs = 15_000,
                intervalMs = 150
            ) {
                zahtevZaPozicijuVozila()
                crtanjemarkera(primljeniString)
            }.collect { napredak ->
                withContext(Dispatchers.Main) {
                    ucitavanje.visibility = View.VISIBLE
                    ucitavanje.isIndeterminate = false
                    ucitavanje.progress = napredak
                }
            }
        }
    }

    fun stopTajmera() {
        timersParentJob.cancel()
        tajmeriPokrenuti = false
    }

    fun restartTajmera() {
        if (tajmeriPokrenuti) return
        timersParentJob = SupervisorJob()
        sviTajmeri = CoroutineScope(Dispatchers.Main + timersParentJob)
        tajmeriPokrenuti = true
    }

    override fun onStop() {
        Internet.zahtev?.cancel()
        stopTajmera()
        super.onStop()
    }

    override fun onResume() {
        if (stanicaId.isNotEmpty())
            lifecycleScope.launch {
                dugmezaosvezavanje(0, 1)
            }
        if ((System.currentTimeMillis().div(1000)) - (Podesavanja.deljenapodesavanja.getLong("zatvoren", 0)) > 300){
            reset()
        }

        restartTajmera()
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Podesavanja.deljenapodesavanja.edit() {
            putLong(
                "zatvoren",
                System.currentTimeMillis().div(1000)
            )
        }
        pratilacLokacije?.let { it1 -> menadzerLokacije.removeUpdates(it1) }
        animacijaJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.cursor?.close()
    }
}