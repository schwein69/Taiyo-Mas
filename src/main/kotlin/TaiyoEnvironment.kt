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

 class TaiyoEnvironment : Environment() {

    companion object {
        val gridConnected: Literal? = Literal.parseLiteral("grid_connected")
        val blackoutActive: Literal? = Literal.parseLiteral("blackout_active")
    }

    private lateinit var logger: Logger
    private lateinit var model: Taiyo
    private lateinit var simulationScope: CoroutineScope

    @Override
    override fun init(args: Array<String>?) {
        super.init(args)
        this.logger = Logger.getLogger("TaiyoEnvironment")
        this.logger.info("Initializing TAIYO-MAS Environment...")

        this.model = GuiApp.sharedModel

        this.simulationScope = CoroutineScope(Dispatchers.Default + Job())

        startPhysicalWorld()
    }

    @Override
    override fun executeAction(agName: String, action: Structure): Boolean {
        var result: Boolean

        when (action.functor) {
            "disconnect_loads" -> {
                result = executeDisconnectLoads(agName)
            }
            "reconnect_loads" -> {
                result = executeReconnectLoads(agName)
            }
            "battery_standby" -> {
                result = executeBatteryStandby(agName)
            }
            "battery_resume" -> {
                result = executeBatteryResume(agName)
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
        try {
            model.house.disconnectNonEssentialLoads()
            logger.info("$agentName executed disconnect_loads. Non-essential loads are OFF.")
            return true
        } catch (e: Exception) {
            System.err.println("Error executing disconnect_loads: ${e.message}")
            return false
        }
    }

    private fun executeReconnectLoads(agentName: String): Boolean {
        try {
            model.house.reconnectLoads()
            logger.info("$agentName executed reconnect_loads. Loads are ON.")
            return true
        } catch (e: Exception) {
            System.err.println("Error executing reconnect_loads: ${e.message}")
            return false
        }
    }

    private fun executeBatteryStandby(agentName: String): Boolean {
        try {
            model.currentBatteryFlow = 0.0
            logger.info("$agentName executed battery_standby.")
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun executeBatteryResume(agentName: String): Boolean {
        try {
            logger.info("$agentName executed battery_resume. Battery re-engaged.")
            return true
        } catch (_: Exception) {
            return false
        }
    }


    private fun updateAgentPercepts() {
        // Clear old state
        clearPercepts("weather")
        clearPercepts("battery")
        clearPercepts("house_grid")

        // 1. Weather Percepts
        val wStatus = model.weather.status.name.lowercase()
        addPercept("weather", ASSyntax.createLiteral("weather_status", ASSyntax.createAtom(wStatus)))

        // 2. Battery Percepts
        addPercept("battery", ASSyntax.createLiteral("battery_soc", ASSyntax.createNumber(model.battery.soc.toDouble())))

        val flowDirection = when {
            model.currentBatteryFlow > 0.1 -> "charging"
            model.currentBatteryFlow < -0.1 -> "discharging"
            else -> "idle"
        }
        addPercept("battery", ASSyntax.createLiteral("battery_flow", ASSyntax.createAtom(flowDirection)))

        // 3. Grid / Blackout Percepts (Shared between battery and house)
        if (model.house.isGridConnected) {
            addPercept("battery", gridConnected)
            addPercept("house_grid", gridConnected)
        }
        if (model.house.isBlackout) {
            addPercept("house_grid", blackoutActive)
        }
    }


    private fun startPhysicalWorld() {
        simulationScope.launch {
            while (isActive) {
                model.timeStep++
                val deltaTimeHours = 1.0

                model.weather.updateRandomly()
                model.currentPvFlow = model.panels.producePower(model.weather)

                model.house.simulateOccupantBehavior()
                model.house.balanceEvCharging(model.currentPvFlow, model.battery.currentChargeKw, model.car.isPluggedIn)

                val netFlow = model.currentPvFlow - model.house.currentConsumptionKw

                if (netFlow > 0) {
                    val absorbed = model.battery.charge(netFlow)
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
                        val providedByBattery = abs(model.battery.discharge(netFlow))
                        model.currentBatteryFlow = -providedByBattery
                        val stillNeeded = abs(netFlow) - providedByBattery
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

                if (model.house.evChargerKw > 0) {
                    model.car.charge(model.house.evChargerKw, deltaTimeHours)
                }

                updateAgentPercepts()
                notifyModelChangedToView()

                delay(3000.milliseconds)
            }
        }
    }

    private fun notifyModelChangedToView() {
        GuiApp.instance?.notifyModelChanged()
    }

    @Override
    override fun stop() {
        super.stop()
        simulationScope.cancel()
        logger.info("Environment stopped.")
    }
}