package model

interface Panel {
    val maxPowerKw: Double
    var efficiency: Double
    var isClean: Boolean

    fun producePower(weather: Weather): Double
}