package com.buspljus

import android.content.Intent
import android.os.Bundle
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
            skidanjeMapeibaze(2)
            skidanjeMapeibaze(3)
        }
    }

    fun skidanjeMapeibaze(arg: Int) {
        Internet().zahtevPremaInternetu(null,arg,object: Internet.ApiResponseCallback {
            override fun onSuccess(response: Response) {
                val preuzeto = response.body!!.source().inputStream()
                when (arg) {
                    2 -> {
                        Internet().gunzip(preuzeto,File(filesDir,"beograd.map"))
                        postotak.progress=50
                    }
                    3 -> {
                        Internet().gunzip(preuzeto, File(getDatabasePath("stanice.db").path))
                        postotak.progress=100
                    }
                }
                proveraprisustvafajlova()
            }

            override fun onFailure(e: IOException) {
                Toster(this@Prvootvaranje).toster(resources.getString(R.string.nema_interneta))
            }
        })
    }

    fun proveraprisustvafajlova() {
        if (File(filesDir,"beograd.map").exists() and File(getDatabasePath("stanice.db").path).exists()) {
            startActivity(Intent(this, Glavna::class.java))
            finish()
        }
    }
}