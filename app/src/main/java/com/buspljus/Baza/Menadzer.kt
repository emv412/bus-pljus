package com.buspljus.Baza

import android.content.Context
import android.database.sqlite.SQLiteDatabase

class DatabaseManager private constructor(appContext: Context) {

    companion object {
        @Volatile private var instance: DatabaseManager? = null

        fun getInstance(context: Context): DatabaseManager {
            return instance ?: synchronized(this) {
                instance ?: DatabaseManager(context.applicationContext).also { instance = it }
            }
        }
    }

    @Volatile private var dbInstance: SQLiteDatabase? = null
    private val dbPath: String = appContext.getDatabasePath(PosrednikBaze.IME_BAZE).absolutePath

    val database: SQLiteDatabase
        @Synchronized
        get() {
            if (dbInstance == null || !dbInstance!!.isOpen) {
                dbInstance = SQLiteDatabase.openDatabase(
                    dbPath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.NO_LOCALIZED_COLLATORS
                ).apply {
                    enableWriteAheadLogging()
                }
            }
            return dbInstance!!
        }

    fun isWritable(): Boolean = dbInstance?.let { it.isOpen && !it.isReadOnly } == true
}
