package com.buspljus.Baza

import android.content.Context
import com.buspljus.Baza.PosrednikBaze.Companion.ID_KOLONA
import com.buspljus.Baza.PosrednikBaze.Companion.IZMENJENALINIJA_BOOLEAN
import com.buspljus.Baza.PosrednikBaze.Companion.IZMENJENATRASA
import com.buspljus.Baza.PosrednikBaze.Companion.LINIJE_TABLE
import com.buspljus.Baza.PosrednikBaze.Companion.LISTA_STAJALISTA
import com.buspljus.Baza.PosrednikBaze.Companion.RED_VOZNJE
import com.buspljus.Baza.PosrednikBaze.Companion.SMER
import com.buspljus.Baza.PosrednikBaze.Companion.TRASA
import com.buspljus.Interfejs
import com.buspljus.IzracunavanjeVremena
import com.buspljus.PopupProzor
import org.json.JSONArray
import org.oscim.core.GeoPoint

class Linije(context: Context) : UpitUBazu(context) {

    fun sveLinije(pretraga: String): List<String> {
        val (upit, niz_upit) = if (pretraga.isNotEmpty()) {
            "${SMER} = ? and ${ID_KOLONA} like ?" to arrayOf("0", "$pretraga%")
        } else {
            "${SMER} = ?" to arrayOf("0")
        }

        return queryAndProcess(
            LINIJE_TABLE,
            arrayOf(ID_KOLONA),
            upit,
            niz_upit,
            ID_KOLONA
        ) { cursor ->
            mutableListOf<String>().apply {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow(ID_KOLONA)))
                }
            }
        }
    }

    fun redVoznjeJednaLinija(linija: String, smer: String): List<Any> {
        return queryAndProcess(
            LINIJE_TABLE,
            arrayOf("*"),
            "${ID_KOLONA} = ? and ${SMER} = ?",
            arrayOf(linija, smer)
        ) { cursor ->
            mutableListOf<Any>().apply {
                while (cursor.moveToNext()) {
                    with(this) {
                        add(cursor.getString(cursor.getColumnIndexOrThrow(ID_KOLONA)))
                        add(cursor.getInt(cursor.getColumnIndexOrThrow(SMER)))
                        add(cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.OKRETNICA_OD)))
                        add(cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.OKRETNICA_DO)))
                        add(cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.DATUM_RV)))
                        add(JSONArray(cursor.getString(cursor.getColumnIndexOrThrow(LISTA_STAJALISTA))))
                        add(cursor.getString(cursor.getColumnIndexOrThrow(RED_VOZNJE)))
                    }
                }
            }
        }
    }

    fun neprijavljenoVozilo(sifraStajalista: String, ifejs: Interfejs.specMarker) {
        try {
            val kursor = SQLzahtev(LINIJE_TABLE, arrayOf(ID_KOLONA, SMER, IZMENJENALINIJA_BOOLEAN, TRASA, IZMENJENATRASA, RED_VOZNJE), "$LISTA_STAJALISTA like ?", arrayOf("%\"$sifraStajalista\"%"), null)
            with (kursor) {
                if (kursor.count > 0) {
                    use {
                        while (moveToNext()) {
                            val id = getString(getColumnIndexOrThrow(ID_KOLONA))
                            val smer = getString(getColumnIndexOrThrow(SMER))
                            val izmenjenaLN = getString(getColumnIndexOrThrow(IZMENJENALINIJA_BOOLEAN))
                            val trasa = getBlob(getColumnIndexOrThrow((if (izmenjenaLN == "0") TRASA else IZMENJENATRASA)))
                            val rv = getString(getColumnIndexOrThrow(RED_VOZNJE))
                            ifejs.crtanjespecMarkera(id, smer, IzracunavanjeVremena().tranziranjeRV(rv, unzip(trasa)))
                        }
                    }
                }
            }
        }
        catch (g:Exception) {
            PopupProzor(context).prikaziGresku(g)
        }
    }

    fun prikaziTrasu(linija: String, sifraStajalista: String, smer: String?): Triple<JSONArray, JSONArray, MutableList<GeoPoint>> {
        var trasaUnzip = ""
        var jsonIDStanice : JSONArray? = null
        val jsonGeoPointStanice = mutableListOf<GeoPoint>()
        val nizZahtev: Array<String>
        val selekcioniZahtev : String

        if (smer != null) {
            nizZahtev = arrayOf(linija, "%\"$sifraStajalista\"%", smer)
            selekcioniZahtev = "$ID_KOLONA = ? and $LISTA_STAJALISTA like ? and smer = ?"
        }
        else {
            nizZahtev = arrayOf(linija, "%\"$sifraStajalista\"%")
            selekcioniZahtev = "$ID_KOLONA = ? and $LISTA_STAJALISTA like ?"
        }


        val kursor = SQLzahtev(LINIJE_TABLE, arrayOf(LISTA_STAJALISTA, IZMENJENALINIJA_BOOLEAN, TRASA, IZMENJENATRASA), selekcioniZahtev, nizZahtev, null)
        with (kursor) {
            if (count > 0) {
                use {
                    moveToFirst()

                    jsonIDStanice = JSONArray(getString(getColumnIndexOrThrow(LISTA_STAJALISTA)))
                    for (n in 0 until jsonIDStanice!!.length()) {
                        jsonGeoPointStanice.add(PosrednikBaze(context).idStaniceuGeoPoint(jsonIDStanice!![n].toString()))
                    }

                    val izmena = getString(getColumnIndexOrThrow(IZMENJENALINIJA_BOOLEAN))
                    val trasa = getBlob(getColumnIndexOrThrow(if (izmena == "0") TRASA else IZMENJENATRASA))
                    trasaUnzip = unzip(trasa)
                }
            }
        }

        return Triple(jsonIDStanice!!, JSONArray(trasaUnzip), jsonGeoPointStanice)
    }
}