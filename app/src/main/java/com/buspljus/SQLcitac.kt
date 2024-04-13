package com.buspljus

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import okhttp3.Response
import org.json.JSONArray
import org.oscim.core.GeoPoint
import java.io.IOException

class SQLcitac(private val context: Context) {
    companion object {
        const val IME_BAZE = "svi_podaci.db"
        const val CIR_KOLONA = "naziv_cir"
        val pronadjeneStanice = mutableListOf<String>()
        lateinit var kursor : Cursor
        lateinit var baza : SQLiteDatabase
        var globalQueryData: SqlQueryData? = null
    }

    interface Callback {
        fun korak(s: String)
    }

    data class SqlQueryData(
        val tabela: String,
        val kolone: Array<String>,
        val odabir: String,
        val parametri: Array<String>,
        val redjanjepo: String?
    )

    fun ponoviupit(): Cursor {
        return SQLzahtev(globalQueryData!!.tabela, globalQueryData!!.kolone, globalQueryData!!.odabir, globalQueryData!!.parametri, globalQueryData!!.redjanjepo)
    }

    fun SQLzahtev(tabela: String, kolone: Array<String>, odabir: String, parametri: Array<String>, redjanjepo: String?): Cursor {
        baza = SQLiteDatabase.openDatabase(context.getDatabasePath(IME_BAZE).absolutePath, null, 0)
        globalQueryData = SqlQueryData(tabela, kolone, odabir, parametri, redjanjepo)
        return baza.query(tabela, kolone, odabir, parametri,null,null, redjanjepo+" LIMIT 50")
    }

    fun pozahtevu_jednastanica(sifra: String): GeoPoint {
        kursor = SQLzahtev("stanice",arrayOf("lt,lg"),"_id = ?", arrayOf(sifra),null)
        kursor.moveToFirst()
        kursor.use {
            it.moveToFirst()
            return GeoPoint(kursor.getString(kursor.getColumnIndexOrThrow("lt")).toDouble(),
                kursor.getString(kursor.getColumnIndexOrThrow("lg")).toDouble())
        }
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

            kursor = SQLzahtev("bgvoz", arrayOf("_id","naziv","redvoznje"),"round(lt,?) = round(?,?) and round(lg,?) = round(?,?)", tacka,null)
            if (kursor.count > 0) {
                kursor.moveToFirst()
                RedVoznje(context).redVoznjeKliknaStanicu()
                break
            }

            if (Glavna.mapa.mapPosition.zoomLevel>=16) {
                kursor = SQLzahtev("stanice", arrayOf("_id","naziv_cir"),"round(lt,?) = round(?,?) and round(lg,?) = round(?,?)", tacka,null)

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
            SQLzahtev("stanice",arrayOf("_id","naziv_cir","staju","sacuvana"),"_id like ?",arrayOf("$rec%"),"sacuvana"+" DESC")
        } else {
            SQLzahtev("stanice",arrayOf("_id","naziv_cir","staju","sacuvana"),"naziv_ascii like ? or naziv_cir like ? or naziv_lat like ?",arrayOf("%$rec%","%$rec%","%$rec%"),
                "sacuvana"+" DESC")
        }
        return kursor
    }

    fun sacuvajStanicu(stanica: String, cuvanjeilibrisanje: Int) {
        val stanicazacuvanje = ContentValues()
        stanicazacuvanje.put("sacuvana", cuvanjeilibrisanje)
        val uspesnoiline = baza.update("stanice", stanicazacuvanje, "_id=?", arrayOf(stanica))
        if (uspesnoiline > 0)
            Toster(context).toster(
                if (cuvanjeilibrisanje == 0)
                    context.resources.getString(R.string.stanicaobrisana)
                else
                    context.resources.getString(R.string.stanicasacuvana)
            )
    }

    fun redvoznjeKliknavozilo(linija: String, stanica: String): Cursor {
        //kursor = SQLzahtev(4, arrayOf(linija,"%\"$stanica\"%"))
        kursor = SQLzahtev("linije",arrayOf("*"),"_id = ? and stajalista like ?",arrayOf(linija, "%\"$stanica\"%"), null)
        if (kursor.count == 0) {
            Internet().zahtevPremaInternetu(stanica,linija,1, object : Internet.odgovorSaInterneta{
                override fun uspesanOdgovor(response: Response) {
                    try {
                        val jsonOdOdgovora = JSONArray(response.body!!.string())
                        spoljnapetlja@ for (i in 0 until jsonOdOdgovora.length()) {
                            val josjedankursor = SQLzahtev("linije",arrayOf("*"),"_id = ? and stajalista like ?",arrayOf(linija, "%\"${jsonOdOdgovora.getString(i)}\"%"), null)
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
                                        josjedankursor.close()
                                        kursor = SQLzahtev("linije",arrayOf("*"),"_id = ? and stajalista like ?",arrayOf(linija, "%\"$stanica\"%"), "sacuvana")
                                        break@spoljnapetlja
                                    }
                                }
                            }

                        }
                    }
                    catch (g: Exception) {
                        Toster(context).toster(g.toString())
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