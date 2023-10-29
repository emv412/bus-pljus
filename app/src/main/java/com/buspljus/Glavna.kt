package com.buspljus

import android.app.Activity
import android.content.Context
import android.database.Cursor
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import org.oscim.android.MapView
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.android.theme.AssetsRenderTheme
import org.oscim.backend.CanvasAdapter
import org.oscim.core.BoundingBox
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
import org.oscim.renderer.GLViewport
import org.oscim.scalebar.DefaultMapScaleBar
import org.oscim.scalebar.MapScaleBar
import org.oscim.scalebar.MapScaleBarLayer
import org.oscim.theme.IRenderTheme
import org.oscim.tiling.source.mapfile.MapFileTileSource
import java.io.File
import java.io.FileInputStream
import java.util.Timer
import kotlin.concurrent.schedule

private const val DEBAG = "BUSPLJUS!"

class Glavna : Activity(),ItemizedLayer.OnItemGestureListener<MarkerInterface> {
    companion object {
        lateinit var kursor : Cursor
    }

    private lateinit var mapa : Map
    private lateinit var pregledmape : MapView
    private lateinit var polje : AutoCompleteTextView
    private lateinit var ucitavanje : ProgressBar
    private lateinit var lista : ListView
    private lateinit var osvezi : ImageButton
    private val spisak_markera : MutableList<MarkerInterface> = ArrayList()
    private var markersimbol : MarkerSymbol? = null
    private lateinit var markeri_vozila : ItemizedLayer
    private lateinit var markeri_pregled_sloj : ItemizedLayer
    private var tema: IRenderTheme? = null
    private var tajmer : Timer? = null
    var stanica : String = ""
    var trazenjepobroju : Boolean = true
    var tastaturasklonjena : Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mapa_xml)

        pregledmape = findViewById(R.id.mapa)
        ucitavanje = findViewById(R.id.napredak)
        polje = findViewById(R.id.polje_za_unos)
        lista = findViewById(R.id.lista)
        osvezi = findViewById(R.id.osvezi)
        mapa = pregledmape.map()

        val baza = SQLcitac(this)
        val fromColumns = arrayOf("_id","naziv")
        val toViews = intArrayOf(android.R.id.text1, android.R.id.text2)
        val adapter = SimpleCursorAdapter(this, android.R.layout.simple_list_item_2,null,fromColumns,toViews,0)

        val odabrano_stajaliste_sloj = ItemizedLayer(mapa,markersimbol)
        val odabrano_stajaliste_marker = MarkerItem(null,null,null,null)

        val promenaunosa = findViewById<Button>(R.id.promenaunosa)

        runOnUiThread { osvezi.visibility = View.GONE }

        osvezi.setOnClickListener {
            kliknutodugme(stanica,odabrano_stajaliste_sloj,odabrano_stajaliste_marker,false)
        }

        promenaunosa.setOnClickListener {
            izbacitastaturu()
            if (promenaunosa.text == "A") {
                promenaunosa.text="1"
                polje.filters=arrayOf(InputFilter.LengthFilter(4))
                trazenjepobroju=true
                polje.inputType=InputType.TYPE_CLASS_NUMBER
                polje.text.clear()
                polje.hint="Broj stanice..."
            }
            else {
                promenaunosa.text="A"
                polje.filters=arrayOf()
                trazenjepobroju=false
                polje.inputType=InputType.TYPE_CLASS_TEXT
                polje.text.clear()
                polje.hint="Naziv stanice..."
            }
        }

        polje.threshold=1
        lista.adapter=adapter
        polje.addTextChangedListener(object: TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                izbacitastaturu()
                runOnUiThread{ lista.visibility=View.VISIBLE }
                adapter.changeCursor(baza.dobavisifre(s.toString(),trazenjepobroju))
                if (trazenjepobroju) {
                    if (adapter.cursor.count == 1) {
                        adapter.cursor.moveToFirst()
                        stanica = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow("_id"))
                        kliknutodugme(stanica,odabrano_stajaliste_sloj,odabrano_stajaliste_marker,true)
                    }
                }

            }

            override fun afterTextChanged(s: Editable?) {
                return
            }
        }
        )

        lista.setOnItemClickListener { parent, view, position, id ->
            stanica = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow("_id"))

            polje.setText(stanica)
            kliknutodugme(stanica,odabrano_stajaliste_sloj,odabrano_stajaliste_marker,true)
        }


        openMap(Uri.fromFile((File(filesDir,"beograd.map"))))
    }

    private fun kliknutodugme(stanica: String, stajaliste_sloj: ItemizedLayer, stajaliste: MarkerItem, animacija: Boolean) {
        if (polje.text.isBlank()) {
            Toster(this).toster("Unesi broj stanice!")
        }
        else {
            try {
                if (tajmer != null)
                    stopTajmera()
                prijemPodataka(stanica)
                runOnUiThread { osvezi.visibility = View.GONE }

                tajmer = Timer()

                sklonitastaturu()

                stajaliste.title=stanica
                stajaliste.geoPoint=SQLcitac(this).pozahtevu(stanica)
                stajaliste.marker=(MarkerSymbol(
                    AndroidBitmap(tekstubitmap().getBitmapFromTitle(stajaliste.getTitle(),this,1)),
                    MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true))
                mapa.layers().add(stajaliste_sloj)

                if (animacija) {
                    mapa.mapPosition = MapPosition(stajaliste.geoPoint.latitude,stajaliste.geoPoint.longitude,2000.0)
                    if (stajaliste_sloj.size() == 0) { mapa.animator().animateZoom(2000, 22.0,0F,0F) }
                    else {mapa.animator().animateTo(500,stajaliste.geoPoint,22.0,true)}
                }

                stajaliste_sloj.addItem(stajaliste)

                mapa.updateMap()
            }
            catch (e: Exception) {
                Toster(this@Glavna).toster("Nije unet broj stanice!")
                Log.d(DEBAG,""+e)
            }

        }

    }


    private fun openMap(uri: Uri) {
        lateinit var okvir : BoundingBox
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
            mapa.setTheme(AssetsRenderTheme(assets, "", "default.xml"))

            // Scale bar
            val mapScaleBar: MapScaleBar = DefaultMapScaleBar(mapa)
            val mapScaleBarLayer = MapScaleBarLayer(mapa, mapScaleBar)
            mapScaleBarLayer.renderer.setPosition(GLViewport.Position.BOTTOM_LEFT)
            mapScaleBarLayer.renderer.setOffset(5 * CanvasAdapter.getScale(), 0f)
            mapa.layers().add(mapScaleBarLayer)

            mapa.setMapPosition(44.821, 20.471, 2000.0)

            mapa.layers().add(BitmapTileLayer(mapa, tileSource))

            markeri_vozila = ItemizedLayer(mapa, spisak_markera, markersimbol,this)
            mapa.layers().add(markeri_vozila)

            izbacitastaturu()

            mapa.events.bind(Map.UpdateListener { e, mapPosition ->
                if (e == Map.ANIM_END) {

                    /*markeri_vozila.removeAllItems()
                    okvir = mapa.getBoundingBox(0)
                    val markeri_pregled = SQLcitac(this@Glavna).prikazstanicapopomeranjumape(okvir.minLatitude.toString(),okvir.maxLatitude.toString(),okvir.minLongitude.toString(),okvir.maxLongitude.toString())
                    for (i in 0 until markeri_pregled.size) {
                        markeri_pregled[i].marker=(MarkerSymbol(
                            AndroidBitmap(tekstubitmap().getBitmapFromTitle(markeri_pregled[i].getTitle(),this,1)),
                            MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true))
                        markeri_vozila.addItem()
                    }
                    //Log.d(DEBAG,""+markeri_pregled_sloj.itemList+", okvir: "+okvir+", markeri_pregled: "+markeri_pregled)
                    Log.d(DEBAG,""+okvir.minLatitude+", "+okvir.maxLatitude+", "+okvir.minLongitude+","+okvir.maxLongitude)
                    
                     */

                    if (!tastaturasklonjena)
                        sklonitastaturu()
                }

            // Srediti

            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun prijemPodataka(stanica: String) {
        runOnUiThread { ucitavanje.visibility = View.VISIBLE }
        val adresa = Request.Builder()
            .url("https://bus-pljus.onrender.com/broj_stanice?st=$stanica")
            .build()

        OkHttpClient().newCall(adresa).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Toster(this@Glavna).toster("Greska: "+e)
                runOnUiThread {
                    ucitavanje.visibility = View.INVISIBLE
                    osvezi.visibility = View.VISIBLE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val odgovor = response.body?.string()
                    try {
                        val json = odgovor?.let { JSONObject(it) }
                        if (json != null) {
                            crtanjemarkera(json)
                        }
                    } catch (e: Exception) {
                        if (odgovor == "0\n") {
                            Toster(this@Glavna).toster("Nema vozila")
                        }
                        else
                            Toster(this@Glavna).toster(e.toString())
                        runOnUiThread {
                            osvezi.visibility = View.VISIBLE
                        }
                        }
                    }
                runOnUiThread { ucitavanje.visibility = View.INVISIBLE }
                }
            }
        )

    }

    fun crtanjemarkera(dobijenipodaci: JSONObject) {
        val linije = dobijenipodaci.keys()

        markeri_vozila.removeAllItems()

        while (linije.hasNext()) {
            val linija = linije.next()
            val maximum_i = dobijenipodaci.getJSONArray(linija).length()
            for (i in 0 until maximum_i) {
                val marker = MarkerItem(null,null,null)

                // funkcija za prikaz garaznog broja vozila
                marker.description = dobijenipodaci.getJSONArray(linija).getJSONObject(i).getString("g")
                    if (marker.description.toString().startsWith("P9") or (marker.description.toString().startsWith("P8")))
                    marker.description = marker.description.drop(2)
                        if (marker.description.toString().startsWith("0"))
                            marker.description = marker.description.drop(1)

                //marker.title = linija+" ("+marker.description+")"
                marker.title=linija
                marker.geoPoint = GeoPoint((dobijenipodaci.getJSONArray(linija).getJSONObject(i).getDouble("lt")),
                                                dobijenipodaci.getJSONArray(linija).getJSONObject(i).getDouble("lg"))
                marker.marker = (MarkerSymbol(
                        AndroidBitmap(tekstubitmap().getBitmapFromTitle(marker.title,this,0)),
                        MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true))
                markeri_vozila.addItem(marker)
            }
        }

        mapa.updateMap()
        tajmer?.schedule(15000) { prijemPodataka(stanica) }
    }

    override fun onItemSingleTapUp(index: Int, item: MarkerInterface?): Boolean {
        //val markerItem = item as MarkerItem
        Toster(this).toster("Uskoro!")
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

    private fun stopTajmera() {
        tajmer?.cancel()
        tajmer?.purge()
        tajmer = null
        markeri_vozila.removeAllItems()
    }
    override fun onDestroy() {
        mapa.destroy()
        tema?.dispose()
        super.onDestroy()
    }

    override fun onPause() {
        pregledmape.onPause()
        stopTajmera()
        super.onPause()
    }

    override fun onResume() {
        if (!stanica.isEmpty())
            runOnUiThread { osvezi.visibility = View.VISIBLE }
        pregledmape.onResume()
        super.onResume()
    }

}
