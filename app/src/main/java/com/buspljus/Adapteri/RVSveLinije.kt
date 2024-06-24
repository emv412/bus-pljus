package com.buspljus.Adapteri

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.buspljus.R
import com.buspljus.RedVoznje
import com.buspljus.SQLcitac
import com.buspljus.Toster
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class RVSveLinije(private val context: Context, private val buttonLabels: List<String>) :
    RecyclerView.Adapter<RVSveLinije.ButtonViewHolder>() {

    class ButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dugme: Button = view.findViewById(R.id.dugme)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.dugme, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.dugme.text = buttonLabels[position]

        /*
        holder.dugme.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 0)
        }

         */

        holder.dugme.setOnClickListener {
            var podaciOLn = SQLcitac(context).redVoznjeJednaLinija(buttonLabels[position],"0")
            val listaPol = mutableListOf<String>()
            var izabranDan = 0

            val dl = BottomSheetDialog(context)

            with (dl) {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = false
                setContentView(R.layout.redvoznje_jednaln)
                val brojln = findViewById<TextView>(R.id.brojlinije)
                val smerovi = findViewById<TabLayout>(R.id.smerovi)
                val dani = findViewById<TabLayout>(R.id.dani)
                val mrezaPolazaka = findViewById<RecyclerView>(R.id.mrezaPolazaka)
                val datum = findViewById<TextView>(R.id.rv_od_datum)

                brojln?.text = podaciOLn[0] as String
                smerovi?.getTabAt(0)?.text = podaciOLn[2] as String
                smerovi?.getTabAt(1)?.text = podaciOLn[3] as String
                datum?.text = RedVoznje(context).prikaziDatumRV(JSONArray(podaciOLn[6] as String))

                fun odradi(dan: Int) {
                    listaPol.clear()
                    try {
                        if (podaciOLn.isNotEmpty()) {
                            val redVoznje = JSONObject(podaciOLn[7] as String).getJSONObject("rv")
                            for (i in 0 until redVoznje.length()) {
                                val sat = redVoznje.keys().asSequence().elementAt(i)
                                for (k in 0 until redVoznje.getJSONArray(sat).getJSONArray(dan).length()) {
                                    val dv = LocalTime.parse(sat + ":" + redVoznje.getJSONArray(sat).getJSONArray(dan)[k], DateTimeFormatter.ofPattern("HH:mm"))
                                    listaPol.add(dv.toString())
                                }
                            }
                        }
                    }
                    catch(e:Exception) {
                        Toster(context).toster(context.resources.getString(R.string.nema_drugog_smera))
                    }

                    mrezaPolazaka?.layoutManager = GridLayoutManager(context, 5)
                    mrezaPolazaka?.adapter = RVSveLinijeInt(context, listaPol)
                }

                fun mikrofn(tab: Int?) {
                    podaciOLn = SQLcitac(context).redVoznjeJednaLinija(buttonLabels[holder.adapterPosition], tab.toString())
                    odradi(izabranDan)
                }

                smerovi?.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        mikrofn(tab?.position)
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {
                    }

                    override fun onTabReselected(tab: TabLayout.Tab?) {
                    }

                })

                dani?.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        if (tab != null) {
                            izabranDan = tab.position
                        }
                        odradi(izabranDan)
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {
                    }

                    override fun onTabReselected(tab: TabLayout.Tab?) {
                    }
                })

                //smerovi?.selectTab(smerovi.getTabAt(1))
                //dani?.selectTab(dani.getTabAt(danunedelji))
                mikrofn(0)

                show()
            }
        }

    }

    override fun getItemCount(): Int = buttonLabels.size
}