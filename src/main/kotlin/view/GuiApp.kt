package view

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import interfaces.Taiyo
import model.TaiyoImpl
import kotlin.system.exitProcess

class GuiApp : Application(), TaiyoView {

    private val dashboard = DashboardView()

    companion object {
        var instance: GuiApp? = null
        val sharedModel : Taiyo = TaiyoImpl()
    }

    init {
        instance = this
    }


    override fun getModel(): Taiyo {
        return sharedModel
    }

    override fun notifyModelChanged() {
        val model = getModel()
        dashboard.updateMetrics(
            pv = model.currentPvFlow,                   // Il flusso di energia calcolato
            load = model.house.currentConsumptionKw,    // Il consumo in tempo reale
            batteryKw = model.currentBatteryFlow,       // Quanta energia entra/esce dalla batteria
            batterySoc = model.battery.soc,             // Percentuale (SoC)
            gridKw = model.currentGridFlow,             // Flusso da/verso la rete
            tick = model.timeStep
        )
        dashboard.updateMode(model.mode.name)
    }


    override fun start(primaryStage: Stage) {
        primaryStage.title = "TAIYO-MAS"
        primaryStage.scene = Scene(dashboard, 950.0, 700.0)

        primaryStage.setOnCloseRequest { exitProcess(0) }
        primaryStage.show()
    }
}

fun main() {
    Application.launch(GuiApp::class.java)
}