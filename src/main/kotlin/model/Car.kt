package model

class Car(
    val batteryCapacityKwh: Double = 50.0,
    val maxChargePowerKw: Double = 7.4
) {
    // Carica attuale dell'auto
    var currentChargeKwh: Double = 5.0
        private set

    var isPluggedIn: Boolean = false
        private set

    val soc: Int
        get() = ((currentChargeKwh / batteryCapacityKwh) * 100).toInt()

    fun plugIn() {
        isPluggedIn = true
    }

    fun unplug() {
        isPluggedIn = false
    }

    fun charge(powerKw: Double, deltaTimeHours: Double) {
        if (!isPluggedIn) return

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