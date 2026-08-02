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

enum class EvChargeOverride {
    NORMAL,
    THROTTLED,
    PAUSED,
    FORCE_CHARGE
}

enum class BatteryOverride {
    NORMAL,
    STANDBY
}

enum class PanelOverride {
    NORMAL,
    STANDBY
}

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

        // AZIONI PANNELLI / MODALITA
        val actPanelStandby: Literal = Literal.parseLiteral("panel_standby")
        val actPanelResume: Literal = Literal.parseLiteral("panel_resume")
        val actSetSellingMode: Literal = Literal.parseLiteral("set_selling_mode")
        val actSetBalancedMode: Literal = Literal.parseLiteral("set_balanced_mode")
        val actSetDirectMode: Literal = Literal.parseLiteral("set_direct_mode")

        // AZIONI AUTO
        val actPauseEvCharging: Literal = Literal.parseLiteral("pause_ev_charging")
        val actResumeEvCharging: Literal = Literal.parseLiteral("resume_ev_charging")
        val actThrottle_ev_charging: Literal = Literal.parseLiteral("throttle_ev_charging")
    }

    private lateinit var logger: Logger
    private lateinit var model: Taiyo
    private lateinit var simulationScope: CoroutineScope

    private var evChargeOverride: EvChargeOverride = EvChargeOverride.NORMAL
    private var batteryOverride: BatteryOverride = BatteryOverride.NORMAL
    private var panelOverride: PanelOverride = PanelOverride.NORMAL

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
            actDisconnectLoads.functor -> result = executeDisconnectLoads(agName)
            actReconnectLoads.functor -> result = executeReconnectLoads(agName)
            actActivateIslandMode.functor -> {
                model.house.disconnectFromGrid()
                logger.info("[$agName] executed activate_island_mode. Casa isolata e sicura.")
                result = true
            }

            actBatteryStandby.functor -> result = executeBatteryStandby(agName)
            actBatteryResume.functor -> result = executeBatteryResume(agName)

            actPanelStandby.functor -> result = executePanelStandby(agName)
            actPanelResume.functor -> result = executePanelResume(agName)

            actSetSellingMode.functor -> { model.mode = SELLING; result = true }
            actSetBalancedMode.functor -> { model.mode = BALANCED; result = true }
            actSetDirectMode.functor -> { model.mode = DIRECT; result = true }

            actPauseEvCharging.functor -> {
                evChargeOverride = EvChargeOverride.PAUSED
                model.car.isCharging = false
                logger.info("[$agName] executed pause_ev_charging. Ricarica sospesa dal Planner.")
                result = true
            }
            actResumeEvCharging.functor -> {
                evChargeOverride = EvChargeOverride.NORMAL
                model.car.isCharging = true
                logger.info("[$agName] executed resume_ev_charging.")
                result = true
            }
            actThrottle_ev_charging.functor -> {
                evChargeOverride = EvChargeOverride.THROTTLED
                logger.info("[$agName] executed throttle_ev_charging. Ricarica limitata (Antiblackout)")
                result = true
            }
            actForceGridCharging.functor -> {
                evChargeOverride = EvChargeOverride.FORCE_CHARGE
                model.car.isCharging = true
                logger.info("[$agName] executed force_grid_charging. Ricarica AUTO forzata alla massima potenza!")
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

    private fun executeBatteryStandby(agentName: String): Boolean {
        return try {
            batteryOverride = BatteryOverride.STANDBY
            model.currentBatteryFlow = 0.0
            logger.info("[$agentName] executed battery_standby.")
            true
        } catch (_: Exception) { false }
    }

    private fun executeBatteryResume(agentName: String): Boolean {
        return try {
            batteryOverride = BatteryOverride.NORMAL
            logger.info("[$agentName] executed battery_resume. Battery re-engaged.")
            true
        } catch (_: Exception) { false }
    }

    private fun executePanelStandby(agentName: String): Boolean {
        return try {
            panelOverride = PanelOverride.STANDBY
            model.currentPvFlow = 0.0
            logger.info("[$agentName] executed panel_standby. Inverter is OFF.")
            true
        } catch (_: Exception) { false }
    }

    private fun executePanelResume(agentName: String): Boolean {
        return try {
            panelOverride = PanelOverride.NORMAL
            logger.info("[$agentName] executed panel_resume. Inverter is ON.")
            true
        } catch (_: Exception) { false }
    }

    internal fun updateAgentPercepts() {
        clearPercepts("weather")
        clearPercepts("battery")
        clearPercepts("house_grid")
        clearPercepts("panels")
        clearPercepts("car")

        addPercept("weather", ASSyntax.createLiteral("weather_status", ASSyntax.createAtom(model.weather.status.name.lowercase())))

        addPercept("battery", ASSyntax.createLiteral("battery_soc", ASSyntax.createNumber(model.battery.soc.toDouble())))
        val flowDirection = when {
            model.currentBatteryFlow > 0.1 -> "charging"
            model.currentBatteryFlow < -0.1 -> "discharging"
            else -> "idle"
        }
        addPercept("battery", ASSyntax.createLiteral("battery_flow", ASSyntax.createAtom(flowDirection)))

        addPercept("house_grid", ASSyntax.createLiteral("system_mode", ASSyntax.createAtom(model.mode.name.lowercase())))
        if (model.house.isGridConnected) {
            if (!model.house.isBlackout) addPercept("house_grid", gridConnected)
            else addPercept("house_grid", blackoutActive)
        }
        if (model.currentGridFlow < -2.9 && model.car.isCharging) {
            addPercept("house_grid", Literal.parseLiteral("overload_risk"))
        }

        if (model.car.isPluggedIn) {
            addPercept("car", Literal.parseLiteral("car_plugged_in"))
            addPercept("car", ASSyntax.createLiteral("car_soc", ASSyntax.createNumber(model.car.soc.toDouble())))
        } else {
            addPercept("car", Literal.parseLiteral("car_unplugged"))
        }

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

                if (panelOverride == PanelOverride.STANDBY) {
                    model.currentPvFlow = 0.0
                } else {
                    model.currentPvFlow = model.panels.producePower(model.weather)
                }

                model.house.simulateOccupantBehavior()
                model.house.balanceEvCharging(model.currentPvFlow, model.battery.currentChargeKw, model.car.isPluggedIn)

                when (evChargeOverride) {
                    EvChargeOverride.PAUSED -> {
                        model.house.evChargerKw = 0.0
                        model.car.isCharging = false
                    }
                    EvChargeOverride.THROTTLED -> {
                        if (model.house.evChargerKw > 1.0) {
                            model.house.evChargerKw = 1.0
                        }
                    }
                    EvChargeOverride.FORCE_CHARGE -> {
                        model.car.isCharging = true
                    }
                    EvChargeOverride.NORMAL -> {}
                }

                val netFlow = model.currentPvFlow - model.house.currentConsumptionKw

                if (netFlow > 0) {
                    if (batteryOverride != BatteryOverride.STANDBY) {
                        val chargeBefore = model.battery.currentChargeKw
                        model.battery.charge(netFlow)
                        val absorbed = model.battery.currentChargeKw - chargeBefore

                        model.currentBatteryFlow = absorbed
                        model.currentGridFlow = netFlow - absorbed
                    } else {
                        model.currentBatteryFlow = 0.0
                        model.currentGridFlow = netFlow
                    }

                } else {
                    val needed = abs(netFlow)
                    if (batteryOverride == BatteryOverride.NORMAL && model.house.isGridConnected && !model.house.isBlackout) {
                        val chargeBefore = model.battery.currentChargeKw
                        model.battery.discharge(needed)
                        val provided = chargeBefore - model.battery.currentChargeKw

                        model.currentBatteryFlow = -provided

                        val stillNeeded = needed - provided
                        model.currentGridFlow = -stillNeeded
                        if (stillNeeded > 0) {
                            model.house.interactWithGrid(stillNeeded, deltaTimeHours)
                        }
                    } else if (batteryOverride == BatteryOverride.STANDBY && model.house.isGridConnected) {
                        model.currentBatteryFlow = 0.0
                        model.currentGridFlow = -needed
                        model.house.interactWithGrid(needed, deltaTimeHours)
                    } else {
                        model.currentBatteryFlow = 0.0
                        model.currentGridFlow = 0.0
                        model.house.checkOverload(model.currentPvFlow, model.battery.currentChargeKw)
                    }
                }

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