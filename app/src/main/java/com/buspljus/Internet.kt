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

    companion object {
        const val POLOZAJ_VOZILA = "http://buspljus.fly.dev/?st="
        const val NADOGRADNJA_PROGRAMA = "https://api.github.com/repos/emv412/bus-pljus/releases/latest"

        const val MAPA_GZ = "https://api.github.com/repos/emv412/buspljus-materijal/contents/beograd.map.gz"
        const val SVIPODACI_GZ = "https://api.github.com/repos/emv412/buspljus-materijal/contents/svi_podaci.db.gz"
        const val MAPA_GZ_DOWNLOAD = "https://raw.githubusercontent.com/emv412/buspljus-materijal/main/beograd.map.gz"
        const val SVIPODACI_GZ_DOWNLOAD = "https://raw.githubusercontent.com/emv412/buspljus-materijal/main/svi_podaci.db.gz"

        const val LINIJA = "&linija="
        const val SLEDECA_STANICA = "&s="

        val adresa = Request.Builder()
        var zahtev: Call? = null
    }

    fun zahtevPremaInternetu(stanica: String?, linija: String?, garBroj: String?, argument: Int, callback: Interfejs.odgovorSaInterneta) {
        when (argument) {
            1 -> if (linija == null) {
                adresa.url(POLOZAJ_VOZILA + stanica)
            }
            else {
                if (garBroj == null)
                    adresa.url(POLOZAJ_VOZILA + stanica + LINIJA + linija)
                else
                    adresa.url(POLOZAJ_VOZILA + stanica + LINIJA + linija + SLEDECA_STANICA + garBroj)
            }
            2 -> adresa.url(MAPA_GZ)
            3 -> adresa.url(SVIPODACI_GZ)
            4 -> adresa.url(NADOGRADNJA_PROGRAMA)
            5 -> adresa.url(MAPA_GZ_DOWNLOAD)
            6 -> adresa.url(SVIPODACI_GZ_DOWNLOAD)
        }
        zahtev = newCall(adresa.build())
        if (!zahtev?.isExecuted()!!) {
            zahtev?.enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    callback.neuspesanOdgovor(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    callback.uspesanOdgovor(response)
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