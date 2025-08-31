package com.buspljus.Baza

import android.content.Context
import android.database.Cursor
import com.buspljus.Interfejs
import com.buspljus.PreradaRV
import com.buspljus.SacuvanaStanica
import com.buspljus.Vozilo.VoziloKlikHandler
import com.buspljus.Vozilo.VoziloRemoteDataSource
import com.buspljus.Vozilo.VoziloRepository
import org.json.JSONObject
import org.oscim.core.GeoPoint
import java.time.LocalTime

class PosrednikBaze(context: Context) : UpitUBazu(context) {
    companion object {
        const val IME_BAZE = "svi_podaci.db"
        const val ID_KOLONA = "_id"
        const val CIR_KOLONA = "naziv_cir"
        const val LAT_KOLONA = "naziv_lat"
        const val ASCII_KOLONA = "naziv_ascii"
        const val STANICABGVOZNAZIV = "naziv"
        const val SACUVANA = "sacuvana"
        const val LATITUDE = "lt"
        const val LONGITUDE = "lg"
        const val STANICE_TABLE = "stanice"
        const val LINIJE_TABLE = "linije"
        const val BGVOZ_TABLE = "bgvoz"
        const val PRAZNIK_TABLE = "praznici"
        const val RED_VOZNJE = "redvoznje"
        const val OKRETNICA_OD = "od"
        const val OKRETNICA_DO = "do"
        const val DATUM_RV = "datumrv"
        const val LISTA_STAJALISTA = "stajalista"
        const val SMER = "smer"
        const val IZMENJENALINIJA_BOOLEAN = "izmenjenaLN"
        const val TRASA = "trasa"
        const val IZMENJENATRASA = "izmenjenaTrasa"
        const val IZMENJENA_STAJALISTA = "izmenjenaStajalista"

        var globalQueryData: SacuvanaStanica? = null
    }

    private val stationService by lazy { Stanice(context) }
    private val holidayService by lazy { Praznik(context) }
    private val mapSearchService by lazy { InterakcijaMapa(context) }



    fun idStaniceuGeoPoint(sifra: String): GeoPoint =
        stationService.idStaniceuGeoPoint(sifra)

    fun idStaniceUNaziv(sifra: String): String =
        stationService.idStaniceUNaziv(sifra)

    fun praznik(): Int =
        holidayService.praznik()

    fun pretragabaze_kliknamapu(lat: String, lng: String, callback: Interfejs.Callback, pozivOdFunkcije: Int) =
        mapSearchService.pretragabaze_kliknamapu(lat, lng, callback, pozivOdFunkcije)

    fun dobavisifre(rec: CharSequence?, trazenjepobroju: Boolean): Cursor =
        stationService.dobaviSifre(rec,trazenjepobroju)

    fun preradaRVJSON(
        redVoznjeJSON: JSONObject?,
        brojVoza: String?,
        sifraOS: String?,
        rezimRada: Int,
        vremeDol: LocalTime?
    ): MutableList<List<String>> {
        return PreradaRV(context).preradaRVJSON(redVoznjeJSON, brojVoza, sifraOS, rezimRada, vremeDol)
    }

    fun klikNaVozilo(
        linija: String,
        smer: String?,
        stanica: String,
        sledeceStajaliste: String?,
        vratiListu: Interfejs.vracenaLista
    ) {
        val repository = VoziloRepository(this)
        val remote = VoziloRemoteDataSource()
        val klikHandler = VoziloKlikHandler(context, repository, remote)

        klikHandler.klikNaVozilo(linija, smer, stanica, sledeceStajaliste, vratiListu)
    }
}