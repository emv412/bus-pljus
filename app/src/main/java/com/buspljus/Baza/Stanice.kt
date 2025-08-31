package com.buspljus.Baza

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.buspljus.Baza.PosrednikBaze.Companion.globalQueryData
import com.buspljus.LineInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

    fun dobaviSifre(rec: CharSequence?, trazenjepobroju: Boolean): Cursor {
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
            arrayOf(PosrednikBaze.ID_KOLONA, PosrednikBaze.CIR_KOLONA, PosrednikBaze.SACUVANA),
            selection,
            args,
            "${PosrednikBaze.SACUVANA} DESC LIMIT 50"
        )
    }

    fun sacuvaneStaniceCursor(filterId: String? = null): Cursor {
        val selection = buildString {
            append("${PosrednikBaze.SACUVANA} = 1")
            if (filterId != null) append(" AND ${PosrednikBaze.ID_KOLONA} = ?")
        }
        val selectionArgs = if (filterId != null) arrayOf(filterId) else emptyArray()
        
        return SQLzahtev(
            PosrednikBaze.STANICE_TABLE,
            arrayOf(PosrednikBaze.ID_KOLONA, PosrednikBaze.CIR_KOLONA, PosrednikBaze.SACUVANA),
            selection,
            selectionArgs,
            null
        )
    }

    fun dobaviSacuvaneStanice(filterId: String? = null): Map<String, Int> {
        val cursor = sacuvaneStaniceCursor(filterId)
        return buildMap {
            cursor.use {
                while (it.moveToNext()) {
                    put(
                        it.getString(it.getColumnIndexOrThrow(PosrednikBaze.ID_KOLONA)),
                        it.getInt(it.getColumnIndexOrThrow(PosrednikBaze.SACUVANA))
                    )
                }
            }
        }
    }

    fun proveriSacuvanuStanicu(id: String): Int? {
        return dobaviSacuvaneStanice(id)[id]
    }

    fun ponoviUpit(): Cursor {
        val q = globalQueryData
            ?: throw IllegalStateException("No previous query to repeat")
        return super.SQLzahtev(q.tabela, q.kolone, q.odabir, q.parametri, q.redjanjepo)
    }

    fun prikazLinijaNaStanici(stanica: String): Map<String, String> {
        val selection = "${PosrednikBaze.LISTA_STAJALISTA} LIKE ?"
        val args = arrayOf("%\"$stanica\"%")

        return queryAndProcess(
            PosrednikBaze.LINIJE_TABLE,
            arrayOf(PosrednikBaze.ID_KOLONA, PosrednikBaze.OKRETNICA_DO),
            selection,
            args,
            null
        ) { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.ID_KOLONA))
                    val doStanice = cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.OKRETNICA_DO))
                    put(id, doStanice)
                }
            }
        }
    }

    fun ucitajLinijePoStanicama(): Map<String, List<LineInfo>> {
        val result = mutableMapOf<String, MutableList<LineInfo>>()

        queryAndProcess(
            PosrednikBaze.LINIJE_TABLE,
            arrayOf(PosrednikBaze.ID_KOLONA, PosrednikBaze.OKRETNICA_DO, PosrednikBaze.LISTA_STAJALISTA),
            null,
            null
        ) { cursor ->
            while (cursor.moveToNext()) {
                val lineId = cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.ID_KOLONA))
                val destination = cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.OKRETNICA_DO))
                val stajalista = cursor.getString(cursor.getColumnIndexOrThrow(PosrednikBaze.LISTA_STAJALISTA))

                val stationIds = extractIdsFromList(stajalista)
                val info = LineInfo(lineId, destination)

                stationIds.forEach { stationId ->
                    result.getOrPut(stationId) { mutableListOf() }.add(info)
                }
            }
        }

        return result
    }


    fun extractIdsFromList(raw: String): List<String> {
        return try {
            val jsonArray = JSONArray(raw)
            List(jsonArray.length()) { index -> jsonArray.getString(index) }
        } catch (e: Exception) {
            emptyList()
        }
    }


    suspend fun sacuvajStanicu(stanica: String, cuvanje: Int) =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(PosrednikBaze.SACUVANA, cuvanje)
            }
            upisUBazu(PosrednikBaze.STANICE_TABLE,values,"${PosrednikBaze.ID_KOLONA} = ?",arrayOf(stanica))
        }
}