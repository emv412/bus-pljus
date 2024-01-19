package com.buspljus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OProgramu: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.o_programu)

        val osm = findViewById<Button>(R.id.osm)
        val vtm = findViewById<Button>(R.id.vtm)

        osm.setOnClickListener{
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org")))
        }

        vtm.setOnClickListener{
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mapsforge/vtm/")))
        }
    }
}