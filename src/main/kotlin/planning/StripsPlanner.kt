package planning

import java.util.LinkedList
import java.util.Queue

class StripsPlanner {

    fun plan(problem: StripsProblem): List<StripsAction>? {
        val queue: Queue<Pair<Set<Proposition>, List<StripsAction>>> = LinkedList()
        val visited = mutableSetOf<Set<Proposition>>()

        queue.add(problem.initialState to emptyList())
        visited.add(problem.initialState)

        while (queue.isNotEmpty()) {
            val (currentState, currentPlan) = queue.poll()

            // Controllo del Goal
            if (currentState.containsAll(problem.goalState)) {
                return currentPlan
            }

            // Esplorazione del dominio
            for (action in problem.domain) {
                if (currentState.containsAll(action.preconditions)) {
                    val nextState = currentState.toMutableSet()
                    nextState.removeAll(action.deleteEffects)
                    nextState.addAll(action.addEffects)

                    if (!visited.contains(nextState)) {
                        visited.add(nextState)
                        queue.add(nextState to (currentPlan + listOf(action)))
                    }
                }
            }
        }
        return null
    }
}