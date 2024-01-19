package com.buspljus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.preference.PreferenceManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.Response
import org.json.JSONArray
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
import org.oscim.layers.marker.MarkerRenderer
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import kotlin.concurrent.schedule

class Glavna : AppCompatActivity(),ItemizedLayer.OnItemGestureListener<MarkerInterface> {

    private lateinit var odabranoStajalisteSloj: ItemizedLayer
    private lateinit var pozicijaPesakaSloj: ItemizedLayer
    private lateinit var markeriVozila: ItemizedLayer
    private lateinit var redvoznje_prozor: ItemizedLayer

    private lateinit var odabranoStajalisteMarker: MarkerItem
    private lateinit var pozicijaPesakaMarker: MarkerItem

    private lateinit var polje: AutoCompleteTextView
    private lateinit var podesavanje: ImageButton
    private lateinit var ucitavanje: ProgressBar
    private lateinit var lista: ListView
    private lateinit var osvezi: ImageButton
    private lateinit var pregledmape: MapView

    private lateinit var gpsdugme: FloatingActionButton

    lateinit var menadzerLokacije: LocationManager

    private var markersimbol: MarkerSymbol? = null
    private val markeriVozilaSpisak: MutableList<MarkerInterface> = ArrayList()

    private var tastaturasklonjena: Boolean = true
    private var kliknalistu: Boolean = false
    private var boja = 1

    private var najbliziAutobusRastojanje = 100000.0
    private lateinit var najbliziAutobusMarker: MarkerItem

    var lociranje = false
    var slobodnopomeranjemape = true
    var primljeniString = ""
    var stanica_id: String = ""
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

        val fromColumns = arrayOf("_id", SQLcitac.CIR_KOLONA, "staju")
        val toViews = intArrayOf(R.id.sifra_stanice, R.id.naziv_stanice, R.id.odredista_sa_stanice)
        adapter = SimpleCursorAdapter(this, R.layout.probna_lista, null, fromColumns, toViews, 0)
        adapter.viewBinder = Selektor()

