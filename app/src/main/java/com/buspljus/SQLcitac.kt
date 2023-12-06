package com.buspljus

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.buspljus.Glavna.Companion.kursor
import org.oscim.core.GeoPoint

class SQLcitac(private val context: Context) : SQLiteOpenHelper(context,IME_BAZE,null,VERZIJA_BAZE) {
    companion object {
        private const val IME_BAZE = "stanice.db"
        private const val VERZIJA_BAZE = 1
        lateinit var komanda : String
        lateinit var pomeranjemapekursor : Cursor
        var niz = arrayOf<String>()
    }

    override fun onCreate(bz: SQLiteDatabase?) {
    }

    override fun onUpgrade(bz: SQLiteDatabase?, p1: Int, p2: Int) {

    }

    fun pozahtevu_jednastanica(sifra: String): GeoPoint {
        val podatak = readableDatabase.rawQuery("select lt,lg from stanice where _id=?", arrayOf(sifra))
        podatak.moveToFirst()
        val ltlg = GeoPoint(podatak.getString(podatak.getColumnIndexOrThrow("lt")).toDouble(),podatak.getString(podatak.getColumnIndexOrThrow("lg")).toDouble())
        podatak.close()
        return ltlg
    }

    fun pretragabaze_kliknamapu(lat: String, lng: String) {
        val preciznost = if (lat.length-3 > lng.length-3){
            lat.length-3
        } else {
            lng.length-3
        }

        for (brojac in preciznost downTo 3) {
            pomeranjemapekursor = readableDatabase.rawQuery("select _id,naziv from stanice where round(lt,?) = round(?, ?) and round(lg,?) = round(?, ?)",
                arrayOf(brojac.toString(),lat,brojac.toString(),brojac.toString(),lng,brojac.toString())
            )

            if (pomeranjemapekursor.count == 1) {
                pomeranjemapekursor.moveToFirst()
                Toster(context).toster(pomeranjemapekursor.getString(pomeranjemapekursor.getColumnIndexOrThrow("naziv"))+", "+
                        pomeranjemapekursor.getString(pomeranjemapekursor.getColumnIndexOrThrow("_id")))
                break
            }
        }

        pomeranjemapekursor.close()
    }

    fun dobavisifre(rec: CharSequence?, trazenjepobroju: Boolean?): Cursor {
        if (trazenjepobroju == true) {
            komanda = "select _id,naziv,staju from stanice where _id like ?"
            niz = arrayOf("$rec%")
        } else {
            komanda = "select _id,naziv,staju from stanice where naziv like ?"
            niz = arrayOf("%$rec%")
        }

        kursor = readableDatabase.rawQuery(komanda,niz)
        return kursor
    }

}