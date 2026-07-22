package planning

data class StripsAction(
    val name: String,
    val preconditions: Set<Proposition>,
    val addEffects: Set<Proposition>,
    val deleteEffects: Set<Proposition>
) {
    override fun toString(): String = name
}