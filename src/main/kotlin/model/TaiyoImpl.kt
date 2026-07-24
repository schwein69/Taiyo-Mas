package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import interfaces.Taiyo

class TaiyoImpl : Taiyo {

    override val panels = PanelImpl(maxPowerKw = 6.0)
    override val battery = BatteryImpl(capacityKw = 10.0, initialChargeKw = 8.0)
    override val house = HouseGridImpl(contractualGridPowerKw = 3.0, essentialLoadKw = 0.5)
    override val car = CarImpl(batteryCapacityKwh = 50.0)
    override val weather = WeatherImpl()

    override var timeStep: Int by mutableStateOf(0)
    override var mode: Mode by mutableStateOf(Mode.BALANCED)

    override var currentPvFlow: Double by mutableStateOf(0.0)
    override var currentBatteryFlow: Double by mutableStateOf(0.0) // Positivo: carica, Negativo: scarica
    override var currentGridFlow: Double by mutableStateOf(0.0)    // Positivo: vendita, Negativo: acquisto
}