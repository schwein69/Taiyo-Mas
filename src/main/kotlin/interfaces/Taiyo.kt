package interfaces

import model.Car
import model.Battery
import model.HouseGrid
import model.Mode
import model.Panel
import model.Weather

interface Taiyo {
    val panels: Panel
    val battery: Battery
    val house: HouseGrid
    val car: Car
    val weather: Weather

    var timeStep: Int
    var mode: Mode

    var currentPvFlow: Double
    var currentBatteryFlow: Double
    var currentGridFlow: Double
}