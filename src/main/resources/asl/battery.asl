grid_connected.
battery_state(ok).


+battery_soc(V) : V >= 20
    <-  -+battery_state(ok);
        .print("[BATTERY] SoC attuale: ", V, "% - Livello sicuro.").

+battery_soc(V) : V < 20 & grid_connected
    <-  -+battery_state(critical);
        .print("[BATTERY] ALLARME: SoC critico (", V, "%)! Rete disponibile.");
        .print("[BATTERY] Chiedo al Planner una strategia di ricarica...");
        planning.calculate_plan([battery_critical, grid_connected], [battery_ok], Plan);
        !execute_list(Plan).

+battery_soc(V) : V < 20 & not grid_connected
    <-  -+battery_state(critical);
        .print("[BATTERY] SoC critico (", V, "%), ma c'è BLACKOUT. Resto fermo.").



+battery_flow(charging)    <- .print("[BATTERY] Flusso: IN CARICA.").
+battery_flow(discharging) <- .print("[BATTERY] Flusso: IN SCARICA.").
+battery_flow(idle)        <- .print("[BATTERY] Flusso: FERMO.").




// Se l'agente 'house_grid' ci dice che la rete è caduta
-grid_connected[source(house_grid)]
    <-  .print("[BATTERY] Messaggio da house_grid: BLACKOUT RILEVATO!");
        .print("[BATTERY] Entro in modalità STANDBY di sicurezza.");
        // Dico all'Environment di bloccare fisicamente l'erogazione della batteria
        battery_standby.

// Se l'agente 'house_grid' ci dice che la rete è tornata
+grid_connected[source(house_grid)]
    <-  .print("[BATTERY] Messaggio da house_grid: Rete ripristinata.");
        battery_resume;
        // Ordino alla casa di riattaccare i carichi
        .send(house_grid, achieve, reconnect_loads).



+!execute_list([]).
+!execute_list([Action | Tail])
    <- .print("   -> Eseguo azione Planner: ", Action);
       !do_action(Action);
       !execute_list(Tail).


+!do_action(disconnect_loads)
    <- .print("      [Comunicazione] Invio ordine a house_grid: stacca i carichi!");
       .send(house_grid, achieve, disconnect_loads).

+!do_action(charge_from_grid)
    <- .print("      [Comunicazione] Invio ordine a house_grid: preleva energia per me!");
       .send(house_grid, achieve, charge_from_grid).