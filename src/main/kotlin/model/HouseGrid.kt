package model

interface HouseGrid {
    val contractualGridPowerKw: Double
    val essentialLoadKw: Double
    val variableLoadKw: Double
    val evChargerKw: Double
    val areLoadsDisconnected: Boolean
    val isBlackout: Boolean
    val isGridConnected: Boolean
    val totalImportedKwh: Double
    val totalExportedKwh: Double
    val currentConsumptionKw: Double

    fun simulateOccupantBehavior()
    fun balanceEvCharging(pvAvailableKw: Double, batteryAvailableKw: Double, isCarPluggedIn: Boolean)
    fun checkOverload(pvAvailableKw: Double, batteryAvailableKw: Double)
    fun interactWithGrid(netPowerKw: Double, deltaTimeHours: Double)
    fun disconnectFromGrid()
    fun reconnectToGrid()
    fun disconnectNonEssentialLoads()
    fun reconnectLoads()
    fun triggerInternalBlackout()
    fun resolveInternalBlackout()
}