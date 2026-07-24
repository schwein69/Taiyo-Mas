package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CarImpl(
    override val batteryCapacityKwh: Double = 50.0,
    override val maxChargePowerKw: Double = 7.4
) : Car {

    override var isCharging: Boolean by mutableStateOf(false)

    override var currentChargeKwh: Double by mutableStateOf(5.0)
        private set

    override var isPluggedIn: Boolean by mutableStateOf(false)
        private set

    override val soc: Int
        get() = ((currentChargeKwh / batteryCapacityKwh) * 100).toInt()

    override fun plugIn() {
        isPluggedIn = true
    }

    override fun unplug() {
        isPluggedIn = false
        isCharging = false
    }

    override fun charge(powerKw: Double, deltaTimeHours: Double) {
        if (!isPluggedIn) return
        isCharging = true

        val actualPowerKw = powerKw.coerceAtMost(maxChargePowerKw)
        val energyAdded = actualPowerKw * deltaTimeHours

        currentChargeKwh = (currentChargeKwh + energyAdded).coerceAtMost(batteryCapacityKwh)
    }

    override fun drive(energyUsedKwh: Double) {
        if (!isPluggedIn) {
            currentChargeKwh = (currentChargeKwh - energyUsedKwh).coerceAtLeast(0.0)
        }
    }
}