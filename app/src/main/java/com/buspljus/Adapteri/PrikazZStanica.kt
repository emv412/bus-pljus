package com.buspljus.Adapteri

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import com.buspljus.R
import com.buspljus.SQLcitac
import java.time.LocalTime

class PrikazZStanica(var context: Context, var spoljnalista: List<List<String>>, var spisak: ExpandableListView) : BaseExpandableListAdapter() {

    var lista = mutableListOf(listOf<String>())
    var novalista = listOf(listOf<String>())
    var poslednjeProsirenaGrupa = -1

    init {
        spisak.setOnGroupClickListener { _, _, groupPosition, _ ->
            if ((groupPosition != poslednjeProsirenaGrupa)) {
                spisak.collapseGroup(poslednjeProsirenaGrupa)
                poslednjeProsirenaGrupa = groupPosition
                spisak.expandGroup(groupPosition)
            }
            else {
                spisak.collapseGroup(groupPosition)
                poslednjeProsirenaGrupa = -1
            }
            true
        }
    }

    override fun getGroupCount(): Int {
        return spoljnalista.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        lista = SQLcitac(context).preradaRVJSON(null, spoljnalista[groupPosition][3], spoljnalista[groupPosition][4], 0, null)
        lista.add(listOf(spoljnalista[groupPosition][0], spoljnalista[groupPosition][2]))
        novalista = lista.sortedBy { it[1] }

        return novalista.size
    }

    override fun getGroup(groupPosition: Int): CharSequence {
        return spoljnalista[groupPosition].toString()
    }

    override fun getChild(groupPosition: Int, childPosition: Int): CharSequence {
        return spoljnalista[groupPosition][childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        with (convertView?: LayoutInflater.from(context).inflate(R.layout.zs_vecalista, parent, false)) {
            val odred = findViewById<TextView>(R.id.odrediste_voz)
            val odredVremepol = findViewById<TextView>(R.id.odrediste_vremepol)

            odred?.text = spoljnalista[groupPosition][0]
            odredVremepol?.text = spoljnalista[groupPosition][1]
            return this
        }
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        with (convertView?: LayoutInflater.from(context).inflate(R.layout.zs_manjalista, parent, false)) {
            val medjust = findViewById<TextView>(R.id.medjust)
            val vreme = findViewById<TextView>(R.id.vreme)

            if (novalista.size > childPosition) {
                medjust?.text = novalista[childPosition][0]
                vreme?.text = novalista[childPosition][1]
                medjust?.setTextColor(resources.getColor(R.color.crna))
                vreme?.setTextColor(resources.getColor(R.color.crna))

                if (LocalTime.parse(novalista[childPosition][1]).isBefore(LocalTime.parse(spoljnalista[groupPosition][1]))) {
                    medjust?.setTextColor(resources.getColor(R.color.siva))
                    vreme?.setTextColor(resources.getColor(R.color.siva))
                }
            }
            return this
        }
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return false
    }
}