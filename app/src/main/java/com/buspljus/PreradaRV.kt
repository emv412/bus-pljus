package com.buspljus

import android.content.Context
import android.database.Cursor
import com.buspljus.Baza.PosrednikBaze
import com.buspljus.Baza.UpitUBazu
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime

class PreradaRV(private val context: Context) {

    fun preradaRVJSON(
        redVoznjeJSON: JSONObject?,
        brojVoza: String?,
        sifraOS: String?,
        rezimRada: Int,
        vremeDol: LocalTime?
    ): MutableList<List<String>> {
        val baza = object : UpitUBazu(context) {}
        val rezultat = mutableListOf<List<String>>()
        var pronadjeno: Boolean
        var radnidanSubotaNedelja: JSONObject
        var sifreStanica: JSONObject
        var sati: JSONObject
        var polasciUSatu: JSONArray

        fun podfunkcijaKursor(kr: Cursor, vecaLista: Boolean, sifraos: String) {
            try {
                with(kr) {
                    if (count > 0) {
                        use {
                            while (moveToNext()) {
                                pronadjeno = false

                                // Ako je redVoznjeJSON null ide se na pretragu po broju voza
                                radnidanSubotaNedelja = redVoznjeJSON ?: JSONObject(getString(getColumnIndexOrThrow(
                                    PosrednikBaze.RED_VOZNJE)))
                                sifreStanica = radnidanSubotaNedelja.getJSONObject(VoziloInfo.danunedelji.toString())

                                sati = sifreStanica.getJSONObject(sifraos)

                                if (!pronadjeno) {
                                    for (sat in sati.keys().iterator()) {
                                        polasciUSatu = sati.getJSONArray(sat)
                                        for (minuti in 0 until polasciUSatu.length()) {
                                            with(polasciUSatu.getJSONObject(minuti)) {
                                                val minut = keys().next()
                                                val brVoza = getString(minut)
                                                val dolazak = getString("d")
                                                val bgVOZ = getString("bgv")
                                                val imeStanice = getString(0)
                                                val polazak = "$sat:$minut"

                                                if (vecaLista) {
                                                    if (LocalTime.parse(polazak).isAfter(vremeDol ?: LocalTime.now().minusMinutes(3))) {
                                                        rezultat.add(listOf(imeStanice, polazak, dolazak, brVoza, sifraos, bgVOZ))
                                                        pronadjeno = rezimRada == 1
                                                    } else pronadjeno = false
                                                } else {
                                                    if (brVoza == brojVoza) {
                                                        rezultat.add(listOf(imeStanice, polazak))
                                                        pronadjeno = true
                                                    } else pronadjeno = false
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
            } catch (e: Exception) {
                Toster(context).toster("Greska $e")
            }
        }

        if (redVoznjeJSON != null) {
            for (sifraOdredisneStanice in redVoznjeJSON.getJSONObject(VoziloInfo.danunedelji.toString()).keys().iterator()) {
                val kursor = baza.SQLzahtev(
                    PosrednikBaze.BGVOZ_TABLE,
                    arrayOf(PosrednikBaze.STANICABGVOZNAZIV),
                    "${PosrednikBaze.ID_KOLONA} = ?",
                    arrayOf(sifraOdredisneStanice),
                    null
                )
                podfunkcijaKursor(kursor, true, sifraOdredisneStanice)
            }
        } else {
            val kursor = baza.SQLzahtev(
                PosrednikBaze.BGVOZ_TABLE,
                arrayOf(PosrednikBaze.STANICABGVOZNAZIV, PosrednikBaze.RED_VOZNJE),
                "${PosrednikBaze.RED_VOZNJE} like ?",
                arrayOf("%\"$brojVoza\"%"),
                null
            )
            if (sifraOS != null) {
                podfunkcijaKursor(kursor, false, sifraOS)
            }
        }

        return rezultat
    }
}