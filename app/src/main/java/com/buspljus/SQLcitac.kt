package com.buspljus

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.buspljus.VoziloInfo.Companion.danunedelji
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
        const val ID_KOLONA = "_id"
        lateinit var baza: SQLiteDatabase
        lateinit var kursor: Cursor
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

    init {
        inicijalizacija(context)
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

    fun SQLzahtev(tabela: String, kolone: Array<String>, odabir: String, parametri: Array<String>, redjanjepo: String?): Cursor {
        baza = SQLiteDatabase.openDatabase(context.getDatabasePath(IME_BAZE).absolutePath, null, 0)
        globalQueryData = SqlQueryData(tabela, kolone, odabir, parametri, redjanjepo)
        return baza.query(tabela, kolone, odabir, parametri, null, null, "$redjanjepo")
    }

    fun idStaniceuGeoPoint(sifra: String): GeoPoint {
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

    fun idStaniceUNaziv(sifra: String): String {
        kursor = SQLzahtev("stanice", arrayOf("naziv_cir"), "_id = ?", arrayOf(sifra), null)
        var nazivCir = ""
        with (kursor) {
            if (count > 0) {
                use {
                    moveToFirst()
                    nazivCir = getString(getColumnIndexOrThrow("naziv_cir"))
                }
            }
        }
        return nazivCir
    }

    fun pretragabaze_kliknamapu(lat: String, lng: String, callback: Interfejs.Callback, pozivOdFunkcije: Int) {
        var i = 0
        var pronadjenihZS = 0
        val tackaGeoPoint = GeoPoint(lat.toDouble(),lng.toDouble())

        var tacka = arrayOf("1", lat, "1", "2", lng, "2")
        //var tacka = arrayOf(lat, lng)
        kursor = SQLzahtev("bgvoz", arrayOf("*"), "round(lt,?) = round(?,?) and round(lg,?) = round(?,?)", tacka, null)
        //kursor = SQLzahtev("bgvoz", arrayOf("*"), "lt like ? and lg like ?", arrayOf("%${lat.take(3)}%", "%${lng.take(3)}%"), null)

        fun pretraga(k: Cursor): Int {
            with (k) {
                use {
                    while (moveToNext()) {
                        val bazaGeoPoint = GeoPoint(getDouble(getColumnIndexOrThrow("lt")), getDouble(getColumnIndexOrThrow("lg")))
                        val rastojanje = bazaGeoPoint.sphericalDistance(tackaGeoPoint)
                        if (rastojanje < if (pozivOdFunkcije == 0) 50 else 400) {
                            val id = getString(getColumnIndexOrThrow("_id"))
                            val naziv = getString(getColumnIndexOrThrow("naziv"))
                            val redvoznje = getString(getColumnIndexOrThrow("redvoznje"))
                            if (pozivOdFunkcije == 0) {
                                VoziloInfo(context).pregledPolaskaVozova(naziv, redvoznje, 0)
                            } else if (pozivOdFunkcije == 1) {
                                callback.koloneBGVOZ(listOf(id, naziv, redvoznje, rastojanje))
                            }
                            pronadjenihZS += 1
                        }
                        i += 1
                    }
                }
            }
            return i
        }

        with (kursor) {
            if (count > 0) {
                pretraga(kursor)
            }
            else if (pozivOdFunkcije == 1) {
                kursor = SQLzahtev("bgvoz", arrayOf("*"), "round(lt,?) = round(?,?) or round(lg,?) = round(?,?)", tacka, null)
                pretraga(kursor)
            } else {
            }
        }

        if ((Glavna.mapa.mapPosition.zoomLevel >= 14) and (pozivOdFunkcije == 0) and (pronadjenihZS == 0)) {
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
                    AlertDialog(context).pronadjeneStaniceAlertDialog(pronadjeneStanice, callback)
                    pronadjenihZS += 1
                }
            }
        }
    }

    fun dobavisifre(rec: CharSequence?, trazenjepobroju: Boolean?): Cursor {
        kursor = SQLzahtev(
            "stanice", arrayOf("_id", "naziv_cir", "staju", "sacuvana"),
            if (trazenjepobroju == true) "_id like ?" else "naziv_ascii like ? or naziv_cir like ? or naziv_lat like ?",
            if (trazenjepobroju == true) arrayOf("$rec%") else arrayOf("%$rec%", "%$rec%", "%$rec%"), "sacuvana" + " DESC LIMIT 50")
        return kursor
    }

    fun dobaviSacuvaneStanice(): List<String> {
        val lista = mutableListOf<String>()
        kursor = SQLzahtev("stanice", arrayOf("_id"),"sacuvana = 1", arrayOf(),null)
        with (kursor) {
            use {
                while (kursor.moveToNext()) {
                    lista.add(getString(getColumnIndexOrThrow("_id")))
                }
            }
        }
        return lista
    }

    fun sacuvajStanicu(stanica: List<String>, cuvanjeilibrisanje: Int) {
        val stanicazacuvanje = ContentValues()
        stanicazacuvanje.put("sacuvana", cuvanjeilibrisanje)
        for (element in stanica) {
            baza.update("stanice", stanicazacuvanje, "_id = ?", arrayOf(element))
        }
    }

    fun preradaRVJSON(redVoznjeJSON: JSONObject?, brojVoza: String?, sifraOS: String?, rezimRada: Int, vremeDol: LocalTime?): MutableList<List<String>> {
        val rezultat = mutableListOf<List<String>>()
        var pronadjeno: Boolean
        var radnidanSubotaNedelja : JSONObject
        var sifreStanica : JSONObject
        var sati : JSONObject
        var polasciUSatu : JSONArray

        fun podfunkcijaKursor(kr: Cursor, vecaLista: Boolean, sifraos: String) {
            try {
                with (kr) {
                    if (count > 0) {
                        use {
                            while (moveToNext()) {
                                pronadjeno = false

                                // Ako je redVoznjeJSON null ide se na pretragu po broju voza
                                radnidanSubotaNedelja = redVoznjeJSON ?: JSONObject(getString(getColumnIndexOrThrow("redvoznje")))
                                sifreStanica = radnidanSubotaNedelja.getJSONObject(danunedelji.toString())

                                sati = sifreStanica.getJSONObject(sifraos)

                                if (!pronadjeno) {
                                    for (sat in sati.keys().iterator()) {
                                        polasciUSatu = sati.getJSONArray(sat)
                                        for (minuti in 0 until polasciUSatu.length()) {
                                            with (polasciUSatu.getJSONObject(minuti)) {
                                                val minut = keys().next()
                                                val brVoza = getString(minut)
                                                val dolazak = getString("d")
                                                val bgVOZ = getString("bgv")
                                                val imeStanice = getString(0)
                                                val polazak = "$sat:$minut"

                                                if (vecaLista) {
                                                    if (LocalTime.parse(polazak).isAfter(vremeDol ?: LocalTime.now().minusMinutes(3))) {
                                                        rezultat.add(listOf(imeStanice, polazak, dolazak, brVoza, sifraos, bgVOZ))
                                                        pronadjeno = if (rezimRada == 1) true else false
                                                    }
                                                    else pronadjeno = false
                                                }
                                                else {
                                                    if (brVoza == brojVoza) {
                                                        rezultat.add(listOf(imeStanice, polazak))
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
            catch(e: Exception) {
                Toster(context).toster("Greska $e")
            }
        }

        if (redVoznjeJSON != null) {
            for (sifraOdredisneStanice in redVoznjeJSON.getJSONObject(danunedelji.toString()).keys().iterator()) {
                kursor = SQLcitac(context).SQLzahtev("bgvoz", arrayOf("naziv"),"_id = ?", arrayOf(sifraOdredisneStanice),null)
                podfunkcijaKursor(kursor, true, sifraOdredisneStanice)
            }
        }
        else {
            kursor = SQLcitac(context).SQLzahtev("bgvoz", arrayOf("naziv", "redvoznje"), "redvoznje like ?", arrayOf("%\"$brojVoza\"%"),null)
            if (sifraOS != null) {
                podfunkcijaKursor(kursor, false, sifraOS)
            }
        }

        return rezultat
    }

    fun kliknavozilo(linija: String, smer: String?, stanica: String, vratiListu: Interfejs.vracenaLista) {
        try {
            fun nastavi() {
                val lista = mutableListOf<String>()
                if (kursor.count == 1) {
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
                vratiListu.vratiListu(lista)
            }

            if (smer != null) {
                kursor = SQLzahtev("linije", arrayOf("*"), "_id = ? and smer = ?", arrayOf(linija, smer), null)
                nastavi()
            }
            else {
                kursor = SQLzahtev("linije", arrayOf("*"), "_id = ? and stajalista like ?", arrayOf(linija, "%\"$stanica\"%"), null)
                if (kursor.count != 1) {
                    Internet().zahtevPremaInternetu(stanica, linija, 1, object : Interfejs.odgovorSaInterneta {
                        override fun uspesanOdgovor(response: Response) {
                            try {
                                var pronadjeno = false
                                val jsonOdOdgovora = JSONArray(response.body!!.string())
                                if (kursor.count > 1) {
                                    kursor = SQLzahtev("linije", arrayOf("*"), "_id = ? and stajalista = ?", arrayOf(linija, jsonOdOdgovora.toString().replace(",",", ")), null)
                                    nastavi()
                                }
                                else {
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
                                        if (pronadjeno) {
                                            kursor = SQLzahtev("linije", arrayOf("*"), "_id = ? and stajalista like ?", arrayOf(linija, "%\"$stanica\"%"), null)
                                            nastavi()
                                        }
                                        else {
                                            Toster(context).toster("Neuspesno pronalazenje relacije linije")
                                        }
                                        break
                                    }
                                }

                            } catch (g: Exception) {
                                AlertDialog(context).prikaziGresku(g)
                            }
                        }

                        override fun neuspesanOdgovor(e: IOException) {
                            Toster(context).toster(context.resources.getString(R.string.greska_sa_vezom))
                        }
                    })
                }
                else nastavi()
            }
        }
        catch (g: Exception) {
            AlertDialog(context).prikaziGresku(g)
        }
        finally {
            kursor.close()
        }
    }

    fun ubacivanjeTestMarkera(sifraStajalista: String, ifejs: Interfejs.specMarker) {
        try {
            kursor = SQLzahtev("linije", arrayOf("_id", "smer", "trasa", "redvoznje"), "stajalista like ?", arrayOf("%\"$sifraStajalista\"%"), null)
            with (kursor) {
                if (kursor.count > 0) {
                    use {
                        while (moveToNext()) {
                            val id = getString(getColumnIndexOrThrow("_id"))
                            val smer = getString(getColumnIndexOrThrow("smer"))
                            val trasa = getBlob(getColumnIndexOrThrow("trasa"))
                            val rv = getString(getColumnIndexOrThrow("redvoznje"))
                            ifejs.crtanjespecMarkera(id, smer, IzracunavanjeVremena().tranziranjeRV(rv, unzip(trasa)))
                        }
                    }
                }
            }
        }
        catch (g:Exception) {
            AlertDialog(context).prikaziGresku(g)
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
            selekcioniZahtev = "_id = ? and stajalista like ? and smer = ?"
        }
        else {
            nizZahtev = arrayOf(linija, "%\"$sifraStajalista\"%")
            selekcioniZahtev = "_id = ? and stajalista like ?"
        }


        kursor = SQLzahtev("linije", arrayOf("stajalista", "trasa"), selekcioniZahtev, nizZahtev, null)
        with (kursor) {
            if (count > 0) {
                use {
                    moveToFirst()

                    jsonIDStanice = JSONArray(getString(getColumnIndexOrThrow("stajalista")))
                    for (n in 0 until jsonIDStanice!!.length()) {
                        jsonGeoPointStanice.add(SQLcitac(context).idStaniceuGeoPoint(jsonIDStanice!![n].toString()))
                    }

                    val trasa = getBlob(getColumnIndexOrThrow("trasa"))
                    trasaUnzip = unzip(trasa)
                }
            }
        }

        return Triple(jsonIDStanice!!, JSONArray(trasaUnzip), jsonGeoPointStanice)
    }

    fun sveLinije(pretraga: String): List<String> {
        val upit : String
        val niz_upit : Array<String>

        if (pretraga.isNotEmpty()) {
            upit = "smer = ? and _id like ?"
            niz_upit = arrayOf("0", "$pretraga%")
        }
        else {
            upit = "smer = ?"
            niz_upit = arrayOf("0")
        }

        kursor = SQLcitac(context).SQLzahtev("linije", arrayOf("_id"), upit, niz_upit, "_id")

        val lista = mutableListOf<String>()

        with (kursor) {
            if (count > 0) {
                use {
                    while (moveToNext()) {
                        lista.add(getString(getColumnIndexOrThrow("_id")))
                    }
                }
            }
        }

        return lista
    }

    fun redVoznjeJednaLinija(linija:  String, smer: String): List<Any> {
        kursor = SQLcitac(context).SQLzahtev("linije", arrayOf("*"), "_id = ? and smer = ?", arrayOf(linija, smer), null)

        val lista = mutableListOf<Any>()

        with (kursor) {
            if (count > 0) {
                use {
                    while (moveToNext()) {
                        with (lista) {
                            add(getString(getColumnIndexOrThrow("_id")))
                            add(getInt(getColumnIndexOrThrow("smer")))
                            add(getString(getColumnIndexOrThrow("od")))
                            add(getString(getColumnIndexOrThrow("do")))
                            add(getString(getColumnIndexOrThrow("datumrv")))
                            add(JSONArray(getString(getColumnIndexOrThrow("stajalista"))))
                            add(getString(getColumnIndexOrThrow("redvoznje")))
                        }
                    }
                }
            }
        }
        return lista
    }

    private fun unzip(podatak: ByteArray): String {
        val inflater = Inflater()
        val bafer = ByteArray(1024)
        val izlaz = ByteArrayOutputStream()

        var brojac: Int

        inflater.setInput(podatak)

        while (!inflater.finished()) {
            brojac = inflater.inflate(bafer)
            izlaz.write(bafer, 0, brojac)
        }
        izlaz.close()

        return izlaz.toString()
    }
}