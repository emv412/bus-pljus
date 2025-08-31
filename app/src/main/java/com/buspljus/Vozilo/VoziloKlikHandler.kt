package com.buspljus.Vozilo

import android.content.Context
import android.database.Cursor
import android.util.Log
import com.buspljus.Baza.PosrednikBaze.Companion.DATUM_RV
import com.buspljus.Baza.PosrednikBaze.Companion.LISTA_STAJALISTA
import com.buspljus.Baza.PosrednikBaze.Companion.OKRETNICA_DO
import com.buspljus.Baza.PosrednikBaze.Companion.OKRETNICA_OD
import com.buspljus.Baza.PosrednikBaze.Companion.RED_VOZNJE
import com.buspljus.Baza.PosrednikBaze.Companion.SMER
import com.buspljus.Interfejs
import com.buspljus.PopupProzor
import com.buspljus.Toster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException

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
            fun obradiStanice() {
                val lista = mutableListOf<String>()

                kursor?.use { cursor ->
                    val kolone = listOf(OKRETNICA_OD, OKRETNICA_DO, LISTA_STAJALISTA, RED_VOZNJE, DATUM_RV)

                    while (cursor.moveToNext()) {
                        val jsonString = cursor.getString(cursor.getColumnIndexOrThrow(LISTA_STAJALISTA))
                        val niz = try {
                            JSONArray(jsonString)
                        } catch (e: JSONException) {
                            Log.e("obradiStanice", "Invalid JSON in LISTA_STAJALISTA", e)
                            continue // skip this row
                        }

                        for (kolona in kolone) {
                            lista.add(cursor.getString(cursor.getColumnIndexOrThrow(kolona)))
                        }
                        break
                    }
                }

                sledeceStajaliste?.let { lista.add(it) }
                vratiListu.vratiListu(lista)
            }


            if (smer != null) {
                kursor = repository.getLinijaBySmer(linija, smer)
                obradiStanice()
            } else {
                kursor = repository.getLinijaByStanica(linija, stanica)
                //kursor.let {
                //    Log.d("KURSOR", DatabaseUtils.dumpCursorToString(it))
                //}
                if (kursor.count == 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        VoziloRemoteDataSource().fetchStaniceFlow(stanica,linija).collect { json ->
                            handleRemoteData(json, linija, { obradiStanice() })
                            if (kursor.count > 0) {
                                obradiStanice()
                            }
                        }
                    }
                } else {
                    obradiStanice()
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