        menadzerLokacije = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val gpsLok = menadzerLokacije.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val mreznaLok = menadzerLokacije.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        }

        val promenaunosa = findViewById<Button>(R.id.promenaunosa)

        gpsdugme.setOnClickListener{
            if (lociranje) {
                lociranje = false
                gpsdugme.backgroundTintList=AppCompatResources.getColorStateList(this, R.color.siva_boja)
                slobodnopomeranjemape = true
                pozicijaPesakaSloj.removeAllItems()
            }
            else {
                lociranje = true
                gpsdugme.backgroundTintList=AppCompatResources.getColorStateList(this, R.color.tramvaj)
                slobodnopomeranjemape = false
            }

            pozicijaPesakaSloj.removeAllItems()

            when {
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                    if (gpsLok) {
                        menadzerLokacije.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0F, object: LocationListener{
                            override fun onLocationChanged(location: Location) {
                                pozicijaPesakaMarker.geoPoint=GeoPoint(location.latitude,location.longitude)
                                if (!slobodnopomeranjemape)
                                    mapa.setMapPosition(location.latitude,location.longitude,70000.0)

                                pozicijaPesakaSloj.addItem(pozicijaPesakaMarker)

                                if (!lociranje)
                                    menadzerLokacije.removeUpdates(this)
                                mapa.updateMap()
                            }
                        })
                    }
                }

                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) -> {
                    Toster(this).toster(resources.getString(R.string.nije_omoguceno_lociranje))
                }

                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        podesavanje.setOnClickListener {
            startActivity(Intent(this, Podesavanja::class.java))
        }

        osvezi.setOnClickListener {
            ukucanastanica(
                this@Glavna,
                stanica_id,
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
                        stanica_id = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow("_id"))
                        stanica_naziv = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow(SQLcitac.CIR_KOLONA))
                        ukucanastanica(this@Glavna, stanica_id, odabranoStajalisteSloj, odabranoStajalisteMarker, true)
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
            stanica_id = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow("_id"))
            stanica_naziv = adapter.cursor.getString(adapter.cursor.getColumnIndexOrThrow(SQLcitac.CIR_KOLONA))
            if (!trazenjepobroju)
                promenaunosa.callOnClick()
            kliknalistu = true
            ukucanastanica(this@Glavna, stanica_id, odabranoStajalisteSloj, odabranoStajalisteMarker, true)
            podesiNaziv()
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
                                stanica_id = s
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
            redvoznje_prozor = ItemizedLayer(mapa,markersimbol)

            odabranoStajalisteSloj = ItemizedLayer(mapa, markersimbol)
            odabranoStajalisteMarker = MarkerItem(null, null, null, null)
            pozicijaPesakaSloj = ItemizedLayer(mapa,markersimbol)
            pozicijaPesakaMarker = MarkerItem(null,null,null,null)

            pozicijaPesakaMarker.marker=MarkerSymbol(
                AndroidBitmap(VectorDrawableCompat.create(
                    resources,R.drawable.glisa,theme)!!.toBitmap()),
                MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true)

            mapa.layers().add(BitmapTileLayer(mapa, tileSource))
            mapa.layers().add(BuildingLayer(mapa, tileLayer))
            mapa.layers().add(LabelLayer(mapa, tileLayer))
            mapa.layers().add(klikNaMapu())
            mapa.layers().add(markeriVozila)
            mapa.layers().add(odabranoStajalisteSloj)
            mapa.layers().add(redvoznje_prozor)
            mapa.layers().add(pozicijaPesakaSloj)

            izbacitastaturu()

            mapa.events.bind(Map.UpdateListener { e, mapPosition ->

                pozicija = mapPosition

                if (e == Map.ANIM_END) {
                    if (!tastaturasklonjena)
                        sklonitastaturu()
                    slobodnopomeranjemape = true
                }
            })

            mapa.setTheme(AssetsRenderTheme(assets, "", "osmarender.xml"))
            mapa.setMapPosition(44.821, 20.471, 2500.0)

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
        /*
        val rastojanjedostanice = markerItem.geoPoint.sphericalDistance(
            GeoPoint(
                odabranoStajalisteMarker.geoPoint.latitude,
                odabranoStajalisteMarker.geoPoint.longitude
            )
        ).div(1000)
        val rastojanjeprer = BigDecimal(rastojanjedostanice).setScale(1, RoundingMode.HALF_EVEN).toString() + " km"

        val kr = SQLcitac(this@Glavna).redvoznjeKliknavozilo(markerItem.title,stanica_id)
        if (kr.count > 0) {
            val trenutnovreme = LocalTime.now()

            val danunedelji = when (LocalDate.now().dayOfWeek.value) {
                    in 1 .. 5 -> 0
                    6 -> 1
                    7 -> 2
                    else -> 0
                }

            val dialog = BottomSheetDialog(this@Glavna)
            dialog.setContentView(R.layout.prozor_redvoznje)

            val linijarv = dialog.findViewById<TextView>(R.id.linija_rv)
            val linijarel = dialog.findViewById<TextView>(R.id.linija_relacija)
            val garBroj = dialog.findViewById<TextView>(R.id.gb_redv)
            val rastojanje = dialog.findViewById<TextView>(R.id.rastojanje)

            kr.moveToFirst()
            try {
                var relacijaLinije = kr.getString(kr.getColumnIndexOrThrow("od"))+" - "+kr.getString(kr.getColumnIndexOrThrow("do"))
                val polasci = JSONObject(kr.getString(kr.getColumnIndexOrThrow("redvoznje")))

                val okretnica = JSONArray(kr.getString(kr.getColumnIndexOrThrow("stajalista"))).get(0).toString()

                if (okretnica == stanica_id)
                    relacijaLinije = kr.getString(kr.getColumnIndexOrThrow("do"))+" - "+kr.getString(kr.getColumnIndexOrThrow("od"))

                if (SQLcitac(this).pozahtevu_jednastanica(okretnica).sphericalDistance(markerItem.geoPoint) < 150) {
                    val sati = polasci.getJSONObject("rv")
                    val sati_k = sati.keys()
                    var brojacDvaPolaska = 0
                    var dobijenoVreme: String

                    val prvipol = dialog.findViewById<TextView>(R.id.prvipolazak)
                    val drugipol = dialog.findViewById<TextView>(R.id.drugipolazak)
                    val redvoznje = dialog.findViewById<TextView>(R.id.redvoznje)
                    val datum_rv = dialog.findViewById<TextView>(R.id.datum_rv)
                    val sledeciPolasci = dialog.findViewById<TextView>(R.id.polasci_textview)
                    val danunedelji_textview = dialog.findViewById<TextView>(R.id.rdsn)
                    val prethodnipol = dialog.findViewById<TextView>(R.id.prethodnipol)

                    prvipol?.visibility=View.VISIBLE
                    drugipol?.visibility=View.VISIBLE
                    redvoznje?.visibility=View.VISIBLE
                    sledeciPolasci?.visibility=View.VISIBLE
                    datum_rv?.visibility=View.VISIBLE
                    prethodnipol?.visibility=View.VISIBLE
                    danunedelji_textview?.visibility=View.VISIBLE

                    when (danunedelji) {
                        0 -> danunedelji_textview?.text="радни дан"
                        1 -> danunedelji_textview?.text="субота"
                        2 -> danunedelji_textview?.text="недеља"
                        else -> {}
                    }

                    with (polasci.getJSONArray("datum")) {
                        val datumRedaVoznje = this.getString(0)+". "+this.getString(1)+". "+this.getString(2)
                        datum_rv?.text=datumRedaVoznje
                    }

                    while (sati_k.hasNext()) {
                        val sat = sati_k.next()
                        for (k in 0 .. sati.getJSONArray(sat).getJSONArray(danunedelji).length()-1) {
                            dobijenoVreme = sat+":"+sati.getJSONArray(sat).getJSONArray(danunedelji)[k]
                            if (LocalTime.parse(dobijenoVreme,DateTimeFormatter.ofPattern("HH:mm")).isBefore(trenutnovreme) and (brojacDvaPolaska == 0)) {
                                prethodnipol?.text=dobijenoVreme
                            }
                            else if (LocalTime.parse(dobijenoVreme,DateTimeFormatter.ofPattern("HH:mm")).isAfter(trenutnovreme) and (brojacDvaPolaska < 2)) {
                                if (brojacDvaPolaska == 0)
                                    prvipol?.text=dobijenoVreme
                                else if (brojacDvaPolaska == 1)
                                    drugipol?.text=dobijenoVreme
                                brojacDvaPolaska += 1
                            }
                        }
                    }
                }

                linijarv?.text=markerItem.title
                linijarel?.text=relacijaLinije
                garBroj?.text=markerItem.description
                rastojanje?.text=rastojanjeprer

                dialog.show()
            }
            catch(e:Exception) {
                Log.d("DEBAG",""+e)
            }
        }
        return true

         */
        RedVoznje(this@Glavna).redvoznjeKliknaVozilo(markerItem, odabranoStajalisteMarker, stanica_id)
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
        Internet().zahtevPremaInternetu(stanica_id, 1, object : Internet.ApiResponseCallback {
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
            polje.hint=stanica_id
        else
            polje.hint=StringBuilder(stanica_id + " / " + stanica_naziv)
        polje.text.clear()
        polje.clearFocus()
    }
    private fun stopTajmera() {
        if (Internet.zahtev?.isExecuted() == true)
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
        if (stanica_id.isNotEmpty())
            dugmezaosvezavanje(0, 1)
        super.onResume()
    }
}
