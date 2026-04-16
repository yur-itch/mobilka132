package com.example.mobilka132.model

sealed class SearchResult {
    data class BuildingResult(val info: BuildingInfo, val matchType: MatchType) : SearchResult()
    data class VenueResult(val venue: VenueInfo, val building: BuildingInfo) : SearchResult()
}

enum class MatchType { NAME, ADDRESS }
