package model

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BatteryImplTest {

    private lateinit var battery: BatteryImpl
    private val capacity = 10.0
    private val initialCharge = 5.0

    @BeforeTest
    fun setUp() {
        battery = BatteryImpl(
            capacityKw = capacity,
            initialChargeKw = initialCharge
        )
    }


    @Test
    fun `charge aumenta la carica corrente e aggiorna il SoC`() {
        battery.charge(2.5)
        assertEquals(7.5, battery.currentChargeKw, "La carica deve salire a 7.5 kW")
        assertEquals(75, battery.soc, "Il SoC deve salire al 75%")
    }

    @Test
    fun `charge si ferma alla capacita massima senza andare in overflow`() {
        battery.charge(20.0)

        assertEquals(capacity, battery.currentChargeKw, "La carica non deve mai superare la capacità fisica")
        assertEquals(100, battery.soc, "Il SoC deve bloccarsi al 100%")
    }

    @Test
    fun `discharge riduce la carica corrente e aggiorna il SoC`() {
        battery.discharge(3.0)

        assertEquals(2.0, battery.currentChargeKw, "La carica deve scendere a 2.0 kW")
        assertEquals(20, battery.soc, "Il SoC deve scendere al 20%")
    }

    @Test
    fun `discharge si ferma a zero senza andare in valori negativi`() {
        battery.discharge(15.0)

        assertEquals(0.0, battery.currentChargeKw, "La carica non deve mai scendere sotto lo 0.0")
        assertEquals(0, battery.soc, "Il SoC minimo deve restare 0%")
    }
}