package com.buspljus

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException


class Podesavanja : AppCompatActivity() {
    companion object {
        lateinit var deljenapodesavanja : SharedPreferences
        val verzija = BuildConfig.VERSION_NAME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_podesavanja)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    class SettingsFragment: PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
        private var stalanprikazgb : Boolean = false
        private var auto : Boolean = true

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.podesavanja, rootKey)

            val prikazgb2: CheckBoxPreference? = findPreference("prikazgb")
            val autounos: CheckBoxPreference? = findPreference("automatskiunos")
            val nadogradnjaPrograma: Preference? = findPreference("nadogradi_program")
            val nadogradnjaMape: Preference? = findPreference("nadogradimapu")
            val nadogradnjaStanica: Preference? = findPreference("nadogradistanice")
            val oProgramu: Preference? = findPreference("o_programu")
            val prikazPoRV: CheckBoxPreference? = findPreference("prikazporv")
            val mnozilac: Preference? = findPreference("mnozilac")
            val vremePolaskaPodesavanje: CheckBoxPreference? = findPreference("vremepolaska")

            deljenapodesavanja = PreferenceManager.getDefaultSharedPreferences(requireContext())
            stalanprikazgb = deljenapodesavanja.getBoolean("prikazgb", false)
            auto = deljenapodesavanja.getBoolean("automatskiunos", true)

            prikazgb2?.onPreferenceChangeListener = this
            autounos?.onPreferenceChangeListener = this
            prikazPoRV?.onPreferenceClickListener = this
            vremePolaskaPodesavanje?.onPreferenceClickListener = this

            nadogradnjaPrograma?.onPreferenceClickListener = this
            nadogradnjaMape?.onPreferenceClickListener = this
            nadogradnjaStanica?.onPreferenceClickListener = this
            oProgramu?.onPreferenceClickListener = this
            mnozilac?.onPreferenceClickListener = this
            vremePolaskaPodesavanje?.onPreferenceClickListener = this
        }

        override fun onPreferenceChange(podesavanje: Preference, podesenaVrednost: Any?): Boolean {
            when (podesavanje.key) {
                "prikazgb" -> {
                    deljenapodesavanja.edit() { putBoolean("prikazgb", stalanprikazgb) }
                    return true
                }
                "automatskiunos" -> {
                    deljenapodesavanja.edit() { putBoolean("automatskiunos", auto) }
                    return true
                }
            }
            return false
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            when (preference.key) {
                "nadogradi_program" -> {
                    Internet().zahtevPremaInternetu(null, null, 4, object : Interfejs.odgovorSaInterneta {
                        override fun uspesanOdgovor(response: Response) {
                            if (response.isSuccessful) {
                                val odgovor = JSONObject(response.body!!.string())
                                if (odgovor.getString("name").toDouble() > verzija.toDouble()) {
                                    val prozorZaNadogradnju = context?.let {
                                        AlertDialog.Builder(it)
                                            .setTitle(resources.getString(R.string.dostupna_nadogradnja))
                                            .setMessage(odgovor.getString("body"))
                                            .setPositiveButton(resources.getString(R.string.preuzmi)) { dialog, _ ->
                                                dialog.dismiss()
                                                startActivity(Intent(Intent.ACTION_VIEW,
                                                    odgovor.getJSONArray("assets").getJSONObject(0)
                                                        .getString("browser_download_url").toUri()))
                                            }
                                    }
                                    Handler(Looper.getMainLooper()).post {
                                        prozorZaNadogradnju?.create()?.show()
                                    }
                                } else {
                                    context?.let { Toster(it).toster(resources.getString(R.string.nema_nadogradnje)) }
                                }
                            }
                        }

                        override fun neuspesanOdgovor(e: IOException) {
                            context?.let { Toster(it).toster(resources.getString(R.string.greska_sa_vezom)) }
                        }
                    })
                }
                "nadogradimapu" -> {
                    Internet().zahtevPremaInternetu(null, null, 2, object : Interfejs.odgovorSaInterneta {
                        override fun uspesanOdgovor(response: Response) {
                            val preuzeto = JSONObject(response.body!!.string())
                            val velicinaMape = preuzeto.getDouble("size")
                            Handler(Looper.getMainLooper()).post {
                                AlertDialog(preference.context).preuzimanjeMapeiliStanica(0, velicinaMape, object: Interfejs.odgovor {
                                    override fun da(odg: Boolean) {
                                        Internet().zahtevPremaInternetu(null, null, 5, object : Interfejs.odgovorSaInterneta {
                                            override fun uspesanOdgovor(response: Response) {
                                                val mapa = response.body!!.source().inputStream()
                                                Internet().gunzip(mapa, File(preference.context.filesDir, "beograd.map"))
                                                Toster(preference.context).toster(preference.context.resources.getString(R.string.mapa_nadogradjena))
                                                Glavna.mapa.updateMap()
                                            }

                                            override fun neuspesanOdgovor(e: IOException) {
                                                Toster(preference.context).toster(resources.getString(R.string.greska_sa_vezom))
                                            }
                                        })
                                    }
                                })
                            }
                        }

                        override fun neuspesanOdgovor(e: IOException) {
                            Toster(preference.context).toster(resources.getString(R.string.greska_sa_vezom))
                        }
                    })
                }
                "nadogradistanice" -> {
                    Internet().zahtevPremaInternetu(null, null, 3, object : Interfejs.odgovorSaInterneta {
                        override fun uspesanOdgovor(response: Response) {
                            val preuzeto = JSONObject(response.body!!.string())
                            val velicinaBaze = preuzeto.getDouble("size")
                            Handler(Looper.getMainLooper()).post {
                                AlertDialog(preference.context).preuzimanjeMapeiliStanica(1, velicinaBaze, object: Interfejs.odgovor {
                                    override fun da(odg: Boolean) {
                                        Internet().zahtevPremaInternetu(null, null, 6, object : Interfejs.odgovorSaInterneta {
                                            override fun uspesanOdgovor(response: Response) {
                                                val baza = response.body!!.source().inputStream()
                                                val sacuvanestanice = SQLcitac(preference.context).dobaviSacuvaneStanice()
                                                preference.context.getDatabasePath(SQLcitac.IME_BAZE)?.path?.let { File(it) }?.let {
                                                    Internet().gunzip(baza, it)
                                                }
                                                SQLcitac(preference.context).sacuvajStanicu(sacuvanestanice,1)
                                                Toster(preference.context).toster(preference.context.resources.getString(R.string.baza_nadogradjena))
                                            }

                                            override fun neuspesanOdgovor(e: IOException) {
                                                Toster(preference.context).toster(resources.getString(R.string.greska_sa_vezom))
                                            }
                                        })
                                    }
                                })
                            }
                        }

                        override fun neuspesanOdgovor(e: IOException) {
                            Toster(preference.context).toster(resources.getString(R.string.greska_sa_vezom))
                        }
                    })
                }
                "o_programu" -> {
                    startActivity(Intent(context, OProgramu::class.java))
                }
                "mnozilac" -> {
                    AlertDialog(preference.context).podesavanjeMnozioca()
                }
            }
            return true
        }
    }
}
