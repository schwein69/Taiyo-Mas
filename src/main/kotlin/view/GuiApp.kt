package view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import interfaces.Taiyo
import model.TaiyoImpl

object GuiApp {
    val sharedModel: Taiyo = TaiyoImpl()

    var triggerUpdate by mutableStateOf(0)
        private set

    fun notifyModelChanged() {
        triggerUpdate++
    }
}

fun main() = application {
    Thread {
        try {
            jason.infra.centralised.RunCentralisedMAS.main(arrayOf("taiyo_simulation.mas2j"))
        } catch (e: Exception) {
            System.err.println("Errore avvio MAS: ${e.message}")
        }
    }.start()

    Window(
        onCloseRequest = ::exitApplication,
        title = "TAIYO-MAS Smart Home Dashboard"
    ) {
        TaiyoDashboard()
    }
}