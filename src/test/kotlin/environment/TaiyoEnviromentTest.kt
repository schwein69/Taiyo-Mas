package environment

import jason.asSyntax.ASSyntax
import jason.asSyntax.Literal
import jason.asSyntax.Structure
import kotlin.test.*
import model.Mode.*
import view.GuiApp

class TaiyoEnvironmentTest {

    private lateinit var env: TaiyoEnvironment
    private val agentName = "test_agent"

    @BeforeTest
    fun setUp() {
        env = TaiyoEnvironment()
        env.init(arrayOf())
    }

    @AfterTest
    fun tearDown() {
        env.stop()
    }


    @Test
    fun `test azione sconosciuta restituisce false`() {
        val unknownAction = Literal.parseLiteral("azione_inesistente") as Structure
        val result = env.executeAction(agentName, unknownAction)
        assertFalse(result, "Un'azione non riconosciuta deve restituire false")
    }

    @Test
    fun `test azioni house grid disconnect_loads e reconnect_loads`() {
        val model = GuiApp.sharedModel

        val resDisconnect = env.executeAction(agentName, TaiyoEnvironment.actDisconnectLoads as Structure)
        assertTrue(resDisconnect)
        assertTrue(model.house.areLoadsDisconnected)

        val resReconnect = env.executeAction(agentName, TaiyoEnvironment.actReconnectLoads as Structure)
        assertTrue(resReconnect)
        assertFalse(model.house.areLoadsDisconnected)
    }

    @Test
    fun `test azione activate_island_mode scollega la casa dalla rete`() {
        val model = GuiApp.sharedModel
        val result = env.executeAction(agentName, TaiyoEnvironment.actActivateIslandMode as Structure)

        assertTrue(result)
        assertFalse(model.house.isGridConnected)
    }

    @Test
    fun `test cambio modalita operativa set_selling_mode set_balanced_mode set_direct_mode`() {
        val model = GuiApp.sharedModel

        env.executeAction(agentName, TaiyoEnvironment.actSetSellingMode as Structure)
        assertEquals(SELLING, model.mode)

        env.executeAction(agentName, TaiyoEnvironment.actSetDirectMode as Structure)
        assertEquals(DIRECT, model.mode)

        env.executeAction(agentName, TaiyoEnvironment.actSetBalancedMode as Structure)
        assertEquals(BALANCED, model.mode)
    }

    @Test
    fun `test azioni auto elettrica pause e resume charging`() {
        val model = GuiApp.sharedModel

        env.executeAction(agentName, TaiyoEnvironment.actPauseEvCharging as Structure)
        assertFalse(model.car.isCharging)

        env.executeAction(agentName, TaiyoEnvironment.actResumeEvCharging as Structure)
        assertTrue(model.car.isCharging)
    }

    @Test
    fun `test azione force_grid_charging attiva la ricarica dell auto`() {
        val model = GuiApp.sharedModel
        env.executeAction(agentName, TaiyoEnvironment.actPauseEvCharging as Structure)

        val result = env.executeAction(agentName, TaiyoEnvironment.actForceGridCharging as Structure)
        assertTrue(result)
        assertTrue(model.car.isCharging)
    }

    @Test
    fun `test azioni batteria standby azzera il flusso corrente`() {
        val model = GuiApp.sharedModel
        model.currentBatteryFlow = 3.5

        val result = env.executeAction(agentName, TaiyoEnvironment.actBatteryStandby as Structure)
        assertTrue(result)
        assertEquals(0.0, model.currentBatteryFlow)
    }

    @Test
    fun `test azioni pannelli standby azzera la produzione corrente`() {
        val model = GuiApp.sharedModel
        model.currentPvFlow = 4.2

        val result = env.executeAction(agentName, TaiyoEnvironment.actPanelStandby as Structure)
        assertTrue(result)
        assertEquals(0.0, model.currentPvFlow)
    }

