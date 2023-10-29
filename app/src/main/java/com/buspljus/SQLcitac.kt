package com.buspljus

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.buspljus.Glavna.Companion.kursor
import org.oscim.android.canvas.AndroidBitmap
import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol

class SQLcitac(context: Context) : SQLiteOpenHelper(context,IME_BAZE,null,VERZIJA_BAZE) {
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

    fun pozahtevu(sifra: String): GeoPoint {
        val podatak = readableDatabase.rawQuery("select lt,lg from stanice where _id=?", arrayOf(sifra))
        podatak.moveToFirst()
        val ltlg = GeoPoint(podatak.getString(podatak.getColumnIndexOrThrow("lt")).toDouble(),podatak.getString(podatak.getColumnIndexOrThrow("lg")).toDouble())
        podatak.close()
        return ltlg
    }

    fun prikazstanicapopomeranjumape(minlt: String, maxlt: String, minlg: String, maxlg: String): ArrayList<MarkerItem> {
        val rezultat = MarkerItem(null,null,null)
        val lista = arrayListOf(rezultat)
        pomeranjemapekursor = readableDatabase.rawQuery("select * from stanice where lt>? and lt<? and lg>? and lg<?",
            arrayOf(minlt,maxlt,minlg,maxlg)
        )

        while (pomeranjemapekursor.moveToNext()) {
            rezultat.title=pomeranjemapekursor.getString(pomeranjemapekursor.getColumnIndexOrThrow("_id"))
            rezultat.geoPoint=GeoPoint(pomeranjemapekursor.getString(pomeranjemapekursor.getColumnIndexOrThrow("lt")).toDouble(),
                pomeranjemapekursor.getString(pomeranjemapekursor.getColumnIndexOrThrow("lg")).toDouble())
            lista.add(rezultat)
        }

        return lista


    }

    fun dobavisifre(rec: CharSequence?, trazenjepobroju: Boolean?): Cursor {
        if (trazenjepobroju == true) {
            komanda = "select _id,naziv from stanice where _id like ?"
            niz = arrayOf("$rec%")
        } else {
            komanda = "select _id,naziv from stanice where naziv like ?"
            niz = arrayOf("%$rec%")
        }

        kursor = readableDatabase.rawQuery(komanda,niz)
        return kursor
    }
}