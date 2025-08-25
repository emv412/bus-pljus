package com.buspljus

import com.buspljus.Adapteri.sifraNaziv
import okhttp3.Response
import org.oscim.core.GeoPoint
import java.io.IOException
import java.time.LocalTime


class Interfejs {
    interface trasa {
        fun prikazTrase(linijarv: String, listasifraNaziv: MutableList<sifraNaziv>)
    }

    interface odgovorSaInterneta {
        fun uspesanOdgovor(response: Response)
        fun neuspesanOdgovor(e: IOException)
    }

    interface Callback {
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