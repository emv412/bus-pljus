package com.buspljus

import com.buspljus.Adapteri.sifraNaziv
import okhttp3.Response
import org.oscim.core.GeoPoint
import java.io.IOException

class Interfejs {
    interface trasa {
        fun prikazTrase(linijarv: String, listasifraNaziv: MutableList<sifraNaziv>)
    }

    interface odgovorSaInterneta {
        fun uspesanOdgovor(response: Response)
        fun neuspesanOdgovor(e: IOException)
    }

    interface Callback {
        fun korak(s: String)
        fun koloneBGVOZ(lista: List<Any>)
    }

    interface specMarker {
        fun crtanjespecMarkera(id : String, g: List<GeoPoint>)
    }

    interface odgovor {
        fun da(odg: Boolean)
    }

    interface upitSlSt {
        fun promenikursor(odg: String)
    }

    interface vracenaLista {
        fun vratiListu(lista: List<String>)
    }
}