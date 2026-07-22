package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Battery(val capacityKw: Double, initialChargeKw: Double) {

    var currentChargeKw: Double by mutableStateOf(initialChargeKw)
        private set

    val soc: Int
        get() = ((currentChargeKw / capacityKw) * 100).toInt()

    fun discharge(amountKw: Double) {
        currentChargeKw = (currentChargeKw - amountKw).coerceIn(0.0, capacityKw)
    }

    fun charge(amountKw: Double) {
        currentChargeKw = (currentChargeKw + amountKw).coerceIn(0.0, capacityKw)
    }
}