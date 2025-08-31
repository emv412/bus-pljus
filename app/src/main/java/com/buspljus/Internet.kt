package com.buspljus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Internet : OkHttpClient() {

    companion object {
        const val POLOZAJ_VOZILA = "http://buspljus.fly.dev/?st="
        const val NADOGRADNJA_PROGRAMA = "https://api.github.com/repos/emv412/bus-pljus/releases/latest"

        const val MAPA_GZ = "https://api.github.com/repos/emv412/buspljus-materijal/contents/beograd.map.gz"
        const val SVIPODACI_GZ = "https://api.github.com/repos/emv412/buspljus-materijal/contents/svi_podaci.db.gz"
        const val MAPA_GZ_DOWNLOAD = "https://raw.githubusercontent.com/emv412/buspljus-materijal/main/beograd.map.gz"
        const val SVIPODACI_GZ_DOWNLOAD = "https://raw.githubusercontent.com/emv412/buspljus-materijal/main/svi_podaci.db.gz"

        const val LINIJA = "&linija="

        val adresa = Request.Builder()
        var zahtev: Call? = null
    }

    fun zahtevPremaInternetu(stanica: String?, linija: String?, argument: Int, callback: Interfejs.odgovorSaInterneta) {
        when (argument) {
            1 -> if (linija == null) {
                adresa.url(POLOZAJ_VOZILA + stanica)
            }
            else {
                adresa.url(POLOZAJ_VOZILA + stanica + LINIJA + linija)
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

    sealed class DownloadResult {
        data class Text(val content: String) : DownloadResult()
        data class FileResult(val file: File, val progress: Int) : DownloadResult()
    }

    fun downloadAsFlow(
        stanica: String?,
        linija: String?,
        argument: Int,
        outputFile: File? = null // ako je null, vraća string
    ): Flow<DownloadResult> = flow {
        // Odredi URL
        when (argument) {
            1 -> if (linija == null) {
                adresa.url(POLOZAJ_VOZILA + stanica)
            } else {
                adresa.url(POLOZAJ_VOZILA + stanica + LINIJA + linija)
            }
            2 -> adresa.url(MAPA_GZ)
            3 -> adresa.url(SVIPODACI_GZ)
            4 -> adresa.url(NADOGRADNJA_PROGRAMA)
            5 -> adresa.url(MAPA_GZ_DOWNLOAD)
            6 -> adresa.url(SVIPODACI_GZ_DOWNLOAD)
            else -> throw IllegalArgumentException("Nepodržan argument: $argument")
        }

        val call = newCall(adresa.build())
        val response = call.execute()
        if (!response.isSuccessful) throw IOException("HTTP greška: ${response.code}")

        val body = response.body ?: throw IOException("Prazan odgovor")

        if (outputFile == null) {
            // Tekstualni odgovor
            emit(DownloadResult.Text(body.string()))
        } else {
            // Fajl sa progresom
            val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesCopied = 0L

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalBytes > 0) {
                            val percent = ((bytesCopied * 100) / totalBytes).toInt()
                            emit(DownloadResult.FileResult(outputFile, percent))
                        }
                    }
                }
            }
            emit(DownloadResult.FileResult(outputFile, 100))
        }
    }.flowOn(Dispatchers.IO)
}