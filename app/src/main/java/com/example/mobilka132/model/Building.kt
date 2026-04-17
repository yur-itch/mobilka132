package com.example.mobilka132.model

enum class BuildingType {
    LANDMARK,
    URBAN
}

data class VenueInfo(
    val name: String,
    val workingHours: String,
    val estimatedVisitTimeMinutes: Int,
    val dishes: List<String>
)

data class BuildingInfo(
    val name: String,
    val address: String,
    val venues: List<VenueInfo> = emptyList(),
    val type: BuildingType = BuildingType.URBAN
)