import interfaces.Taiyo
import jason.asSyntax.ASSyntax
import jason.asSyntax.Literal
import jason.asSyntax.Structure
import jason.environment.Environment
import kotlinx.coroutines.*
import view.GuiApp
import java.util.logging.Logger
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import model.Mode.*

class TaiyoEnvironment : Environment() {

    companion object {
        // PERCEZIONI
        val gridConnected: Literal = Literal.parseLiteral("grid_connected")
        val blackoutActive: Literal = Literal.parseLiteral("blackout_active")

        // AZIONI HOUSE GRID
        val actDisconnectLoads: Literal = Literal.parseLiteral("disconnect_loads")
        val actReconnectLoads: Literal = Literal.parseLiteral("reconnect_loads")
        val actActivateIslandMode: Literal = Literal.parseLiteral("activate_island_mode")

        // AZIONI BATTERIA
        val actBatteryStandby: Literal = Literal.parseLiteral("battery_standby")
        val actBatteryResume: Literal = Literal.parseLiteral("battery_resume")
        val actForceGridCharging: Literal = Literal.parseLiteral("force_grid_charging")

        // AZIONI PANNELLI / MODALITA'
        val actPanelStandby: Literal = Literal.parseLiteral("panel_standby")
        val actPanelResume: Literal = Literal.parseLiteral("panel_resume")
        val actSetSellingMode: Literal = Literal.parseLiteral("set_selling_mode")
        val actSetBalancedMode: Literal = Literal.parseLiteral("set_balanced_mode")
        val actSetDirectMode: Literal = Literal.parseLiteral("set_direct_mode")

        // AZIONI AUTO
        val actCarStartCharging: Literal = Literal.parseLiteral("car_start_charging")
        val actCarStopCharging: Literal = Literal.parseLiteral("car_stop_charging")
        val actPauseEvCharging: Literal = Literal.parseLiteral("pause_ev_charging")
        val actResumeEvCharging: Literal = Literal.parseLiteral("resume_ev_charging")
        val actThrottle_ev_charging: Literal = Literal.parseLiteral("throttle_ev_charging")
    }

    private lateinit var logger: Logger
    private lateinit var model: Taiyo
    private lateinit var simulationScope: CoroutineScope

    private var evChargeOverride: String = "NORMAL" // "NORMAL", "THROTTLED" o "PAUSED"
    private var batteryOverride: String = "NORMAL"  // "NORMAL", "STANDBY" o "FORCE_CHARGE"

    override fun init(args: Array<String>?) {
        super.init(args)
        this.logger = Logger.getLogger("TaiyoEnvironment")
        this.logger.info("Initializing TAIYO-MAS Environment...")

        this.model = GuiApp.sharedModel
        this.simulationScope = CoroutineScope(Dispatchers.Default + Job())

        startPhysicalWorld()
    }

    override fun executeAction(agName: String, action: Structure): Boolean {
        var result = false

        when (action.functor) {
            // --- AZIONI HOUSE GRID ---
            actDisconnectLoads.functor -> result = executeDisconnectLoads(agName)
            actReconnectLoads.functor -> result = executeReconnectLoads(agName)
            actActivateIslandMode.functor -> {
                model.house.disconnectFromGrid()
                logger.info("[$agName] executed activate_island_mode. Casa isolata e sicura.")
                result = true
            }

            // --- AZIONI BATTERIA ---
            actBatteryStandby.functor -> {
                batteryOverride = "STANDBY"
                logger.info("[$agName] executed battery_standby. Batteria bloccata dal Planner.")
                result = true
            }
            actBatteryResume.functor -> {
                batteryOverride = "NORMAL"
                logger.info("[$agName] executed battery_resume. Batteria sbloccata.")
                result = true
            }
            actForceGridCharging.functor -> {
                batteryOverride = "FORCE_CHARGE"
                logger.info("[$agName] executed force_grid_charging. Prelievo forzato abilitato!")
                result = true
            }

            // --- AZIONI PANNELLI ---
            actPanelStandby.functor -> {
                logger.info("[$agName] executed panel_standby. Inverter is OFF.")
                result = true
            }
            actPanelResume.functor -> {
                logger.info("[$agName] executed panel_resume. Inverter is ON.")
                result = true
            }

            // --- AZIONI MODALITA' ---
            actSetSellingMode.functor -> { model.mode = SELLING; result = true }
            actSetBalancedMode.functor -> { model.mode = BALANCED; result = true }
            actSetDirectMode.functor -> { model.mode = DIRECT; result = true }

            // --- AZIONI AUTO ---
            actCarStartCharging.functor -> {
                evChargeOverride = "NORMAL"
                model.car.isCharging = true
                result = true
            }
            actCarStopCharging.functor -> {
                evChargeOverride = "PAUSED"
                model.car.isCharging = false
                result = true
            }
            actPauseEvCharging.functor -> {
                evChargeOverride = "PAUSED"
                model.car.isCharging = false
                logger.info("[$agName] executed pause_ev_charging. Ricarica sospesa dal Planner.")
                result = true
            }
            actResumeEvCharging.functor -> {
                evChargeOverride = "NORMAL"
                model.car.isCharging = true
                logger.info("[$agName] executed resume_ev_charging.")
                result = true
            }
            actThrottle_ev_charging.functor -> {
                evChargeOverride = "THROTTLED"
                logger.info("[$agName] executed throttle_ev_charging. Ricarica limitata (Antiblackout)")
                result = true
            }

            else -> {
                System.err.println("Unknown action: $action by agent $agName")
                return false
            }
        }

        if (result) notifyModelChangedToView()
        return result
    }

