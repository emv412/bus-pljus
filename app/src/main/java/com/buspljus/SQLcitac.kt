package com.buspljus

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.oscim.core.GeoPoint
import java.io.IOException

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
        baza = SQLiteDatabase.openDatabase(context.getDatabasePath(IME_BAZE).absolutePath, null, 0)
        when (rad) {
            0 -> str = "select lt,lg from stanice where _id=?"
            1 -> str = "select _id,naziv_cir from stanice where round(lt,?) = round(?,?) and round(lg,?) = round(?,?)"
            2 -> str = "select _id,naziv_cir,staju from stanice where _id like ?"
            3 -> str = "select _id,naziv_cir,staju from stanice where naziv_ascii like" + "? or naziv_cir like ? or naziv_lat like ?"
            4 -> str = "select * from linije where _id=? and stajalista like ?"
            5 -> str = "select _id,naziv,redvoznje from bgvoz where round(lt,?) = round(?,?) and round(lg,?) = round(?,?)"
            6 -> str = "select naziv from bgvoz where _id=?"
            7 -> str = "select * from praznici"
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
        val preciznost = if (lat.length-3 > lng.length-3) {
            lat.length-3
        } else {
            lng.length-3
        }

        pronadjeneStanice.clear()
        for (brojac in preciznost downTo 3) {
            val tacka = arrayOf(brojac.toString(),lat,brojac.toString(),brojac.toString(),lng,brojac.toString())

            kursor = SQLzahtev(5,tacka)
            if (kursor.count > 0) {
                RedVoznje(context).redVoznjeKliknaStanicu(kursor)
                break
            }

            if (Glavna.mapa.mapPosition.zoomLevel>=16) {
                kursor = SQLzahtev(1,tacka)

                if (kursor.count > 0) {
                    while (kursor.moveToNext()) {
                        pronadjeneStanice.add(kursor.getString(kursor.getColumnIndexOrThrow("_id")))
                    }
                    AlertDialog(context).pronadjeneStaniceAlertDialog(callback)
                    break
                }
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
        if (kursor.count == 0) {
            Toster(context).toster("Linija nema azurirana stajalista u bazi")
            Internet().zahtevPremaInternetu(stanica,linija,1, object : Internet.odgovorSaInterneta{
                override fun uspesanOdgovor(response: Response) {
                    try {
                        val jsonOdOdgovora = JSONArray(response.body!!.string())
                        spoljnapetlja@ for (i in 0 until jsonOdOdgovora.length()) {
                            val josjedankursor = SQLzahtev(4, arrayOf(linija,"%\"${jsonOdOdgovora.getString(i)}\"%"))
                            if (josjedankursor.count == 1) {
                                josjedankursor.moveToFirst()
                                val nizStanicaUBazi = JSONArray(josjedankursor.getString(josjedankursor.getColumnIndexOrThrow("stajalista")))
                                for (n in 0 until nizStanicaUBazi.length()) {
                                    if (jsonOdOdgovora[i] == nizStanicaUBazi[n]) {
                                        val smerKretanjaLinije = josjedankursor.getString(josjedankursor.getColumnIndexOrThrow("smer"))
                                        val vrednostiZaUpis = ContentValues()
                                        vrednostiZaUpis.put("stajalista", jsonOdOdgovora.toString())
                                        val brojUpisanihRedova = baza.update("linije", vrednostiZaUpis, "_id = ? and smer = ?", arrayOf(linija, smerKretanjaLinije))
                                        if (brojUpisanihRedova > 0) {
                                            Toster(context).toster("OK")
                                        }
                                        break@spoljnapetlja
                                    }
                                }
                            }

                        }
                    }
                    catch (g: Exception) {
                        Toster(context).toster("greska")
                    }
                }

                override fun neuspesanOdgovor(e: IOException) {
                    Toster(context).toster(context.resources.getString(R.string.nema_interneta))
                }
            })
        }
        return kursor
    }
}