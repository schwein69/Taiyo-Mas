package model

import interfaces.Taiyo

class TaiyoImpl : Taiyo {

    override val panels = Panel(maxPowerKw = 6.0)
    override val battery = Battery(capacityKw = 10.0, initialChargeKw = 8.0)
    override val house = HouseGrid(contractualGridPowerKw = 3.0, essentialLoadKw = 0.5)
    override val car = Car(batteryCapacityKwh = 50.0)
    override val weather = Weather()

    override var timeStep: Int = 0
    override var mode: Mode = Mode.BALANCED

    override var currentPvFlow: Double = 0.0
    override var currentBatteryFlow: Double = 0.0 // Positivo: carica, Negativo: scarica
    override var currentGridFlow: Double = 0.0    // Positivo: vendita, Negativo: acquisto
}