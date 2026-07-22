package planning

object BatteryDomain {
    val actions = setOf(
        StripsAction(
            name = "disconnect_loads",
            preconditions = setOf(Proposition("grid_connected")),
            addEffects = setOf(Proposition("loads_disconnected")),
            deleteEffects = emptySet()
        ),
        StripsAction(
            name = "charge_from_grid",
            preconditions = setOf(Proposition("grid_connected"), Proposition("loads_disconnected"), Proposition("battery_critical")),
            addEffects = setOf(Proposition("battery_ok")),
            deleteEffects = setOf(Proposition("battery_critical"))
        )
    )
}