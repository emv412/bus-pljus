package com.buspljus.Vozilo

import android.content.ContentValues
import android.database.Cursor
import com.buspljus.Baza.PosrednikBaze.Companion.ID_KOLONA
import com.buspljus.Baza.PosrednikBaze.Companion.IZMENJENALINIJA_BOOLEAN
import com.buspljus.Baza.PosrednikBaze.Companion.IZMENJENATRASA
import com.buspljus.Baza.PosrednikBaze.Companion.LINIJE_TABLE
import com.buspljus.Baza.PosrednikBaze.Companion.LISTA_STAJALISTA
import com.buspljus.Baza.PosrednikBaze.Companion.RED_VOZNJE
import com.buspljus.Baza.PosrednikBaze.Companion.SMER
import com.buspljus.Baza.PosrednikBaze.Companion.TRASA
import com.buspljus.Baza.UpitUBazu

class VoziloRepository(private val pomocnik: UpitUBazu)  {

    fun getLinijaBySmer(linija: String, smer: String): Cursor =
        pomocnik.SQLzahtev(LINIJE_TABLE, arrayOf("*"), "$ID_KOLONA = ? and $SMER = ?", arrayOf(linija, smer), null)

    fun getLinijaByStanica(linija: String, stanica: String): Cursor =
        pomocnik.SQLzahtev(LINIJE_TABLE, arrayOf("*"), "$ID_KOLONA = ? and $LISTA_STAJALISTA like ?", arrayOf(linija, "%\"$stanica\"%"), null)

    fun getLinijaByListaStajalista(linija: String, lista: String): Cursor =
        pomocnik.SQLzahtev(LINIJE_TABLE, arrayOf("*"), "$ID_KOLONA = ? and $LISTA_STAJALISTA = ?", arrayOf(linija, lista), null)

    fun updateListaStajalista(linija: String, smer: String, novaLista: String): Int {
        val vrednosti = ContentValues().apply {
            put(LISTA_STAJALISTA, novaLista)
        }
        return pomocnik.upisUBazu(LINIJE_TABLE, vrednosti, "$ID_KOLONA = ? and $SMER = ?", arrayOf(linija, smer))
    }

    fun dobaviKursorZaNeprijavljenoVozilo(sifraStajalista: String): Cursor =
        pomocnik.SQLzahtev(LINIJE_TABLE, arrayOf(ID_KOLONA, SMER, IZMENJENALINIJA_BOOLEAN, TRASA, IZMENJENATRASA, RED_VOZNJE), "$LISTA_STAJALISTA like ?", arrayOf("%\"$sifraStajalista\"%"), null)
    }
