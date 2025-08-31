package com.buspljus.Vozilo

import com.buspljus.Internet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.IOException
import org.json.JSONArray

class VoziloRemoteDataSource {

    fun fetchStaniceFlow(
        stanica: String,
        linija: String
    ): Flow<JSONArray> = Internet()
        .downloadAsFlow(stanica, linija, 1)
        .map { rezultat ->
            if (rezultat is Internet.DownloadResult.Text) {
                JSONArray(rezultat.content)
            } else {
                throw IOException("Neočekivan tip rezultata")
            }
        }
}


