package com.buspljus

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.oscim.core.GeoPoint

class SQLcitac(private val context: Context) : SQLiteOpenHelper(context,IME_BAZE,null,VERZIJA_BAZE) {
    companion object {
        private const val IME_BAZE = "stanice.db"
        private const val VERZIJA_BAZE = 1
        val pronadjeneStanice = mutableListOf<String>()
    }
    lateinit var kursor : Cursor
    lateinit var str : String

    interface Callback {
        fun korak1(s: String)
    }
    override fun onCreate(bz: SQLiteDatabase?) {
    }

    override fun onUpgrade(bz: SQLiteDatabase?, p1: Int, p2: Int) {

    }

    fun SQLzahtev(rad: Int, niz: Array<String>): Cursor {
        when (rad) {
            0 -> str = "select lt,lg from stanice where _id=?"
            1 -> str = "select _id,naziv from stanice where round(lt,?) = round(?, ?) and round(lg,?) = round(?, ?)"
            2 -> str = "select _id,naziv,staju from stanice where _id like ?"
            3 -> str = "select _id,naziv,staju from stanice where naziv like ?"
        }
        return readableDatabase.rawQuery(str,niz)
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
            SQLzahtev(3,arrayOf("%$rec%"))
        }
        return kursor
    }
}