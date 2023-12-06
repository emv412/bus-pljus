package com.buspljus

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
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
import okhttp3.Response
import org.json.JSONObject
import org.oscim.android.MapView
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.android.theme.AssetsRenderTheme
import org.oscim.core.GeoPoint
import org.oscim.core.MapPosition
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

private const val DEBAG = "BUSPLJUS!"

class Glavna : AppCompatActivity(),ItemizedLayer.OnItemGestureListener<MarkerInterface> {
    companion object {
        lateinit var kursor : Cursor
    }

    private lateinit var podesavanje : ImageButton
    private lateinit var ucitavanje : ProgressBar
    private lateinit var lista : ListView
    private lateinit var osvezi : ImageButton
    private lateinit var mapa : Map
    private lateinit var pregledmape : MapView
    private lateinit var polje : AutoCompleteTextView
    private lateinit var markeri_vozila : ItemizedLayer

    private var markersimbol : MarkerSymbol? = null
    private val markeri_vozila_spisak : MutableList<MarkerInterface> = ArrayList()
    private lateinit var odabrano_stajaliste_sloj : ItemizedLayer
    private lateinit var odabrano_stajaliste_marker : MarkerItem

    private var tajmer : Timer? = null
    private var tastaturasklonjena : Boolean = true
    private var kliknalistu : Boolean = false
    private var boja = 1

    var primljeni_string = ""
    var stanica : String = ""
    var trazenjepobroju : Boolean = true


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

        mapa = pregledmape.map()

        val fromColumns = arrayOf("_id", "naziv", "staju")
        val toViews = intArrayOf(R.id.sifra_stanice, R.id.naziv_stanice, R.id.odredista_sa_stanice)
        val adapter = SimpleCursorAdapter(this, R.layout.probna_lista,null, fromColumns, toViews,0)
        adapter.viewBinder=Selektor()

        val promenaunosa = findViewById<Button>(R.id.promenaunosa)

        podesavanje.setOnClickListener{
            startActivity(Intent(this, Podesavanja::class.java))
        }

        osvezi.setOnClickListener {
            ukucanastanica(stanica, odabrano_stajaliste_sloj, odabrano_stajaliste_marker,false)
        }

        promenaunosa.setOnClickListener {
            fun unosteksta() {
                if (promenaunosa.text == "A") {
                    promenaunosa.text="1"
                    polje.filters=arrayOf(InputFilter.LengthFilter(4))
                    trazenjepobroju=true
                    polje.inputType=InputType.TYPE_CLASS_NUMBER
                    polje.text.clear()
                    polje.hint=resources.getString(R.string.broj_stanice)
                }
                else {
                    promenaunosa.text="A"
                    polje.filters=arrayOf()
                    trazenjepobroju=false
                    polje.inputType=InputType.TYPE_CLASS_TEXT
                    polje.text.clear()
                    polje.hint=resources.getString(R.string.naziv_stanice)
                }
            }
            unosteksta()
        }


