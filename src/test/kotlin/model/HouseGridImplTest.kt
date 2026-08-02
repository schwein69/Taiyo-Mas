package model

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HouseGridImplTest {

    private lateinit var house: HouseGridImpl

    @BeforeTest
    fun setUp() {
        // Contatore rete = 3.0 kW, Carico essenziale = 0.5 kW
        house = HouseGridImpl(
            contractualGridPowerKw = 3.0,
            essentialLoadKw = 0.5
        )
    }

    @Test
    fun `test auto collegata in abbondanza solare carica al massimo`() {
        val pvAvailable = 5.0
        val batteryAvailable = 0.0
        val isCarPluggedIn = true

        house.balanceEvCharging(pvAvailable, batteryAvailable, isCarPluggedIn)

        assertEquals(7.4, house.evChargerKw, "L'auto dovrebbe assorbire il suo massimo fisico di 7.4 kW")
    }

    @Test
    fun `test auto collegata con poca energia si limita ai watt rimanenti`() {
        val pvAvailable = 0.5
        val batteryAvailable = 0.0
        val isCarPluggedIn = true

        house.balanceEvCharging(pvAvailable, batteryAvailable, isCarPluggedIn)

        assertEquals(3.0, house.evChargerKw, "L'auto deve assorbire solo l'energia rimanente (3.0 kW)")
    }

    @Test
    fun `test auto scollegata o in blackout ha assorbimento zero`() {
        house.balanceEvCharging(pvAvailableKw = 5.0, batteryAvailableKw = 2.0, isCarPluggedIn = false)
        assertEquals(0.0, house.evChargerKw, "Se l'auto è scollegata l'assorbimento deve essere 0")

        house.triggerInternalBlackout()
        house.balanceEvCharging(pvAvailableKw = 5.0, batteryAvailableKw = 2.0, isCarPluggedIn = true)
        assertEquals(0.0, house.evChargerKw, "Durante un blackout la ricarica deve essere 0")
    }

    @Test
    fun `test disconnectNonEssentialLoads spegne carichi variabili e ricarica auto`() {
        house.balanceEvCharging(pvAvailableKw = 5.0, batteryAvailableKw = 0.0, isCarPluggedIn = true)
        assertTrue(house.evChargerKw > 0.0)
        house.disconnectNonEssentialLoads()
        assertTrue(house.areLoadsDisconnected, "Il flag areLoadsDisconnected deve essere true")
        assertEquals(0.0, house.evChargerKw, "L'auto si deve spegnere")
        assertEquals(house.essentialLoadKw, house.currentConsumptionKw, "Deve restare attivo solo il carico essenziale")
    }

    @Test
    fun `test isolamento dalla rete attiva la modalita isola`() {
        house.disconnectFromGrid()
        assertFalse(house.isGridConnected, "La casa deve risultare scollegata dalla rete")
    }
}