package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

class WeatherImpl : Weather {

    private var _status by mutableStateOf(WeatherStatus.SUNNY)

    override var status: WeatherStatus
        get() = _status
        set(value) {
            _status = value
            updateEffects(value)
        }

    override var solarIrradiance: Double by mutableStateOf(1.0)
        private set

    override var negativeImpact: Boolean by mutableStateOf(false)
        private set

    init {
        updateEffects(_status)
    }

    private fun updateEffects(value: WeatherStatus) {
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

    override fun updateRandomly() {
        val allStatuses = WeatherStatus.entries.toTypedArray()
        val randomIndex = Random.nextInt(allStatuses.size)
        this.status = allStatuses[randomIndex]
    }
}