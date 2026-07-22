package planning

data class StripsProblem(
    val initialState: Set<Proposition>,
    val goalState: Set<Proposition>,
    val domain: Set<StripsAction> // L'insieme delle azioni applicabili per questo specifico problema
)