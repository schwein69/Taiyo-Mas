package model

import kotlin.random.Random

class HouseGrid(
    val contractualGridPowerKw: Double = 3.0,
    val essentialLoadKw: Double = 0.5
) {
    // --- Consumi interni ---
    var variableLoadKw: Double = 0.0
        private set

    var evChargerKw: Double = 0.0
        private set

    var areLoadsDisconnected: Boolean = false
        private set

    var isBlackout: Boolean = false
        private set

    // Import/Export verso l'esterno
    var isGridConnected: Boolean = true
        private set

    var totalImportedKwh: Double = 0.0
        private set

    var totalExportedKwh: Double = 0.0
        private set

    // --- GETTER DEL Consumo totale ---
    val currentConsumptionKw: Double
        get() {
            if (isBlackout) return 0.0
            return if (areLoadsDisconnected) {
                essentialLoadKw
            } else {
                essentialLoadKw + variableLoadKw + evChargerKw
            }
        }

    fun simulateOccupantBehavior() {
        if (!areLoadsDisconnected && !isBlackout) {
            variableLoadKw = Random.nextDouble(0.0, 3.5)
        } else {
            variableLoadKw = 0.0
        }
    }

    fun balanceEvCharging(pvAvailableKw: Double, batteryAvailableKw: Double, isCarPluggedIn: Boolean) {
        if (!isCarPluggedIn || isBlackout || areLoadsDisconnected) {
            evChargerKw = 0.0
            return
        }

        val totalPowerAvailable = contractualGridPowerKw + pvAvailableKw + batteryAvailableKw
        val powerLeftForEv = totalPowerAvailable - (essentialLoadKw + variableLoadKw)

        evChargerKw = if (powerLeftForEv > 0) minOf(powerLeftForEv, 7.4) else 0.0
    }

    fun checkOverload(pvAvailableKw: Double, batteryAvailableKw: Double) {
        val maxPhysicalPower = contractualGridPowerKw + pvAvailableKw + batteryAvailableKw

        if (currentConsumptionKw > maxPhysicalPower) {
            triggerInternalBlackout() // il contatore salta
        }
    }


    fun interactWithGrid(netPowerKw: Double, deltaTimeHours: Double) {
        if (!isGridConnected) return

        val energyKwh = netPowerKw * deltaTimeHours
        if (energyKwh < 0) {
            totalImportedKwh += -energyKwh // Energia comprata
        } else {
            totalExportedKwh += energyKwh  // Energia venduta
        }
    }

    fun disconnectFromGrid() { isGridConnected = false }
    fun reconnectToGrid() { isGridConnected = true }


    fun disconnectNonEssentialLoads() {
        areLoadsDisconnected = true
        variableLoadKw = 0.0
        evChargerKw = 0.0
    }

    fun reconnectLoads() {
        areLoadsDisconnected = false
    }

    fun triggerInternalBlackout() {
        isBlackout = true
        variableLoadKw = 0.0
        evChargerKw = 0.0
    }

    fun resolveInternalBlackout() {
        isBlackout = false
    }
}