    private fun executeDisconnectLoads(agentName: String): Boolean {
        return try { model.house.disconnectNonEssentialLoads(); true } catch (e: Exception) { false }
    }
    private fun executeReconnectLoads(agentName: String): Boolean {
        return try { model.house.reconnectLoads(); true } catch (e: Exception) { false }
    }


    private fun updateAgentPercepts() {
        clearPercepts("weather")
        clearPercepts("battery")
        clearPercepts("house_grid")
        clearPercepts("panels")
        clearPercepts("car")

        // Meteo
        addPercept("weather", ASSyntax.createLiteral("weather_status", ASSyntax.createAtom(model.weather.status.name.lowercase())))

        // Batteria
        addPercept("battery", ASSyntax.createLiteral("battery_soc", ASSyntax.createNumber(model.battery.soc.toDouble())))
        val flowDirection = when {
            model.currentBatteryFlow > 0.1 -> "charging"
            model.currentBatteryFlow < -0.1 -> "discharging"
            else -> "idle"
        }
        addPercept("battery", ASSyntax.createLiteral("battery_flow", ASSyntax.createAtom(flowDirection)))

        // House Grid
        addPercept("house_grid", ASSyntax.createLiteral("system_mode", ASSyntax.createAtom(model.mode.name.lowercase())))
        if (model.house.isGridConnected) {
            if (!model.house.isBlackout) addPercept("house_grid", gridConnected)
            else addPercept("house_grid", blackoutActive)
        }
        // Sovraccarico Contatore (prelievo superiore a 2.9 kW)
        if (model.currentGridFlow < -2.9 && model.car.isCharging) {
            addPercept("house_grid", Literal.parseLiteral("overload_risk"))
        }

        // Auto
        if (model.car.isPluggedIn) {
            addPercept("car", Literal.parseLiteral("car_plugged_in"))
            addPercept("car", ASSyntax.createLiteral("car_soc", ASSyntax.createNumber(model.car.soc.toDouble())))
        } else {
            addPercept("car", Literal.parseLiteral("car_unplugged"))
        }

        // Pannelli
        if (model.mode == SELLING) {
            addPercept("panels", Literal.parseLiteral("pv_flow(full_grid_injection)"))
        } else {
            val netFlow = model.currentPvFlow - model.house.currentConsumptionKw
            if (netFlow < 0) addPercept("panels", Literal.parseLiteral("pv_flow(high_load_all_sources_to_house)"))
            else if (model.battery.soc >= 100) addPercept("panels", Literal.parseLiteral("pv_flow(battery_full_surplus_to_grid)"))
            else addPercept("panels", Literal.parseLiteral("pv_flow(combined_distribution)"))
        }
    }


