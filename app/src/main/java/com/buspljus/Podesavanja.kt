package com.buspljus

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.buspljus.Baza.PosrednikBaze
import com.buspljus.Baza.Stanice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    lifecycleScope.launch {
                        Internet().downloadAsFlow(null, null, 4).collect { rezultat ->
                            if (rezultat is Internet.DownloadResult.Text) {
                                val odgovor = JSONObject(rezultat.content)
                                val remoteVersion = odgovor.getString("name").toDoubleOrNull()
                                val localVersion = verzija.toDoubleOrNull()

                                if (remoteVersion != null && localVersion != null && remoteVersion > localVersion) {
                                    val dialog = AlertDialog.Builder(requireContext())
                                        .setTitle(getString(R.string.dostupna_nadogradnja))
                                        .setMessage(odgovor.getString("body"))
                                        .setPositiveButton(getString(R.string.preuzmi)) { d, _ ->
                                            d.dismiss()
                                            startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    odgovor.getJSONArray("assets")
                                                        .getJSONObject(0)
                                                        .getString("browser_download_url")
                                                        .toUri()
                                                )
                                            )
                                        }
                                        .create()

                                    dialog.show()
                                } else {
                                    Toster(requireContext()).toster(getString(R.string.nema_nadogradnje))
                                }
                            }
                        }
                    }
                }
                "nadogradimapu" -> lifecycleScope.launch {
                    try {
                        // 1. Preuzmi JSON sa veličinom mape
                        Internet().downloadAsFlow(null, null, 2)
                            .collect { rezultat ->
                                if (rezultat is Internet.DownloadResult.Text) {
                                    val preuzeto = JSONObject(rezultat.content)
                                    val velicinaMape = preuzeto.getDouble("size")

                                    // 2. Popup na glavnom thread-u
                                    withContext(Dispatchers.Main) {
                                        PopupProzor(preference.context)
                                            .preuzimanjeMapeiliStanica(0, velicinaMape, object : Interfejs.odgovor {
                                                override fun da(odg: Boolean) {
                                                    if (odg) {
                                                        // 3. Skidanje i raspakivanje mape
                                                        lifecycleScope.launch {
                                                            val tempMapFile = File(preference.context.cacheDir, "mapa.gz")
                                                            Internet().downloadAsFlow(null, null, 5, tempMapFile)
                                                                .collect { fileResult ->
                                                                    if (fileResult is Internet.DownloadResult.FileResult &&
                                                                        fileResult.progress == 100
                                                                    ) {
                                                                        Raspakivanje().gunzip(
                                                                            tempMapFile.inputStream(),
                                                                            File(preference.context.filesDir, "beograd.map")
                                                                        )
                                                                        withContext(Dispatchers.Main) {
                                                                            Toster(preference.context).toster(
                                                                                preference.context.getString(R.string.mapa_nadogradjena)
                                                                            )
                                                                            Glavna.mapa.updateMap()
                                                                        }
                                                                    }
                                                                }
                                                        }
                                                    }
                                                }
                                            })
                                    }
                                }
                            }
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            Toster(preference.context).toster(
                                preference.context.getString(R.string.greska_sa_vezom)
                            )
                        }
                    }
                }

                "nadogradistanice" -> lifecycleScope.launch {
                    try {
                        // 1. Preuzmi JSON sa veličinom baze
                        Internet().downloadAsFlow(null, null, 3)
                            .collect { rezultat ->
                                if (rezultat is Internet.DownloadResult.Text) {
                                    val preuzeto = JSONObject(rezultat.content)
                                    val velicinaBaze = preuzeto.getDouble("size")

                                    // 2. Popup na glavnom thread-u
                                    withContext(Dispatchers.Main) {
                                        PopupProzor(preference.context)
                                            .preuzimanjeMapeiliStanica(1, velicinaBaze, object : Interfejs.odgovor {
                                                override fun da(odg: Boolean) {
                                                    if (odg) {
                                                        // 3. Skidanje i raspakivanje baze
                                                        lifecycleScope.launch {
                                                            val tempDbFile = File(preference.context.cacheDir, "baza.gz")
                                                            Internet().downloadAsFlow(null, null, 6, tempDbFile)
                                                                .collect { fileResult ->
                                                                    if (fileResult is Internet.DownloadResult.FileResult &&
                                                                        fileResult.progress == 100
                                                                    ) {
                                                                        val sacuvaneStanice =
                                                                            Stanice(preference.context).dobaviSacuvaneStanice()
                                                                        Raspakivanje().gunzip(
                                                                            tempDbFile.inputStream(),
                                                                            preference.context.getDatabasePath(PosrednikBaze.IME_BAZE)
                                                                        )
                                                                        for (i in sacuvaneStanice.keys) {
                                                                            Stanice(preference.context)
                                                                                .sacuvajStanicu(i, 1)
                                                                        }
                                                                        withContext(Dispatchers.Main) {
                                                                            Toster(preference.context).toster(
                                                                                preference.context.getString(R.string.baza_nadogradjena)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                        }
                                                    }
                                                }
                                            })
                                    }
                                }
                            }
                    } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            Toster(preference.context).toster(
                                preference.context.getString(R.string.greska_sa_vezom)
                            )
                        }
                    }
                }

                "o_programu" -> {
                    startActivity(Intent(context, OProgramu::class.java))
                }

                "mnozilac" -> {
                    PopupProzor(preference.context).podesavanjeMnozioca()
                }
            }
            return true
        }
    }
}
