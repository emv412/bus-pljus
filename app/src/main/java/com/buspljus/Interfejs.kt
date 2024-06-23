package com.buspljus

import com.buspljus.Adapteri.sifraNaziv
import okhttp3.Response
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
}