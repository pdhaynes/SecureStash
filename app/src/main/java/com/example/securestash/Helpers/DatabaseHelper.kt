package com.example.securestash.Helpers

import android.content.Context
import net.sqlcipher.database.SQLiteDatabase as SQLCipherDatabase
import net.sqlcipher.database.SQLiteOpenHelper as SQLCipherOpenHelper

class DatabaseHelper private constructor(context: Context) : SQLCipherOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    init {
        SQLCipherDatabase.loadLibs(context)
    }

    override fun onCreate(db: SQLCipherDatabase) {
        val createRouteTable = """
            CREATE TABLE IF NOT EXISTS $ROUTINGTABLE (
                $ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $FILENAME TEXT,
                $DISPLAYNAME TEXT,
                $FILETYPE TEXT,
                $FILEKIND TEXT,
                $FULLPATH TEXT,
                $LOCK INT,
                $FILEPASS TEXT,
                $ENCRYPTION TEXT
            )
        """
        db.execSQL(createRouteTable)

        val createUserTable = """
            CREATE TABLE IF NOT EXISTS $PINTABLE (
                $ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $USERPIN TEXT,
                $EMAILS TEXT
            )
        """
        db.execSQL(createUserTable)
        db.rawExecSQL("PRAGMA cipher_memory_security = ON;")
        db.rawExecSQL("PRAGMA key = '$DATABASE_PASSWORD';")
    }

    override fun onUpgrade(db: SQLCipherDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $ROUTINGTABLE")
        db.execSQL("DROP TABLE IF EXISTS $PINTABLE")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "data.db"
        private const val DATABASE_VERSION = 1
        private const val DATABASE_PASSWORD = "RnWzqSBBvo"

        // Global
        private const val ID = "id"

        // Routing Table
        private const val ROUTINGTABLE = "routing"
        private const val FILENAME = "filename"
        private const val DISPLAYNAME = "displayname"
        private const val FILETYPE = "filetype"
        private const val FILEKIND = "filekind"
        private const val FULLPATH = "path"
        private const val LOCK = "locked"
        private const val FILEPASS = "password"
        private const val ENCRYPTION = "encryption"

        //User Table
        private const val PINTABLE = "pins"
        private const val USERPIN = "pin"
        private const val EMAILS = "email"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
        }
    }

}
