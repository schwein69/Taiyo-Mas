grid_status(connected).
battery_state(ok).

// ==========================================
// GESTIONE STATO DI RICARICA (SoC)
// ==========================================

+battery_soc(V) : V >= 20
    <-  -+battery_state(ok);
        .print("[BATTERY] SoC attuale: ", V, "% - Livello sicuro.").

+battery_soc(V) : V < 20 & grid_status(connected)
    <-  -+battery_state(critical);
        .print("[BATTERY] ALLARME: SoC critico (", V, "%)! Rete disponibile.");
        .print("[BATTERY] Chiedo al Planner una strategia di ricarica...");

        planning.CalculatePlan("battery", [battery_critical, grid_connected], [battery_ok], Plan);
        !execute_list(Plan).

+battery_soc(V) : V < 20 & grid_status(blackout)
    <-  -+battery_state(critical);
        .print("[BATTERY] SoC critico (", V, "%), ma c'è BLACKOUT. Resto fermo.").


// ==========================================
// GESTIONE DEL FLUSSO
// ==========================================

+battery_flow(charging)    <- .print("[BATTERY] Flusso: IN CARICA.").
+battery_flow(discharging) <- .print("[BATTERY] Flusso: IN SCARICA.").
+battery_flow(idle)        <- .print("[BATTERY] Flusso: FERMO.").


// ==========================================
// REAZIONE ALLE INFORMAZIONI DEGLI AGENTI
// ==========================================

// --- Reazione Autonoma alla Rete ---
+grid_status(blackout)[source(A)]
    <-  -+grid_status(blackout);
        .print("[BATTERY] Messaggio da ", A, ": BLACKOUT RILEVATO");
        .print("[BATTERY] Entro in modalità STANDBY.");
        !do_action(battery_standby).

+grid_status(connected)[source(A)]
    <-  -+grid_status(connected);
        .print("[BATTERY] Messaggio da ", A, ": Rete ripristinata.");
        .print("[BATTERY] Ritorno operativa.");
        !do_action(battery_resume).

// --- Reazione Autonoma alle Strategie ---
+operation_mode(direct)[source(house_grid)]
    <-  .print("[BATTERY] House Grid avvisa: modalità DIRECT. Entro in standby.");
        !do_action(battery_standby).

+operation_mode(balanced)[source(house_grid)]
    <-  .print("[BATTERY] House Grid avvisa: modalità BALANCED. Torno operativa.");
        !do_action(battery_resume).

+operation_mode(selling)[source(house_grid)]
    <-  .print("[BATTERY] House Grid avvisa: modalità SELLING. Pronta a scaricare verso la rete.");
        !do_action(battery_selling).


// ==========================================
// MOTORE DI ESECUZIONE DEL PIANO STRIPS
// ==========================================

+!execute_list([]).
+!execute_list([Action | Tail])
    <- .print("   -> Eseguo azione Planner: ", Action);
       !do_action(Action);
       !execute_list(Tail).



// Azioni del Planner (Richiedono aiuto a House Grid)
+!do_action(disconnect_loads)
    <- .print("      [Richiesta] Chiedo a house_grid di staccare i carichi per aiutarmi a ricaricare");
       .send(house_grid, achieve, disconnect_loads).

+!do_action(charge_from_grid)
    <- .print("      [Richiesta] Chiedo a house_grid di prelevare energia dalla rete per me!");
       .send(house_grid, achieve, charge_from_grid).

+!do_action(battery_standby) <- battery_standby.
+!do_action(battery_resume)  <- battery_resume.
+!do_action(battery_selling)  <- battery_selling.