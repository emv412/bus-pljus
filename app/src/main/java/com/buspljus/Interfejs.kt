package com.buspljus

import okhttp3.Response
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import java.io.IOException
import java.time.LocalTime


class Interfejs {
    interface novaTrasa {
        fun prikazTrase(voziloMarker: MarkerItem, listaStajalista: List<MarkerItem>, listaTacakaTrase: List<GeoPoint>, listaSifraNaziv: List<sifraNaziv>)
    }

    interface odgovorSaInterneta {
        fun uspesanOdgovor(response: Response)
        fun neuspesanOdgovor(e: IOException)
    }

    interface MapTapListener {
        fun podesiTextView(idStanice: String, nazivStanice: String)
        fun koloneBGVOZ(lista: List<Any>)
    }

    interface specMarker {
        fun crtanjespecMarkera(id : String, smer: String, g: List<Pair<GeoPoint, LocalTime>>)
    }

    interface odgovor {
        fun da(odg: Boolean)
    }

    interface vracenaLista {
        fun vratiListu(lista: List<String>)
    }
}