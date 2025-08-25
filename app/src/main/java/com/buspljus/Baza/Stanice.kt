package com.buspljus.Baza

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.core.database.sqlite.transaction
import org.oscim.core.GeoPoint

class Stanice(context: Context) : UpitUBazu(context) {

    fun idStaniceuGeoPoint(sifra: String): GeoPoint {
        return queryAndProcess(
            PosrednikBaze.STANICE_TABLE,
            arrayOf(PosrednikBaze.LATITUDE, PosrednikBaze.LONGITUDE),
            "${PosrednikBaze.ID_KOLONA} = ?",
            arrayOf(sifra)
        ) { c ->
            if (c.moveToFirst()) {
                GeoPoint(
                    c.getDouble(c.getColumnIndexOrThrow(PosrednikBaze.LATITUDE)),
                    c.getDouble(c.getColumnIndexOrThrow(PosrednikBaze.LONGITUDE))
                )
            } else {
                GeoPoint(0.0, 0.0)
            }
        }
    }

    fun idStaniceUNaziv(sifra: String): String {
        return queryAndProcess(
            PosrednikBaze.STANICE_TABLE,
            arrayOf(PosrednikBaze.CIR_KOLONA),
            "${PosrednikBaze.ID_KOLONA} = ?",
            arrayOf(sifra)
        ) { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.CIR_KOLONA)) else ""
        }
    }

    fun dobavisifre(rec: CharSequence?, trazenjepobroju: Boolean): Cursor {
        val selection: String
        val args: Array<String>

        if (trazenjepobroju) {
            selection = "${PosrednikBaze.ID_KOLONA} LIKE ?"
            args = arrayOf("$rec%")
        } else {
            selection = "${PosrednikBaze.ASCII_KOLONA} LIKE ? OR ${PosrednikBaze.CIR_KOLONA} LIKE ? OR ${PosrednikBaze.LAT_KOLONA} LIKE ?"
            args = arrayOf("%$rec%", "%$rec%", "%$rec%")
        }

        return SQLzahtev(
            PosrednikBaze.STANICE_TABLE,
            arrayOf(PosrednikBaze.ID_KOLONA, PosrednikBaze.CIR_KOLONA, PosrednikBaze.STAJU, PosrednikBaze.SACUVANA),
            selection,
            args,
            "${PosrednikBaze.SACUVANA} DESC LIMIT 50"
        )
    }

    fun dobaviSacuvaneStanice(): List<String> {
        return queryAndProcess(
            PosrednikBaze.STANICE_TABLE,
            arrayOf(PosrednikBaze.ID_KOLONA),
            "${PosrednikBaze.SACUVANA} = 1",
            emptyArray()
        ) { cursor ->
            mutableListOf<String>().apply {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.ID_KOLONA)))
                }
            }
        }
    }

    fun sacuvajStanicu(stanice: List<String>, cuvanjeilibrisanje: Int) {
        baza.transaction {
            try {
                stanice.forEach { stanica ->
                    val values = ContentValues().apply {
                        put(PosrednikBaze.SACUVANA, cuvanjeilibrisanje)
                    }
                    update(
                        PosrednikBaze.STANICE_TABLE,
                        values,
                        "${PosrednikBaze.ID_KOLONA} = ?",
                        arrayOf(stanica)
                    )
                }
            } finally {
            }
        }
    }
}