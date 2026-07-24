package model

interface Car {
    val batteryCapacityKwh: Double
    val maxChargePowerKw: Double
    val isCharging: Boolean
    val currentChargeKwh: Double
    val isPluggedIn: Boolean
    val soc: Int

    fun plugIn()
    fun unplug()
    fun charge(powerKw: Double, deltaTimeHours: Double)
    fun drive(energyUsedKwh: Double)
}