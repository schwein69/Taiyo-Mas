package model

class Battery(val capacityKw: Double, initialChargeKw: Double) {

    var currentChargeKw: Double = initialChargeKw
        set(value) {
            field = value.coerceIn(0.0, capacityKw)
        }

    val soc: Int
        get() = ((currentChargeKw / capacityKw) * 100).toInt()

    fun discharge(amountKw: Double): Double {
        return currentChargeKw - amountKw
    }
    fun charge(amountKw: Double): Double {
        return currentChargeKw + amountKw
    }
}