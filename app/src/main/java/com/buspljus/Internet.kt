package com.buspljus

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

class Internet : OkHttpClient() {

    interface ApiResponseCallback {
        fun onSuccess(response: Response)
        fun onFailure(e: IOException)
    }

    companion object {
        const val POLOZAJ_FLY = "http://buspljus.fly.dev/broj_stanice?st="
        const val MAPA_GZ = "https://github.com/emv412/buspljus-materijal/raw/main/beograd.map.gz"
        const val SVIPODACI_GZ = "https://github.com/emv412/buspljus-materijal/raw/main/svi_podaci.db.gz"
        const val NADOGRADNJA = "https://api.github.com/repos/emv412/bus-pljus/releases/latest"
        val adresa = Request.Builder()
        var zahtev: Call? = null
    }

    fun zahtevPremaInternetu(stanica: String?, argument: Int, callback: ApiResponseCallback) {
        when (argument) {
            1 -> if (stanica != null)
                adresa.url(POLOZAJ_FLY + stanica)
            2 -> adresa.url(MAPA_GZ)
            3 -> adresa.url(SVIPODACI_GZ)
            4 -> adresa.url(NADOGRADNJA)
        }
        zahtev = newCall(adresa.build())
        if (!zahtev?.isExecuted()!!) {
            zahtev?.enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    callback.onFailure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    callback.onSuccess(response)
                }
            })
        }
    }

    fun gunzip(gzipFile: InputStream, outputFile: File) {
        try {
            GZIPInputStream(gzipFile).use { gzipInputStream ->
                FileOutputStream(outputFile).use { fileOutputStream ->
                    val buffer = ByteArray(1024)
                    var len = gzipInputStream.read(buffer)
                    while (len > 0) {
                        fileOutputStream.write(buffer, 0, len)
                        len = gzipInputStream.read(buffer)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}