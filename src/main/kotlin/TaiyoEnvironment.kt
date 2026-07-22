import interfaces.Taiyo
import jason.asSyntax.ASSyntax
import jason.asSyntax.Literal
import jason.asSyntax.Structure
import jason.environment.Environment
import kotlinx.coroutines.*
import view.GuiApp
import java.util.logging.Logger
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import model.Mode.*

class TaiyoEnvironment : Environment() {

    companion object {
        val gridConnected: Literal = Literal.parseLiteral("grid_connected")
        val blackoutActive: Literal = Literal.parseLiteral("blackout_active")
    }

    private lateinit var logger: Logger
    private lateinit var model: Taiyo
    private lateinit var simulationScope: CoroutineScope

    override fun init(args: Array<String>?) {
        super.init(args)
        this.logger = Logger.getLogger("TaiyoEnvironment")
        this.logger.info("Initializing TAIYO-MAS Environment...")

        this.model = GuiApp.sharedModel
        this.simulationScope = CoroutineScope(Dispatchers.Default + Job())

        startPhysicalWorld()
    }

    override fun executeAction(agName: String, action: Structure): Boolean {
        var result: Boolean

        when (action.functor) {
            // AZIONI HOUSE GRID
            "disconnect_loads" -> result = executeDisconnectLoads(agName)
            "reconnect_loads" -> result = executeReconnectLoads(agName)

            // AZIONI BATTERIA
            "battery_standby" -> result = executeBatteryStandby(agName)
            "battery_resume" -> result = executeBatteryResume(agName)

            // AZIONI PANNELLI
            "panel_standby" -> {
                logger.info("[$agName] executed panel_standby. Inverter is OFF.")
                result = true
            }
            "panel_resume" -> {
                logger.info("[$agName] executed panel_resume. Inverter is ON.")
                result = true
            }
            "set_selling_mode" -> {
                model.mode = SELLING
                logger.info("[$agName] executed set_selling_mode. Mode is SELLING.")
                result = true
            }
            "set_balanced_mode" -> {
                model.mode = BALANCED
                logger.info("[$agName] executed set_balanced_mode. Mode is BALANCED.")
                result = true
            }
            "set_direct_mode" -> {
                model.mode = DIRECT
                logger.info("[$agName] executed set_direct_mode. Mode is DIRECT.")
                result = true
            }

            // AZIONI AUTO
            "car_start_charging" -> {
                model.car.isCharging = true
                logger.info("[$agName] executed car_start_charging.")
                result = true
            }
            "car_stop_charging" -> {
                model.car.isCharging = false
                logger.info("[$agName] executed car_stop_charging.")
                result = true
            }

            else -> {
                System.err.println("Unknown action: $action by agent $agName")
                return false
            }
        }

        if (result) {
            notifyModelChangedToView()
        }
        return result
    }

    private fun executeDisconnectLoads(agentName: String): Boolean {
        return try {
            model.house.disconnectNonEssentialLoads()
            logger.info("[$agentName] executed disconnect_loads. Non-essential loads are OFF.")
            true
        } catch (e: Exception) {
            System.err.println("Error executing disconnect_loads: ${e.message}")
            false
        }
    }

    private fun executeReconnectLoads(agentName: String): Boolean {
        return try {
            model.house.reconnectLoads()
            logger.info("[$agentName] executed reconnect_loads. Loads are ON.")
            true
        } catch (e: Exception) {
            System.err.println("Error executing reconnect_loads: ${e.message}")
            false
        }
    }

    private fun executeBatteryStandby(agentName: String): Boolean {
        return try {
            model.currentBatteryFlow = 0.0
            logger.info("[$agentName] executed battery_standby.")
            true
        } catch (_: Exception) { false }
    }

    private fun executeBatteryResume(agentName: String): Boolean {
        return try {
            logger.info("[$agentName] executed battery_resume. Battery re-engaged.")
            true
        } catch (_: Exception) { false }
    }


