package com.buspljus.Adapteri

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.buspljus.Glavna
import com.buspljus.R
import com.buspljus.Toster
import com.buspljus.VoziloInfo
import org.json.JSONArray
import org.oscim.layers.marker.MarkerItem
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class RVLinijeAdapter(private val context: Context, private val buttonLabels: List<String>) :
    RecyclerView.Adapter<RVLinijeAdapter.ButtonViewHolder>() {

    class ButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dugme: Button = view.findViewById(R.id.dugme)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.dugme, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.dugme.text = buttonLabels[position]

        holder.dugme.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 0)
        }

        holder.dugme.setOnClickListener {
            val ime = SpisakLinijaAdapter.odabranaLinija[0] as String
            val st = SpisakLinijaAdapter.odabranaLinija[1] as JSONArray

            try {
                Glavna().reset()
                VoziloInfo(context).crtanjeTrase(
                    MarkerItem(ime,null, null),
                    MarkerItem(st[0] as String, null, null), null,
                    LocalTime.parse(buttonLabels[position], DateTimeFormatter.ofPattern("HH:mm")), true
                )
            }
            catch (e: Exception) {
                Toster(context).toster(context.resources.getString(R.string.nema_trase))
            }
        }

    }

    override fun getItemCount(): Int = buttonLabels.size
}