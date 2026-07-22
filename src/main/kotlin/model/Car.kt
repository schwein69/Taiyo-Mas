package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Car(
    val batteryCapacityKwh: Double = 50.0,
    val maxChargePowerKw: Double = 7.4
) {
    var isCharging: Boolean by mutableStateOf(false)

    var currentChargeKwh: Double by mutableStateOf(5.0)
        private set

    var isPluggedIn: Boolean by mutableStateOf(false)
        private set

    val soc: Int
        get() = ((currentChargeKwh / batteryCapacityKwh) * 100).toInt()

    fun plugIn() {
        isPluggedIn = true
    }

    fun unplug() {
        isPluggedIn = false
        isCharging = false
    }

    fun charge(powerKw: Double, deltaTimeHours: Double) {
        if (!isPluggedIn) return
        isCharging = true

        val actualPowerKw = powerKw.coerceAtMost(maxChargePowerKw)

        val energyAdded = actualPowerKw * deltaTimeHours

        currentChargeKwh = (currentChargeKwh + energyAdded).coerceAtMost(batteryCapacityKwh)
    }

    /**
     * Simula il consumo dell'auto quando il proprietario è in giro.
     */
    fun drive(energyUsedKwh: Double) {
        if (!isPluggedIn) {
            currentChargeKwh = (currentChargeKwh - energyUsedKwh).coerceAtLeast(0.0)
        }
    }
}