    // --- AGGIORNAMENTO PERCEZIONI ---
    private fun updateAgentPercepts() {
        clearPercepts("weather")
        clearPercepts("battery")
        clearPercepts("house_grid")
        clearPercepts("panels")
        clearPercepts("car")

        // 1. Percezioni Meteo
        val wStatus = model.weather.status.name.lowercase()
        addPercept("weather", ASSyntax.createLiteral("weather_status", ASSyntax.createAtom(wStatus)))

        // 2. Percezioni Batteria
        addPercept("battery", ASSyntax.createLiteral("battery_soc", ASSyntax.createNumber(model.battery.soc.toDouble())))
        val flowDirection = when {
            model.currentBatteryFlow > 0.1 -> "charging"
            model.currentBatteryFlow < -0.1 -> "discharging"
            else -> "idle"
        }
        addPercept("battery", ASSyntax.createLiteral("battery_flow", ASSyntax.createAtom(flowDirection)))

        // 3. Percezioni House Grid
        val currentMode = model.mode.name.lowercase()
        addPercept("house_grid", ASSyntax.createLiteral("system_mode", ASSyntax.createAtom(currentMode)))

        if (model.house.isGridConnected) {
            if (!model.house.isBlackout) {
                addPercept("house_grid", gridConnected)
            } else {
                addPercept("house_grid", blackoutActive)
            }
        }

        // 4. Percezioni ricarica auto
        if (model.car.isPluggedIn) {
            addPercept("car", Literal.parseLiteral("car_plugged_in"))
            addPercept("car", ASSyntax.createLiteral("car_soc", ASSyntax.createNumber(model.car.soc.toDouble())))
        } else {
            addPercept("car", Literal.parseLiteral("car_unplugged"))
        }

        // 5. Percezioni modalità pannelli
        if (model.mode == SELLING) {
            addPercept("panels", Literal.parseLiteral("pv_flow(full_grid_injection)"))
        } else {
            val netFlow = model.currentPvFlow - model.house.currentConsumptionKw
            if (netFlow < 0) {
                addPercept("panels", Literal.parseLiteral("pv_flow(high_load_all_sources_to_house)"))
            } else if (model.battery.soc >= 100) {
                addPercept("panels", Literal.parseLiteral("pv_flow(battery_full_surplus_to_grid)"))
            } else {
                addPercept("panels", Literal.parseLiteral("pv_flow(combined_distribution)"))
            }
        }
    }


    private fun startPhysicalWorld() {
        simulationScope.launch {
            while (isActive) {
                model.timeStep++
                val deltaTimeHours = 1.0

                //model.weather.updateRandomly()
                model.currentPvFlow = model.panels.producePower(model.weather)

                model.house.simulateOccupantBehavior()
                model.house.balanceEvCharging(model.currentPvFlow, model.battery.currentChargeKw, model.car.isPluggedIn)

                val netFlow = model.currentPvFlow - model.house.currentConsumptionKw

                if (netFlow > 0) {
                    val chargeBefore = model.battery.currentChargeKw
                    model.battery.charge(netFlow)
                    val absorbed = model.battery.currentChargeKw - chargeBefore
                    model.currentBatteryFlow = absorbed

                    val leftover = netFlow - absorbed
                    if (leftover > 0) {
                        model.currentGridFlow = leftover
                        model.house.interactWithGrid(leftover, deltaTimeHours)
                    } else {
                        model.currentGridFlow = 0.0
                    }
                } else if (netFlow < 0) {
                    if (model.house.isGridConnected && !model.house.isBlackout) {
                        val needed = abs(netFlow)
                        val chargeBefore = model.battery.currentChargeKw
                        model.battery.discharge(needed)
                        val providedByBattery = chargeBefore - model.battery.currentChargeKw

                        model.currentBatteryFlow = -providedByBattery
                        val stillNeeded = needed - providedByBattery

                        if (stillNeeded > 0) {
                            model.currentGridFlow = -stillNeeded
                            model.house.interactWithGrid(-stillNeeded, deltaTimeHours)
                        } else {
                            model.currentGridFlow = 0.0
                        }
                    } else {
                        model.house.checkOverload(model.currentPvFlow, model.battery.currentChargeKw)
                    }
                } else {
                    model.currentBatteryFlow = 0.0
                    model.currentGridFlow = 0.0
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