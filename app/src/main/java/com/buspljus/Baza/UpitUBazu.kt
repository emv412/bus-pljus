package com.buspljus.Baza

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.buspljus.Baza.PosrednikBaze.Companion.globalQueryData
import com.buspljus.SacuvanaStanica
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.Inflater

abstract class UpitUBazu(val context: Context) {

    protected val baza: SQLiteDatabase
        get() = DatabaseManager.getInstance(context.applicationContext).database

    protected fun isDatabaseOpen(): Boolean =
        DatabaseManager.getInstance(context.applicationContext).isOpen()

    protected fun isDatabaseWritable(): Boolean =
        DatabaseManager.getInstance(context.applicationContext).isWritable()

    protected inline fun <T> queryAndProcess(
        table: String,
        columns: Array<String>,
        selection: String,
        selectionArgs: Array<String>,
        orderBy: String? = null,
        processor: (Cursor) -> T
    ): T {
        return baza.query(table, columns, selection, selectionArgs, null, null, orderBy).use(processor)
    }

    fun SQLzahtev(
        tabela: String,
        kolone: Array<String>,
        odabir: String,
        parametri: Array<String>,
        redjanjepo: String?
    ): Cursor {
        globalQueryData = SacuvanaStanica(tabela, kolone, odabir, parametri, redjanjepo)
        return baza.query(tabela, kolone, odabir, parametri, null, null, redjanjepo)
    }

    fun upisUBazu(
        table: String,
        values: ContentValues,
        whereClause: String,
        whereArgs: Array<String>
    ): Int {
        if (!isDatabaseWritable()) throw IllegalStateException("Database is not writable")
        return baza.update(table, values, whereClause, whereArgs)
    }

    fun unzip(podatak: ByteArray, charset: Charset = Charsets.UTF_8): String {
        val inflater = Inflater()
        val buffer = ByteArray(1024)
        val output = ByteArrayOutputStream()
        try {
            inflater.setInput(podatak)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                output.write(buffer, 0, count)
            }
            return output.toString(charset.name())
        } finally {
            inflater.end()
            output.close()
        }
    }
}
