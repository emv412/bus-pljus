package com.buspljus

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.buspljus.RedVoznje.Companion.danunedelji
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.oscim.core.GeoPoint
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalTime
import java.util.zip.Inflater

class SQLcitac(private val context: Context) {
    companion object {
        const val IME_BAZE = "svi_podaci.db"
        const val CIR_KOLONA = "naziv_cir"
        lateinit var baza: SQLiteDatabase
        var globalQueryData: SqlQueryData? = null

        fun inicijalizacija(context: Context) {
            if (!::baza.isInitialized) {
                baza = SQLiteDatabase.openDatabase(
                    context.getDatabasePath(IME_BAZE).absolutePath,
                    null,
                    0
                )
            }
        }
    }
    lateinit var kursor: Cursor

    init {
        inicijalizacija(context)
    }

    interface Callback {
        fun korak(s: String)

        fun koloneBGVOZ(lista: List<String>)
    }

    data class SqlQueryData(
        val tabela: String,
        val kolone: Array<String>,
        val odabir: String,
        val parametri: Array<String>,
        val redjanjepo: String?
    )

    fun ponoviupit(): Cursor {
        return SQLzahtev(
            globalQueryData!!.tabela,
            globalQueryData!!.kolone,
            globalQueryData!!.odabir,
            globalQueryData!!.parametri,
            globalQueryData!!.redjanjepo
        )
    }

    fun SQLzahtev(
        tabela: String,
        kolone: Array<String>,
        odabir: String,
        parametri: Array<String>,
        redjanjepo: String?
    ): Cursor {
        baza = SQLiteDatabase.openDatabase(context.getDatabasePath(IME_BAZE).absolutePath, null, 0)
        globalQueryData = SqlQueryData(tabela, kolone, odabir, parametri, redjanjepo)
        return baza.query(tabela, kolone, odabir, parametri, null, null, redjanjepo + " LIMIT 50")
    }

    fun pozahtevu_jednastanica(sifra: String): GeoPoint {
        kursor = SQLzahtev("stanice", arrayOf("lt,lg"), "_id = ?", arrayOf(sifra), null)
        var lt = 0.0
        var lg = 0.0
        with (kursor) {
            if (count > 0) {
                use {
                    moveToFirst()
                    lt = getString(getColumnIndexOrThrow("lt")).toDouble()
                    lg = getString(getColumnIndexOrThrow("lg")).toDouble()
                }
            }
        }
        return GeoPoint(lt,lg)
    }

    fun pretragabaze_kliknamapu(lat: String, lng: String, callback: Callback, pozivOdFunkcije: Int) {
        var prekiniForPetlju = false

        var tacka = arrayOf("1", lat, "1", "1", lng, "1")
        val tackaGeoPoint = GeoPoint(tacka[1].toDouble(),tacka[4].toDouble())

        kursor = SQLzahtev("bgvoz", arrayOf("*"), "round(lt,?) = round(?,?) and round(lg,?) = round(?,?)", tacka, null)

        with (kursor) {
            if (count > 0) {
                use {
                    while (moveToNext()) {
                        val bazaGeoPoint = GeoPoint(getDouble(getColumnIndexOrThrow("lt")), getDouble(getColumnIndexOrThrow("lg")))
                        val rastojanje = bazaGeoPoint.sphericalDistance(tackaGeoPoint)
                        if (rastojanje < if (pozivOdFunkcije == 0) 20 else 400) {
                            val id = getString(getColumnIndexOrThrow("_id"))
                            val naziv = getString(getColumnIndexOrThrow("naziv"))
                            val redvoznje = getString(getColumnIndexOrThrow("redvoznje"))
                            if (pozivOdFunkcije == 0) {
                                RedVoznje(context).pregledPolaskaVozova(naziv, redvoznje, 0)
                            } else if (pozivOdFunkcije == 1) {
                                callback.koloneBGVOZ(listOf(id, naziv, redvoznje))
                            }
                            prekiniForPetlju = true
                        }
                    }
                }
            }
        }

        if ((Glavna.mapa.mapPosition.zoomLevel >= 16) and (pozivOdFunkcije == 0) and (!prekiniForPetlju)) {
            val pronadjeneStanice = mutableListOf<String>()
            tacka = arrayOf("2", lat, "2", "2", lng, "2")
            kursor = SQLzahtev("stanice", arrayOf("_id", "naziv_cir", "lt", "lg"), "round(lt,?) = round(?,?) and round(lg,?) = round(?,?)", tacka, null)

            with (kursor) {
                if (count > 0) {
                    use {
                        while (moveToNext())
                            if (GeoPoint(getDouble(getColumnIndexOrThrow("lt")), getDouble(getColumnIndexOrThrow("lg"))).sphericalDistance(tackaGeoPoint) < 20)
                                pronadjeneStanice.add(getString(getColumnIndexOrThrow("_id")))
                    }
                    AlertDialog(context,pronadjeneStanice).pronadjeneStaniceAlertDialog(callback)
                    prekiniForPetlju = true
                }
            }
        }

        /*

        for (brojac in preciznost downTo 3) {
            val x = (if (pozivOdFunkcije == 1) 1 else brojac).toString()
            val tacka = arrayOf(x, lat, x, x, lng, x)

            kursor = SQLzahtev("bgvoz", arrayOf("*"), "round(lt,?) = round(?,?) and round(lg,?) = round(?,?)", tacka, null)

            with (kursor) {
                if (count > 0) {
                    use {
                        while (moveToNext()) {
                            val tacka1 = GeoPoint(getDouble(getColumnIndexOrThrow("lt")), getDouble(getColumnIndexOrThrow("lg")))
                            val tacka2 = GeoPoint(tacka[1].toDouble(),tacka[4].toDouble())
                            val tacka3 = tacka1.sphericalDistance(tacka2)
                            if (tacka3 < if (pozivOdFunkcije == 2) 50 else 400) {
                                val id = getString(getColumnIndexOrThrow("_id"))
                                val naziv = getString(getColumnIndexOrThrow("naziv"))
                                val redvoznje = getString(getColumnIndexOrThrow("redvoznje"))
                                if (pozivOdFunkcije == 2) {
                                    RedVoznje(context).pregledPolaskaVozova(naziv, redvoznje, 0)
                                } else if (pozivOdFunkcije == 1) {
                                    callback.koloneBGVOZ(listOf(id, naziv, redvoznje))
                                }
                                prekiniForPetlju = true
                            }
                            if (pozivOdFunkcije == 1) prekiniForPetlju = true
                        }
                    }
                }
            }

            if ((Glavna.mapa.mapPosition.zoomLevel >= 16) and (pozivOdFunkcije == 2)) {
                kursor = SQLzahtev("stanice", arrayOf("_id", "naziv_cir"), "round(lt,?) = round(?,?) and round(lg,?) = round(?,?)", tacka, null)

                with (kursor) {
                    if (count > 0) {
                        use {
                            while (moveToNext())
                                pronadjeneStanice.add(getString(getColumnIndexOrThrow("_id")))
                        }
                        AlertDialog(context,pronadjeneStanice).pronadjeneStaniceAlertDialog(callback)
                        prekiniForPetlju = true
                    }
                }
            }
            if (prekiniForPetlju)
                break
        }

         */
    }

