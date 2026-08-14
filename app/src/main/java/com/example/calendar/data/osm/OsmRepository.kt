package com.example.calendar.data.osm

import android.content.Context
import android.database.Cursor
import com.example.calendar.data.osm.model.OsmPoi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class OsmRepository(
    private val context: Context
) {

    suspend fun searchPoi(
        keyword: String,
        limit: Int = 20
    ): List<OsmPoi> = withContext(Dispatchers.IO) {

        if (keyword.isBlank()) {
            return@withContext emptyList()
        }

        val db = OsmDatabase.getDatabase(context)

        val result = mutableListOf<OsmPoi>()

        val cursor = db.rawQuery(
            """
            SELECT
                id,
                name,
                category_id,
                address,
                latitude,
                longitude
            FROM poi
            WHERE name LIKE ?
            ORDER BY name ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                "%${keyword.trim()}%",
                limit.toString()
            )
        )

        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    OsmPoi(
                        id = it.getLong(
                            it.getColumnIndexOrThrow("id")
                        ),
                        name = it.getString(
                            it.getColumnIndexOrThrow("name")
                        ),
                        categoryId = it.getLongOrNull("category_id"),
                        address = it.getStringOrNull("address"),
                        latitude = it.getDouble(
                            it.getColumnIndexOrThrow("latitude")
                        ),
                        longitude = it.getDouble(
                            it.getColumnIndexOrThrow("longitude")
                        )
                    )
                )
            }
        }

        result
    }

    suspend fun searchNearby(
        latitude: Double,
        longitude: Double,
        radius: Double = 0.01,
        limit: Int = 20
    ): List<OsmPoi> = withContext(Dispatchers.IO) {

        val db = OsmDatabase.getDatabase(context)

        val result = mutableListOf<OsmPoi>()

        val cursor = db.rawQuery(
            """
            SELECT
                id,
                name,
                category_id,
                address,
                latitude,
                longitude
            FROM poi
            WHERE latitude BETWEEN ? AND ?
              AND longitude BETWEEN ? AND ?
            ORDER BY
                (latitude - ?) * (latitude - ?)
                +
                (longitude - ?) * (longitude - ?)
            LIMIT ?
            """.trimIndent(),
            arrayOf(
                (latitude - radius).toString(),
                (latitude + radius).toString(),
                (longitude - radius).toString(),
                (longitude + radius).toString(),
                latitude.toString(),
                latitude.toString(),
                longitude.toString(),
                longitude.toString(),
                limit.toString()
            )
        )

        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    OsmPoi(
                        id = it.getLong(
                            it.getColumnIndexOrThrow("id")
                        ),
                        name = it.getString(
                            it.getColumnIndexOrThrow("name")
                        ),
                        categoryId = it.getLongOrNull("category_id"),
                        address = it.getStringOrNull("address"),
                        latitude = it.getDouble(
                            it.getColumnIndexOrThrow("latitude")
                        ),
                        longitude = it.getDouble(
                            it.getColumnIndexOrThrow("longitude")
                        )
                    )
                )
            }
        }

        result
    }

    private fun Cursor.getLongOrNull(
        columnName: String
    ): Long? {
        val index = getColumnIndexOrThrow(columnName)

        return if (isNull(index)) {
            null
        } else {
            getLong(index)
        }
    }

    private fun Cursor.getStringOrNull(
        columnName: String
    ): String? {
        val index = getColumnIndexOrThrow(columnName)

        return if (isNull(index)) {
            null
        } else {
            getString(index)
        }
    }
}