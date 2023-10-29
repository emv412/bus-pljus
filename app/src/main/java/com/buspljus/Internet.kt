package com.buspljus

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class Internet: Activity() {

    fun prijemPodataka(stanica: String, ucitavanje: ProgressBar, context: Context) {
        runOnUiThread { ucitavanje.visibility = View.VISIBLE }
        val adresa = Request.Builder()
            .url("http://193.104.68.114:32222/bus_stanice.php?st=$stanica")
            .build()

        OkHttpClient().newCall(adresa).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Toster(context).toster("Nema interneta, ili je greska na serveru")
                runOnUiThread { ucitavanje.visibility = View.GONE }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val odgovor = response.body?.string()
                    try {
                        val json = odgovor?.let { JSONObject(it) }
                        if (json != null) {
                            Glavna().crtanjemarkera(json)
                        }
                    } catch (e: Exception) {
                        if (odgovor == "0\n") {
                            Toster(context).toster("Nema vozila")
                        }
                        else {
                            Toster(context).toster(e.toString())
                            Log.d("DEBAG",""+e)
                        }

                        runOnUiThread{ucitavanje.visibility = View.GONE}
                    }
                }
            }
        }
        )
    }
}