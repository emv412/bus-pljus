package com.buspljus.grafika

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.buspljus.R

class OProgramu: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.o_programu)

        val osm = findViewById<Button>(R.id.osm)
        val vtm = findViewById<Button>(R.id.vtm)

        osm.setOnClickListener{
            startActivity(Intent(Intent.ACTION_VIEW, "https://www.openstreetmap.org".toUri()))
        }

        vtm.setOnClickListener{
            startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/mapsforge/vtm/".toUri()))
        }
    }
}