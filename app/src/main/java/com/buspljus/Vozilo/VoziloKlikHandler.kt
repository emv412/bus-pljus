package com.buspljus.Vozilo

import android.content.Context
import android.database.Cursor
import com.buspljus.Baza.PosrednikBaze.Companion.DATUM_RV
import com.buspljus.Baza.PosrednikBaze.Companion.LISTA_STAJALISTA
import com.buspljus.Baza.PosrednikBaze.Companion.OKRETNICA_DO
import com.buspljus.Baza.PosrednikBaze.Companion.OKRETNICA_OD
import com.buspljus.Baza.PosrednikBaze.Companion.RED_VOZNJE
import com.buspljus.Baza.PosrednikBaze.Companion.SMER
import com.buspljus.Interfejs
import com.buspljus.PopupProzor
import com.buspljus.R
import com.buspljus.Toster
import org.json.JSONArray

class VoziloKlikHandler(
    private val context: Context,
    private val repository: VoziloRepository,
    private val remote: VoziloRemoteDataSource
) {

    fun klikNaVozilo(
        linija: String,
        smer: String?,
        stanica: String,
        sledeceStajaliste: String?,
        vratiListu: Interfejs.vracenaLista
    ) {
        var kursor: Cursor? = null
        try {
            fun nastavi() {
                val lista = mutableListOf<String>()
                if (kursor?.count == 1) {
                    kursor?.use {
                        val kolone = listOf(OKRETNICA_OD, OKRETNICA_DO, LISTA_STAJALISTA, RED_VOZNJE, DATUM_RV)
                        it.moveToFirst()
                        for (kolona in kolone) {
                            lista.add(it.getString(it.getColumnIndexOrThrow(kolona)))
                        }
                    }
                }
                sledeceStajaliste?.let { lista.add(it) }
                vratiListu.vratiListu(lista)
            }

            if (smer != null) {
                kursor = repository.getLinijaBySmer(linija, smer)
                nastavi()
            } else {
                kursor = repository.getLinijaByStanica(linija, stanica)
                if (kursor.count != 1) {
                    remote.fetchStanice(
                        stanica, linija,
                        onSuccess = { json ->
                            handleRemoteData(json, linija, { nastavi() })
                        },
                        onError = {
                            Toster(context).toster(context.getString(R.string.greska_sa_vezom))
                        }
                    )
                } else {
                    nastavi()
                }
            }
        } catch (g: Exception) {
            PopupProzor(context).prikaziGresku(g)
        } finally {
            kursor?.close()
        }
    }

    private fun handleRemoteData(json: JSONArray, linija: String, nastavi: () -> Unit) {
        var pronadjeno = false
        val kursor = repository.getLinijaByListaStajalista(linija, json.toString().replace(",", ", "))
        if (kursor.count > 1) {
            nastavi()
            return
        }

        for (i in 0 until json.length()) {
            val cursorCandidate = repository.getLinijaByStanica(linija, json.getString(i))
            if (cursorCandidate.count == 1) {
                cursorCandidate.use {
                    it.moveToFirst()
                    val nizStanicaUBazi = JSONArray(it.getString(it.getColumnIndexOrThrow(LISTA_STAJALISTA)))
                    for (n in 0 until nizStanicaUBazi.length()) {
                        if (json[i] == nizStanicaUBazi[n]) {
                            val smerKretanja = it.getString(it.getColumnIndexOrThrow(SMER))
                            if (repository.updateListaStajalista(linija, smerKretanja, json.toString()) > 0) {
                                Toster(context).toster("OK")
                            }
                            pronadjeno = true
                            break
                        }
                    }
                }
            }
            if (pronadjeno) {
                nastavi()
                return
            }
        }
        Toster(context).toster("Neuspesno pronalazenje relacije linije")
    }
}
