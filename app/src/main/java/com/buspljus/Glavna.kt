package com.buspljus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleCursorAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Timer
import kotlin.concurrent.schedule

class Glavna : AppCompatActivity(),ItemizedLayer.OnItemGestureListener<MarkerInterface> {

    private lateinit var odabranoStajalisteSloj: ItemizedLayer
    private lateinit var odabranoStajalisteMarker: MarkerItem
    private lateinit var polje: AutoCompleteTextView
    private lateinit var podesavanje: ImageButton
    private lateinit var ucitavanje: ProgressBar
    private lateinit var lista: ListView
    private lateinit var osvezi: ImageButton
    private lateinit var pregledmape: MapView
    private lateinit var markeriVozila: ItemizedLayer
    private lateinit var gpsdugme: FloatingActionButton

    private var markersimbol: MarkerSymbol? = null
    private val markeriVozilaSpisak: MutableList<MarkerInterface> = ArrayList()

    private var tastaturasklonjena: Boolean = true
    private var kliknalistu: Boolean = false
    private var boja = 1

    private var najbliziAutobusRastojanje = 100000.0
    private lateinit var najbliziAutobusMarker: MarkerItem

    var primljeniString = ""
    var stanica: String = ""
    var stanica_naziv: String = ""
    var pozicija = MapPosition()
    var trazenjepobroju: Boolean = true
    private var prviput: Boolean = true

    private lateinit var adapter : SimpleCursorAdapter
    private lateinit var mapa: Map
    private var tajmer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setContentView(R.layout.mapa_glavna)

        Podesavanja.deljenapodesavanja = PreferenceManager.getDefaultSharedPreferences(this)

        pregledmape = findViewById(R.id.mapa)
        ucitavanje = findViewById(R.id.napredak)
        polje = findViewById(R.id.polje_za_unos)
        lista = findViewById(R.id.lista)
        osvezi = findViewById(R.id.osvezi)
        podesavanje = findViewById(R.id.podesavanja)
        gpsdugme = findViewById(R.id.gps)

        mapa = pregledmape.map()

        val fromColumns = arrayOf("_id", "naziv", "staju")
        val toViews = intArrayOf(R.id.sifra_stanice, R.id.naziv_stanice, R.id.odredista_sa_stanice)
        adapter = SimpleCursorAdapter(this, R.layout.probna_lista, null, fromColumns, toViews, 0)
        adapter.viewBinder = Selektor()

        val promenaunosa = findViewById<Button>(R.id.promenaunosa)

        gpsdugme.setOnClickListener{
            Toster(this@Glavna).toster("Dugme radi!")
        }

        podesavanje.setOnClickListener {
            startActivity(Intent(this, Podesavanja::class.java))
        }

        osvezi.setOnClickListener {
            ukucanastanica(
                this@Glavna,
                stanica,
                odabranoStajalisteSloj,
                odabranoStajalisteMarker,
                false
            )
        }

        promenaunosa.setOnClickListener {
            fun unosteksta() {
                if (promenaunosa.text == "A") {
                    promenaunosa.text = "1"
                    polje.filters = arrayOf(InputFilter.LengthFilter(4))
                    trazenjepobroju = true
                    polje.inputType = InputType.TYPE_CLASS_NUMBER
                    polje.text.clear()
                    polje.hint = resources.getString(R.string.broj_stanice)
                } else {
                    promenaunosa.text = "A"
                    polje.filters = arrayOf()
                    trazenjepobroju = false
                    polje.inputType = InputType.TYPE_CLASS_TEXT
                    polje.text.clear()
                    polje.hint = resources.getString(R.string.naziv_stanice)
                }
            }
            unosteksta()
        }

