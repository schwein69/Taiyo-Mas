package model

class Panel(val maxPowerKw: Double, var efficiency: Double = 1.00) {

    var isClean: Boolean = true

    fun producePower(weather: Weather): Double {
        val dirtFactor = if (isClean) 1.0 else 0.8
        return maxPowerKw * weather.solarIrradiance * efficiency * dirtFactor
    }
}