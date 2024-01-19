package com.buspljus

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class Podesavanja : AppCompatActivity() {
    companion object {
        lateinit var deljenapodesavanja : SharedPreferences
        val verzija = BuildConfig.VERSION_NAME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment: PreferenceFragmentCompat(),Preference.OnPreferenceChangeListener {
        private var stalanprikazgb : Boolean = false
        var auto : Boolean = true

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.podesavanja, rootKey)
            val prikazgb2: CheckBoxPreference? = findPreference("prikazgb")
            val autounos: CheckBoxPreference? = findPreference("automatskiunos")
            val nadogradnja: Preference? = findPreference("nadogradnja")
            val oProgramu: Preference? = findPreference("o_programu")

            deljenapodesavanja = PreferenceManager.getDefaultSharedPreferences(requireContext())
            stalanprikazgb = deljenapodesavanja.getBoolean("prikazgb", false)
            auto = deljenapodesavanja.getBoolean("automatskiunos",true)

            prikazgb2?.onPreferenceChangeListener = this
            autounos?.onPreferenceChangeListener = this

            nadogradnja?.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ : Preference? ->
                Internet().zahtevPremaInternetu(null,4,object: Internet.ApiResponseCallback{
                    override fun onSuccess(response: Response) {
                        if (response.isSuccessful) {
                            val odgovor = JSONObject(response.body!!.string())
                            if (odgovor.getString("name").toDouble() > verzija.toDouble()) {

                                val prozorZaNadogradnju = context?.let {
                                    AlertDialog.Builder(it)
                                        .setTitle(resources.getString(R.string.dostupna_nadogradnja))
                                        .setMessage(odgovor.getString("body"))
                                        .setPositiveButton(resources.getString(R.string.preuzmi)) { dialog, _ ->
                                            dialog.dismiss()
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                                                odgovor.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"))))
                                        }
                                }

                                Handler(Looper.getMainLooper()).post {
                                    prozorZaNadogradnju?.create()?.show()
                                }
                            }
                            else context?.let { Toster(it).toster(resources.getString(R.string.nema_nadogradnje)) }
                        }
                    }

                    override fun onFailure(e: IOException) {
                        context?.let {Toster(it).toster(resources.getString(R.string.nema_interneta))}
                    }
                })
                true
            }

            oProgramu?.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ : Preference? ->
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