package model

import kotlin.random.Random

enum class WeatherStatus {
    SUNNY, NIGHT, FOGGY, RAINY
}

class Weather {

    var status: WeatherStatus = WeatherStatus.SUNNY
        set(value) {
            field = value
            when (value) {
                WeatherStatus.SUNNY -> {
                    solarIrradiance = 1.0
                    negativeImpact = false
                }
                WeatherStatus.NIGHT -> {
                    solarIrradiance = 0.0
                    negativeImpact = false
                }
                WeatherStatus.FOGGY -> {
                    solarIrradiance = 0.4
                    negativeImpact = true
                }
                WeatherStatus.RAINY -> {
                    solarIrradiance = 0.1
                    negativeImpact = true
                }
            }
        }

    var solarIrradiance: Double = 1.0
        private set

    var negativeImpact: Boolean = false
        private set

    fun updateRandomly() {
        val allStatuses = WeatherStatus.entries.toTypedArray()

        val randomIndex = Random.nextInt(allStatuses.size)

        this.status = allStatuses[randomIndex]
    }
}