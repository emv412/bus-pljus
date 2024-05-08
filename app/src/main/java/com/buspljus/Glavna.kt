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
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.preference.PreferenceManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol
import org.oscim.layers.tile.bitmap.BitmapTileLayer
import org.oscim.layers.tile.buildings.BuildingLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.map.Map
import org.oscim.tiling.source.mapfile.MapFileTileSource
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Timer
import kotlin.concurrent.schedule

class Glavna : AppCompatActivity(),ItemizedLayer.OnItemGestureListener<MarkerInterface> {

    companion object {
        lateinit var mapa: Map
        lateinit var adapter : KursorAdapterAutobus
    }

    private lateinit var odabranoStajalisteSloj: ItemizedLayer
    private lateinit var pozicijaPesakaSloj: ItemizedLayer
    private lateinit var markeriVozila: ItemizedLayer
    private lateinit var redvoznjeProzor: ItemizedLayer
    private lateinit var odabranoStajalisteMarker: MarkerItem
    private lateinit var pozicijaPesakaMarker: MarkerItem
    private lateinit var polje: AutoCompleteTextView
    private lateinit var podesavanje: ImageButton
    private lateinit var ucitavanje: ProgressBar
    private lateinit var lista: ListView
    private lateinit var osvezi: ImageButton
    private lateinit var pregledmape: MapView
    private lateinit var promenaunosa: Button
    private lateinit var gpsdugme: FloatingActionButton
    private lateinit var menadzerLokacije: LocationManager
    private lateinit var pratilacLokacije: LocationListener

    private var markersimbol: MarkerSymbol? = null
    private var slobodnopomeranjemape = true
    private var pozicija = MapPosition()
    private var prviput: Boolean = true
    private var tajmer: Timer? = null
    private var najbliziAutobusRastojanje = 100000.0
    private var najbliziAutobusMarker = MarkerItem(null,null,null,null)
    private var tastaturasklonjena: Boolean = true
    private var kliknalistu: Boolean = false
    private var omogucenoLociranje = false
    private var boja = 1

    private val markeriVozilaSpisak: MutableList<MarkerInterface> = ArrayList()

    var primljeniString = ""
    var stanicaId: String = ""
    var stanicaNaziv: String = ""
    var trazenjepobroju: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setContentView(R.layout.mapa_glavna)

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

        mapa = pregledmape.map()

        adapter = KursorAdapterAutobus(this,SQLcitac(this).SQLzahtev("stanice", arrayOf("_id","naziv_cir","staju","sacuvana"),"sacuvana = ?", arrayOf("1"),null))
        lista.adapter = adapter

