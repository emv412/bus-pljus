package com.buspljus.Trasa

import android.content.Context
import androidx.core.graphics.drawable.toBitmap
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.buspljus.Interfejs
import com.buspljus.R
import com.buspljus.sifraNaziv
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol

class TrasaRender(
    private val context: Context,
    private val pozivTrasaInterface: Interfejs.novaTrasa? = null
) {

    fun crtanjeTrase(
        brojLinije: MarkerItem,
        koordinateTrase: List<GeoPoint>,
        spisakStajalista: List<sifraNaziv>,
        presedackaSt: String?
    ) {
        val lista = mutableListOf<GeoPoint>()
        val markerListaStajalista = mutableListOf<MarkerItem>()

        for (i in 0 until koordinateTrase.size) {
            lista.add(GeoPoint(koordinateTrase[i].latitude, koordinateTrase[i].longitude))
        }

        for (n in 0 until spisakStajalista.size) {
            val ltlg = spisakStajalista[n].pozicija
            val marker = MarkerItem(null,
                spisakStajalista[n].naziv as String?, null, GeoPoint(ltlg.latitude, ltlg.longitude))
            val iconRes = if (presedackaSt != null && presedackaSt == spisakStajalista[n].sifra) {
                R.drawable.crvena_tacka
            } else {
                R.drawable.plava_tacka
            }
            marker.marker = MarkerSymbol(
                AndroidBitmap(VectorDrawableCompat.create(context.resources, iconRes, context.theme)!!.toBitmap()),
                MarkerSymbol.HotspotPlace.BOTTOM_CENTER,
                true
            )
            markerListaStajalista.add(marker)
        }
        pozivTrasaInterface?.prikazTrase(brojLinije, markerListaStajalista,koordinateTrase, spisakStajalista)
    }
}