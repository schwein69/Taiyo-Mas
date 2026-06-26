package view

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.*

class DashboardView : BorderPane() {

    private val pvLabel = Label("0.00 kW")
    private val batteryLabel = Label("0.00 kW (0%)")
    private val loadLabel = Label("0.00 kW")
    private val gridLabel = Label("0.00 kW")
    private val modeLabel = Label("Strategia: BALANCED")

    private val pvSeries = XYChart.Series<Number, Number>().apply { name = "Produzione Solare (kW)" }

    init {
        style = "-fx-background-color: #0b0f19;"
        padding = Insets(25.0)

        top = buildHeader()
        center = buildCenterArea()
        bottom = buildControlBar()
    }

    private fun buildHeader(): VBox {
        return VBox(5.0).apply {
            children.addAll(
                Label("TAIYO-MAS Dashboard").apply { style = "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #ffffff;" },
                modeLabel.apply { style = "-fx-font-size: 14px; -fx-text-fill: #10b981; -fx-font-weight: bold;" }
            )
        }
    }

    private fun buildCenterArea(): VBox {
        val grid = GridPane().apply {
            hgap = 20.0; vgap = 20.0; padding = Insets(20.0, 0.0, 20.0, 0.0)
        }

        grid.add(createCard("FOTOVOLTAICO", pvLabel, "#fbbf24"), 0, 0)
        grid.add(createCard("BATTERIA", batteryLabel, "#10b981"), 1, 0)
        grid.add(createCard("CARICO", loadLabel, "#ef4444"), 0, 1)
        grid.add(createCard("RETE", gridLabel, "#3b82f6"), 1, 1)

        val xAxis = NumberAxis().apply { label = "Time-steps" }
        val yAxis = NumberAxis().apply { label = "Potenza (kW)" }
        val chart = LineChart(xAxis, yAxis).apply {
            data.add(pvSeries)
            style = "-fx-background-color: transparent;"
            lookup(".chart-plot-background").style = "-fx-background-color: #111827;"
        }

        return VBox(15.0).apply { children.addAll(grid, chart) }
    }

    private fun buildControlBar(): HBox {
        return HBox(15.0).apply {
            alignment = Pos.CENTER; padding = Insets(15.0, 0.0, 0.0, 0.0)
            children.addAll(
                Button("Balanced").apply { setOnAction { updateMode("BALANCED") } },
                Button("Direct").apply { setOnAction { updateMode("DIRECT") } },
                Button("Selling").apply { setOnAction { updateMode("SELLING") } }
            )
        }
    }

    private fun createCard(title: String, valueLabel: Label, colorHex: String): VBox {
        return VBox(8.0).apply {
            padding = Insets(20.0)
            style = "-fx-background-color: #1f2937; -fx-background-radius: 10px; -fx-border-color: $colorHex; -fx-border-radius: 10px; -fx-border-width: 1.5px;"
            prefWidth = 240.0
            children.addAll(
                Label(title).apply { style = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #9ca3af;" },
                valueLabel.apply { style = "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f3f4f6;" }
            )
        }
    }

    fun updateMetrics(pv: Double, load: Double, batteryKw: Double, batterySoc: Int, gridKw: Double, tick: Int) {
        Platform.runLater {
            pvLabel.text = String.format("%.2f kW", pv)
            loadLabel.text = String.format("%.2f kW", load)
            batteryLabel.text = String.format("%.2f kW (%d%%)", batteryKw, batterySoc)
            gridLabel.text = String.format("%.2f kW", gridKw)

            pvSeries.data.add(XYChart.Data(tick, pv))
            if (pvSeries.data.size > 20) pvSeries.data.removeAt(0)
        }
    }

    fun updateMode(mode: String) {
        Platform.runLater { modeLabel.text = "Strategia: $mode" }
    }
}