package com.foxdog.strucalendar.data.holiday

import com.foxdog.strucalendar.data.dao.HolidayDao
import com.foxdog.strucalendar.data.entity.HolidayEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class HolidayRepository(
    private val holidayDao: HolidayDao,
    private val apiService: HolidayApiService = HolidayApiService()
) {
    /**
     * 指定年・指定国の祝日を Map<日付, 祝日名> で返す。
     * DBにキャッシュがあればそれを使い、無ければAPIから取得してDBに保存する。
     *
     * ★ 変更：以前は固定の国リスト（JP, US, GB, DE）を全てマージして返していたが、
     * それだと他国の祝日まで表示されてしまうため、呼び出し元（端末のロケール）から
     * 渡された countryCode 1件だけを対象にするよう変更。
     * Nager.Date API が対応している国コード（ISO 3166-1 alpha-2）であれば、
     * リストにない国でもそのまま動作する＝実質対応国が大幅に増える。
     */
    suspend fun getHolidayMap(year: Int, countryCode: String): Map<LocalDate, String> = withContext(Dispatchers.IO) {
        val yearPrefix = year.toString()
        var entities = holidayDao.getHolidaysForYear(countryCode, yearPrefix)

        if (entities.isEmpty()) {
            entities = try {
                val fetched = apiService.fetchHolidays(year, countryCode)
                val toInsert = fetched.map {
                    HolidayEntity(date = it.date, countryCode = countryCode, localName = it.localName)
                }
                if (toInsert.isNotEmpty()) {
                    holidayDao.insertAll(toInsert)
                }
                toInsert
            } catch (e: Exception) {
                // 未対応の国コードやネットワーク失敗時は、アプリを落とさず「祝日なし」扱いにする
                emptyList()
            }
        }

        entities.associate { LocalDate.parse(it.date) to it.localName }
    }

    /**
     * 指定日・指定国の祝日名を取得する（DBキャッシュのみを参照、API通信はしない）。
     * ★ 変更：以前は該当国が見つからなければ他国の祝日名にフォールバックしていたが、
     * それが「他国の祝日が出てしまう」原因だったため、指定国のみに限定する。
     */
    suspend fun getHolidayName(date: LocalDate, countryCode: String): String? = withContext(Dispatchers.IO) {
        holidayDao.getHolidaysForDate(date.toString())
            .firstOrNull { it.countryCode == countryCode }
            ?.localName
    }
}