    private fun startPhysicalWorld() {
        simulationScope.launch {
            while (isActive) {
                model.timeStep++
                val deltaTimeHours = 1.0

                // LA FISICA PROVA A FARE IL SUO CORSO NORMALE
                model.currentPvFlow = model.panels.producePower(model.weather)
                model.house.simulateOccupantBehavior()
                model.house.balanceEvCharging(model.currentPvFlow, model.battery.currentChargeKw, model.car.isPluggedIn)

                // L'AGENTE IMPONE LA SUA VOLONTÀ SULL'AUTO
                when (evChargeOverride) {
                    "PAUSED" -> {
                        model.house.evChargerKw = 0.0
                        model.car.isCharging = false
                    }
                    "THROTTLED" -> {
                        if (model.house.evChargerKw > 1.0) {
                            model.house.evChargerKw = 1.0
                        }
                    }
                }

                // CALCOLO BILANCIO ENERGETICO CON AUTO EVENTUALMENTE FRENATA
                val netFlow = model.currentPvFlow - model.house.currentConsumptionKw

                // DISTRIBUZIONE FLUSSI E VOLONTÀ SULLA BATTERIA
                if (netFlow > 0) { // C'E' SOLE IN ABBONDANZA
                    if (batteryOverride != "STANDBY") {
                        val chargeBefore = model.battery.currentChargeKw
                        model.battery.charge(netFlow)
                        val absorbed = model.battery.currentChargeKw - chargeBefore

                        model.currentBatteryFlow = absorbed
                        model.currentGridFlow = netFlow - absorbed // Il resto si vende alla rete (+)
                    } else {
                        // Se la batteria è bloccata, vendiamo tutto
                        model.currentBatteryFlow = 0.0
                        model.currentGridFlow = netFlow
                    }

                } else { // LA CASA E' IN DEFICIT
                    val needed = abs(netFlow)

                    if (batteryOverride == "FORCE_CHARGE" && model.house.isGridConnected) {
                        // L'AGENTE FORZA LA RICARICA DA RETE
                        val forcedChargeKw = 3.0 // Ricarica rapida a 3kW
                        val chargeBefore = model.battery.currentChargeKw
                        model.battery.charge(forcedChargeKw)
                        val absorbed = model.battery.currentChargeKw - chargeBefore

                        model.currentBatteryFlow = absorbed // Segno + perché si sta caricando!

                        // La rete deve sostenere la casa (needed) + la ricarica (absorbed)
                        val totalFromGrid = needed + absorbed
                        model.currentGridFlow = -totalFromGrid
                        model.house.interactWithGrid(totalFromGrid, deltaTimeHours)

                    } else if (batteryOverride == "NORMAL" && model.house.isGridConnected && !model.house.isBlackout) {
                        // COMPORTAMENTO NORMALE: LA BATTERIA AIUTA LA CASA
                        val chargeBefore = model.battery.currentChargeKw
                        model.battery.discharge(needed)
                        val provided = chargeBefore - model.battery.currentChargeKw

                        model.currentBatteryFlow = -provided // Segno - perché si sta scaricando

                        val stillNeeded = needed - provided
                        model.currentGridFlow = -stillNeeded
                        if (stillNeeded > 0) {
                            model.house.interactWithGrid(stillNeeded, deltaTimeHours)
                        }
                    } else if (batteryOverride == "STANDBY" && model.house.isGridConnected) {
                        // BATTERIA IN STANDBY: PRELEVIAMO TUTTO DALLA RETE
                        model.currentBatteryFlow = 0.0
                        model.currentGridFlow = -needed
                        model.house.interactWithGrid(needed, deltaTimeHours)
                    } else {
                        // BLACKOUT O RETE OFFLINE
                        model.currentBatteryFlow = 0.0
                        model.currentGridFlow = 0.0
                        model.house.checkOverload(model.currentPvFlow, model.battery.currentChargeKw)
                    }
                }

                // Ricarica materiale del veicolo
                if (model.house.evChargerKw > 0 && model.car.isCharging) {
                    model.car.charge(model.house.evChargerKw, deltaTimeHours)
                }

                updateAgentPercepts()
                notifyModelChangedToView()

                delay(5000L.milliseconds)
            }
        }
    }

    private fun notifyModelChangedToView() {
        GuiApp.notifyModelChanged()
    }

    override fun stop() {
        super.stop()
        simulationScope.cancel()
        logger.info("Environment stopped.")
    }
}