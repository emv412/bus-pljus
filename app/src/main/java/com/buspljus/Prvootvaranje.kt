package com.buspljus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Response
import java.io.File
import java.io.IOException


class Prvootvaranje: AppCompatActivity() {

    private lateinit var postotak : ProgressBar
    private lateinit var preuzimanje : Button
    override fun onCreate(SavedInstanceState: Bundle?) {
        super.onCreate(SavedInstanceState)
        setContentView(R.layout.prvootvaranje)
        preuzimanje = findViewById(R.id.preuzmi)
        postotak = findViewById(R.id.postotak)

        proveraprisustvafajlova()

        preuzimanje.setOnClickListener {
            preuzimanje.isEnabled=false
            skidanjeMapeibaze()
        }
    }

    fun skidanjeMapeibaze() {
        for (i in 2 .. 3){
            Internet().zahtevPremaInternetu(null,null, i, object: Internet.odgovorSaInterneta {
                override fun uspesanOdgovor(response: Response) {
                    val preuzeto = response.body!!.source().inputStream()
                    when (i) {
                        2 -> {
                            Internet().gunzip(preuzeto,File(filesDir,"beograd.map"))
                            Log.d(resources.getString(R.string.debug),"Skinut beograd.map")
                            postotak.progress=100
                        }
                        3 -> {
                            Internet().gunzip(preuzeto, File(getDatabasePath(SQLcitac.IME_BAZE).path))
                            Log.d(resources.getString(R.string.debug),"Skinuti svi_podaci.db")
                            postotak.progress=50
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
        if (File(filesDir,"beograd.map").exists() and File(getDatabasePath(SQLcitac.IME_BAZE).path).exists()) {
            startActivity(Intent(this, Glavna::class.java))
            finish()
        }
    }
}