    fun dobavisifre(rec: CharSequence?, trazenjepobroju: Boolean?): Cursor {
        kursor = SQLzahtev(
            "stanice", arrayOf("_id", "naziv_cir", "staju", "sacuvana"),
            if (trazenjepobroju == true) "_id like ?" else "naziv_ascii like ? or naziv_cir like ? or naziv_lat like ?",
            if (trazenjepobroju == true) arrayOf("$rec%") else arrayOf("%$rec%", "%$rec%", "%$rec%"), "sacuvana" + " DESC")
        return kursor
    }

    fun sacuvajStanicu(stanica: String, cuvanjeilibrisanje: Int) {
        val stanicazacuvanje = ContentValues()
        stanicazacuvanje.put("sacuvana", cuvanjeilibrisanje)
        val uspesnoiline = baza.update("stanice", stanicazacuvanje, "_id = ?", arrayOf(stanica))
        if (uspesnoiline > 0)
            Toster(context).toster(
                if (cuvanjeilibrisanje == 0)
                    context.resources.getString(R.string.stanicaobrisana)
                else
                    context.resources.getString(R.string.stanicasacuvana)
            )
    }

    fun preradaRVJSON(redVoznjeJSON: JSONObject?, brojVoza: String?, sifraOS: String?, rezimRada : Int, vremeDol: LocalTime?): MutableList<List<String>> {
        val rezultat = mutableListOf<List<String>>()
        var pronadjeno: Boolean
        var radnidanSubotaNedelja : JSONObject
        var sifre_stanica : JSONObject
        var sati : JSONObject
        var polasciUSatu : JSONArray

        fun podfunkcijaKursor(kr: Cursor, vecaLista: Boolean, sifraos: String) {
            with (kr) {
                if (count > 0) {
                    use {
                        while (moveToNext()) {
                            pronadjeno = false

                            // Ako je redVoznjeJSON null ide se na pretragu po broju voza
                            radnidanSubotaNedelja = redVoznjeJSON ?: JSONObject(getString(getColumnIndexOrThrow("redvoznje")))
                            sifre_stanica = radnidanSubotaNedelja.getJSONObject(danunedelji.toString())

                            sati = sifre_stanica.getJSONObject(sifraos)

                            if (!pronadjeno) {
                                for (sat in sati.keys().iterator()) {
                                    polasciUSatu = sati.getJSONArray(sat)
                                    for (minuti in 0 until polasciUSatu.length()) {
                                        with (polasciUSatu.getJSONObject(minuti)) {
                                            val minut = keys().next()
                                            val brVoza = getString(minut)
                                            val dolazak = getString("d")
                                            val ime_stanice = getString(0)
                                            val polazak = "$sat:$minut"

                                            if (vecaLista) {
                                                if (LocalTime.parse(polazak).isAfter(vremeDol ?: LocalTime.now().minusMinutes(3))) {
                                                    rezultat.add(listOf(ime_stanice, polazak, dolazak, brVoza, sifraos))
                                                    pronadjeno = if (rezimRada == 1) true else false
                                                }
                                                else pronadjeno = false
                                            }
                                            else {
                                                if (brVoza == brojVoza) {
                                                    rezultat.add(listOf(ime_stanice, polazak))
                                                    pronadjeno = true
                                                }
                                                else pronadjeno = false
                                            }
                                        }
                                        if (pronadjeno) break
                                    }
                                    if (pronadjeno) break
                                }
                            }
                        }
                    }
                }
            }
        }

        if (redVoznjeJSON != null) {
            for (sifraOdredisneStanice in redVoznjeJSON.getJSONObject(danunedelji.toString()).keys().iterator()) {
                kursor = SQLcitac(context).SQLzahtev("bgvoz", arrayOf("naziv"),"_id = ?", arrayOf(sifraOdredisneStanice),null)
                podfunkcijaKursor(kursor, true, sifraOdredisneStanice)
            }
        }
        else {
            kursor = SQLcitac(context).SQLzahtev("bgvoz", arrayOf("naziv", "redvoznje"), "redvoznje like ?", arrayOf("%$brojVoza%"),null)
            if (sifraOS != null) {
                podfunkcijaKursor(kursor, false, sifraOS)
            }
        }

        return rezultat
    }

