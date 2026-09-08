package com.nekogps.app.features.stats

data class FuelStation(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val pricePerLiter: Double = 0.0,
    val fuelType: FuelType = FuelType.REGULAR,
    val distanceMeters: Double = 0.0
) {
    enum class FuelType(val displayName: String, val emoji: String) {
        REGULAR("Regular", "⛽"),
        PREMIUM("Premium", "🏎️"),
        DIESEL("Diesel", "🚛"),
        LPG("LPG", "🔥")
    }
}
