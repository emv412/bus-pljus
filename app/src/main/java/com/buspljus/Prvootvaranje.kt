package com.buspljus

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Response
import java.io.File
import java.io.IOException


class Prvootvaranje: AppCompatActivity() {

    private lateinit var postotak : ProgressBar
    private lateinit var preuzimanje : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.prvootvaranje)
        preuzimanje = findViewById(R.id.preuzmi)
        postotak = findViewById(R.id.postotak)

        proveraprisustvafajlova()

        preuzimanje.setOnClickListener {
            skidanjeMapeibaze()
        }
    }

    fun skidanjeMapeibaze() {
        for (i in 5 .. 6) {
            Internet().zahtevPremaInternetu(null, null, i, object: Interfejs.odgovorSaInterneta {
                override fun uspesanOdgovor(response: Response) {
                    Handler(Looper.getMainLooper()).post { preuzimanje.isEnabled = false }
                    val preuzeto = response.body!!.source().inputStream()
                    when (i) {
                        5 -> {
                            Internet().gunzip(preuzeto,File(filesDir,"beograd.map"))
                        }
                        6 -> {
                            Internet().gunzip(preuzeto, File(getDatabasePath(SQLcitac.IME_BAZE).path))
                        }
                    }
                    proveraprisustvafajlova()
                }

                override fun neuspesanOdgovor(e: IOException) {
                    Toster(this@Prvootvaranje).toster(resources.getString(R.string.nema_interneta))
                }
            })
        }

    }

    fun proveraprisustvafajlova() {
        if (File(filesDir,"beograd.map").exists()) {
            Handler(Looper.getMainLooper()).post { postotak.progress=50 }
        }
        if (File(filesDir,"beograd.map").exists() and File(getDatabasePath(SQLcitac.IME_BAZE).path).exists()) {
            Handler(Looper.getMainLooper()).post { postotak.progress = 100 }
            startActivity(Intent(this, Glavna::class.java))
            finish()
        }
    }
}