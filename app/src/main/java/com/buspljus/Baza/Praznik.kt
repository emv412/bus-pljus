package com.buspljus.Baza

import android.content.Context
import com.buspljus.PopupProzor
import java.time.LocalDate

class Praznik(context: Context) : UpitUBazu(context) {

    fun praznik(): Int {
        return try {
            queryAndProcess(
                PosrednikBaze.PRAZNIK_TABLE,
                arrayOf("dan", "mesec"),
                "godina = ? and mesec = ? and dan = ?",
                arrayOf(
                    LocalDate.now().year.toString(),
                    LocalDate.now().month.value.toString(),
                    LocalDate.now().dayOfMonth.toString()
                )
            ) { cursor -> if (cursor.count > 0) 2 else 0 }
        } catch (g: Exception) {
            PopupProzor(context).prikaziGresku(g)
            0
        }
    }
}