    fun redvoznjeKliknavozilo(linija: String, stanica: String): List<String> {
        val lista = mutableListOf<String>()

        kursor = SQLzahtev("linije", arrayOf("*"), "_id = ? and stajalista like ?", arrayOf(linija, "%\"$stanica\"%"), null)

        if (kursor.count == 0) {
            Internet().zahtevPremaInternetu(stanica, linija, 1, object : Internet.odgovorSaInterneta {
                override fun uspesanOdgovor(response: Response) {
                    try {
                        var pronadjeno = false
                        val jsonOdOdgovora = JSONArray(response.body!!.string())
                        for (i in 0 until jsonOdOdgovora.length()) {
                            with (SQLzahtev("linije", arrayOf("*"), "_id = ? and stajalista like ?",
                                arrayOf(linija, "%\"${jsonOdOdgovora.getString(i)}\"%"), null)) {
                                if (count == 1) {
                                    use {
                                        moveToFirst()
                                        val nizStanicaUBazi = JSONArray(getString(getColumnIndexOrThrow("stajalista")))
                                        for (n in 0 until nizStanicaUBazi.length()) {
                                            if (jsonOdOdgovora[i] == nizStanicaUBazi[n]) {
                                                val smerKretanjaLinije = getString(getColumnIndexOrThrow("smer"))
                                                val vrednostiZaUpis = ContentValues()
                                                vrednostiZaUpis.put("stajalista", jsonOdOdgovora.toString())
                                                val brojUpisanihRedova = baza.update("linije", vrednostiZaUpis, "_id = ? and smer = ?",
                                                    arrayOf(linija, smerKretanjaLinije))
                                                if (brojUpisanihRedova > 0) { Toster(context).toster("OK")
                                                }
                                                pronadjeno = true
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                            if (pronadjeno)
                                kursor = SQLzahtev("linije", arrayOf("*"), "_id = ? and stajalista like ?", arrayOf(linija, "%\"$stanica\"%"), null)
                            break
                        }
                    } catch (g: Exception) {
                        Toster(context).toster(g.toString())
                    }
                }

                    override fun neuspesanOdgovor(e: IOException) {
                        Toster(context).toster(context.resources.getString(R.string.nema_interneta))
                    }
                })
        }
        if (kursor.count > 0) {
            with (kursor) {
                use {
                    val kolone = listOf("od", "do", "stajalista", "redvoznje", "datumrv")
                    moveToFirst()
                    for (i in kolone) {
                        lista.add(getString(getColumnIndexOrThrow(i)))
                    }
                }
            }
        }
        else kursor.close()
        return lista
    }

    fun prikaziTrasu(linija: String, sifraStajalista: String): List<JSONArray> {
        val inflater = Inflater()
        val spisakLtLg = ByteArrayOutputStream()
        val bafer = ByteArray(1024)
        var sveStanice = ""

        kursor = SQLzahtev("linije", arrayOf("stajalista","trasa"), "_id = ? and stajalista like ?", arrayOf(linija, "%\"$sifraStajalista\"%"), null)
        with (kursor) {
            if (count > 0) {
                use {
                    moveToFirst()
                    sveStanice = getString(getColumnIndexOrThrow("stajalista"))
                    val jsonObjekat = getBlob(getColumnIndexOrThrow("trasa"))

                    inflater.setInput(jsonObjekat)

                    while (!inflater.finished()) {
                        val count = inflater.inflate(bafer)
                        spisakLtLg.write(bafer, 0, count)
                    }

                    spisakLtLg.close()
                }
            }
        }
        return listOf(JSONArray(sveStanice),JSONArray(spisakLtLg.toString()))
    }
}