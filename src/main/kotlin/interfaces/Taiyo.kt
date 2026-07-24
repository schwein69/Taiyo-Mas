package interfaces

import model.CarImpl
import model.BatteryImpl
import model.HouseGridImpl
import model.Mode
import model.PanelImpl
import model.Panelmpl
import model.Weather

interface Taiyo {
    val panels: PanelImpl
    val battery: BatteryImpl
    val house: HouseGridImpl
    val car: CarImpl
    val weather: Weather

    var timeStep: Int
    var mode: Mode

    var currentPvFlow: Double
    var currentBatteryFlow: Double
    var currentGridFlow: Double
}