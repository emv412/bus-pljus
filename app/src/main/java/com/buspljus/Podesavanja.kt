package com.buspljus

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


class Podesavanja : AppCompatActivity() {
    companion object {
        lateinit var deljenapodesavanja : SharedPreferences
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment: PreferenceFragmentCompat(),Preference.OnPreferenceChangeListener {
        var stalanprikazgb : Boolean = false
        var auto : Boolean = true

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.podesavanja, rootKey)
            val prikazgb2: CheckBoxPreference? = findPreference("prikazgb")
            val autounos: CheckBoxPreference? = findPreference("automatskiunos")
            val nadogradnja: Preference? = findPreference("nadogradnja")
            val o_programu: Preference? = findPreference("o_programu")

            deljenapodesavanja = PreferenceManager.getDefaultSharedPreferences(requireContext())
            stalanprikazgb = deljenapodesavanja.getBoolean("prikazgb", false)
            auto = deljenapodesavanja.getBoolean("automatskiunos",true)

            prikazgb2?.onPreferenceChangeListener = this
            autounos?.onPreferenceChangeListener = this

            nadogradnja?.onPreferenceClickListener = Preference.OnPreferenceClickListener { podesavanje : Preference? ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/emv412/bus-pljus/releases")))
                true
            }

            o_programu?.onPreferenceClickListener = Preference.OnPreferenceClickListener { podesavanje : Preference? ->
                startActivity(Intent(context, OProgramu::class.java))
                true
            }
        }

        override fun onPreferenceChange(podesavanje: Preference, podesenaVrednost: Any?): Boolean {
            when (podesavanje.key) {
                "prikazgb" -> {
                    deljenapodesavanja.edit().putBoolean("prikazgb", stalanprikazgb).apply()
                    return true
                }
                "automatskiunos" -> {
                    deljenapodesavanja.edit().putBoolean("automatskiunos",auto).apply()
                    return true
                }
            }
            return false
        }
    }
}