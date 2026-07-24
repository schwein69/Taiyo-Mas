package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PanelImpl(
    override val maxPowerKw: Double,
    initialEfficiency: Double = 1.00
) : Panel {

    override var efficiency: Double by mutableStateOf(initialEfficiency)

    override var isClean: Boolean by mutableStateOf(true)

    override fun producePower(weather: Weather): Double {
        val dirtFactor = if (isClean) 1.0 else 0.8
        return maxPowerKw * weather.solarIrradiance * efficiency * dirtFactor
    }
}