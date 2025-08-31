package com.buspljus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.buspljus.Baza.PosrednikBaze
import kotlinx.coroutines.launch
import java.io.File


class Prvootvaranje : AppCompatActivity() {

    private lateinit var postotak: ProgressBar
    private lateinit var preuzimanje: Button
    private val internet = Internet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.prvootvaranje)

        postotak = findViewById(R.id.postotak)
        preuzimanje = findViewById(R.id.preuzmi)

        checkFilesPresence()

        preuzimanje.setOnClickListener {
            preuzimanje.isEnabled = false
            lifecycleScope.launch {
                val tempMapFile = File(cacheDir, "mapa.gz")
                Internet().downloadAsFlow(null, null, 5, tempMapFile)
                    .collect { result ->
                        if (result is Internet.DownloadResult.FileResult) {
                            postotak.progress = result.progress / 2
                        }
                    }
                Raspakivanje().gunzip(tempMapFile.inputStream(), File(filesDir, "beograd.map"))

                val tempDbFile = File(cacheDir, "baza.gz")
                Internet().downloadAsFlow(null, null, 6, tempDbFile)
                    .collect { result ->
                        if (result is Internet.DownloadResult.FileResult) {
                            postotak.progress = 50 + (result.progress / 2)
                        }
                    }
                Raspakivanje().gunzip(tempDbFile.inputStream(), getDatabasePath(PosrednikBaze.IME_BAZE))

                checkFilesPresence()
            }

        }
    }

    private fun checkFilesPresence() {
        if (File(filesDir, "beograd.map").exists() &&
            File(getDatabasePath(PosrednikBaze.IME_BAZE).path).exists()
        ) {
            startActivity(Intent(this, Glavna::class.java))
            finish()
        } else {
            preuzimanje.isEnabled = true
        }
    }
}
