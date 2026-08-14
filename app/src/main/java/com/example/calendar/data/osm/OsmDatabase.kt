package com.example.calendar.data.osm

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

object OsmDatabase {

    private const val DB_NAME = "osm_normalized.db"

    @Volatile
    private var INSTANCE: SQLiteDatabase? = null

    /**
     * OSM DBを取得する。
     *
     * assets/osm_normalized.db を
     * アプリ内部の databases ディレクトリへ
     * 初回のみコピーしてから読み取り専用で開く。
     */
    fun getDatabase(context: Context): SQLiteDatabase {

        return INSTANCE ?: synchronized(this) {

            INSTANCE?.let {
                return@synchronized it
            }

            val databaseFile = getDatabaseFile(context)

            copyDatabaseIfNeeded(
                context = context,
                databaseFile = databaseFile
            )

            val database = SQLiteDatabase.openDatabase(
                databaseFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            INSTANCE = database

            database
        }
    }

    /**
     * アプリ内部のOSM DBファイルを取得する。
     */
    private fun getDatabaseFile(
        context: Context
    ): File {
        return context.getDatabasePath(DB_NAME)
    }

    /**
     * assetsからOSM DBを初回のみコピーする。
     */
    private fun copyDatabaseIfNeeded(
        context: Context,
        databaseFile: File
    ) {
        if (databaseFile.exists()) {
            return
        }

        databaseFile.parentFile?.mkdirs()

        context.assets.open(DB_NAME).use { input ->
            databaseFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}