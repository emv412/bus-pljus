package com.buspljus.Adapteri

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.buspljus.Baza.Linije
import com.buspljus.R
import com.buspljus.Toster
import com.buspljus.VoziloInfo
import com.buspljus.VoziloInfo.Companion.danunedelji
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SpisakLinijaAdapter(private val context: Context, private val buttonLabels: List<String>) :
    RecyclerView.Adapter<SpisakLinijaAdapter.ButtonViewHolder>() {

    companion object {
        var odabranaLinija = mutableListOf<Any>()
    }

    class ButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dugme: Button = view.findViewById(R.id.dugme)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.dugme, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.dugme.text = buttonLabels[position]

        holder.dugme.setOnClickListener {
            val listaPol = mutableListOf<String>()
            var izabranDan = 0

            val dl = BottomSheetDialog(context)

            with (dl) {
                var podaciOLn = Linije(context).redVoznjeJednaLinija(buttonLabels[position],"0")

                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = false
                setContentView(R.layout.redvoznje_jednaln)
                val brojln = findViewById<TextView>(R.id.brojlinije)
                val smerovi = findViewById<TabLayout>(R.id.smerovi)
                val dani = findViewById<TabLayout>(R.id.dani)
                val nemaPolIMG = findViewById<ImageView>(R.id.nemapol_img)
                val nemaPolTextView = findViewById<TextView>(R.id.nemapol_tv)
                val mrezaPolazaka = findViewById<RecyclerView>(R.id.mrezaPolazaka)
                val datum = findViewById<TextView>(R.id.rv_od_datum)

                brojln?.text = podaciOLn[0] as String
                smerovi?.getTabAt(0)?.text = podaciOLn[2] as String
                smerovi?.getTabAt(1)?.text = podaciOLn[3] as String
                datum?.text = VoziloInfo(context).prikaziDatumRV(JSONArray(podaciOLn[4] as String))

                fun odradi(dan: Int) {
                    listaPol.clear()
                    try {
                        if (podaciOLn.isNotEmpty()) {
                            val redVoznje = JSONObject(podaciOLn[6] as String).getJSONObject("rv")
                            for (i in 0 until redVoznje.length()) {
                                val sat = redVoznje.keys().asSequence().elementAt(i)
                                for (k in 0 until redVoznje.getJSONArray(sat).getJSONArray(dan).length()) {
                                    val dv = LocalTime.parse(sat + ":" + redVoznje.getJSONArray(sat).getJSONArray(dan)[k], DateTimeFormatter.ofPattern("HH:mm"))
                                    listaPol.add(dv.toString())
                                }
                            }
                            with (odabranaLinija) {
                                clear()
                                add(buttonLabels[position])
                                add(podaciOLn[5] as JSONArray)
                                add(podaciOLn[1] as Int)
                            }
                        }

                        if (listaPol.size == 0) {
                            Handler(Looper.getMainLooper()).post {
                                nemaPolIMG?.visibility = View.VISIBLE
                                nemaPolTextView?.visibility = View.VISIBLE
                            }
                        }
                        else {
                            Handler(Looper.getMainLooper()).post {
                                nemaPolIMG?.visibility = View.GONE
                                nemaPolTextView?.visibility = View.GONE
                            }
                        }
                    }

                    catch (e:Exception) {
                        Toster(context).toster(context.resources.getString(R.string.nema_drugog_smera))
                    }

                    mrezaPolazaka?.layoutManager = GridLayoutManager(context, 5)
                    mrezaPolazaka?.adapter = RVLinijeAdapter(context, listaPol)
                }

                fun mikrofn(tab: Int?) {
                    podaciOLn = Linije(context).redVoznjeJednaLinija(buttonLabels[holder.adapterPosition], tab.toString())
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

                mikrofn(0)
                dani?.selectTab(dani.getTabAt(danunedelji))

                show()
            }
        }

    }

    override fun getItemCount(): Int = buttonLabels.size
}