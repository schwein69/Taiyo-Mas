package view

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class GuiApp : Application() {

    // Istanziamo la nostra View personalizzata
    private val dashboard = DashboardView()

    // Scope per il background (il finto motore)
    private val engineScope = CoroutineScope(Dispatchers.Default + Job())
    private var timeStep = 0

    override fun start(primaryStage: Stage) {
        primaryStage.title = "TAIYO-MAS"
        primaryStage.scene = Scene(dashboard, 950.0, 700.0)
        primaryStage.show()

        // Avviamo il motore fittizio
        startMockEngine()
    }

    private fun startMockEngine() {
        engineScope.launch {
            while (isActive) {
                // 1. Generiamo i dati (Model)
                val pv = Random.nextDouble(0.0, 6.0)
                val load = Random.nextDouble(0.8, 4.0)
                val soc = Random.nextInt(10, 100)

                val netFlow = pv - load
                val gridKw = if (netFlow < 0) -netFlow else 0.0
                val batKw = if (netFlow > 0) netFlow else 0.0

                // 2. Passiamo i dati alla View tramite l'API pubblica che abbiamo creato
                dashboard.updateMetrics(pv, load, batKw, soc, gridKw, timeStep++)

                // 3. Attendiamo il prossimo ciclo
                delay(1000.milliseconds)
            }
        }
    }

    override fun stop() {
        super.stop()
        engineScope.cancel() // Fondamentale per chiudere i thread fantasma
    }
}

// Entry point di avvio
fun main() {
    Application.launch(GuiApp::class.java)
}