    @Test
    fun `test percezioni house_grid per rete connessa e modalita operativa`() {
        val model = GuiApp.sharedModel
        model.mode = BALANCED
        model.house.reconnectToGrid()
        model.house.resolveInternalBlackout()

        print(env.consultPercepts("house_grid"))
        env.updateAgentPercepts()

        val percepts = env.consultPercepts("house_grid")
        assertNotNull(percepts, "La lista percezioni di house_grid non deve essere null")
        assertTrue(percepts.contains(TaiyoEnvironment.gridConnected), "Deve percepire grid_connected")
        assertTrue(
            percepts.contains(Literal.parseLiteral("system_mode(balanced)")),
            "Deve percepire system_mode(balanced)"
        )
        print(percepts)
    }

    @Test
    fun `test percezione blackout_active su house_grid durante blackout`() {
        val model = GuiApp.sharedModel
        model.house.triggerInternalBlackout()

        env.updateAgentPercepts()

        val percepts = env.consultPercepts("house_grid")
        assertNotNull(percepts)
        assertTrue(percepts.contains(TaiyoEnvironment.blackoutActive), "Deve percepire blackout_active")
        assertFalse(percepts.contains(TaiyoEnvironment.gridConnected), "Non deve percepire grid_connected")
        print(percepts)
    }

    @Test
    fun `test percezione overload_risk su house_grid se prelievo elevato con auto in carica`() {
        val model = GuiApp.sharedModel
        model.currentGridFlow = -3.5
        model.car.isCharging = true

        env.updateAgentPercepts()

        val percepts = env.consultPercepts("house_grid")
        assertNotNull(percepts)
        assertTrue(
            percepts.contains(Literal.parseLiteral("overload_risk")),
            "Deve percepire overload_risk quando si supera il limite"
        )
        print(percepts)
    }

    @Test
    fun `test percezioni car per auto collegata scollegata e soc`() {
        val model = GuiApp.sharedModel

        model.car.isPluggedIn = false
        env.updateAgentPercepts()
        var percepts = env.consultPercepts("car")
        assertNotNull(percepts)
        assertTrue(percepts.contains(Literal.parseLiteral("car_unplugged")))

        model.car.isPluggedIn = true
        env.updateAgentPercepts()
        percepts = env.consultPercepts("car")
        assertNotNull(percepts)
        assertTrue(percepts.contains(Literal.parseLiteral("car_plugged_in")))
        assertTrue(
            percepts.contains(
                ASSyntax.createLiteral("car_soc", ASSyntax.createNumber(model.car.soc.toDouble()))
            ),
            "Deve trasmettere il SoC attuale dell'auto"
        )
    }

    @Test
    fun `test percezioni battery per stato di carica e direzione flusso`() {
        val model = GuiApp.sharedModel
        model.currentBatteryFlow = 2.0
        env.updateAgentPercepts()

        val percepts = env.consultPercepts("battery")
        assertNotNull(percepts)
        assertTrue(
            percepts.contains(Literal.parseLiteral("battery_flow(charging)")),
            "Flusso > 0.1 deve generare battery_flow(charging)"
        )
        assertTrue(
            percepts.contains(
                ASSyntax.createLiteral("battery_soc", ASSyntax.createNumber(model.battery.soc.toDouble()))
            )
        )
    }

    @Test
    fun `test percezioni panels per immissione totale in modalita SELLING`() {
        val model = GuiApp.sharedModel
        model.mode = SELLING

        env.updateAgentPercepts()

        val percepts = env.consultPercepts("panels")
        assertNotNull(percepts)
        assertTrue(
            percepts.contains(Literal.parseLiteral("pv_flow(full_grid_injection)")),
            "In modalità SELLING i pannelli devono percepire full_grid_injection"
        )
    }

    @Test
    fun `test percezioni weather inviate all agente weather`() {
        val model = GuiApp.sharedModel

        env.updateAgentPercepts()

        val percepts = env.consultPercepts("weather")
        assertNotNull(percepts)
        val expectedLiteral = ASSyntax.createLiteral(
            "weather_status",
            ASSyntax.createAtom(model.weather.status.name.lowercase())
        )
        assertTrue(percepts.contains(expectedLiteral), "L'agente deve ricevere il meteo corrente")
    }
}