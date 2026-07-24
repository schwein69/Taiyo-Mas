package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

class HouseGridImpl(
    override val contractualGridPowerKw: Double = 3.0,
    override val essentialLoadKw: Double = 0.5
) : HouseGrid {

    override var variableLoadKw: Double by mutableStateOf(0.0)
        private set

    override var evChargerKw: Double by mutableStateOf(0.0)
        private set

    override var areLoadsDisconnected: Boolean by mutableStateOf(false)
        private set

    override var isBlackout: Boolean by mutableStateOf(false)

    override var isGridConnected: Boolean by mutableStateOf(true)
        private set

    override var totalImportedKwh: Double by mutableStateOf(0.0)
        private set

    override var totalExportedKwh: Double by mutableStateOf(0.0)
        private set

    override val currentConsumptionKw: Double
        get() {
            if (isBlackout) return 0.0
            return if (areLoadsDisconnected) {
                essentialLoadKw
            } else {
                essentialLoadKw + variableLoadKw + evChargerKw
            }
        }

    override fun simulateOccupantBehavior() {
        if (!areLoadsDisconnected && !isBlackout) {
            variableLoadKw = Random.nextDouble(0.0, 3.5)
        } else {
            variableLoadKw = 0.0
        }
    }

    override fun balanceEvCharging(pvAvailableKw: Double, batteryAvailableKw: Double, isCarPluggedIn: Boolean) {
        if (!isCarPluggedIn || isBlackout || areLoadsDisconnected) {
            evChargerKw = 0.0
            return
        }

        val totalPowerAvailable = contractualGridPowerKw + pvAvailableKw + batteryAvailableKw
        val powerLeftForEv = totalPowerAvailable - (essentialLoadKw + variableLoadKw)

        evChargerKw = if (powerLeftForEv > 0) minOf(powerLeftForEv, 7.4) else 0.0
    }

    override fun checkOverload(pvAvailableKw: Double, batteryAvailableKw: Double) {
        val maxPhysicalPower = contractualGridPowerKw + pvAvailableKw + batteryAvailableKw

        if (currentConsumptionKw > maxPhysicalPower) {
            triggerInternalBlackout()
        }
    }

    override fun interactWithGrid(netPowerKw: Double, deltaTimeHours: Double) {
        if (!isGridConnected) return

        val energyKwh = netPowerKw * deltaTimeHours
        if (energyKwh < 0) {
            totalImportedKwh += -energyKwh
        } else {
            totalExportedKwh += energyKwh
        }
    }

    override fun disconnectFromGrid() {
        isGridConnected = false
    }

    override fun reconnectToGrid() {
        isGridConnected = true
    }

    override fun disconnectNonEssentialLoads() {
        areLoadsDisconnected = true
        variableLoadKw = 0.0
        evChargerKw = 0.0
    }

    override fun reconnectLoads() {
        areLoadsDisconnected = false
    }

    override fun triggerInternalBlackout() {
        isBlackout = true
        variableLoadKw = 0.0
        evChargerKw = 0.0
    }

    override fun resolveInternalBlackout() {
        isBlackout = false
    }
}