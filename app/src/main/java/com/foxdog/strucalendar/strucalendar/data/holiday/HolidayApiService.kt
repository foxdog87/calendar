package com.foxdog.strucalendar.data.holiday

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HolidayApiService(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchHolidays(year: Int, countryCode: String): List<HolidayDto> =
        withContext(Dispatchers.IO) {
            val url = "https://date.nager.at/api/v3/PublicHolidays/$year/$countryCode"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Holiday API failed: ${response.code} for $countryCode/$year")
                }
                val bodyString = response.body?.string() ?: "[]"
                json.decodeFromString(bodyString)
            }
        }
}