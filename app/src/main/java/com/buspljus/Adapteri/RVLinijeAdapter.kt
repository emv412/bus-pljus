package com.buspljus.Adapteri

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.buspljus.R
import org.json.JSONArray
import org.oscim.layers.marker.MarkerItem
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class RVLinijeAdapter(
    private val vremenaPolazaka: List<String>,
    private val nacrtajTrasu: (MarkerItem, MarkerItem, String?, LocalTime) -> Unit
) : RecyclerView.Adapter<RVLinijeAdapter.ButtonViewHolder>() {

    inner class ButtonViewHolder(val dugme: Button) : RecyclerView.ViewHolder(dugme) {
        fun bind(label: String) {
            dugme.text = label
            dugme.setOnClickListener {
                val ime = SpisakLinijaAdapter.odabranaLinija.getOrNull(0) as? String ?: return@setOnClickListener
                val stName = (SpisakLinijaAdapter.odabranaLinija.getOrNull(1) as? JSONArray)?.optString(0)
                    ?.takeIf { it.isNotBlank() } ?: return@setOnClickListener
                val time = LocalTime.parse(label, DateTimeFormatter.ofPattern("HH:mm"))

                nacrtajTrasu(
                    MarkerItem(ime, null, null),
                    MarkerItem(stName, null, null),
                    null,
                    time
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.dugme, parent, false) as Button
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bind(vremenaPolazaka[position])
    }

    override fun getItemCount(): Int = vremenaPolazaka.size
}
