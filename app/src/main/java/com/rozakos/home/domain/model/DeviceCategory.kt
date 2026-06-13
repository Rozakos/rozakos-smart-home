package com.rozakos.home.domain.model

enum class DeviceCategory(
    val id: String,
    val displayName: String,
) {
    StockTickers("stock_tickers", "Stock Tickers"),
    CleaningAppliances("cleaning_appliances", "Cleaning Appliances"),
    HouseholdSecurity("household_security", "Household Security"),
    Sensors("sensors", "Sensors"),
    Displays("displays", "Displays"),
    Lighting("lighting", "Lighting"),
    WeatherStations("weather_stations", "Weather Stations");

    companion object {
        fun fromId(id: String): DeviceCategory =
            entries.firstOrNull { it.id == id } ?: StockTickers
    }
}
