package com.buspljus

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class Internet : OkHttpClient() {

    interface ApiResponseCallback {
        fun onSuccess(response: Response)
        fun onFailure(e: IOException)
    }

    companion object {
        val PROVERA_POLOZAJA = "http://buspljus.azurewebsites.net/broj_stanice?st="
        val PREUZIMANJE_ZIP = "https://github.com/emv412/buspljus/raw/main/za_app.zip"
        val adresa = Request.Builder()
        private lateinit var zahtev: Call
    }

    fun zahtev_prema_internetu(stanica: String?, argument: Int, callback: ApiResponseCallback) {
        when (argument) {
            0 -> adresa.url(PREUZIMANJE_ZIP)
            1 -> if (stanica != null)
                adresa.url(PROVERA_POLOZAJA + stanica)
        }
        zahtev = newCall(adresa.build())
        if (!zahtev.isExecuted()){
            zahtev.enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    callback.onFailure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    callback.onSuccess(response)
                }
            })
        }
    }
}