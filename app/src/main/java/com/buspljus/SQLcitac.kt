package com.buspljus

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.oscim.core.GeoPoint
import java.io.File

class SQLcitac(private val context: Context) {
    companion object {
        const val IME_BAZE = "svi_podaci.db"
        const val CIR_KOLONA = "naziv_cir"
        val pronadjeneStanice = mutableListOf<String>()
        lateinit var kursor : Cursor
        lateinit var baza : SQLiteDatabase
        lateinit var str : String
    }

    interface Callback {
        fun korak(s: String)
    }

    fun SQLzahtev(rad: Int, niz: Array<String>): Cursor {
        baza = SQLiteDatabase.openDatabase(File(context.getDatabasePath(IME_BAZE).absolutePath).toString(),null,0)
        when (rad) {
            0 -> str = "select lt,lg from stanice where _id=?"
            1 -> str = "select _id,naziv_cir from stanice where round(lt,?) = round(?, ?) and round(lg,?) = round(?, ?)"
            2 -> str = "select _id,naziv_cir,staju from stanice where _id like ?"
            3 -> str = "select _id,naziv_cir,staju from stanice where naziv_ascii like" +
                    "? or naziv_cir like ? or naziv_lat like ?"
            4 -> str = "select _id, od, do, stajalista, redvoznje from linije where _id=? and stajalista like ?"
        }
        return baza.rawQuery(str,niz)
    }

    fun pozahtevu_jednastanica(sifra: String): GeoPoint {
        kursor = SQLzahtev(0, arrayOf(sifra))
        kursor.moveToFirst()
        val ltlg = GeoPoint(kursor.getString(kursor.getColumnIndexOrThrow("lt")).toDouble(),
            kursor.getString(kursor.getColumnIndexOrThrow("lg")).toDouble())
        kursor.close()
        return ltlg
    }

    fun pretragabaze_kliknamapu(lat: String, lng: String, callback: Callback) {
        val preciznost = if (lat.length-3 > lng.length-3){
            lat.length-3
        } else {
            lng.length-3
        }

        pronadjeneStanice.clear()

        for (brojac in preciznost downTo 3) {
            kursor = SQLzahtev(1,arrayOf(brojac.toString(),lat,brojac.toString(),brojac.toString(),lng,brojac.toString()))

            if (kursor.count > 0) {
                while (kursor.moveToNext()) {
                    pronadjeneStanice.add(kursor.getString(kursor.getColumnIndexOrThrow("_id")))
                }
                AlertDialog(context).pronadjeneStaniceAlertDialog(callback)
                break
            }
        }
        kursor.close()
    }

    fun dobavisifre(rec: CharSequence?, trazenjepobroju: Boolean?): Cursor {
        kursor = if (trazenjepobroju == true) {
            SQLzahtev(2,arrayOf("$rec%"))
        } else {
            SQLzahtev(3,arrayOf("%$rec%","%$rec%","%$rec%"))
        }
        return kursor
    }

    fun redvoznjeKliknavozilo(linija: String, stanica: String): Cursor {
        kursor = SQLzahtev(4, arrayOf(linija,"%\"$stanica\"%"))
        return kursor
    }
}