        lista.adapter = adapter
        polje.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                runOnUiThread { lista.visibility = View.VISIBLE }
                tastaturasklonjena = false
                if (polje.length() > 0) {
                    adapter.changeCursor(
                        SQLcitac(this@Glavna).dobavisifre(s.toString(), trazenjepobroju)
                    )
                    if ((trazenjepobroju) and //Trazenje po broju, a ne imenu stanice
                        (adapter.cursor.count == 1) and // U listi je ostala jedna stanica
                        (!kliknalistu) and // Na stanicu nije kliknuto na listi, vec se odabira samo kucanjem
                        (Podesavanja.deljenapodesavanja.getBoolean("automatskiunos", true))
                    ) { //Podesavanje ukljuceno/iskljuceno

                        adapter.cursor.moveToFirst()
                        stanica = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow("_id"))
                        stanica_naziv = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow("naziv"))
                        ukucanastanica(this@Glavna, stanica, odabranoStajalisteSloj, odabranoStajalisteMarker, true)
                        podesiNaziv()
                    }
                    kliknalistu = false
                } else {
                    lista.visibility = View.GONE
                }

            }

            override fun afterTextChanged(s: Editable?) {
                return
            }
        }
        )

        lista.setOnItemClickListener { _, _, _, _ ->
            stanica = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow("_id"))
            stanica_naziv = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow("naziv"))
            if (!trazenjepobroju)
                promenaunosa.callOnClick()
            kliknalistu = true
            podesiNaziv()
            ukucanastanica(this@Glavna, stanica, odabranoStajalisteSloj, odabranoStajalisteMarker, true)
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

            stajaliste.title = stanica
            stajaliste.geoPoint = SQLcitac(this).pozahtevu_jednastanica(stanica)
            stajaliste.marker = (MarkerSymbol(
                AndroidBitmap(tekstubitmap().getBitmapFromTitle(stajaliste.getTitle(), this, 0)),
                MarkerSymbol.HotspotPlace.NONE, true
            ))

            if (animacija) {
                mapa.mapPosition =
                    MapPosition(stajaliste.geoPoint.latitude, stajaliste.geoPoint.longitude, 2000.0)
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
            Toster(context).toster(context.resources.getString(R.string.greska_u_programu))
            Log.d("BUSPLJUS!", "" + e)
        }
    }

    private fun otvoriMapu() {
        try {
            class klikNaMapu : Layer(mapa), GestureListener {
                override fun onGesture(g: Gesture, e: MotionEvent): Boolean {
                    if (g == Gesture.TAP) {
                        val p = mMap.viewport().fromScreenPoint(e.x, e.y)
                        SQLcitac(this@Glavna).pretragabaze_kliknamapu(p.latitude.toString(),p.longitude.toString(), object: SQLcitac.Callback {
                            override fun korak1(s: String) {
                                stanica = s
                                stanica_naziv = ""
                                podesiNaziv()
                                ukucanastanica(this@Glavna, s, odabranoStajalisteSloj, odabranoStajalisteMarker, false)
                            }
                        })
                    }
                    return false
                }
            }

            val tileSource = MapFileTileSource()
            val fis = contentResolver.openInputStream(
                Uri.fromFile(
                    (File(
                        filesDir,
                        "beograd.map"
                    ))
                )
            ) as FileInputStream?

            tileSource.setMapFileInputStream(fis)

            val tileLayer = mapa.setBaseMap(tileSource)

            markeriVozila = ItemizedLayer(mapa, markeriVozilaSpisak, markersimbol, this)
            odabranoStajalisteSloj = ItemizedLayer(mapa, markersimbol)
            odabranoStajalisteMarker = MarkerItem(null, null, null, null)

            mapa.layers().add(BitmapTileLayer(mapa, tileSource))
            mapa.layers().add(BuildingLayer(mapa, tileLayer))
            mapa.layers().add(LabelLayer(mapa, tileLayer))
            mapa.layers().add(klikNaMapu())
            mapa.layers().add(markeriVozila)
            mapa.layers().add(odabranoStajalisteSloj)

            izbacitastaturu()

            mapa.events.bind(Map.UpdateListener { e, mapPosition ->

                pozicija = mapPosition

                if (e == Map.ANIM_END) {
                    if (!tastaturasklonjena)
                        sklonitastaturu()
                }
            })

            mapa.setTheme(AssetsRenderTheme(assets, "", "osmarender.xml"))
            mapa.setMapPosition(44.821, 20.471, 2000.0)

        } catch (e: Exception) {
            Log.d("BUSPLJUS!", "" + e)
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
                        }
                        if (marker.description.toString()[2] == '0')
                            marker.description = marker.description.drop(3)
                        else marker.description = marker.description.drop(2)
                    } else boja = 1

                    if (Podesavanja.deljenapodesavanja.getBoolean("prikazgb", false))
                        marker.title = linija + " (" + marker.description + ")"
                    else marker.title = linija

                    marker.geoPoint = GeoPoint(
                        (json.getJSONArray(linija).getJSONObject(i).getDouble("lt")),
                        json.getJSONArray(linija).getJSONObject(i).getDouble("lg")
                    )
                    marker.marker = (MarkerSymbol(
                        AndroidBitmap(tekstubitmap().getBitmapFromTitle(marker.title, this, boja)),
                        MarkerSymbol.HotspotPlace.BOTTOM_CENTER, true
                    ))
                    markeriVozila.addItem(marker)

                    if (odabranoStajalisteMarker.geoPoint.sphericalDistance(
                            GeoPoint(marker.geoPoint.latitude, marker.geoPoint.longitude)) < najbliziAutobusRastojanje) {

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
                if (odgovor == "0\n")
                    this.toster(resources.getString(R.string.nema_vozila))
                else {
                    this.toster(resources.getString(R.string.greska_u_programu))
                    Log.d("BUSPLJUS!", "" + e)
                }
            }
            dugmezaosvezavanje(0, 1)
        }
    }

    override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
        val markerItem = item as MarkerItem
        val rastojanjedostanice = markerItem.geoPoint.sphericalDistance(
            GeoPoint(
                odabranoStajalisteMarker.geoPoint.latitude,
                odabranoStajalisteMarker.geoPoint.longitude
            )
        ).div(1000)
        Toster(this).toster(
            markerItem.description + "\n" + BigDecimal(rastojanjedostanice).setScale(
                1,
                RoundingMode.HALF_EVEN
            ).toString() + " km"
        )
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
        runOnUiThread { lista.visibility = View.GONE }
        tastaturasklonjena = true
    }

    private fun izbacitastaturu() {
        polje.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        tastaturasklonjena = false
    }

    fun dugmezaosvezavanje(kruzic: Int, taster: Int) {
        runOnUiThread {
            when (kruzic) {
                0 -> ucitavanje.visibility = View.INVISIBLE
                1 -> ucitavanje.visibility = View.VISIBLE
            }
            when (taster) {
                0 -> osvezi.visibility = View.INVISIBLE
                1 -> osvezi.visibility = View.VISIBLE
            }
        }
    }

    private fun zahtevZaPozicijuVozila() {
        dugmezaosvezavanje(1, 0)
        Internet().zahtevPremaInternetu(stanica, 1, object : Internet.ApiResponseCallback {
            override fun onSuccess(response: Response) {
                if (response.isSuccessful) {
                    primljeniString = response.body!!.string()
                    crtanjemarkera(primljeniString)
                }
            }

            override fun onFailure(e: IOException) {
                Toster(this@Glavna).toster(resources.getString(R.string.nema_interneta))
                dugmezaosvezavanje(0, 1)
            }
        })
    }

    fun podesiNaziv() {
        if (stanica_naziv.isEmpty())
            polje.hint=stanica
        else
            polje.hint=StringBuilder(stanica + " / " + stanica_naziv)
        polje.text.clear()
        polje.clearFocus()
    }
    private fun stopTajmera() {
        Internet.zahtev?.cancel()

        tajmer?.cancel()
        tajmer?.purge()
        tajmer = null
    }

    override fun onStop() {
        stopTajmera()
        super.onStop()
    }

    override fun onResume() {
        if (stanica.isNotEmpty())
            dugmezaosvezavanje(0, 1)
        super.onResume()
    }
}
