package planning

import jason.asSemantics.DefaultInternalAction
import jason.asSemantics.TransitionSystem
import jason.asSemantics.Unifier
import jason.asSyntax.ASSyntax
import jason.asSyntax.ListTerm
import jason.asSyntax.Term

class CalculatePlan : DefaultInternalAction() {

    override fun execute(ts: TransitionSystem, un: Unifier, args: Array<Term>): Boolean {
        try {
            val modelType = args[0].toString().replace("\"", "")

            val initialList = args[1] as ListTerm
            val initialState = initialList.map { Proposition(it.toString()) }.toSet()

            val goalList = args[2] as ListTerm
            val goalState = goalList.map { Proposition(it.toString()) }.toSet()

            val domainActions = when (modelType) {
                "battery" -> BatteryDomain.actions
                // "emergency" -> EmergencyDomain.actions
                else -> {
                    ts.logger.severe("Dominio sconosciuto: $modelType")
                    return false
                }
            }

            val problem = StripsProblem(initialState, goalState, domainActions)
            val planner = StripsPlanner()
            val plan = planner.plan(problem)

            if (plan != null) {
                ts.logger.info("[$modelType Planner] Trovato: $plan")
                val planTerm = ASSyntax.createList()
                plan.forEach { action -> planTerm.add(ASSyntax.createAtom(action.name)) }
                return un.unifies(planTerm, args[3])
            } else {
                ts.logger.warning("[$modelType Planner] Nessun piano possibile!")
                return false
            }

        } catch (e: Exception) {
            ts.logger.severe("[Planner] Errore: ${e.message}")
            return false
        }
    }
}