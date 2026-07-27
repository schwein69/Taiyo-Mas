package planning.domain

import planning.Proposition
import planning.StripsAction

object HouseGridDomain {
    val actions = setOf(

        // BLACKOUT
        // Stacca i carichi della casa
        StripsAction(
            name = "disconnect_loads",
            preconditions = setOf(Proposition("grid_offline"), Proposition("loads_connected")),
            addEffects = setOf(Proposition("loads_disconnected"), Proposition("house_safe")),
            deleteEffects = setOf(Proposition("loads_connected"))
        ),

        // Se l'auto è in carica durante il blackout, la stacca per salvare la batteria
        StripsAction(
            name = "pause_ev_charging",
            preconditions = setOf(Proposition("grid_offline"), Proposition("car_charging")),
            addEffects = setOf(Proposition("car_paused"), Proposition("ev_safe")),
            deleteEffects = setOf(Proposition("car_charging"))
        ),

        //  Se la casa e l'auto sono in sicurezza e la batteria è carica, attiva l'isola
        StripsAction(
            name = "activate_island_mode",
            preconditions = setOf(
                Proposition("grid_offline"),
                Proposition("house_safe"),
                Proposition("ev_safe"),
                Proposition("battery_ok")
            ),
            addEffects = setOf(Proposition("system_safe")),
            deleteEffects = emptySet()
        ),

        // SOVRACCARICO E MALTEMPO

        // Quando il consumo supera la produzione (es. Contatore al limite o brutto tempo), limita l'auto
        StripsAction(
            name = "throttle_ev_charging",
            preconditions = setOf(Proposition("overload_risk"), Proposition("car_charging")),
            addEffects = setOf(Proposition("ev_throttled"), Proposition("load_balanced")),
            deleteEffects = setOf(Proposition("overload_risk"))
        ),

        // NOTTE CON BATTERIA SCARICA

        // Se è notte, la batteria è sotto il 20% e l'auto succhia energia, blocca l'auto
        StripsAction(
            name = "suspend_ev_for_night",
            preconditions = setOf(Proposition("night_time"), Proposition("battery_low"), Proposition("car_charging")),
            addEffects = setOf(Proposition("battery_protected"), Proposition("ev_paused")),
            deleteEffects = setOf(Proposition("car_charging"))
        ),

        // RIPRISTINO

        StripsAction(
            name = "reconnect_loads",
            preconditions = setOf(Proposition("grid_connected"), Proposition("loads_disconnected")),
            addEffects = setOf(Proposition("loads_connected")),
            deleteEffects = setOf(Proposition("loads_disconnected"))
        ),

        StripsAction(
            name = "resume_ev_charging",
            preconditions = setOf(Proposition("grid_connected"), Proposition("battery_ok")),
            addEffects = setOf(Proposition("car_charging")),
            deleteEffects = setOf(Proposition("car_paused"), Proposition("ev_throttled"))
        )
    )
}