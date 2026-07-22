package view

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import interfaces.Taiyo
import model.Mode
import model.WeatherStatus

@Composable
fun PanelsCard(model: Taiyo) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pannelli Solari", fontWeight = FontWeight.Bold)
            Text("Produzione attuale: ${String.format("%.2f", model.currentPvFlow)} kW")
        }
    }
}

@Composable
fun BatteryCard(model: Taiyo) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Accumulo (Batteria)", fontWeight = FontWeight.Bold)
            Text("SoC: ${model.battery.soc}% (${String.format("%.2f", model.battery.currentChargeKw)} kWh)")
            LinearProgressIndicator(
                progress = model.battery.soc / 100f,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Text("Flusso: ${String.format("%.2f", model.currentBatteryFlow)} kW")
        }
    }
}

@Composable
fun CarCard(model: Taiyo) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Auto Elettrica (EV)", fontWeight = FontWeight.Bold)
            Text("stato: ${if (model.car.isPluggedIn) "Collegata" else "In viaggio / Scollegata"}")
            Text("In Ricarica: ${if (model.car.isCharging) "SÌ" else "NO"}")

            Spacer(modifier = Modifier.height(8.dp))
            Text("SoC: ${model.car.soc}%")
            LinearProgressIndicator(
                progress = model.car.soc / 100f,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { model.car.plugIn() }, enabled = !model.car.isPluggedIn) {
                    Text("Collega")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { model.car.unplug() }, enabled = model.car.isPluggedIn) {
                    Text("scollega")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { model.car.drive(4.0) },
                    enabled = !model.car.isPluggedIn,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF9800))
                ) {
                    Text("Simula Viaggio (Consuma)", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HouseGridCard(model: Taiyo) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Casa & Rete", fontWeight = FontWeight.Bold)
            Text("Consumo Casa: ${String.format("%.2f", model.house.currentConsumptionKw)} kW")
            Text("Scambio Rete: ${String.format("%.2f", model.currentGridFlow)} kW (Positivo = Immetto)")

            Spacer(modifier = Modifier.height(8.dp))
            val gridStatus = if (model.house.isBlackout) "BLACKOUT" else "Connessa"
            Text("Stato Rete Esterna: $gridStatus", color = if (model.house.isBlackout) Color.Red else Color.Black)
        }
    }
}

@Composable
fun ControlsCard(model: Taiyo) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Comandi Utente (Simulazione)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            // Pulsante Blackout
            Button(
                onClick = { model.house.isBlackout = !model.house.isBlackout },
                colors = ButtonDefaults.buttonColors(backgroundColor = if (model.house.isBlackout) Color.Green else Color.Red)
            ) {
                Text(if (model.house.isBlackout) "Ripristina Rete (Fine Blackout)" else "Simula BLACKOUT", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Modalità di sistema MAS:", fontWeight = FontWeight.SemiBold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ModeButton("BALANCED", model.mode == Mode.BALANCED) { model.mode = Mode.BALANCED }
                ModeButton("DIRECT", model.mode == Mode.DIRECT) { model.mode = Mode.DIRECT }
                ModeButton("SELLING", model.mode == Mode.SELLING) { model.mode = Mode.SELLING }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Meteo Attuale:", fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WeatherButton("SUNNY", model.weather.status == WeatherStatus.SUNNY) {
                    model.weather.status = WeatherStatus.SUNNY
                }
                WeatherButton("RAINY", model.weather.status == WeatherStatus.RAINY) {
                    model.weather.status = WeatherStatus.RAINY
                }
                WeatherButton("FOGGY", model.weather.status == WeatherStatus.FOGGY) {
                    model.weather.status = WeatherStatus.FOGGY
                }
                WeatherButton("NIGHT", model.weather.status == WeatherStatus.NIGHT) {
                    model.weather.status = WeatherStatus.NIGHT
                }
            }
        }
    }
}

@Composable
fun ModeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) MaterialTheme.colors.primary else Color.LightGray
        )
    ) {
        Text(label, color = if (isSelected) Color.White else Color.Black)
    }
}

@Composable
fun WeatherButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) Color(0xFF3F51B5) else Color.LightGray
        )
    ) {
        Text(label, color = if (isSelected) Color.White else Color.Black)
    }
}