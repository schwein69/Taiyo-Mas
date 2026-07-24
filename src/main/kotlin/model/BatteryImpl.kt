package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class BatteryImpl(
    override val capacityKw: Double,
    initialChargeKw: Double
) : Battery {

    override var currentChargeKw: Double by mutableStateOf(initialChargeKw)
        private set

    override val soc: Int
        get() = ((currentChargeKw / capacityKw) * 100).toInt()

    override fun discharge(amountKw: Double) {
        currentChargeKw = (currentChargeKw - amountKw).coerceIn(0.0, capacityKw)
    }

    override fun charge(amountKw: Double) {
        currentChargeKw = (currentChargeKw + amountKw).coerceIn(0.0, capacityKw)
    }
}