        lista.adapter=adapter
        polje.addTextChangedListener(object: TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                runOnUiThread { lista.visibility = View.VISIBLE }
                if (polje.length() > 0) {
                    adapter.changeCursor(SQLcitac(this@Glavna).dobavisifre(s.toString(),trazenjepobroju))
                    if ( (trazenjepobroju) and //Trazenje po broju, a ne imenu stanice
                        (adapter.cursor.count == 1) and // U listi je ostala jedna stanica
                        (!kliknalistu) and // Na stanicu nije kliknuto na listi, vec se odabira samo kucanjem
                        (Podesavanja.deljenapodesavanja.getBoolean("automatskiunos",true)) ) { //Podesavanje ukljuceno/iskljuceno

                        adapter.cursor.moveToFirst()
                        stanica = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow("_id"))
                        ukucanastanica(stanica,odabrano_stajaliste_sloj,odabrano_stajaliste_marker,true)

                    }
                    kliknalistu = false
                }
                else lista.visibility=View.GONE
            }

            override fun afterTextChanged(s: Editable?) {
                return
            }
        }
        )

        lista.setOnItemClickListener { parent, view, position, id ->
            stanica = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow("_id"))
            if (!trazenjepobroju)
                promenaunosa.callOnClick()
            kliknalistu = true
            polje.setText(stanica)
            ukucanastanica(stanica,odabrano_stajaliste_sloj,odabrano_stajaliste_marker,true)
        }
        openMap(Uri.fromFile((File(filesDir,"beograd.map"))))
    }

    private fun ukucanastanica(stanica: String, stajaliste_sloj: ItemizedLayer, stajaliste: MarkerItem, animacija: Boolean) {
        try {
            markeri_vozila.removeAllItems()

            if (tajmer != null)
                stopTajmera()
            zahtev_za_poziciju_vozila()
            dugmezaosvezavanje(1,0)

            tajmer = Timer()

            sklonitastaturu()

            stajaliste.title=stanica
            stajaliste.geoPoint=SQLcitac(this).pozahtevu_jednastanica(stanica)
            stajaliste.marker=(MarkerSymbol(
                AndroidBitmap(tekstubitmap().getBitmapFromTitle(stajaliste.getTitle(),this,0)),
                MarkerSymbol.HotspotPlace.NONE,true))

            if (animacija) {
                mapa.mapPosition = MapPosition(stajaliste.geoPoint.latitude,stajaliste.geoPoint.longitude,2000.0)
                if (stajaliste_sloj.size() == 0) { mapa.animator().animateZoom(2000, 22.0,0F,0F) }
                else {mapa.animator().animateTo(500,stajaliste.geoPoint,22.0,true)}
            }
            if (stajaliste_sloj.size() != 0)
                stajaliste_sloj.removeItem(0)

            stajaliste_sloj.addItem(stajaliste)

            mapa.updateMap()
        }

        catch (e: Exception) {
                Toster(this@Glavna).toster(resources.getString(R.string.greska_u_programu))
        }


    }

    private fun openMap(uri: Uri) {
        try {
            // Tile source
            val tileSource = MapFileTileSource()
            val fis = contentResolver.openInputStream(uri) as FileInputStream?
            tileSource.setMapFileInputStream(fis)

            // Vector layer
            val tileLayer = mapa.setBaseMap(tileSource)

            // Building layer
            mapa.layers().add(BuildingLayer(mapa, tileLayer))

            // Label layer
            mapa.layers().add(LabelLayer(mapa, tileLayer))

            // Render theme
            mapa.setTheme(AssetsRenderTheme(assets, "", "osmarender.xml"))

            mapa.setMapPosition(44.821, 20.471, 2000.0)

            mapa.layers().add(BitmapTileLayer(mapa, tileSource))

            markeri_vozila = ItemizedLayer(mapa, markeri_vozila_spisak, markersimbol,this)

            odabrano_stajaliste_sloj = ItemizedLayer(mapa,markersimbol)
            odabrano_stajaliste_marker = MarkerItem(null,null,null,null)

            mapa.layers().add(Kliknamapu(mapa,this))
            mapa.layers().add(markeri_vozila)
            mapa.layers().add(odabrano_stajaliste_sloj)

            izbacitastaturu()

            mapa.events.bind(Map.UpdateListener { e, mapPosition ->

                if (e == Map.ANIM_END) {
                    if (!tastaturasklonjena)
                        sklonitastaturu()

                    Kliknamapu.pozicija=mapPosition
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun crtanjemarkera(odgovor: String) {
        try {
            val json = odgovor.let { JSONObject(it) }
            val linije = json.keys()

            markeri_vozila.removeAllItems()

            while (linije.hasNext()) {
                val linija = linije.next()
                val maximum_i = json.getJSONArray(linija).length()
                for (i in 0 until maximum_i) {
                    val marker = MarkerItem(null, null, null)

                    // funkcija za prikaz garaznog broja vozila
                    marker.description = json.getJSONArray(linija).getJSONObject(i).getString("g")
                    if ((marker.description.toString()
                            .startsWith("P9")) or (marker.description.toString().startsWith("P8"))
                    ) {
                        if (marker.description.toString().startsWith("P9"))
                            boja = 1
                        if (marker.description.toString().startsWith("P8")) {
                            if (marker.description.toString()[2] == '0' || marker.description.toString()[2] == '1')
                                boja = 2 //tramvaj
                            else boja = 3 //trolejbus
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
                    markeri_vozila.addItem(marker)
                }
            }

            dugmezaosvezavanje(0, 0)
            mapa.updateMap()
            tajmer?.schedule(15000) { zahtev_za_poziciju_vozila() }

        }
        catch (e: Exception) {
            if (odgovor == "0\n") {
                Toster(this@Glavna).toster(resources.getString(R.string.nema_vozila))
            } else
                Toster(this@Glavna).toster(e.toString())
            dugmezaosvezavanje(0,1)
        }
    }

    override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
        val markerItem = item as MarkerItem
        Toster(this).toster(markerItem.description)
        return true
    }

    override fun onItemLongPress(index: Int, item: MarkerInterface?): Boolean {
        //val markerItem = item as MarkerItem
        return true
    }

    private fun sklonitastaturu() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken,0)
        runOnUiThread{lista.visibility=View.GONE}
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

    fun zahtev_za_poziciju_vozila() {
        dugmezaosvezavanje(1,0)
        Internet().zahtev_prema_internetu(stanica,1, object: Internet.ApiResponseCallback{
            override fun onSuccess(response: Response) {
                if (response.isSuccessful) {
                    primljeni_string = response.body!!.string()
                    crtanjemarkera(primljeni_string)
                }
            }

            override fun onFailure(e: IOException) {
                Toster(this@Glavna).toster(resources.getString(R.string.nema_interneta))
                dugmezaosvezavanje(0,1)
            }
        })
    }

    private fun stopTajmera() {
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
            dugmezaosvezavanje(0,1)
        super.onResume()
    }
}
