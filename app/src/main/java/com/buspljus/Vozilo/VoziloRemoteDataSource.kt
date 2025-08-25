package com.buspljus.Vozilo

import com.buspljus.Interfejs
import com.buspljus.Internet
import okhttp3.Response
import okio.IOException
import org.json.JSONArray

class VoziloRemoteDataSource {

    fun fetchStanice(
        stanica: String,
        linija: String,
        onSuccess: (JSONArray) -> Unit,
        onError: (IOException) -> Unit
    ) {
        Internet().zahtevPremaInternetu(stanica, linija, 1, object : Interfejs.odgovorSaInterneta {
            override fun uspesanOdgovor(response: Response) {
                val json = JSONArray(response.body!!.string())
                onSuccess(json)
            }

            override fun neuspesanOdgovor(e: IOException) {
                onError(e)
            }
        })
    }
}
