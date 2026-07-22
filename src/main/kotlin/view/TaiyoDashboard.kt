package view

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TaiyoDashboard() {
    // La lettura di triggerUpdate rende l'intera funzione reattiva ai cambiamenti!
    val updateCount = GuiApp.triggerUpdate
    val model = GuiApp.sharedModel

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {

                Text("Sistema TAIYO (Tick: ${model.timeStep})", style = MaterialTheme.typography.h4)
                Text("Meteo attuale: ${model.weather.status.name}", style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.weight(1f)) {
                    // Colonna SX: Fonti ed Erogatori
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PanelsCard(model)
                        Spacer(modifier = Modifier.height(8.dp))
                        BatteryCard(model)
                        Spacer(modifier = Modifier.height(8.dp))
                        CarCard(model)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Colonna DX: Casa e Controlli
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        HouseGridCard(model)
                        Spacer(modifier = Modifier.height(8.dp))
                        ControlsCard(model)
                    }
                }
            }
        }
    }
}