        lista.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                adapter.cursor?.apply {
                    moveToPosition(position)
                    stanicaId = getString(getColumnIndexOrThrow("_id"))
                    stanicaNaziv = getString(getColumnIndexOrThrow(SQLcitac.CIR_KOLONA))
                }
                if (!trazenjepobroju)
                    promenaunosa.callOnClick()
                kliknalistu = true
                ukucanastanica(this@Glavna, stanicaId, odabranoStajalisteSloj, odabranoStajalisteMarker, true)
                podesiNaziv()
                prikaziListu(0)
            }

        gpsdugme.setOnClickListener {
            menadzerLokacije = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            //val mreznaLok = menadzerLokacije.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                if (omogucenoLociranje) {
                    omogucenoLociranje = false
                    gpsdugme.backgroundTintList=AppCompatResources.getColorStateList(this, R.color.siva)
                    slobodnopomeranjemape = true
                    pozicijaPesakaSloj.removeAllItems()
                    menadzerLokacije.removeUpdates(pratilacLokacije)
                }
                else {
                    omogucenoLociranje = true
                    gpsdugme.backgroundTintList=AppCompatResources.getColorStateList(this, R.color.crvena)
                    slobodnopomeranjemape = false

                    if (menadzerLokacije.isProviderEnabled(GPS_PROVIDER)) {
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
                        menadzerLokacije.requestLocationUpdates(GPS_PROVIDER, 5000, 0F, pratilacLokacije)
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
                odabranoStajalisteMarker,
                false
            )
        }

        promenaunosa.setOnClickListener {
            with (polje) {
                filters = (if (trazenjepobroju) arrayOf() else arrayOf(InputFilter.LengthFilter(4)))
                inputType = (if (trazenjepobroju) TYPE_CLASS_TEXT else TYPE_CLASS_NUMBER)
                hint = (if (trazenjepobroju) resources.getString(R.string.naziv_stanice) else resources.getString(R.string.broj_stanice))
                text.clear()
            }
            promenaunosa.text = (if (trazenjepobroju) "A" else "1")
            trazenjepobroju = (!trazenjepobroju)
            izbacitastaturu()
        }

        polje.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                prikaziListu(1)
                tastaturasklonjena = false
                if (polje.length() > 0) {
                    adapter.changeCursor(SQLcitac(this@Glavna).dobavisifre(s.toString(), trazenjepobroju))
                    lista.setSelection(0)

                    if ((trazenjepobroju) and //Trazenje po broju, a ne imenu stanice
                        (adapter.cursor.count == 1) and // U listi je ostala jedna stanica
                        (!kliknalistu) and // Na stanicu nije kliknuto na listi, vec se odabira samo kucanjem
                        (Podesavanja.deljenapodesavanja.getBoolean("automatskiunos", true)))
                    { //Podesavanje ukljuceno/iskljuceno

                        with (adapter.cursor) {
                            moveToFirst()
                            stanicaId = getString(getColumnIndexOrThrow("_id"))
                            stanicaNaziv = getString(getColumnIndexOrThrow(SQLcitac.CIR_KOLONA))
                        }

                        ukucanastanica(this@Glavna, stanicaId, odabranoStajalisteSloj, odabranoStajalisteMarker, true)

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

        polje.setOnClickListener {
            if (polje.length() == 0) {
                prikazSacuvanihStanica()
                if (lista.adapter.count > 0)
                    prikaziListu(1)
            }
        }
        otvoriMapu()
    }

    fun ukucanastanica(context: Context, stanica: String, stajalisteSloj: ItemizedLayer, stajaliste: MarkerItem, animacija: Boolean) {
        try {
            sklonitastaturu()

            markeriVozila.removeAllItems()
            prviput = true

            if (tajmer != null)
                stopTajmera()
            zahtevZaPozicijuVozila()
            dugmezaosvezavanje(1, 0)
            tajmer = Timer()

            with (stajaliste) {
                title = stanica
                geoPoint = SQLcitac(this@Glavna).pozahtevu_jednastanica(stanica)
                marker = (MarkerSymbol(
                    AndroidBitmap(tekstubitmap().getBitmapFromTitle(stajaliste.getTitle(), this@Glavna, 0)),
                    MarkerSymbol.HotspotPlace.NONE, true
                ))
            }

            if (animacija) {
                mapa.mapPosition = MapPosition(stajaliste.geoPoint.latitude, stajaliste.geoPoint.longitude, 2000.0)
                if (stajalisteSloj.size() == 0) {
                    mapa.animator().animateZoom(2000, 22.0, 0F, 0F)
                } else {
                    mapa.animator().animateTo(500, stajaliste.geoPoint, 22.0, true)
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
                        SQLcitac(this@Glavna).pretragabaze_kliknamapu(p.latitude.toString(),p.longitude.toString(), object: SQLcitac.Callback {
                            override fun korak(s: String) {
                                stanicaId = s
                                stanicaNaziv = ""
                                podesiNaziv()
                                ukucanastanica(this@Glavna, s, odabranoStajalisteSloj, odabranoStajalisteMarker, false)
                            }

                            override fun koloneBGVOZ(lista: List<String>) {
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
            redvoznjeProzor = ItemizedLayer(mapa,markersimbol)

            odabranoStajalisteSloj = ItemizedLayer(mapa, markersimbol)
            odabranoStajalisteMarker = MarkerItem(null, null, null, null)
            pozicijaPesakaSloj = ItemizedLayer(mapa,markersimbol)
            pozicijaPesakaMarker = MarkerItem(null,null,null,null)

            pozicijaPesakaMarker.marker=MarkerSymbol(
                AndroidBitmap(VectorDrawableCompat.create(
                    resources,R.drawable.glisa,theme)!!.toBitmap()),
                MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true)

            with (mapa.layers()) {
                val listaSlojeva = listOf(
                    BitmapTileLayer(mapa, tileSource),
                    BuildingLayer(mapa, tileLayer),
                    LabelLayer(mapa, tileLayer),Kliknamapu(),
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

                if (e == Map.ANIM_END) {
                    if (!tastaturasklonjena)
                        sklonitastaturu()
                    prikaziListu(0)
                    slobodnopomeranjemape = true
                }
            })

            mapa.setTheme(AssetsRenderTheme(assets, "", "osmarender.xml"))
            mapa.setMapPosition(44.821, 20.471, 2500.0)

            if (adapter.cursor.count > 0)
                prikaziListu(1)
            else
                izbacitastaturu()

        } catch (e: Exception) {
            Log.d(resources.getString(R.string.debug), "" + e)
        }
    }

    fun crtanjemarkera(odgovor: String) {
        try {
            najbliziAutobusRastojanje = 100000.0
            val json = JSONObject(odgovor)
            val linije = json.keys()

            markeriVozila.removeAllItems()

            while (linije.hasNext()) {
                val linija = linije.next()
                val maximumI = json.getJSONArray(linija).length()
                for (i in 0 until maximumI) {
                    val marker = MarkerItem(null, null, null)

                    // funkcija za prikaz garaznog broja vozila
                    marker.title=linija
                    marker.description = json.getJSONArray(linija).getJSONObject(i).getString("g")
                    if ((marker.description.toString()
                            .startsWith("P9")) or (marker.description.toString().startsWith("P8"))
                    ) {
                        if (marker.description.toString().startsWith("P9"))
                            boja = 1
                        if (marker.description.toString().startsWith("P8")) {
                            boja =
                                if (marker.description.toString()[2] == '0' || marker.description.toString()[2] == '1')
                                    2 //tramvaj
                                else 3 //trolejbus
                            if (marker.description.toString()[2] == '0')
                                marker.description = marker.description.replaceFirst("0","2")
                        }
                        if (marker.description.toString()[2] == '0')
                            marker.description = marker.description.drop(3)
                        else marker.description = marker.description.drop(2)
                    } else boja = 1

                    marker.geoPoint = GeoPoint(
                        (json.getJSONArray(linija).getJSONObject(i).getDouble("lt")),
                        json.getJSONArray(linija).getJSONObject(i).getDouble("lg")
                    )
                    marker.marker = (MarkerSymbol(
                        AndroidBitmap(tekstubitmap().getBitmapFromTitle(
                            when (Podesavanja.deljenapodesavanja.getBoolean("prikazgb", false)) {
                                true -> marker.title + " (" + marker.description + ")"
                                false -> marker.title
                            }, this, boja)),
                        MarkerSymbol.HotspotPlace.BOTTOM_CENTER, true
                    ))
                    markeriVozila.addItem(marker)

                    if ((odabranoStajalisteMarker.geoPoint.sphericalDistance(GeoPoint(marker.geoPoint.latitude, marker.geoPoint.longitude)) < najbliziAutobusRastojanje)
                        and ((odabranoStajalisteMarker.geoPoint.sphericalDistance(GeoPoint(marker.geoPoint.latitude, marker.geoPoint.longitude))>100))) {

                        najbliziAutobusRastojanje =
                            odabranoStajalisteMarker.geoPoint.sphericalDistance(
                                GeoPoint(
                                    marker.geoPoint.latitude,
                                    marker.geoPoint.longitude
                                )
                            )
                        najbliziAutobusMarker = MarkerItem(
                            null,
                            null,
                            GeoPoint(marker.geoPoint.latitude, marker.geoPoint.longitude)
                        )
                    }
                }
            }

            dugmezaosvezavanje(0, 0)
            if ((prviput) and (najbliziAutobusMarker.geoPoint != null)) {
                runOnUiThread {
                    mapa.setMapPosition(
                        (odabranoStajalisteMarker.geoPoint.latitude + najbliziAutobusMarker.geoPoint.latitude) / 2,
                        (odabranoStajalisteMarker.geoPoint.longitude + najbliziAutobusMarker.geoPoint.longitude) / 2,
                        1 / najbliziAutobusRastojanje * 25000000
                    )
                }
                prviput = false
            }

            mapa.updateMap()
            tajmer?.schedule(15000) { zahtevZaPozicijuVozila() }

        } catch (e: Exception) {
            with(Toster(this@Glavna)) {
                if (odgovor == "0")
                    this.toster(resources.getString(R.string.nema_vozila))
                else {
                    this.toster(odgovor)
                    Log.d(resources.getString(R.string.debug), "" + odgovor,e)
                }
            }
            dugmezaosvezavanje(0, 1)
        }
    }

    override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
        val markerItem = item as MarkerItem
        RedVoznje(this@Glavna).redvoznjeKliknaVozilo(markerItem, odabranoStajalisteMarker, stanicaId)
        return true
    }

    override fun onItemLongPress(index: Int, item: MarkerInterface?): Boolean {
        //val markerItem = item as MarkerItem
        return true
    }

    private fun sklonitastaturu() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            currentFocus?.windowToken,
            0
        )
        tastaturasklonjena = true
    }

    private fun izbacitastaturu() {
        polje.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        tastaturasklonjena = false
    }

    fun dugmezaosvezavanje(kruzic: Int, taster: Int) {
        runOnUiThread {
            ucitavanje.visibility = (if (kruzic == 0) View.INVISIBLE else View.VISIBLE)
            osvezi.visibility = (if (taster == 0) View.INVISIBLE else View.VISIBLE)
        }
    }

    private fun zahtevZaPozicijuVozila() {
        dugmezaosvezavanje(1, 0)
        Internet().zahtevPremaInternetu(stanicaId, null, 1,  object : Internet.odgovorSaInterneta {
            override fun uspesanOdgovor(response: Response) {
                if (response.isSuccessful) {
                    primljeniString = response.body!!.string()
                    crtanjemarkera(primljeniString)
                }
            }

            override fun neuspesanOdgovor(e: IOException) {
                if (Internet.zahtev?.isCanceled() == false)
                    Toster(this@Glavna).toster(resources.getString(R.string.nema_interneta))
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

        tajmer?.cancel()
        tajmer?.purge()
        tajmer = null
    }

    private fun prikaziListu(prikaz: Int) {
        runOnUiThread {
            lista.visibility = (if (prikaz == 0) View.GONE else View.VISIBLE)
            gpsdugme.visibility = (if (prikaz == 0) View.VISIBLE else View.GONE)
        }
    }

    private fun prikazSacuvanihStanica() {
        adapter = KursorAdapterAutobus(this, SQLcitac(this).SQLzahtev("stanice", arrayOf("_id","naziv_cir","staju","sacuvana"),"sacuvana = ?", arrayOf("1"),null))
        lista.adapter = adapter
    }

    override fun onStop() {
        stopTajmera()
        super.onStop()
    }

    override fun onResume() {
        if (stanicaId.isNotEmpty())
            dugmezaosvezavanje(0, 1)
        super.onResume()
    }
}