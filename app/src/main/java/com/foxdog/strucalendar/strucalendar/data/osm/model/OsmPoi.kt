package com.foxdog.strucalendar.data.osm.model

data class OsmPoi(
    val id: Long,
    val name: String,
    val categoryId: Long?,
    val address: String?,
    val latitude: Double,
    val longitude: Double
)
