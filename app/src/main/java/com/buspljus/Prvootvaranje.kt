package com.buspljus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.system.exitProcess


class Prvootvaranje: Activity() {
    override fun onCreate(SavedInstanceState: Bundle?) {
        super.onCreate(SavedInstanceState)
        setContentView(R.layout.prvootvaranje)
        val preuzimanje = findViewById<Button>(R.id.preuzmi)
        //val postotak = findViewById<ProgressBar>(R.id.postotak) //mrzi me sad


        if (File(filesDir,"beograd.map").exists()) {
            try {
                val intent = Intent(this, Glavna::class.java)
                startActivity(intent)
                finish()
            } catch (g: Exception) {
                Toster(this).toster("greska: "+g)
                exitProcess(0)
            }
        }
        preuzimanje.setOnClickListener {
            skidanjemape()
            preuzimanje.isClickable=false
            preuzimanje.isEnabled=false
        }

    }


    fun skidanjemape() {
        OkHttpClient().newCall(Request.Builder().url("https://github.com/emv412/buspljus/raw/main/za_app.zip").build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Toster(this@Prvootvaranje).toster("Nema interneta, ili je greska na serveru")
            }

            override fun onResponse(call: Call, response: Response) {
                val downloadedFile = File(cacheDir, "beograd.zip")
                val sink: BufferedSink = downloadedFile.sink().buffer()
                sink.writeAll(response.body!!.source())
                sink.close()
                try {
                    unzip(downloadedFile,filesDir)
                    try {
                        //Kopiranje a onda brisanje fajla stanice.db. Dok se ne osmisli nesto bolje (premestanje)!
                        File(filesDir,"stanice.db").copyTo(File(getDatabasePath("stanice.db").path),true)
                        File(filesDir,"stanice.db").delete()
                        File(cacheDir,"beograd.zip").delete()
                        startActivity(Intent(this@Prvootvaranje, Glavna::class.java))
                        finish()

                    }
                    catch(e: Exception) {
                        Toster(this@Prvootvaranje).toster("Greska pri podesavanju baze stanica : "+e)
                    }

                }
                catch (e: Exception){
                    Toster(this@Prvootvaranje).toster("Greska prilikom raspakivanja zip fajla: "+e)
                }

            }
        }
        )
    }

    fun unzip(zipFile: File, destinationDir: File) {
        // Create the destination directory if it doesn't exist
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val buffer = ByteArray(1024)

        try {
            // Create a ZipInputStream to read the ZIP file
            val zipInputStream = ZipInputStream(FileInputStream(zipFile))

            var entry: ZipEntry? = zipInputStream.nextEntry

            while (entry != null) {
                val entryFile = File(destinationDir, entry.name)
                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    // Create parent directories if they don't exist
                    entryFile.parentFile?.mkdirs()

                    // Create an output stream to write the entry content
                    val outputStream = FileOutputStream(entryFile)

                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } > 0) {
                        outputStream.write(buffer, 0, len)
                    }

                    outputStream.close()
                }

                entry = zipInputStream.nextEntry
            }

            // Close the ZipInputStream
            zipInputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}